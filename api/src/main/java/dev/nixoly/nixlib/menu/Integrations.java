package dev.nixoly.nixlib.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
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
        Object provider = papiClass();
        if (provider == null) {
            Warnings.once("papi", "PlaceholderAPI is not installed; placeholder action skipped.");
            return text;
        }
        try {
            Method method = ((Class<?>) provider).getMethod("setPlaceholders", Player.class, String.class);
            Object result = method.invoke(null, player, text);
            return result == null ? text : result.toString();
        } catch (Throwable t) {
            Warnings.once("papi-error", "PlaceholderAPI parsing failed: " + t.getMessage());
            return text;
        }
    }

    private static Object papiClass() {
        try {
            return Class.forName("me.clip.placeholderapi.PlaceholderAPI");
        } catch (Throwable t) {
            return null;
        }
    }

    static boolean money(Player player, double amount, boolean deposit) {
        Object economy = economy();
        if (economy == null) {
            Warnings.once("vault-economy", "Vault economy not found; money action skipped.");
            return false;
        }
        try {
            String method = deposit ? "depositPlayer" : "withdrawPlayer";
            economy.getClass()
                    .getMethod(method, org.bukkit.OfflinePlayer.class, double.class)
                    .invoke(economy, player, amount);
            return true;
        } catch (Throwable t) {
            Warnings.once("vault-economy-error", "Vault economy action failed: " + t.getMessage());
            return false;
        }
    }

    static boolean permission(Player player, String node, boolean add) {
        Object permissions = permissions();
        if (permissions == null) {
            Warnings.once("vault-permission", "Vault permission provider not found; permission action skipped.");
            return false;
        }
        try {
            String method = add ? "playerAdd" : "playerRemove";
            permissions.getClass()
                    .getMethod(method, Player.class, String.class)
                    .invoke(permissions, player, node);
            return true;
        } catch (Throwable t) {
            Warnings.once("vault-permission-error", "Vault permission action failed: " + t.getMessage());
            return false;
        }
    }

    private static Object economy() {
        return service("net.milkbowl.vault.economy.Economy");
    }

    private static Object permissions() {
        return service("net.milkbowl.vault.permission.Permission");
    }

    private static Object service(String className) {
        try {
            Class<?> type = Class.forName(className);
            Object registration = Bukkit.getServicesManager().getRegistration(type.asSubclass(Object.class));
            if (registration == null) {
                return null;
            }
            return registration.getClass().getMethod("getProvider").invoke(registration);
        } catch (Throwable t) {
            return null;
        }
    }
}
