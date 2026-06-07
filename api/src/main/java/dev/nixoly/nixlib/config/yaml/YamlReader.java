package dev.nixoly.nixlib.config.yaml;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlReader {

    private YamlReader() {}

    private static Yaml strictYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(64);
        options.setCodePointLimit(Integer.MAX_VALUE);
        options.setAllowDuplicateKeys(false);
        return new Yaml(new SafeConstructor(options));
    }

    private static Yaml lenientYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(64);
        options.setCodePointLimit(Integer.MAX_VALUE);
        options.setAllowDuplicateKeys(true);
        return new Yaml(new MergingSafeConstructor(options));
    }

    public static Map<String, Object> read(String yamlString) {
        if (yamlString == null || yamlString.isBlank()) {
            return new LinkedHashMap<>();
        }
        return read(new StringReader(yamlString));
    }

    public static Map<String, Object> read(Reader reader) {
        return readWith(reader, strictYaml());
    }

    public static Map<String, Object> readMergingDuplicates(String yamlString) {
        if (yamlString == null || yamlString.isBlank()) {
            return new LinkedHashMap<>();
        }
        return readMergingDuplicates(new StringReader(yamlString));
    }

    public static Map<String, Object> readMergingDuplicates(Reader reader) {
        return readWith(reader, lenientYaml());
    }

    private static Map<String, Object> readWith(Reader reader, Yaml yaml) {
        Object loaded = yaml.load(reader);
        if (loaded == null) {
            return new LinkedHashMap<>();
        }
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalStateException("root yaml node is not a mapping");
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            out.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return out;
    }

    private static final class MergingSafeConstructor extends SafeConstructor {

        MergingSafeConstructor(LoaderOptions options) {
            super(options);
        }

        @Override
        protected void flattenMapping(MappingNode node) {
            super.flattenMapping(node);
        }

        @Override
        protected void constructMapping2ndStep(MappingNode node, Map<Object, Object> mapping) {
            for (NodeTuple tuple : node.getValue()) {
                Node keyNode = tuple.getKeyNode();
                Node valueNode = tuple.getValueNode();
                Object key = constructObject(keyNode);
                Object value = constructObject(valueNode);
                if (mapping.containsKey(key)) {
                    Object existing = mapping.get(key);
                    mapping.put(key, mergeValues(existing, value));
                } else {
                    mapping.put(key, value);
                }
            }
        }

        @Override
        protected Map<Object, Object> constructMapping(MappingNode node) {
            Map<Object, Object> mapping = createDefaultMap(node.getValue().size());
            constructMapping2ndStep(node, mapping);
            return mapping;
        }

        @SuppressWarnings("unchecked")
        private static Object mergeValues(Object existing, Object incoming) {
            List<Object> merged = new ArrayList<>();
            if (existing instanceof List<?> list) {
                merged.addAll((List<Object>) list);
            } else {
                merged.add(existing);
            }
            if (incoming instanceof List<?> list) {
                merged.addAll((List<Object>) list);
            } else {
                merged.add(incoming);
            }
            return merged;
        }
    }
}
