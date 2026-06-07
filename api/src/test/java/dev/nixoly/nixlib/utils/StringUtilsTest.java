package dev.nixoly.nixlib.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void blankAndEmptyDetected() {
        assertThat(StringUtils.isBlank(null)).isTrue();
        assertThat(StringUtils.isBlank("   ")).isTrue();
        assertThat(StringUtils.isBlank("x")).isFalse();
    }

    @Test
    void orDefaultPicksFallback() {
        assertThat(StringUtils.orDefault("", "x")).isEqualTo("x");
        assertThat(StringUtils.orDefault(null, "x")).isEqualTo("x");
        assertThat(StringUtils.orDefault("y", "x")).isEqualTo("y");
    }

    @Test
    void capitalizeOnlyFirstLetter() {
        assertThat(StringUtils.capitalize("hello")).isEqualTo("Hello");
        assertThat(StringUtils.capitalize("HELLO")).isEqualTo("Hello");
        assertThat(StringUtils.capitalize("")).isEmpty();
    }

    @Test
    void prettyEnumSpaces() {
        assertThat(StringUtils.prettyEnum(Sample.WARRIOR_OF_LIGHT)).isEqualTo("Warrior Of Light");
        assertThat(StringUtils.prettyEnum(Sample.MAGE)).isEqualTo("Mage");
    }

    @Test
    void wrapBreaksLongLines() {
        List<String> wrapped = StringUtils.wrap("the quick brown fox jumps over the lazy dog", 12);
        assertThat(wrapped).hasSizeGreaterThanOrEqualTo(3);
        for (String line : wrapped) assertThat(line.length()).isLessThanOrEqualTo(12);
    }

    @Test
    void truncateWithEllipsis() {
        assertThat(StringUtils.truncate("hello world", 5, "...")).isEqualTo("he...");
        assertThat(StringUtils.truncate("hi", 5, "...")).isEqualTo("hi");
    }

    enum Sample { WARRIOR_OF_LIGHT, MAGE }
}
