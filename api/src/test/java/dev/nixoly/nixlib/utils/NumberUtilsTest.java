package dev.nixoly.nixlib.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NumberUtilsTest {

    @Test
    void parsesIntegers() {
        assertThat(NumberUtils.tryInt("42")).contains(42);
        assertThat(NumberUtils.tryInt("  -7 ")).contains(-7);
        assertThat(NumberUtils.tryInt("oops")).isEmpty();
        assertThat(NumberUtils.tryInt(null)).isEmpty();
    }

    @Test
    void clampBounds() {
        assertThat(NumberUtils.clamp(50, 0, 10)).isEqualTo(10);
        assertThat(NumberUtils.clamp(-3, 0, 10)).isZero();
        assertThat(NumberUtils.clamp(5, 0, 10)).isEqualTo(5);
    }

    @Test
    void compactFormatting() {
        assertThat(NumberUtils.formatCompact(500)).isEqualTo("500");
        assertThat(NumberUtils.formatCompact(1500)).isEqualTo("1.5K");
        assertThat(NumberUtils.formatCompact(2_500_000)).isEqualTo("2.5M");
        assertThat(NumberUtils.formatCompact(7_000_000_000L)).isEqualTo("7B");
    }

    @Test
    void commasInLargeNumbers() {
        assertThat(NumberUtils.formatWithCommas(1234567)).isEqualTo("1,234,567");
    }

    @Test
    void percentFormatter() {
        assertThat(NumberUtils.formatPercent(0.5)).isEqualTo("50%");
        assertThat(NumberUtils.formatPercent(0.123)).isEqualTo("12.3%");
    }
}
