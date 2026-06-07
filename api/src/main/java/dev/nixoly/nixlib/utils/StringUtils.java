package dev.nixoly.nixlib.utils;

import java.util.ArrayList;
import java.util.List;

public final class StringUtils {

    private StringUtils() {}

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String orDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    public static String capitalize(String s) {
        if (isNullOrEmpty(s)) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    public static String prettyEnum(Enum<?> value) {
        if (value == null) return "";
        String[] parts = value.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(capitalize(parts[i]));
        }
        return sb.toString();
    }

    public static List<String> wrap(String text, int maxLineLength) {
        List<String> out = new ArrayList<>();
        if (isBlank(text)) {
            out.add("");
            return out;
        }
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (line.length() == 0) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= maxLineLength) {
                line.append(' ').append(word);
            } else {
                out.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    public static String truncate(String s, int max, String ellipsis) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        if (ellipsis == null) ellipsis = "";
        int keep = Math.max(0, max - ellipsis.length());
        return s.substring(0, keep) + ellipsis;
    }

    public static String join(Iterable<?> items, String separator) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object o : items) {
            if (!first) sb.append(separator);
            sb.append(o);
            first = false;
        }
        return sb.toString();
    }
}
