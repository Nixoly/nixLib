package dev.nixoly.nixlib.placeholders;

import org.bukkit.OfflinePlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderRegistry {

    private static final Pattern TOKEN = Pattern.compile("%([a-zA-Z0-9_\\-]+)(?::([^%]+))?%");

    private final Map<String, Placeholder> handlers = new LinkedHashMap<>();

    public PlaceholderRegistry register(String key, Placeholder handler) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("placeholder key cannot be blank");
        }
        handlers.put(key.toLowerCase(), handler);
        return this;
    }

    public PlaceholderRegistry unregister(String key) {
        handlers.remove(key.toLowerCase());
        return this;
    }

    public boolean has(String key) {
        return handlers.containsKey(key.toLowerCase());
    }

    public String apply(OfflinePlayer player, String input) {
        if (input == null || input.indexOf('%') < 0) return input;
        Matcher m = TOKEN.matcher(input);
        StringBuilder out = new StringBuilder(input.length());
        int cursor = 0;
        while (m.find()) {
            out.append(input, cursor, m.start());
            Placeholder handler = handlers.get(m.group(1).toLowerCase());
            if (handler == null) {
                out.append(m.group());
            } else {
                String resolved = handler.resolve(player, m.group(2));
                out.append(resolved == null ? "" : resolved);
            }
            cursor = m.end();
        }
        out.append(input, cursor, input.length());
        return out.toString();
    }

    public int size() {
        return handlers.size();
    }
}
