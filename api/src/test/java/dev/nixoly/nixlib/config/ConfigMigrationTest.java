package dev.nixoly.nixlib.config;

import dev.nixoly.nixlib.config.annotations.ConfigVersion;
import dev.nixoly.nixlib.config.annotations.Path;
import dev.nixoly.nixlib.config.migration.MigrationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigMigrationTest {

    @ConfigVersion(3)
    static final class Plugin extends Config {
        @Path("prefix") public String prefix = "&8[!] ";
        @Path("messages.no-perm") public String noPerm = "&cno";
        @Path("limits.max-stack") public int max = 64;

        @Override
        protected void registerMigrations(MigrationRegistry registry) {
            registry.register(1, 2, ctx -> ctx.rename("noperm", "messages.no-perm"));
            registry.register(2, 3, ctx -> {
                ctx.rename("stackLimit", "limits.max-stack");
                ctx.remove("legacy.option");
            });
        }
    }

    @ConfigVersion(2)
    static final class Plain extends Config {
        @Path("hello") public String hello = "world";
    }

    @Test
    void chainedMigrationApplied(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("p.yml");
        Files.writeString(file,
                "config-version: 1\n" +
                "prefix: kept\n" +
                "noperm: '&cstop'\n" +
                "stackLimit: 32\n" +
                "legacy:\n  option: drop\n");

        Plugin p = new Plugin();
        p.load(file);

        assertThat(p.prefix).isEqualTo("kept");
        assertThat(p.noPerm).isEqualTo("&cstop");
        assertThat(p.max).isEqualTo(32);

        String body = Files.readString(file);
        assertThat(body).contains("config-version: 3");
        assertThat(body).doesNotContain("legacy:");
        assertThat(body).doesNotContain("stackLimit:");
    }

    @Test
    void missingMigrationStepThrows(@TempDir java.nio.file.Path dir) {
        java.nio.file.Path file = dir.resolve("p.yml");
        try {
            Files.writeString(file, "config-version: 1\nhello: x\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertThatThrownBy(() -> new Plain().load(file))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("no migration");
    }

    @Test
    void nonSequentialRegistrationIsRejected() {
        MigrationRegistry r = new MigrationRegistry();
        assertThatThrownBy(() -> r.register(1, 3, ctx -> {}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
