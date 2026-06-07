package dev.nixoly.nixlib.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ColorUtilsTest {

    @Test
    void parsesAmpersandToComponent() {
        Component c = ColorUtils.parse("&aHello");
        assertThat(c).isNotNull();
        String legacy = ColorUtils.toLegacySection(c);
        assertThat(legacy).contains("\u00a7a");
        assertThat(ColorUtils.stripColor(c)).isEqualTo("Hello");
    }

    @Test
    void parsesBukkitStyleHex() {
        Component c = ColorUtils.parse("&#FF8800orange");
        assertThat(ColorUtils.stripColor(c)).isEqualTo("orange");

        TextColor expected = TextColor.fromHexString("#FF8800");
        assertThat(c.color() != null ? c.color() : findFirstColor(c)).isEqualTo(expected);
    }

    @Test
    void stripsBothAmpersandAndHex() {
        String stripped = ColorUtils.stripColor("&aHello &#FF8800World");
        assertThat(stripped).isEqualTo("Hello World");
    }

    @Test
    void miniMessageParsesTags() {
        Component c = ColorUtils.miniMessage("<red>danger</red>");
        TextColor colour = c.color() != null ? c.color() : findFirstColor(c);
        assertThat(colour).isEqualTo(NamedTextColor.RED);
        assertThat(ColorUtils.stripColor(c)).isEqualTo("danger");
    }

    @Test
    void nullAndEmptyAreSafe() {
        assertThat(ColorUtils.parse(null)).isEqualTo(Component.empty());
        assertThat(ColorUtils.parse("")).isEqualTo(Component.empty());
        assertThat(ColorUtils.stripColor((String) null)).isNull();
        assertThat(ColorUtils.toLegacySection(null)).isEmpty();
    }

    @Test
    void plainTextRoundTripsThroughLegacySerializer() {
        Component c = ColorUtils.parse("plain text");
        assertThat(ColorUtils.toLegacySection(c)).isEqualTo("plain text");
    }

    @Test
    void parseAllOnListProducesMatchingSize() {
        var parsed = ColorUtils.parseAll(java.util.List.of("&aa", "&bb", "&cc"));
        assertThat(parsed).hasSize(3);
        assertThat(ColorUtils.stripColor(parsed.get(0))).isEqualTo("a");
    }

    @Test
    void translateMixesAmpCodesAndMiniMessageTags() {
        Component c = ColorUtils.translate("&aGreen <bold>and bold</bold>");
        assertThat(ColorUtils.stripColor(c)).isEqualTo("Green and bold");
        String mm = ColorUtils.toMiniMessage(c);
        assertThat(mm).contains("green");
        assertThat(mm).contains("bold");
    }

    @Test
    void translateConvertsBukkitHexToMiniMessageHex() {
        Component c = ColorUtils.translate("&#FF8800orange");
        TextColor colour = c.color() != null ? c.color() : findFirstColor(c);
        assertThat(colour).isEqualTo(TextColor.fromHexString("#FF8800"));
        assertThat(ColorUtils.stripColor(c)).isEqualTo("orange");
    }

    @Test
    void translatePassesThroughPureMiniMessage() {
        Component c = ColorUtils.translate("<red>danger</red> ahead");
        TextColor colour = c.color() != null ? c.color() : findFirstColor(c);
        assertThat(colour).isEqualTo(NamedTextColor.RED);
        assertThat(ColorUtils.stripColor(c)).isEqualTo("danger ahead");
    }

    @Test
    void translateNullAndEmptyReturnEmpty() {
        assertThat(ColorUtils.translate(null)).isEqualTo(Component.empty());
        assertThat(ColorUtils.translate("")).isEqualTo(Component.empty());
    }

    @Test
    void translateAllMatchesInputSize() {
        var lines = java.util.List.of("&aone", "<red>two</red>", "plain three");
        var components = ColorUtils.translateAll(lines);
        assertThat(components).hasSize(3);
        assertThat(ColorUtils.stripColor(components.get(0))).isEqualTo("one");
        assertThat(ColorUtils.stripColor(components.get(1))).isEqualTo("two");
        assertThat(ColorUtils.stripColor(components.get(2))).isEqualTo("plain three");
    }

    private static TextColor findFirstColor(Component component) {
        if (component.color() != null) return component.color();
        for (Component child : component.children()) {
            TextColor found = findFirstColor(child);
            if (found != null) return found;
        }
        return null;
    }
}
