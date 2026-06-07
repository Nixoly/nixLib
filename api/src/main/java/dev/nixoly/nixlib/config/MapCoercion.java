package dev.nixoly.nixlib.config;

import dev.nixoly.nixlib.utils.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MapCoercion {

    private MapCoercion() {
    }

    public static double readSeconds(@Nullable Object raw, double fallback) {
        return readSeconds(raw, fallback, 0.0);
    }

    public static double readSeconds(@Nullable Object raw, double fallback, double minimum) {
        if (raw instanceof Number number) {
            return Math.max(minimum, number.doubleValue());
        }
        if (raw != null) {
            var parsed = NumberUtils.tryDouble(raw.toString().trim());
            if (parsed.isPresent()) {
                return Math.max(minimum, parsed.get());
            }
        }
        return fallback;
    }

    public static int intValue(@Nullable Object raw, int fallback) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw != null) {
            return NumberUtils.tryInt(raw.toString().trim()).orElse(fallback);
        }
        return fallback;
    }

    public static boolean boolAt(@NotNull Map<String, Object> root, @NotNull String dottedPath, boolean fallback) {
        String[] parts = dottedPath.split("\\.");
        Object current = root;
        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map)) {
                return fallback;
            }
            current = map.get(part);
            if (current == null) {
                return fallback;
            }
        }
        if (current instanceof Boolean b) {
            return b;
        }
        if (current instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return fallback;
    }

    public static @NotNull Set<String> readWorldNames(@Nullable Object raw) {
        Set<String> out = new HashSet<>();
        if (raw instanceof List<?> list) {
            addNames(out, list, true);
        } else if (raw instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                if (key != null) {
                    out.add(key.toString().toLowerCase(Locale.ROOT));
                }
            }
        } else if (raw instanceof Collection<?> collection) {
            addNames(out, collection, true);
        } else if (raw instanceof String single && !single.isBlank()) {
            out.add(single.toLowerCase(Locale.ROOT));
        }
        return out.isEmpty() ? Set.of() : Set.copyOf(out);
    }

    public static @NotNull Set<String> readStringSet(@Nullable Object raw, boolean lowercase) {
        if (raw instanceof List<?> list) {
            Set<String> out = new HashSet<>();
            addNames(out, list, lowercase);
            return Set.copyOf(out);
        }
        if (raw instanceof Collection<?> collection) {
            Set<String> out = new HashSet<>();
            addNames(out, collection, lowercase);
            return Set.copyOf(out);
        }
        return Set.of();
    }

    private static void addNames(@NotNull Set<String> out, @NotNull Iterable<?> items, boolean lowercase) {
        for (Object o : items) {
            if (o == null) {
                continue;
            }
            String value = o.toString().trim();
            if (!value.isEmpty()) {
                out.add(lowercase ? value.toLowerCase(Locale.ROOT) : value.toUpperCase(Locale.ROOT));
            }
        }
    }
}
