package dev.nixoly.nixlib.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TypeCoercion {

    private TypeCoercion() {}

    public static Optional<Object> coerce(Object value, Class<?> target) {
        if (value == null) return Optional.empty();
        if (target.isInstance(value)) return Optional.of(value);

        if (target == String.class) {
            return Optional.of(String.valueOf(value));
        }
        if (target == Boolean.class || target == boolean.class) {
            return coerceBoolean(value).map(o -> o);
        }
        if (Number.class.isAssignableFrom(box(target))) {
            return coerceNumber(value, box(target));
        }
        if (target == Character.class || target == char.class) {
            String s = value.toString();
            return s.isEmpty() ? Optional.empty() : Optional.of(s.charAt(0));
        }
        if (target.isEnum()) {
            return coerceEnum(value, target);
        }
        if (List.class.isAssignableFrom(target)) {
            return Optional.of(toList(value));
        }
        if (Set.class.isAssignableFrom(target)) {
            return Optional.of(new LinkedHashSet<>(toList(value)));
        }
        if (Map.class.isAssignableFrom(target) && value instanceof Map<?, ?> m) {
            LinkedHashMap<Object, Object> out = new LinkedHashMap<>(m.size());
            for (Map.Entry<?, ?> e : m.entrySet()) out.put(e.getKey(), e.getValue());
            return Optional.of(out);
        }
        return Optional.empty();
    }

    private static Class<?> box(Class<?> c) {
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        if (c == short.class) return Short.class;
        if (c == byte.class) return Byte.class;
        return c;
    }

    private static Optional<Boolean> coerceBoolean(Object v) {
        if (v instanceof Boolean b) return Optional.of(b);
        if (v instanceof Number n) return Optional.of(n.intValue() != 0);
        String s = v.toString().trim().toLowerCase();
        return switch (s) {
            case "true", "yes", "on", "1"  -> Optional.of(true);
            case "false", "no", "off", "0" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    private static Optional<Object> coerceNumber(Object v, Class<?> target) {
        Number num;
        if (v instanceof Number n) {
            num = n;
        } else {
            try {
                num = Double.parseDouble(v.toString().trim());
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        if (target == Integer.class) return Optional.of(num.intValue());
        if (target == Long.class)    return Optional.of(num.longValue());
        if (target == Double.class)  return Optional.of(num.doubleValue());
        if (target == Float.class)   return Optional.of(num.floatValue());
        if (target == Short.class)   return Optional.of(num.shortValue());
        if (target == Byte.class)    return Optional.of(num.byteValue());
        return Optional.of(num);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Optional<Object> coerceEnum(Object v, Class<?> target) {
        String s = v.toString().trim();
        for (Object constant : target.getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(s)) {
                return Optional.of(constant);
            }
        }
        try {
            return Optional.of(Enum.valueOf((Class<Enum>) target, s.toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static List<Object> toList(Object v) {
        if (v instanceof Collection<?> c) return new ArrayList<>(c);
        if (v.getClass().isArray()) {
            Object[] arr = (Object[]) v;
            List<Object> out = new ArrayList<>(arr.length);
            for (Object o : arr) out.add(o);
            return out;
        }
        List<Object> single = new ArrayList<>(1);
        single.add(v);
        return single;
    }
}
