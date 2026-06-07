package dev.nixoly.nixlib.version;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemanticVersionTest {

    @Test
    void parsesMajorMinorPatch() {
        SemanticVersion v = SemanticVersion.parse("1.2.3");
        assertThat(v.major()).isEqualTo(1);
        assertThat(v.minor()).isEqualTo(2);
        assertThat(v.patch()).isEqualTo(3);
    }

    @Test
    void parsesShortFormsWithImplicitZero() {
        assertThat(SemanticVersion.parse("2")).isEqualTo(SemanticVersion.of(2, 0, 0));
        assertThat(SemanticVersion.parse("2.5")).isEqualTo(SemanticVersion.of(2, 5, 0));
    }

    @Test
    void stripsLeadingVAndBuildMetadata() {
        assertThat(SemanticVersion.parse("v1.0.6")).isEqualTo(SemanticVersion.of(1, 0, 6));
        assertThat(SemanticVersion.parse("1.0.7-beta-1")).isEqualTo(SemanticVersion.of(1, 0, 7));
        assertThat(SemanticVersion.parse("1.2.3+build.42")).isEqualTo(SemanticVersion.of(1, 2, 3));
    }

    @Test
    void comparesOlderAndNewer() {
        SemanticVersion a = SemanticVersion.of(1, 0, 0);
        SemanticVersion b = SemanticVersion.of(1, 0, 1);
        assertThat(a.isOlderThan(b)).isTrue();
        assertThat(b.isNewerThan(a)).isTrue();
        assertThat(a.compareTo(a)).isZero();
    }

    @Test
    void parseOrFallsBackOnGarbage() {
        SemanticVersion fallback = SemanticVersion.of(1, 0, 0);
        assertThat(SemanticVersion.parseOr("not-a-version-at-all", fallback)).isEqualTo(SemanticVersion.of(0, 0, 0));
        assertThat(SemanticVersion.parseOr(null, fallback)).isSameAs(fallback);
    }

    @Test
    void rejectsNegativeComponents() {
        assertThatThrownBy(() -> SemanticVersion.of(-1, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
