package dev.nixoly.nixlib.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class ReflectionUtils {

    private ReflectionUtils() {}

    public static boolean classExists(String fqn) {
        try {
            Class.forName(fqn, false, ReflectionUtils.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Optional<Class<?>> findClass(String fqn) {
        try {
            return Optional.of(Class.forName(fqn));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    public static Optional<Method> findMethod(Class<?> owner, String name, Class<?>... params) {
        try {
            Method m = owner.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return Optional.of(m);
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    public static Optional<Field> findField(Class<?> owner, String name) {
        Class<?> c = owner;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return Optional.of(f);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        return Optional.empty();
    }

    public static <T> T invokeUnchecked(Method method, Object instance, Object... args) {
        try {
            @SuppressWarnings("unchecked")
            T result = (T) method.invoke(instance, args);
            return result;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("reflection invoke failed: " + method.getName(), e);
        }
    }
}
