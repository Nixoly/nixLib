package dev.nixoly.nixlib.config.yaml;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Nodes {

    private Nodes() {}

    public static Object get(Map<String, Object> root, String path) {
        Object cursor = root;
        for (String segment : path.split("\\.")) {
            if (!(cursor instanceof Map<?, ?> map)) {
                return null;
            }
            cursor = map.get(segment);
            if (cursor == null) return null;
        }
        return cursor;
    }

    @SuppressWarnings("unchecked")
    public static void set(Map<String, Object> root, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> cursor = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = cursor.get(parts[i]);
            if (!(next instanceof Map<?, ?>)) {
                next = new LinkedHashMap<String, Object>();
                cursor.put(parts[i], next);
            }
            cursor = (Map<String, Object>) next;
        }
        cursor.put(parts[parts.length - 1], value);
    }

    public static boolean contains(Map<String, Object> root, String path) {
        Object cursor = root;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            if (!(cursor instanceof Map<?, ?> map)) return false;
            if (!map.containsKey(parts[i])) return false;
            cursor = map.get(parts[i]);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean remove(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> cursor = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = cursor.get(parts[i]);
            if (!(next instanceof Map<?, ?>)) return false;
            cursor = (Map<String, Object>) next;
        }
        return cursor.remove(parts[parts.length - 1]) != null;
    }
}
