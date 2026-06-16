package dev.nixoly.nixlib.menu;

import dev.nixoly.nixlib.gui.Gui;
import dev.nixoly.nixlib.gui.PagedGui;
import dev.nixoly.nixlib.utils.ChatUtils;
import dev.nixoly.nixlib.utils.ColorUtils;
import dev.nixoly.nixlib.utils.SoundUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

final class DefaultActions {

    private DefaultActions() {
    }

    static void install(ActionRegistry registry, Plugin plugin) {
        Warnings.logger(plugin.getLogger());
        Integrations.registerBungee(plugin);

        registry.register("player", (ctx, arg) -> ctx.player().performCommand(stripSlash(arg)));
        registry.register("commandevent", (ctx, arg) -> ctx.player().performCommand(stripSlash(arg)));
        registry.register("console", (ctx, arg) ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(arg)));
        registry.register("chat", (ctx, arg) -> ctx.player().chat(arg));

        registry.register("message", (ctx, arg) -> ctx.player().sendMessage(ColorUtils.translate(arg)));
        registry.register("minimessage", (ctx, arg) -> ctx.player().sendMessage(ColorUtils.translate(arg)));
        registry.register("broadcast", (ctx, arg) -> ChatUtils.broadcast(ColorUtils.translate(arg)));
        registry.register("minibroadcast", (ctx, arg) -> ChatUtils.broadcast(ColorUtils.translate(arg)));

        registry.register("json", (ctx, arg) -> ctx.player().sendMessage(json(arg)));
        registry.register("jsonbroadcast", (ctx, arg) -> Bukkit.getServer().sendMessage(json(arg)));

        registry.register("placeholder", (ctx, arg) -> Integrations.parsePlaceholders(ctx.player(), arg));

        registry.register("sound", (ctx, arg) -> playSound(ctx.player(), arg));
        registry.register("broadcastsound", (ctx, arg) -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                playSound(online, arg);
            }
        });
        registry.register("broadcastsoundworld", (ctx, arg) -> {
            for (Player online : ctx.player().getWorld().getPlayers()) {
                playSound(online, arg);
            }
        });

        registry.register("close", (ctx, arg) -> {
            dev.nixoly.nixlib.gui.GuiNavigation.suppressBack(ctx.player().getUniqueId());
            ctx.player().closeInventory();
        });
        registry.register("refresh", (ctx, arg) -> {
        });
        registry.register("openguimenu", (ctx, arg) -> {
        });
        registry.register("nextpage", (ctx, arg) -> {
            Gui gui = ctx.gui();
            if (gui instanceof PagedGui paged) {
                paged.nextPage();
            }
        });
        registry.register("prevpage", (ctx, arg) -> {
            Gui gui = ctx.gui();
            if (gui instanceof PagedGui paged) {
                paged.previousPage();
            }
        });
        registry.register("previouspage", (ctx, arg) -> {
            Gui gui = ctx.gui();
            if (gui instanceof PagedGui paged) {
                paged.previousPage();
            }
        });

        registry.register("connect", (ctx, arg) -> Integrations.connect(plugin, ctx.player(), arg));

        registry.register("givemoney", (ctx, arg) -> Integrations.money(ctx.player(), amount(arg), true));
        registry.register("takemoney", (ctx, arg) -> Integrations.money(ctx.player(), amount(arg), false));
        registry.register("givepermission", (ctx, arg) -> Integrations.permission(ctx.player(), arg.trim(), true));
        registry.register("takepermission", (ctx, arg) -> Integrations.permission(ctx.player(), arg.trim(), false));

        registry.register("giveexp", (ctx, arg) -> exp(ctx.player(), arg, true));
        registry.register("takeexp", (ctx, arg) -> exp(ctx.player(), arg, false));

        registry.register("meta", (ctx, arg) -> meta(plugin, ctx.player(), arg));
    }

    private static String stripSlash(String command) {
        String trimmed = command.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }

    private static Component json(String value) {
        try {
            return GsonComponentSerializer.gson().deserialize(value);
        } catch (Throwable t) {
            return ColorUtils.translate(value);
        }
    }

    private static void playSound(Player player, String arg) {
        String[] parts = arg.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return;
        }
        float volume = parts.length > 1 ? parseFloat(parts[1], 1.0f) : 1.0f;
        float pitch = parts.length > 2 ? parseFloat(parts[2], 1.0f) : 1.0f;
        SoundUtils.playSound(player, parts[0], volume, pitch);
    }

    private static void exp(Player player, String arg, boolean give) {
        String value = arg.trim();
        boolean levels = value.toLowerCase(Locale.ROOT).endsWith("l");
        if (levels) {
            value = value.substring(0, value.length() - 1).trim();
        }
        int amount = (int) parseDouble(value, 0);
        if (amount == 0) {
            return;
        }
        int signed = give ? amount : -amount;
        if (levels) {
            player.giveExpLevels(signed);
        } else {
            player.giveExp(signed);
        }
    }

    private static void meta(Plugin plugin, Player player, String arg) {
        String[] parts = arg.trim().split("\\s+", 4);
        if (parts.length < 3) {
            return;
        }
        String operation = parts[0].toLowerCase(Locale.ROOT);
        NamespacedKey key = new NamespacedKey(plugin, sanitizeKey(parts[1]));
        String type = parts[2].toLowerCase(Locale.ROOT);
        String value = parts.length > 3 ? parts[3] : "";
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        switch (operation) {
            case "remove" -> pdc.remove(key);
            case "switch" -> {
                boolean current = Boolean.TRUE.equals(pdc.get(key, PersistentDataType.BOOLEAN));
                pdc.set(key, PersistentDataType.BOOLEAN, !current);
            }
            case "set" -> setMeta(pdc, key, type, value);
            case "add", "subtract" -> adjustMeta(pdc, key, type, value, operation.equals("subtract"));
            default -> {
            }
        }
    }

    private static void setMeta(PersistentDataContainer pdc, NamespacedKey key, String type, String value) {
        switch (type) {
            case "int", "integer" -> pdc.set(key, PersistentDataType.INTEGER, (int) parseDouble(value, 0));
            case "long" -> pdc.set(key, PersistentDataType.LONG, (long) parseDouble(value, 0));
            case "double" -> pdc.set(key, PersistentDataType.DOUBLE, parseDouble(value, 0));
            case "boolean", "bool" -> pdc.set(key, PersistentDataType.BOOLEAN, Boolean.parseBoolean(value.trim()));
            default -> pdc.set(key, PersistentDataType.STRING, value);
        }
    }

    private static void adjustMeta(PersistentDataContainer pdc, NamespacedKey key, String type,
                                   String value, boolean subtract) {
        double delta = parseDouble(value, 0) * (subtract ? -1 : 1);
        switch (type) {
            case "long" -> {
                long current = pdc.has(key, PersistentDataType.LONG) ? pdc.get(key, PersistentDataType.LONG) : 0L;
                pdc.set(key, PersistentDataType.LONG, current + (long) delta);
            }
            case "double" -> {
                double current = pdc.has(key, PersistentDataType.DOUBLE) ? pdc.get(key, PersistentDataType.DOUBLE) : 0d;
                pdc.set(key, PersistentDataType.DOUBLE, current + delta);
            }
            default -> {
                int current = pdc.has(key, PersistentDataType.INTEGER) ? pdc.get(key, PersistentDataType.INTEGER) : 0;
                pdc.set(key, PersistentDataType.INTEGER, current + (int) delta);
            }
        }
    }

    private static String sanitizeKey(String raw) {
        String key = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
        return key.isEmpty() ? "meta" : key;
    }

    private static double amount(String arg) {
        return parseDouble(arg, 0);
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
