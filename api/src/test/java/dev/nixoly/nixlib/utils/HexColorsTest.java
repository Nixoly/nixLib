package dev.nixoly.nixlib.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HexColorsTest {

    @Test
    void normalizeAddsHashAndUpperCases() {
        assertThat(HexColors.normalize("ff8800")).isEqualTo("#FF8800");
        assertThat(HexColors.normalize("#ff8800")).isEqualTo("#FF8800");
        assertThat(HexColors.normalize("  #Ab12Cd  ")).isEqualTo("#AB12CD");
    }

    @Test
    void normalizeRejectsInvalidInput() {
        assertThat(HexColors.normalize(null)).isNull();
        assertThat(HexColors.normalize("")).isNull();
        assertThat(HexColors.normalize("xyz")).isNull();
        assertThat(HexColors.normalize("#FFF")).isEqualTo("#FFFFFF");
        assertThat(HexColors.normalize("#1234567")).isNull();
        assertThat(HexColors.normalize("gggggg")).isNull();
    }

    @Test
    void isValidMatchesNormalize() {
        assertThat(HexColors.isValid("00ff00")).isTrue();
        assertThat(HexColors.isValid("#00ff00")).isTrue();
        assertThat(HexColors.isValid("#00ff62")).isTrue();
        assertThat(HexColors.isValid("00ff62")).isTrue();
        assertThat(HexColors.isValid("&#00ff62")).isTrue();
        assertThat(HexColors.isValid("nope")).isFalse();
        assertThat(HexColors.isValid(null)).isFalse();
    }

    @Test
    void normalizeStripsLegacyFormatting() {
        assertThat(HexColors.normalize("§f#00ff62")).isEqualTo("#00FF62");
        assertThat(HexColors.normalize("§x§0§0§f§f§6§2")).isEqualTo("#00FF62");
    }

    @Test
    void colorParsesNormalizedHex() {
        assertThat(HexColors.color("#FF8800").value()).isEqualTo(0xFF8800);
        assertThat(HexColors.color("00FF00").value()).isEqualTo(0x00FF00);
    }
}
