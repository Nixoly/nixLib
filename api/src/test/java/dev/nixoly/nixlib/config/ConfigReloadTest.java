package dev.nixoly.nixlib.config;

import dev.nixoly.nixlib.config.annotations.ConfigVersion;
import dev.nixoly.nixlib.config.annotations.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigReloadTest {

    @ConfigVersion(1)
    static final class Sample extends Config {
        @Path("value") public String value = "alpha";
        @Path("counter") public int counter = 0;

        int onLoadCount;

        @Override
        protected void onLoad() {
            onLoadCount++;
        }
    }

    @Test
    void reloadReadsFileAgain(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("r.yml");
        Sample s = new Sample();
        s.load(file);

        Files.writeString(file, "config-version: 1\nvalue: beta\ncounter: 9\n");
        s.reload();

        assertThat(s.value).isEqualTo("beta");
        assertThat(s.counter).isEqualTo(9);
    }

    @Test
    void onLoadFiresEachReload(@TempDir java.nio.file.Path dir) {
        java.nio.file.Path file = dir.resolve("r.yml");
        Sample s = new Sample();
        s.load(file);
        s.reload();
        s.reload();

        assertThat(s.onLoadCount).isEqualTo(3);
    }

    @Test
    void reloadWithoutLoadFails() {
        assertThatThrownBy(() -> new Sample().reload())
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("no file bound");
    }

    @Test
    void warningsAreClearedBetweenLoads(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("r.yml");
        Files.writeString(file, "config-version: 1\n");

        Sample s = new Sample();
        s.load(file);
        assertThat(s.warnings()).isNotEmpty();

        Files.writeString(file, "config-version: 1\nvalue: x\ncounter: 1\n");
        s.reload();
        assertThat(s.warnings()).isEmpty();
    }
}
