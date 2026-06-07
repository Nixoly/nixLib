package dev.nixoly.nixlib.config;

import dev.nixoly.nixlib.config.yaml.YamlReader;
import dev.nixoly.nixlib.config.yaml.YamlWriter;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class YamlWriterTest {

    @Test
    void roundTripsScalarMap() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("name", "nixLib");
        data.put("count", 42);
        data.put("enabled", true);
        data.put("ratio", 0.75);

        String yaml = YamlWriter.write(data, Map.of());
        Map<String, Object> parsed = YamlReader.read(yaml);

        assertThat(parsed.get("name")).isEqualTo("nixLib");
        assertThat(((Number) parsed.get("count")).intValue()).isEqualTo(42);
        assertThat(parsed.get("enabled")).isEqualTo(true);
        assertThat(((Number) parsed.get("ratio")).doubleValue()).isEqualTo(0.75);
    }

    @Test
    void writesNestedMaps() {
        LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
        inner.put("deep", "value");
        LinkedHashMap<String, Object> outer = new LinkedHashMap<>();
        outer.put("section", inner);

        String yaml = YamlWriter.write(outer, Map.of());

        assertThat(yaml).contains("section:");
        assertThat(yaml).contains("  deep: value");
    }

    @Test
    void writesListsWithDashStyle() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("items", List.of("a", "b", "c"));

        String yaml = YamlWriter.write(data, Map.of());
        assertThat(yaml).contains("items:");
        assertThat(yaml).contains("- a");
        assertThat(yaml).contains("- b");
        assertThat(yaml).contains("- c");
    }

    @Test
    void quotesAmbiguousStrings() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("a", "true");
        data.put("b", "123");
        data.put("c", "yes");
        data.put("d", "plain text");

        String yaml = YamlWriter.write(data, Map.of());
        assertThat(yaml).contains("a: \"true\"");
        assertThat(yaml).contains("b: \"123\"");
        assertThat(yaml).contains("c: \"yes\"");
        assertThat(yaml).contains("d: plain text");
    }

    @Test
    void writesCommentsBeforeKeys() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("name", "test");

        String yaml = YamlWriter.write(data, Map.of("name", List.of("This is the server name", "Second comment line")));

        assertThat(yaml).contains("# This is the server name\n");
        assertThat(yaml).contains("# Second comment line\n");
        assertThat(yaml.indexOf("# This")).isLessThan(yaml.indexOf("name:"));
    }
}
