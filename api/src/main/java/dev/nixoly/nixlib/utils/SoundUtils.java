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

        Sound found = Registry.SOUNDS.get(NamespacedKey.minecraft(key));
        if (found != null) return found;

        if (!key.contains(".") && key.contains("_")) {
            found = Registry.SOUNDS.get(NamespacedKey.minecraft(key.replace('_', '.')));
        }
        return found;
    }

    public static void playSound(Player player, String name, float volume, float pitch) {
        if (player == null) return;
        Sound sound = parseSound(name);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
