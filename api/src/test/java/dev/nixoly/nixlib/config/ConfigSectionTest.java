package dev.nixoly.nixlib.config;

import dev.nixoly.nixlib.config.annotations.ConfigVersion;
import dev.nixoly.nixlib.config.annotations.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigSectionTest {

    @ConfigVersion(1)
    static final class Deep extends Config {
        @Path("a.b.c.d.value") public String deep = "fallback";
        @Path("a.b.sibling") public int sibling = 7;
        @Path("a.list") public List<String> list = List.of("x", "y");
    }

    @Test
    void nestedPathsRoundTrip(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("d.yml");
        Files.writeString(file,
                "config-version: 1\n" +
                "a:\n" +
                "  b:\n" +
                "    c:\n" +
                "      d:\n" +
                "        value: leaf\n" +
                "    sibling: 42\n" +
                "  list:\n" +
                "    - one\n" +
                "    - two\n");

        Deep d = new Deep();
        d.load(file);

        assertThat(d.deep).isEqualTo("leaf");
        assertThat(d.sibling).isEqualTo(42);
        assertThat(d.list).containsExactly("one", "two");
    }

    @Test
    void writerProducesIndentedNesting(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("d.yml");
        Deep d = new Deep();
        d.load(file);

        String body = Files.readString(file);
        assertThat(body).contains("a:\n");
        assertThat(body).contains("  b:\n");
        assertThat(body).contains("    c:\n");
        assertThat(body).contains("        value:");
    }

    @Test
    void unknownExtraKeysSurviveLoadWithoutCrash(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("d.yml");
        Files.writeString(file,
                "config-version: 1\n" +
                "a:\n  b:\n    c:\n      d:\n        value: leaf\n    sibling: 1\n  list: []\n" +
                "ignored-extra: hello\n");

        Deep d = new Deep();
        d.load(file);

        assertThat(d.deep).isEqualTo("leaf");
    }
}
