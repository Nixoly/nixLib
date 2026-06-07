package dev.nixoly.nixlib.config;

import dev.nixoly.nixlib.config.annotations.ConfigVersion;
import dev.nixoly.nixlib.config.annotations.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigDefaultsTest {

    @ConfigVersion(1)
    static final class C extends Config {
        @Path("a") public String a = "alpha";
        @Path("b") public int b = 42;
        @Path("nested.c") public String c = "gamma";
    }

    @Test
    void missingKeysFallBackToDefaults(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("c.yml");
        Files.writeString(file, "config-version: 1\na: written\n");

        C cfg = new C();
        cfg.load(file);

        assertThat(cfg.a).isEqualTo("written");
        assertThat(cfg.b).isEqualTo(42);
        assertThat(cfg.c).isEqualTo("gamma");
        assertThat(cfg.warnings()).anyMatch(w -> w.contains("b"));
        assertThat(cfg.warnings()).anyMatch(w -> w.contains("nested.c"));
    }

    @Test
    void defaultsAreWrittenBackToFile(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("c.yml");
        Files.writeString(file, "config-version: 1\na: keep\n");

        new C().load(file);

        String body = Files.readString(file);
        assertThat(body).contains("a: keep", "b: 42", "c: gamma");
    }
}
