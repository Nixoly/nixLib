package dev.nixoly.nixlib.menu;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.messaging.Messenger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;

final class Integrations {

    private static final String BUNGEE_CHANNEL = "BungeeCord";

    private Integrations() {
    }

    static void registerBungee(Plugin plugin) {
        try {
            Messenger messenger = plugin.getServer().getMessenger();
            if (!messenger.isOutgoingChannelRegistered(plugin, BUNGEE_CHANNEL)) {
                messenger.registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
            }
        } catch (Throwable ignored) {
        }
    }

    static void connect(Plugin plugin, Player player, String server) {
        if (server == null || server.isBlank()) {
            return;
        }
        try {
            registerBungee(plugin);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            out.writeUTF("Connect");
            out.writeUTF(server.trim());
            player.sendPluginMessage(plugin, BUNGEE_CHANNEL, stream.toByteArray());
        } catch (Throwable t) {
            Warnings.once("bungee", "Could not send BungeeCord connect message: " + t.getMessage());
        }
    }

    static String parsePlaceholders(Player player, String text) {
        Method setPlaceholders = Hooks.SET_PLACEHOLDERS;
        if (setPlaceholders == null) {
            Warnings.once("papi", "PlaceholderAPI is not installed; placeholder action skipped.");
            return text;
        }
        try {
            Object result = setPlaceholders.invoke(null, player, text);
            return result == null ? text : result.toString();
        } catch (Throwable t) {
            Warnings.once("papi-error", "PlaceholderAPI parsing failed: " + t.getMessage());
            return text;
        }
    }

    static boolean money(Player player, double amount, boolean deposit) {
        Method method = deposit ? Hooks.DEPOSIT : Hooks.WITHDRAW;
        Object economy = provider(Hooks.ECONOMY);
        if (economy == null || method == null) {
            Warnings.once("vault-economy", "Vault economy not found; money action skipped.");
            return false;
        }
        try {
            method.invoke(economy, player, amount);
            return true;
        } catch (Throwable t) {
            Warnings.once("vault-economy-error", "Vault economy action failed: " + t.getMessage());
            return false;
        }
    }

    static boolean permission(Player player, String node, boolean add) {
        Method method = add ? Hooks.PERMISSION_ADD : Hooks.PERMISSION_REMOVE;
        Object permissions = provider(Hooks.PERMISSION);
        if (permissions == null || method == null) {
            Warnings.once("vault-permission", "Vault permission provider not found; permission action skipped.");
            return false;
        }
        try {
            method.invoke(permissions, player, node);
            return true;
        } catch (Throwable t) {
            Warnings.once("vault-permission-error", "Vault permission action failed: " + t.getMessage());
            return false;
        }
    }

    // The provider is looked up per call because Vault services can be swapped while the server runs.
    private static Object provider(Class<?> service) {
        if (service == null) {
            return null;
        }
        RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(service);
        return registration == null ? null : registration.getProvider();
    }

    // Resolved on first use. A missing plugin costs a ClassNotFoundException, which is too slow to
    // repeat on every menu click.
    private static final class Hooks {
        static final Method SET_PLACEHOLDERS = method(
                type("me.clip.placeholderapi.PlaceholderAPI"), "setPlaceholders", Player.class, String.class);
        static final Class<?> ECONOMY = type("net.milkbowl.vault.economy.Economy");
        static final Class<?> PERMISSION = type("net.milkbowl.vault.permission.Permission");
        static final Method DEPOSIT = method(ECONOMY, "depositPlayer", OfflinePlayer.class, double.class);
        static final Method WITHDRAW = method(ECONOMY, "withdrawPlayer", OfflinePlayer.class, double.class);
        static final Method PERMISSION_ADD = method(PERMISSION, "playerAdd", Player.class, String.class);
        static final Method PERMISSION_REMOVE = method(PERMISSION, "playerRemove", Player.class, String.class);

        private static Class<?> type(String name) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        private static Method method(Class<?> owner, String name, Class<?>... parameters) {
            if (owner == null) {
                return null;
            }
            try {
                return owner.getMethod(name, parameters);
            } catch (NoSuchMethodException e) {
                return null;
            }
        }
    }
}
