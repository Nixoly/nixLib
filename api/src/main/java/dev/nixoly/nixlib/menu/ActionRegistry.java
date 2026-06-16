package dev.nixoly.nixlib.menu;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ActionRegistry {

    private final Map<String, MenuAction> actions = new HashMap<>();

    public @NotNull ActionRegistry register(@NotNull String token, @NotNull MenuAction action) {
        actions.put(normalize(token), action);
        return this;
    }

    public boolean has(@NotNull String token) {
        return actions.containsKey(normalize(token));
    }

    public void runAll(@NotNull List<String> lines, @NotNull ActionContext context) {
        for (String line : lines) {
            run(line, context);
        }
    }

    public void run(@NotNull String line, @NotNull ActionContext context) {
        String resolved = context.apply(line);
        if (resolved == null) {
            return;
        }
        resolved = resolved.trim();
        if (resolved.isEmpty() || resolved.charAt(0) != '[') {
            return;
        }
        int end = resolved.indexOf(']');
        if (end < 1) {
            return;
        }
        String token = normalize(resolved.substring(1, end));
        String argument = end + 1 < resolved.length() ? resolved.substring(end + 1).trim() : "";
        MenuAction action = actions.get(token);
        if (action == null) {
            Warnings.once("action:" + token, "Unknown menu action [" + token + "] in configuration.");
            return;
        }
        try {
            action.run(context, argument);
        } catch (Throwable t) {
            Warnings.once("action-error:" + token,
                    "Menu action [" + token + "] failed: " + t.getMessage());
        }
    }

    public static @NotNull ActionRegistry createDefault(@NotNull Plugin plugin) {
        ActionRegistry registry = new ActionRegistry();
        DefaultActions.install(registry, plugin);
        return registry;
    }

    private static String normalize(String token) {
        return token.trim().toLowerCase(Locale.ROOT)
                .replace("[", "").replace("]", "");
    }
}
