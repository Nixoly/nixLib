package dev.nixoly.nixlib.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class SoundUtils {

    private SoundUtils() {}

    public static Sound parseSound(String name) {
        if (name == null || name.isBlank()) return null;

        String key = name.toLowerCase(Locale.ROOT);
        if (key.startsWith("minecraft:")) {
            key = key.substring("minecraft:".length());
        }

        Sound found = lookup(key);
        if (found != null) return found;

        if (!key.contains(".") && key.contains("_")) {
            found = lookup(legacyEnumKey(key));
            if (found != null) return found;
            found = lookup(key.replace('_', '.'));
        }
        return found;
    }

    private static Sound lookup(String key) {
        if (key == null || key.isBlank()) return null;
        return Registry.SOUNDS.get(NamespacedKey.minecraft(key));
    }

    static String legacyEnumKey(String upperUnderscoreName) {
        String[] parts = upperUnderscoreName.toUpperCase(Locale.ROOT).split("_");
        if (parts.length < 2) return upperUnderscoreName;

        String category = switch (parts[0]) {
            case "BLOCK" -> "block";
            case "ENTITY" -> "entity";
            case "UI" -> "ui";
            case "ITEM" -> "item";
            case "MUSIC" -> "music";
            default -> parts[0].toLowerCase(Locale.ROOT);
        };

        if (parts.length == 2) {
            return category + "." + parts[1].toLowerCase(Locale.ROOT);
        }

        String action = parts[parts.length - 1].toLowerCase(Locale.ROOT);
        StringBuilder source = new StringBuilder();
        for (int i = 1; i < parts.length - 1; i++) {
            if (i > 1) source.append('_');
            source.append(parts[i].toLowerCase(Locale.ROOT));
        }
        return category + "." + source + "." + action;
    }

    public static void playSound(Player player, String name, float volume, float pitch) {
        if (player == null) return;
        Sound sound = parseSound(name);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
