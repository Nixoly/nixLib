package dev.nixoly.nixlib.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;

public final class NbtUtils {

    private NbtUtils() {}

    public static ItemStack setString(Plugin plugin, ItemStack item, String key, String value) {
        return apply(item, meta -> meta.getPersistentDataContainer().set(keyOf(plugin, key), PersistentDataType.STRING, value));
    }

    public static Optional<String> getString(Plugin plugin, ItemStack item, String key) {
        return read(item, pdc -> pdc.get(keyOf(plugin, key), PersistentDataType.STRING));
    }

    public static ItemStack setInt(Plugin plugin, ItemStack item, String key, int value) {
        return apply(item, meta -> meta.getPersistentDataContainer().set(keyOf(plugin, key), PersistentDataType.INTEGER, value));
    }

    public static Optional<Integer> getInt(Plugin plugin, ItemStack item, String key) {
        return read(item, pdc -> pdc.get(keyOf(plugin, key), PersistentDataType.INTEGER));
    }

    public static ItemStack setLong(Plugin plugin, ItemStack item, String key, long value) {
        return apply(item, meta -> meta.getPersistentDataContainer().set(keyOf(plugin, key), PersistentDataType.LONG, value));
    }

    public static Optional<Long> getLong(Plugin plugin, ItemStack item, String key) {
        return read(item, pdc -> pdc.get(keyOf(plugin, key), PersistentDataType.LONG));
    }

    public static ItemStack setDouble(Plugin plugin, ItemStack item, String key, double value) {
        return apply(item, meta -> meta.getPersistentDataContainer().set(keyOf(plugin, key), PersistentDataType.DOUBLE, value));
    }

    public static Optional<Double> getDouble(Plugin plugin, ItemStack item, String key) {
        return read(item, pdc -> pdc.get(keyOf(plugin, key), PersistentDataType.DOUBLE));
    }

    public static ItemStack setBoolean(Plugin plugin, ItemStack item, String key, boolean value) {
        return setInt(plugin, item, key, value ? 1 : 0);
    }

    public static boolean getBoolean(Plugin plugin, ItemStack item, String key) {
        return getInt(plugin, item, key).map(v -> v != 0).orElse(false);
    }

    public static ItemStack setUuid(Plugin plugin, ItemStack item, String key, UUID uuid) {
        return setString(plugin, item, key, uuid.toString());
    }

    public static Optional<UUID> getUuid(Plugin plugin, ItemStack item, String key) {
        return getString(plugin, item, key).map(s -> {
            try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
        });
    }

    public static boolean has(Plugin plugin, ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey nk = keyOf(plugin, key);
        return pdc.has(nk, PersistentDataType.STRING)
                || pdc.has(nk, PersistentDataType.INTEGER)
                || pdc.has(nk, PersistentDataType.LONG)
                || pdc.has(nk, PersistentDataType.DOUBLE);
    }

    public static ItemStack remove(Plugin plugin, ItemStack item, String key) {
        return apply(item, meta -> meta.getPersistentDataContainer().remove(keyOf(plugin, key)));
    }

    private static NamespacedKey keyOf(Plugin plugin, String key) {
        return new NamespacedKey(plugin, key);
    }

    private static ItemStack apply(ItemStack item, java.util.function.Consumer<ItemMeta> op) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        op.accept(meta);
        item.setItemMeta(meta);
        return item;
    }

    private static <T> Optional<T> read(ItemStack item, java.util.function.Function<PersistentDataContainer, T> op) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        return Optional.ofNullable(op.apply(item.getItemMeta().getPersistentDataContainer()));
    }
}
