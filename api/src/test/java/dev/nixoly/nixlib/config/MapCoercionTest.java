package dev.nixoly.nixlib.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MapCoercionTest {

    @Test
    void boolAtReadsNestedPath() {
        Map<String, Object> root = Map.of(
                "MessageOptions", Map.of("RemindVanishedOnJoin", true)
        );
        assertThat(MapCoercion.boolAt(root, "MessageOptions.RemindVanishedOnJoin", false)).isTrue();
    }

    @Test
    void readWorldNamesAcceptsStar() {
        assertThat(MapCoercion.readWorldNames(List.of("*"))).containsExactly("*");
    }
}
