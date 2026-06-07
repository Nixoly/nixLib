package dev.nixoly.nixlib.config;

import dev.nixoly.nixlib.config.annotations.Comment;
import dev.nixoly.nixlib.config.annotations.ConfigVersion;
import dev.nixoly.nixlib.config.annotations.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigCommentTest {

    @ConfigVersion(1)
    static final class Sample extends Config {
        @Path("prefix")
        @Comment({"Prepended to every chat message.", "Supports & colour codes."})
        public String prefix = "&8[!] ";

        @Path("limits.max")
        @Comment("Maximum slots a player may open at once.")
        public int max = 5;
    }

    @Test
    void commentsAppearAboveFields(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("c.yml");
        new Sample().load(file);

        String body = Files.readString(file);
        assertThat(body).contains("# Prepended to every chat message.");
        assertThat(body).contains("# Supports & colour codes.");
        assertThat(body).contains("# Maximum slots a player may open at once.");
    }

    @Test
    void commentsSurviveReSave(@TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("c.yml");
        Sample s = new Sample();
        s.load(file);
        s.max = 99;
        s.save();

        String body = Files.readString(file);
        assertThat(body).contains("# Maximum slots a player may open at once.");
        assertThat(body).contains("max: 99");
    }
}
