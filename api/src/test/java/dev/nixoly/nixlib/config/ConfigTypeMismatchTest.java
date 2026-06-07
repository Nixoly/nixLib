package dev.nixoly.nixlib.config;

import dev.nixoly.nixlib.config.annotations.ConfigVersion;
import dev.nixoly.nixlib.config.annotations.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigTypeMismatchTest {

    @ConfigVersion(1)
    static final class Sample extends Config {
        @Path("count") public int count = 1;
        @Path("ratio") public double ratio = 0.5;
        @Path("flag") public boolean flag = false;
        @Path("words") public List<String> words = List.of("default");
        @Path("mode") public Mode mode = Mode.A;

        enum Mode { A, B, C }
    }

    @Test
    void stringNumberIsCoercedToInt(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("t.yml");
        Files.writeString(file, "config-version: 1\ncount: '17'\nratio: 0.5\nflag: false\nwords: []\nmode: A\n");

        Sample s = new Sample();
        s.load(file);

        assertThat(s.count).isEqualTo(17);
    }

    @Test
    void garbageStringForNumberKeepsDefault(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("t.yml");
        Files.writeString(file, "config-version: 1\ncount: hello\nratio: 0.5\nflag: false\nwords: []\nmode: A\n");

        Sample s = new Sample();
        s.load(file);

        assertThat(s.count).isEqualTo(1);
        assertThat(s.warnings()).anyMatch(w -> w.contains("count"));
    }

    @Test
    void looseBooleanParsing(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("t.yml");
        Files.writeString(file, "config-version: 1\ncount: 5\nratio: 0.5\nflag: yes\nwords: []\nmode: A\n");

        Sample s = new Sample();
        s.load(file);

        assertThat(s.flag).isTrue();
    }

    @Test
    void enumIsCaseInsensitive(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("t.yml");
        Files.writeString(file, "config-version: 1\ncount: 5\nratio: 0.5\nflag: false\nwords: []\nmode: b\n");

        Sample s = new Sample();
        s.load(file);

        assertThat(s.mode).isEqualTo(Sample.Mode.B);
    }

    @Test
    void numberToDoubleIsExact(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("t.yml");
        Files.writeString(file, "config-version: 1\ncount: 5\nratio: 3\nflag: false\nwords: []\nmode: A\n");

        Sample s = new Sample();
        s.load(file);

        assertThat(s.ratio).isEqualTo(3.0);
    }
}
