package dev.nixoly.nixlib.version;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServerCapabilitiesTest {

    @Test
    void foliaImpliesMultithreadedScheduler() {
        ServerCapabilities caps = new ServerCapabilities(ServerType.FOLIA, ServerVersion.V1_21);
        assertThat(caps.supportsFolia()).isTrue();
        assertThat(caps.supportsAdventure()).isTrue();
    }

    @Test
    void textDisplaysGatedAt1_19_4() {
        ServerCapabilities legacy = new ServerCapabilities(ServerType.PAPER, ServerVersion.V1_19);
        ServerCapabilities modern = new ServerCapabilities(ServerType.PAPER, ServerVersion.V1_19_4);

        assertThat(legacy.supportsTextDisplays()).isFalse();
        assertThat(modern.supportsTextDisplays()).isTrue();
    }

    @Test
    void itemComponentsRequire1_20_5() {
        assertThat(new ServerCapabilities(ServerType.PAPER, ServerVersion.V1_20).supportsItemComponentApi()).isFalse();
        assertThat(new ServerCapabilities(ServerType.PAPER, ServerVersion.V1_20_5).supportsItemComponentApi()).isTrue();
    }

    @Test
    void canvasIsTreatedAsMultithreadedPaperFork() {
        ServerCapabilities caps = new ServerCapabilities(ServerType.CANVAS, ServerVersion.V1_21_4);
        assertThat(caps.supportsFolia()).isTrue();
        assertThat(caps.supportsAdventure()).isTrue();
    }
}
