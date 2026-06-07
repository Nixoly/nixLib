package dev.nixoly.nixlib.version;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServerVersionTest {

    @Test
    void parsesBukkitStyleString() {
        ServerVersion v = ServerVersion.parse("git-Paper-432 (MC: 1.21.4)");
        assertThat(v.major()).isEqualTo(1);
        assertThat(v.minor()).isEqualTo(21);
        assertThat(v.patch()).isEqualTo(4);
    }

    @Test
    void parsesShortVersion() {
        ServerVersion v = ServerVersion.parse("1.17");
        assertThat(v.patch()).isZero();
        assertThat(v).hasToString("1.17");
    }

    @Test
    void parsesSnapshotApiString() {
        ServerVersion v = ServerVersion.parse("1.20.6-R0.1-SNAPSHOT");
        assertThat(v).isEqualTo(ServerVersion.V1_20_6);
    }

    @Test
    void comparisonHoldsAcrossComponents() {
        assertThat(ServerVersion.V1_19_4.isAtLeast(ServerVersion.V1_19)).isTrue();
        assertThat(ServerVersion.V1_19.isOlderThan(ServerVersion.V1_19_4)).isTrue();
        assertThat(ServerVersion.V1_21.compareTo(ServerVersion.V1_20_6)).isPositive();
    }

    @Test
    void rejectsGarbageInput() {
        assertThatThrownBy(() -> ServerVersion.parse("nope")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServerVersion.parse("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServerVersion.parse(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalityAndHashing() {
        assertThat(ServerVersion.of(1, 21, 0)).isEqualTo(ServerVersion.V1_21);
        assertThat(ServerVersion.of(1, 21, 0)).hasSameHashCodeAs(ServerVersion.V1_21);
        assertThat(ServerVersion.V1_21).isNotEqualTo(ServerVersion.V1_20);
    }
}
