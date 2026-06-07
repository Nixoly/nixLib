package dev.nixoly.nixlib.config;

import dev.nixoly.nixlib.config.annotations.ConfigVersion;
import dev.nixoly.nixlib.config.annotations.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigBasicTest {

    @ConfigVersion(1)
    static final class Sample extends Config {
        @Path("prefix") public String prefix = "&8[&bnixLib&8] ";
        @Path("messages.no-permission") public String noPermission = "&cno permission";
        @Path("limits.max-stack") public int maxStack = 64;
        @Path("debug.enabled") public boolean debug = false;
    }

    @Test
    void writesDefaultsWhenFileMissing(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("config.yml");
        Sample s = new Sample();
        s.load(file);

        assertThat(Files.exists(file)).isTrue();
        String body = Files.readString(file);
        assertThat(body).contains("prefix:", "max-stack: 64", "config-version: 1");
    }

    @Test
    void roundTripsAllFieldTypes(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("config.yml");

        Sample first = new Sample();
        first.load(file);
        first.prefix = "&7>> ";
        first.maxStack = 99;
        first.debug = true;
        first.save();

        Sample second = new Sample();
        second.load(file);
        assertThat(second.prefix).isEqualTo("&7>> ");
        assertThat(second.maxStack).isEqualTo(99);
        assertThat(second.debug).isTrue();
        assertThat(second.noPermission).isEqualTo("&cno permission");
    }

    @Test
    void dumpEmitsConfigVersionFirst() {
        Sample s = new Sample();
        String out = s.dump();
        assertThat(out.lines().filter(l -> !l.startsWith("#") && !l.isBlank()).findFirst())
                .hasValueSatisfying(line -> assertThat(line).startsWith("config-version: 1"));
    }
}
