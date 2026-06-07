package dev.nixoly.nixlib.placeholders;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceholderRegistryTest {

    @Test
    void resolvesSimpleToken() {
        PlaceholderRegistry r = new PlaceholderRegistry();
        r.register("name", (p, a) -> "nixoly");

        assertThat(r.apply(null, "hello %name%!")).isEqualTo("hello nixoly!");
    }

    @Test
    void resolvesArgumentVariant() {
        PlaceholderRegistry r = new PlaceholderRegistry();
        r.register("upper", (p, a) -> a == null ? "" : a.toUpperCase());

        assertThat(r.apply(null, "[%upper:hello world%]")).isEqualTo("[HELLO WORLD]");
    }

    @Test
    void leavesUnknownTokensIntact() {
        PlaceholderRegistry r = new PlaceholderRegistry();
        assertThat(r.apply(null, "%mystery%")).isEqualTo("%mystery%");
    }

    @Test
    void multipleTokensInOneString() {
        PlaceholderRegistry r = new PlaceholderRegistry();
        r.register("a", (p, x) -> "1");
        r.register("b", (p, x) -> "2");

        assertThat(r.apply(null, "%a%-%b%-%a%")).isEqualTo("1-2-1");
    }

    @Test
    void rejectsBlankKey() {
        PlaceholderRegistry r = new PlaceholderRegistry();
        assertThatThrownBy(() -> r.register("", (p, a) -> "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void caseInsensitiveLookup() {
        PlaceholderRegistry r = new PlaceholderRegistry();
        r.register("Foo", (p, a) -> "bar");
        assertThat(r.apply(null, "%FOO%")).isEqualTo("bar");
    }
}
