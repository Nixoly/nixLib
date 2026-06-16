package dev.nixoly.nixlib.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;

public final class ChatUtils {

    private ChatUtils() {}

    public static void send(CommandSender target, String legacy) {
        if (target == null || legacy == null) return;
        if (legacy.isBlank()) {
            target.sendMessage(Component.empty());
            return;
        }
        target.sendMessage(ColorUtils.parse(legacy));
    }

    public static void send(CommandSender target, Component component) {
        if (target == null || component == null) return;
        target.sendMessage(component);
    }

    public static void send(CommandSender target, List<String> lines) {
        if (target == null || lines == null) return;
        for (String line : lines) send(target, line);
    }

    public static boolean isDisabled(String message) {
        return message == null || message.isEmpty();
    }

    public static void sendIfPresent(CommandSender target, String legacy) {
        if (target == null || isDisabled(legacy)) return;
        target.sendMessage(ColorUtils.parse(legacy));
    }

    public static void sendTranslatedIfPresent(CommandSender target, String message) {
        if (target == null || isDisabled(message)) return;
        target.sendMessage(ColorUtils.translate(message));
    }

    public static void sendTranslatedIfPresent(CommandSender target, List<String> lines) {
        if (target == null || lines == null) return;
        for (String line : lines) sendTranslatedIfPresent(target, line);
    }

    public static void sendTranslated(CommandSender target, String message) {
        if (target == null || message == null) return;
        if (message.isBlank()) {
            target.sendMessage(Component.empty());
            return;
        }
        target.sendMessage(ColorUtils.translate(message));
    }

    public static void sendTranslatedAll(CommandSender target, List<String> lines) {
        if (target == null || lines == null) return;
        for (String line : lines) sendTranslated(target, line);
    }

    public static void broadcast(String legacy) {
        if (legacy == null) return;
        broadcast(legacy.isBlank() ? Component.empty() : ColorUtils.parse(legacy));
    }

    public static void broadcast(Component component) {
        if (component == null) return;
        Bukkit.getServer().sendMessage(component);
    }

    public static void broadcastTranslated(String message) {
        if (message == null) return;
        broadcast(message.isBlank() ? Component.empty() : ColorUtils.translate(message));
    }

    public static void broadcastWithPermission(String permission, String legacy) {
        if (legacy == null) return;
        Component msg = legacy.isBlank() ? Component.empty() : ColorUtils.parse(legacy);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(permission)) p.sendMessage(msg);
        }
    }

    public static void broadcastTranslatedWithPermission(String permission, String message) {
        if (message == null) return;
        Component msg = message.isBlank() ? Component.empty() : ColorUtils.translate(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(permission)) p.sendMessage(msg);
        }
    }

    public static void title(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (player == null) return;
        Component head = ColorUtils.parse(title == null ? "" : title);
        Component sub = ColorUtils.parse(subtitle == null ? "" : subtitle);
        Title.Times times = Title.Times.times(
                ticksToDuration(fadeInTicks),
                ticksToDuration(stayTicks),
                ticksToDuration(fadeOutTicks)
        );
        player.showTitle(Title.title(head, sub, times));
    }

    public static void title(Player player, Component title, Component subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (player == null) return;
        Title.Times times = Title.Times.times(
                ticksToDuration(fadeInTicks),
                ticksToDuration(stayTicks),
                ticksToDuration(fadeOutTicks)
        );
        player.showTitle(Title.title(
                title == null ? Component.empty() : title,
                subtitle == null ? Component.empty() : subtitle,
                times
        ));
    }

    public static void actionBar(Player player, String legacy) {
        if (player == null || StringUtils.isBlank(legacy)) return;
        player.sendActionBar(ColorUtils.parse(legacy));
    }

    public static void actionBar(Player player, Component component) {
        if (player == null || component == null) return;
        player.sendActionBar(component);
    }

    private static Duration ticksToDuration(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }
}
