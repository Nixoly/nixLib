package dev.nixoly.nixlib.utils;

import net.kyori.adventure.text.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

// Two generations instead of one map that is wiped when full: when the young map fills up it becomes
// the old one, and anything read from the old map moves back. Lines a menu renders every second stay
// cached while one-off strings (live coordinates, timestamps) age out. Memory stays under 2 * limit.
final class ComponentCache {

    private final int generationLimit;
    private final Function<String, Component> parser;
    private volatile ConcurrentHashMap<String, Component> young = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<String, Component> old = new ConcurrentHashMap<>();

    ComponentCache(int generationLimit, Function<String, Component> parser) {
        this.generationLimit = generationLimit;
        this.parser = parser;
    }

    Component get(String input) {
        Component hit = young.get(input);
        if (hit != null) {
            return hit;
        }
        hit = old.get(input);
        if (hit == null) {
            hit = parser.apply(input);
        }
        store(input, hit);
        return hit;
    }

    int size() {
        return young.size() + old.size();
    }

    private void store(String input, Component value) {
        ConcurrentHashMap<String, Component> current = young;
        if (current.size() >= generationLimit) {
            synchronized (this) {
                if (young == current) {
                    old = current;
                    young = new ConcurrentHashMap<>();
                }
                current = young;
            }
        }
        current.put(input, value);
    }
}
