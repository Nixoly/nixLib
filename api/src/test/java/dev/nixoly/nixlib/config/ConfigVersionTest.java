package dev.nixoly.nixlib.config;

import dev.nixoly.nixlib.config.annotations.ConfigVersion;
import dev.nixoly.nixlib.config.annotations.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigVersionTest {

    @ConfigVersion(3)
    static final class V3 extends Config {
        @Path("name") public String name = "default";
    }

    @ConfigVersion(value = 2, key = "schema")
    static final class CustomKey extends Config {
        @Path("name") public String name = "default";
    }

    @Test
    void declaredVersionIsExposed() {
        assertThat(new V3().version()).isEqualTo(3);
    }

    @Test
    void writesVersionHeaderToFile(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("v.yml");
        new V3().load(file);

        String body = Files.readString(file);
        assertThat(body).contains("config-version: 3");
    }

    @Test
    void newerFileVersionEmitsWarningButLoads(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("v.yml");
        Files.writeString(file, "config-version: 99\nname: future\n");

        V3 cfg = new V3();
        cfg.load(file);

        assertThat(cfg.name).isEqualTo("future");
        assertThat(cfg.warnings()).anyMatch(w -> w.contains("newer"));
    }

    @Test
    void customVersionKeyIsRespected(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("v.yml");
        new CustomKey().load(file);
        String body = Files.readString(file);
        assertThat(body).contains("schema: 2");
        assertThat(body).doesNotContain("config-version:");
    }
}
