package dev.nixoly.nixlib.serializers;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

public final class ItemStackSerializer {

    private ItemStackSerializer() {}

    public static String toBase64(ItemStack item) {
        if (item == null) return null;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeObject(item);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("failed to serialize item", e);
        }
    }

    public static Optional<ItemStack> fromBase64(String data) {
        if (data == null || data.isBlank()) return Optional.empty();
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream in = new BukkitObjectInputStream(bytes)) {
            return Optional.of((ItemStack) in.readObject());
        } catch (IOException | ClassNotFoundException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static String toBase64Array(ItemStack[] items) {
        if (items == null) return null;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeInt(items.length);
            for (ItemStack i : items) out.writeObject(i);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("failed to serialize array", e);
        }
    }

    public static Optional<ItemStack[]> fromBase64Array(String data) {
        if (data == null || data.isBlank()) return Optional.empty();
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream in = new BukkitObjectInputStream(bytes)) {
            int length = in.readInt();
            ItemStack[] out = new ItemStack[length];
            for (int i = 0; i < length; i++) out[i] = (ItemStack) in.readObject();
            return Optional.of(out);
        } catch (IOException | ClassNotFoundException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
