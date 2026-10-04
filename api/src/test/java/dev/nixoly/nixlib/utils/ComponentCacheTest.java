package dev.nixoly.nixlib.utils;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentCacheTest {

    @Test
    void parsesEachInputOnce() {
        AtomicInteger parses = new AtomicInteger();
        ComponentCache cache = new ComponentCache(8, input -> {
            parses.incrementAndGet();
            return Component.text(input);
        });

        Component first = cache.get("&aHello");
        Component second = cache.get("&aHello");

        assertThat(second).isSameAs(first);
        assertThat(parses).hasValue(1);
    }

    @Test
    void lineReadEveryRenderSurvivesARotation() {
        AtomicInteger parses = new AtomicInteger();
        ComponentCache cache = new ComponentCache(4, input -> {
            parses.incrementAndGet();
            return Component.text(input);
        });

        cache.get("title");
        for (int i = 0; i < 12; i++) {
            cache.get("coords " + i);
            cache.get("title");
        }

        assertThat(parses).hasValue(13);
    }

    @Test
    void sizeStaysBoundedByTwoGenerations() {
        ComponentCache cache = new ComponentCache(16, Component::text);
        for (int i = 0; i < 1_000; i++) {
            cache.get("line " + i);
        }
        assertThat(cache.size()).isLessThanOrEqualTo(32);
    }
}
