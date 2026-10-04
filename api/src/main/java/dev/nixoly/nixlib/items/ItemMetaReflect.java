package dev.nixoly.nixlib.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ItemMetaReflect {

    private static final Object MISSING = new Object();
    // Keyed by meta class first so a lookup does not build a "class#method" string per item.
    private static final ClassValue<Map<String, Object>> RESOLVED = new ClassValue<>() {
        @Override
        protected Map<String, Object> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private ItemMetaReflect() {
    }

    static void itemModel(ItemMeta meta, String key) {
        NamespacedKey parsed = parseKey(key);
        if (parsed != null) {
            invoke(meta, resolve(meta.getClass(), "setItemModel", NamespacedKey.class), parsed);
        }
    }

    static void tooltipStyle(ItemMeta meta, String key) {
        NamespacedKey parsed = parseKey(key);
        if (parsed != null) {
            invoke(meta, resolve(meta.getClass(), "setTooltipStyle", NamespacedKey.class), parsed);
        }
    }

    static boolean hideTooltip(ItemMeta meta, boolean hide) {
        Method setter = resolve(meta.getClass(), "setHideTooltip", boolean.class);
        if (setter == null || !invoke(meta, setter, hide)) {
            return false;
        }
        Method getter = resolve(meta.getClass(), "isHideTooltip");
        if (getter != null) {
            try {
                Object current = getter.invoke(meta);
                if (current instanceof Boolean bool) {
                    return bool == hide;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return true;
    }

    private static NamespacedKey parseKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return NamespacedKey.fromString(key.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean invoke(ItemMeta meta, Method method, Object value) {
        if (method == null) {
            return false;
        }
        try {
            method.invoke(meta, value);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    // Method names here are unique per signature, so the name alone is a safe cache key.
    private static Method resolve(Class<?> metaClass, String name, Class<?>... parameters) {
        Map<String, Object> methods = RESOLVED.get(metaClass);
        Object cached = methods.get(name);
        if (cached == null) {
            try {
                cached = metaClass.getMethod(name, parameters);
            } catch (NoSuchMethodException e) {
                cached = MISSING;
            }
            methods.put(name, cached);
        }
        return cached == MISSING ? null : (Method) cached;
    }
}
