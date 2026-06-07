package dev.nixoly.nixlib.config;

import dev.nixoly.nixlib.config.annotations.Comment;
import dev.nixoly.nixlib.config.annotations.Path;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ConfigField {

    final Field field;
    final String path;
    final List<String> comments;

    private ConfigField(Field field, String path, List<String> comments) {
        this.field = field;
        this.path = path;
        this.comments = comments;
    }

    static List<ConfigField> scan(Class<?> type) {
        List<ConfigField> out = new ArrayList<>();
        Class<?> c = type;
        while (c != null && c != Object.class && c != Config.class) {
            for (Field f : c.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                if (java.lang.reflect.Modifier.isTransient(f.getModifiers())) continue;
                Path p = f.getAnnotation(Path.class);
                if (p == null) continue;
                f.setAccessible(true);
                Comment cmt = f.getAnnotation(Comment.class);
                List<String> lines = cmt == null ? Collections.emptyList() : List.of(cmt.value());
                out.add(new ConfigField(f, p.value(), lines));
            }
            c = c.getSuperclass();
        }
        return out;
    }

    Object read(Object holder) {
        try {
            return field.get(holder);
        } catch (IllegalAccessException e) {
            throw new ConfigException("cannot read field " + field.getName(), e);
        }
    }

    void write(Object holder, Object value) {
        try {
            field.set(holder, value);
        } catch (IllegalAccessException e) {
            throw new ConfigException("cannot write field " + field.getName(), e);
        }
    }
}
