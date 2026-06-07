package dev.nixoly.nixlib.config.validation;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

public final class Validators {

    private Validators() {}

    public static void validate(Field field, Object value) {
        Range range = field.getAnnotation(Range.class);
        if (range != null && value instanceof Number n) {
            double d = n.doubleValue();
            if (d < range.min() || d > range.max()) {
                throw new ValidationException("value " + d + " out of range [" + range.min() + ", " + range.max() + "]");
            }
        }
        Regex regex = field.getAnnotation(Regex.class);
        if (regex != null && value != null) {
            String s = value.toString();
            if (!Pattern.matches(regex.value(), s)) {
                throw new ValidationException("value '" + s + "' does not match pattern " + regex.value());
            }
        }
        NotEmpty notEmpty = field.getAnnotation(NotEmpty.class);
        if (notEmpty != null && isEmpty(value)) {
            throw new ValidationException("value must not be empty");
        }
        OneOf oneOf = field.getAnnotation(OneOf.class);
        if (oneOf != null && value != null) {
            String s = value.toString();
            boolean ok = false;
            for (String allowed : oneOf.value()) {
                if (oneOf.caseSensitive() ? allowed.equals(s) : allowed.equalsIgnoreCase(s)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw new ValidationException("value '" + s + "' not in allowed set " + String.join(",", oneOf.value()));
            }
        }
    }

    private static boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof CharSequence cs) return cs.length() == 0;
        if (value instanceof Collection<?> c) return c.isEmpty();
        if (value instanceof Map<?, ?> m) return m.isEmpty();
        if (value.getClass().isArray()) return java.lang.reflect.Array.getLength(value) == 0;
        return false;
    }
}
