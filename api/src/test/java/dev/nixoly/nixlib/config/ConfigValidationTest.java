package dev.nixoly.nixlib.config;

import dev.nixoly.nixlib.config.annotations.ConfigVersion;
import dev.nixoly.nixlib.config.annotations.Path;
import dev.nixoly.nixlib.config.validation.NotEmpty;
import dev.nixoly.nixlib.config.validation.OneOf;
import dev.nixoly.nixlib.config.validation.Range;
import dev.nixoly.nixlib.config.validation.Regex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigValidationTest {

    @ConfigVersion(1)
    static final class Sample extends Config {
        @Path("limit") @Range(min = 1, max = 100) public int limit = 10;
        @Path("name") @NotEmpty public String name = "ok";
        @Path("mode") @OneOf({"easy", "hard"}) public String mode = "easy";
        @Path("ident") @Regex("[a-z]+") public String ident = "abc";
    }

    @Test
    void rangeViolationFallsBackToDefault(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("v.yml");
        Files.writeString(file, "config-version: 1\nlimit: 9999\nname: ok\nmode: easy\nident: abc\n");

        Sample s = new Sample();
        s.load(file);

        assertThat(s.limit).isEqualTo(10);
        assertThat(s.warnings()).anyMatch(w -> w.contains("limit") && w.contains("range"));
    }

    @Test
    void emptyStringIsRejected(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("v.yml");
        Files.writeString(file, "config-version: 1\nlimit: 5\nname: ''\nmode: easy\nident: abc\n");

        Sample s = new Sample();
        s.load(file);

        assertThat(s.name).isEqualTo("ok");
        assertThat(s.warnings()).anyMatch(w -> w.contains("name") && w.contains("empty"));
    }

    @Test
    void oneOfRejectsUnknownChoice(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("v.yml");
        Files.writeString(file, "config-version: 1\nlimit: 5\nname: ok\nmode: insane\nident: abc\n");

        Sample s = new Sample();
        s.load(file);

        assertThat(s.mode).isEqualTo("easy");
    }

    @Test
    void regexEnforced(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("v.yml");
        Files.writeString(file, "config-version: 1\nlimit: 5\nname: ok\nmode: easy\nident: ABC123\n");

        Sample s = new Sample();
        s.load(file);

        assertThat(s.ident).isEqualTo("abc");
    }

    @Test
    void validValuesPassUnchanged(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("v.yml");
        Files.writeString(file, "config-version: 1\nlimit: 50\nname: server\nmode: hard\nident: zeta\n");

        Sample s = new Sample();
        s.load(file);

        assertThat(s.limit).isEqualTo(50);
        assertThat(s.name).isEqualTo("server");
        assertThat(s.mode).isEqualTo("hard");
        assertThat(s.ident).isEqualTo("zeta");
        assertThat(s.warnings()).isEmpty();
    }
}
