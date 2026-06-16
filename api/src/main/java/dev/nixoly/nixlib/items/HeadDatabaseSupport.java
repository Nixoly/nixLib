package dev.nixoly.nixlib.items;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public final class HeadDatabaseSupport {

    private static volatile Object api;
    private static volatile boolean resolved;

    private HeadDatabaseSupport() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("HeadDatabase") != null;
    }

    public static ItemStack head(String id) {
        if (id == null || id.isBlank() || !isAvailable()) {
            return null;
        }
        try {
            Object database = api();
            if (database == null) {
                return null;
            }
            Object result = database.getClass().getMethod("getItemHead", String.class).invoke(database, id.trim());
            return result instanceof ItemStack stack ? stack : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object api() {
        if (resolved) {
            return api;
        }
        synchronized (HeadDatabaseSupport.class) {
            if (resolved) {
                return api;
            }
            try {
                Class<?> apiClass = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");
                api = apiClass.getConstructor().newInstance();
            } catch (Throwable ignored) {
                api = null;
            }
            resolved = true;
            return api;
        }
    }
}
