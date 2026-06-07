package dev.nixoly.nixlib.config;

import dev.nixoly.nixlib.config.annotations.ConfigVersion;
import dev.nixoly.nixlib.config.migration.MigrationRegistry;
import dev.nixoly.nixlib.config.validation.ValidationException;
import dev.nixoly.nixlib.config.validation.Validators;
import dev.nixoly.nixlib.config.yaml.Nodes;
import dev.nixoly.nixlib.config.yaml.YamlReader;
import dev.nixoly.nixlib.config.yaml.YamlWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Config {

    private final List<ConfigField> fields;
    private final int declaredVersion;
    private final String versionKey;
    private final MigrationRegistry migrations = new MigrationRegistry();

    private Path file;
    private final List<String> warnings = new ArrayList<>();

    protected Config() {
        this.fields = ConfigField.scan(getClass());
        ConfigVersion ver = getClass().getAnnotation(ConfigVersion.class);
        this.declaredVersion = ver == null ? 1 : ver.value();
        this.versionKey = ver == null ? "config-version" : ver.key();
        registerMigrations(migrations);
    }

    protected void registerMigrations(MigrationRegistry registry) {}

    public final int version() {
        return declaredVersion;
    }

    public final List<String> warnings() {
        return List.copyOf(warnings);
    }

    public final void load(Path file) {
        this.file = file;
        try {
            if (!Files.exists(file)) {
                if (file.getParent() != null) Files.createDirectories(file.getParent());
                save();
                warnings.clear();
                onLoad();
                return;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            loadFromString(text);
            save();
        } catch (IOException e) {
            throw new ConfigException("failed to load " + file, e);
        }
    }

    public final void loadFromString(String text) {
        Map<String, Object> data = YamlReader.read(new StringReader(text));
        applyData(data);
    }

    public final void loadFromReader(Reader reader) {
        applyData(YamlReader.read(reader));
    }

    private void applyData(Map<String, Object> data) {
        warnings.clear();
        int fileVersion = readVersion(data);
        if (fileVersion < declaredVersion) {
            migrations.apply(fileVersion, declaredVersion, data);
            data.put(versionKey, declaredVersion);
        } else if (fileVersion > declaredVersion) {
            warnings.add("config file is newer (v" + fileVersion + ") than expected (v" + declaredVersion + "); reading anyway");
        }

        for (ConfigField field : fields) {
            Object raw = Nodes.get(data, field.path);
            if (raw == null) {
                warnings.add("missing key: " + field.path + " (using default)");
                continue;
            }
            applyField(field, raw);
        }
        onLoad();
    }

    private void applyField(ConfigField field, Object raw) {
        Class<?> type = field.field.getType();
        var coerced = TypeCoercion.coerce(raw, type);
        if (coerced.isEmpty()) {
            warnings.add("type mismatch at " + field.path + ": expected " + type.getSimpleName()
                    + " but got " + raw.getClass().getSimpleName() + " (using default)");
            return;
        }
        Object value = coerced.get();
        try {
            Validators.validate(field.field, value);
        } catch (ValidationException ex) {
            warnings.add("validation failed at " + field.path + ": " + ex.getMessage() + " (using default)");
            return;
        }
        field.write(this, value);
    }

    private int readVersion(Map<String, Object> data) {
        Object v = data.get(versionKey);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return declaredVersion;
    }

    public final void save() {
        if (file == null) throw new ConfigException("no file bound; call load(path) first");
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.writeString(file, dump(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ConfigException("failed to save " + file, e);
        }
    }

    public final String dump() {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        LinkedHashMap<String, List<String>> comments = new LinkedHashMap<>();
        root.put(versionKey, declaredVersion);
        comments.put(versionKey, List.of("Schema version for this file. Do not edit by hand."));
        for (ConfigField field : fields) {
            Nodes.set(root, field.path, field.read(this));
            if (!field.comments.isEmpty()) {
                comments.put(field.path, field.comments);
            }
        }
        return YamlWriter.write(root, comments);
    }

    public final void reload() {
        if (file == null) throw new ConfigException("no file bound; call load(path) first");
        load(file);
    }

    protected void onLoad() {}
}
