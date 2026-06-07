package dev.nixoly.nixlib.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class PlaceholderApiBridge {

    private PlaceholderApiBridge() {}

    public static boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public static boolean register(Plugin plugin, String identifier, PlaceholderRegistry registry) {
        if (!isPresent()) return false;
        new Expansion(plugin, identifier, registry).register();
        return true;
    }

    private static final class Expansion extends PlaceholderExpansion {

        private final Plugin plugin;
        private final String identifier;
        private final PlaceholderRegistry registry;

        Expansion(Plugin plugin, String identifier, PlaceholderRegistry registry) {
            this.plugin = Objects.requireNonNull(plugin);
            this.identifier = identifier.toLowerCase();
            this.registry = registry;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public String getAuthor() {
            return String.join(",", plugin.getDescription().getAuthors());
        }

        @Override
        public String getVersion() {
            return plugin.getDescription().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onRequest(OfflinePlayer player, String params) {
            return registry.apply(player, "%" + params + "%");
        }
    }
}
