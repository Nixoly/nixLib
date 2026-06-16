package dev.nixoly.nixlib.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CommandRegistrar {

    private CommandRegistrar() {
    }

    public static boolean register(@NotNull Plugin plugin, @NotNull CommandSpec spec) {
        CommandMap map = commandMap(plugin);
        if (map == null) {
            plugin.getLogger().warning("Could not resolve the server command map; /" + spec.label()
                    + " was not registered.");
            return false;
        }
        unregister(plugin, spec.label(), spec.aliases());
        NixCommand command = new NixCommand(spec);
        boolean primary = map.register(fallbackPrefix(plugin), command);
        if (!primary) {
            plugin.getLogger().info("/" + spec.label() + " was already taken; it is also reachable as /"
                    + fallbackPrefix(plugin) + ":" + spec.label() + ".");
        }
        syncCommands();
        return true;
    }

    public static void unregister(@NotNull Plugin plugin, @NotNull CommandSpec spec) {
        unregister(plugin, spec.label(), spec.aliases());
    }

    public static void unregister(@NotNull Plugin plugin, @NotNull String label, @NotNull List<String> aliases) {
        CommandMap map = commandMap(plugin);
        if (map == null) {
            return;
        }
        Map<String, Command> known = knownCommands(map);
        if (known == null) {
            return;
        }
        String prefix = fallbackPrefix(plugin);
        List<String> labels = new ArrayList<>();
        labels.add(label);
        labels.addAll(aliases);
        for (String name : labels) {
            String lower = name.toLowerCase(Locale.ROOT);
            removeIfOurs(known, lower);
            removeIfOurs(known, prefix + ":" + lower);
        }
        syncCommands();
    }

    private static void removeIfOurs(@NotNull Map<String, Command> known, @NotNull String key) {
        Command existing = known.get(key);
        if (existing instanceof NixCommand) {
            known.remove(key);
            existing.unregister(null);
        }
    }

    private static String fallbackPrefix(@NotNull Plugin plugin) {
        return plugin.getName().toLowerCase(Locale.ROOT);
    }

    private static CommandMap commandMap(@NotNull Plugin plugin) {
        try {
            Method getter = Bukkit.getServer().getClass().getMethod("getCommandMap");
            Object result = getter.invoke(Bukkit.getServer());
            if (result instanceof CommandMap map) {
                return map;
            }
        } catch (Throwable ignored) {
        }
        try {
            Field field = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            Object result = field.get(Bukkit.getPluginManager());
            if (result instanceof CommandMap map) {
                return map;
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Command map lookup failed: " + t.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> knownCommands(@NotNull CommandMap map) {
        Class<?> type = map.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("knownCommands");
                field.setAccessible(true);
                Object value = field.get(map);
                if (value instanceof Map<?, ?>) {
                    return (Map<String, Command>) value;
                }
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static void syncCommands() {
        try {
            Bukkit.getServer().getClass().getMethod("syncCommands").invoke(Bukkit.getServer());
        } catch (Throwable ignored) {
        }
    }
}
