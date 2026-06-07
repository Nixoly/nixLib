package dev.nixoly.nixlib.placeholders;

import org.bukkit.OfflinePlayer;

@FunctionalInterface
public interface Placeholder {

    String resolve(OfflinePlayer player, String argument);
}
