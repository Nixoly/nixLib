package dev.nixoly.nixlib.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

final class ItemMetaReflect {

    private static final Object MISSING = new Object();
    private static final Map<String, Object> RESOLVED = new ConcurrentHashMap<>();

    private ItemMetaReflect() {
    }

    static void itemModel(ItemMeta meta, String key) {
        NamespacedKey parsed = parseKey(key);
        if (parsed != null) {
            invoke(meta, "setItemModel", NamespacedKey.class, parsed);
        }
    }

    static void tooltipStyle(ItemMeta meta, String key) {
        NamespacedKey parsed = parseKey(key);
        if (parsed != null) {
            invoke(meta, "setTooltipStyle", NamespacedKey.class, parsed);
        }
    }

    static boolean hideTooltip(ItemMeta meta, boolean hide) {
        Method setter = resolve(meta.getClass(), "setHideTooltip", boolean.class);
        if (setter == null) {
            return false;
        }
        try {
            setter.invoke(meta, hide);
        } catch (Throwable t) {
            return false;
        }
        Method getter = resolveNoArg(meta.getClass(), "isHideTooltip");
        if (getter != null) {
            try {
                Object current = getter.invoke(meta);
                if (current instanceof Boolean bool) {
                    return bool == hide;
                }
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    private static NamespacedKey parseKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            return NamespacedKey.fromString(key.trim().toLowerCase());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void invoke(ItemMeta meta, String name, Class<?> argType, Object value) {
        Method method = resolve(meta.getClass(), name, argType);
        if (method == null) {
            return;
        }
        try {
            method.invoke(meta, value);
        } catch (Throwable ignored) {
        }
    }

    private static Method resolve(Class<?> metaClass, String name, Class<?> argType) {
        String cacheKey = metaClass.getName() + "#" + name;
        Object cached = RESOLVED.get(cacheKey);
        if (cached == MISSING) {
            return null;
        }
        if (cached instanceof Method method) {
            return method;
        }
        try {
            Method method = metaClass.getMethod(name, argType);
            RESOLVED.put(cacheKey, method);
            return method;
        } catch (Throwable t) {
            RESOLVED.put(cacheKey, MISSING);
            return null;
        }
    }

    private static Method resolveNoArg(Class<?> metaClass, String name) {
        String cacheKey = metaClass.getName() + "#" + name + "()";
        Object cached = RESOLVED.get(cacheKey);
        if (cached == MISSING) {
            return null;
        }
        if (cached instanceof Method method) {
            return method;
        }
        try {
            Method method = metaClass.getMethod(name);
            RESOLVED.put(cacheKey, method);
            return method;
        } catch (Throwable t) {
            RESOLVED.put(cacheKey, MISSING);
            return null;
        }
    }
}
