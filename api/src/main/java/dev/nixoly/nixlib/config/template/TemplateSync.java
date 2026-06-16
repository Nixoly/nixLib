package dev.nixoly.nixlib.config.template;

import dev.nixoly.nixlib.config.ConfigException;
import dev.nixoly.nixlib.config.yaml.YamlReader;
import dev.nixoly.nixlib.config.yaml.YamlWriter;
import dev.nixoly.nixlib.version.SemanticVersion;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TemplateSync {

    private TemplateSync() {}

    public static SyncResult synchronize(String templateYaml, String userYaml, SyncOptions options) {
        SyncOptions opts = options == null ? SyncOptions.create() : options;
        if (templateYaml == null) {
            throw new ConfigException("template yaml is null");
        }

        boolean mergeDuplicates = opts.shouldMergeDuplicateKeys();
        Map<String, Object> template = mergeDuplicates
                ? YamlReader.readMergingDuplicates(templateYaml)
                : YamlReader.read(templateYaml);
        Map<String, Object> user = (userYaml == null || userYaml.isBlank())
                ? new LinkedHashMap<>()
                : (mergeDuplicates ? YamlReader.readMergingDuplicates(userYaml) : YamlReader.read(userYaml));

        SemanticVersion templateVersion = readVersion(template, opts.versionKey());
        SemanticVersion userVersion = readVersion(user, opts.versionKey());

        Map<String, Object> merged = mergeMap(template, user, "", opts);
        merged.put(opts.versionKey(), templateVersion.toString());
        if (opts.shouldVersionKeyLast()) {
            merged = withVersionKeyLast(merged, opts.versionKey());
        }

        Map<String, List<String>> comments = CommentScanner.extract(templateYaml);
        String mergedYaml = YamlWriter.write(merged, comments);

        boolean changed = userYaml == null || !mergedYaml.equals(userYaml);
        return new SyncResult(mergedYaml, merged, changed, templateVersion, userVersion);
    }

    public static SyncResult synchronizeFile(InputStream templateStream, Path userFile, SyncOptions options) {
        if (templateStream == null) {
            throw new ConfigException("template stream is null");
        }
        String templateText = readAll(templateStream);
        String userText = null;
        try {
            if (userFile != null && Files.exists(userFile)) {
                userText = Files.readString(userFile, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new ConfigException("failed to read user file " + userFile, e);
        }

        SyncResult result = synchronize(templateText, userText, options);

        if (userFile != null && (userText == null || result.changed())) {
            try {
                if (userFile.getParent() != null) {
                    Files.createDirectories(userFile.getParent());
                }
                Files.writeString(userFile, result.mergedYaml(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new ConfigException("failed to write merged config to " + userFile, e);
            }
        }
        return result;
    }

    private static String readAll(InputStream in) {
        try (Reader reader = new java.io.InputStreamReader(in, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new ConfigException("failed to read template stream", e);
        }
    }

    private static Map<String, Object> withVersionKeyLast(Map<String, Object> merged, String versionKey) {
        if (!merged.containsKey(versionKey)) {
            return merged;
        }
        Object version = merged.remove(versionKey);
        LinkedHashMap<String, Object> ordered = new LinkedHashMap<>(merged.size() + 1);
        ordered.putAll(merged);
        ordered.put(versionKey, version);
        return ordered;
    }

    private static SemanticVersion readVersion(Map<String, Object> data, String key) {
        Object raw = data.get(key);
        if (raw == null) return SemanticVersion.ZERO;
        return SemanticVersion.parseOr(String.valueOf(raw), SemanticVersion.ZERO);
    }

    private static Map<String, Object> mergeMap(Map<String, Object> template, Map<String, Object> user,
                                                String prefix, SyncOptions opts) {
        boolean freeform = opts.isFreeform(prefix);

        if (freeform) {
            LinkedHashMap<String, Object> kept = new LinkedHashMap<>(user.size());
            for (Map.Entry<String, Object> entry : user.entrySet()) {
                String key = entry.getKey();
                if (prefix.isEmpty() && key.equals(opts.versionKey())) {
                    continue;
                }
                String path = prefix.isEmpty() ? key : prefix + "." + key;
                Object userValue = entry.getValue();
                Object templateValue = template.get(key);
                if (templateValue instanceof Map<?, ?> templateMap && userValue instanceof Map<?, ?> userMap) {
                    kept.put(key, mergeMap(asStringMap(templateMap), asStringMap(userMap), path, opts));
                } else {
                    kept.put(key, userValue);
                }
            }
            return kept;
        }

        LinkedHashMap<String, Object> out = new LinkedHashMap<>(template.size());

        for (Map.Entry<String, Object> entry : template.entrySet()) {
            String key = entry.getKey();
            if (prefix.isEmpty() && key.equals(opts.versionKey())) {
                continue;
            }
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            Object templateValue = entry.getValue();

            if (!user.containsKey(key)) {
                if (freeform) {
                    continue;
                }
                out.put(key, deepCopy(templateValue));
                continue;
            }

            Object userValue = user.get(key);

            if (templateValue instanceof Map<?, ?> templateMap) {
                if (userValue instanceof Map<?, ?> userMap) {
                    out.put(key, mergeMap(asStringMap(templateMap), asStringMap(userMap), path, opts));
                } else if (freeform) {
                    out.put(key, userValue);
                } else {
                    opts.warn("type mismatch at " + path + ": expected map, using template default");
                    out.put(key, deepCopy(templateValue));
                }
                continue;
            }

            if (templateValue instanceof Collection<?> templateList) {
                if (userValue == null) {
                    out.put(key, freeform ? null : deepCopyList(templateList));
                } else if (userValue instanceof Collection<?> userList) {
                    out.put(key, new ArrayList<>(userList));
                } else if (freeform) {
                    out.put(key, userValue);
                } else {
                    opts.warn("type mismatch at " + path + ": expected list, using template default");
                    out.put(key, deepCopyList(templateList));
                }
                continue;
            }

            if (userValue instanceof Map<?, ?> || userValue instanceof Collection<?>) {
                if (freeform) {
                    out.put(key, userValue);
                } else {
                    opts.warn("type mismatch at " + path + ": expected scalar, using template default");
                    out.put(key, deepCopy(templateValue));
                }
                continue;
            }

            out.put(key, userValue);
        }

        boolean keepUnknown = !opts.shouldDropUnknown() || freeform
                || (opts.freeformRoot() && prefix.isEmpty());
        if (keepUnknown) {
            for (Map.Entry<String, Object> entry : user.entrySet()) {
                String userKey = entry.getKey();
                if (userKey.equals(opts.versionKey())) {
                    continue;
                }
                if (!template.containsKey(userKey)) {
                    out.put(userKey, entry.getValue());
                }
            }
        } else {
            for (String userKey : user.keySet()) {
                if (userKey.equals(opts.versionKey()) || template.containsKey(userKey)) {
                    continue;
                }
                String path = prefix.isEmpty() ? userKey : prefix + "." + userKey;
                opts.warn("dropping unknown key: " + path);
            }
        }

        return out;
    }

    @SuppressWarnings("unchecked")
    private static Object deepCopy(Object value) {
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> copy = new LinkedHashMap<>(map.size());
            for (Map.Entry<?, ?> e : map.entrySet()) {
                copy.put(String.valueOf(e.getKey()), deepCopy(e.getValue()));
            }
            return copy;
        }
        if (value instanceof Collection<?> col) {
            return deepCopyList(col);
        }
        return value;
    }

    private static List<Object> deepCopyList(Collection<?> col) {
        ArrayList<Object> copy = new ArrayList<>(col.size());
        for (Object item : col) {
            copy.add(deepCopy(item));
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asStringMap(Map<?, ?> map) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(map.size());
        for (Map.Entry<?, ?> e : map.entrySet()) {
            out.put(String.valueOf(e.getKey()), e.getValue());
        }
        return out;
    }
}
