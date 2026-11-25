package dev.nixoly.nixlib.gui;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class InventoryViews {

    private static final MethodHandle TOP_INVENTORY = find("getTopInventory", MethodType.methodType(Inventory.class));
    private static final MethodHandle PLAYER = find("getPlayer", MethodType.methodType(HumanEntity.class));
    private static final MethodHandle SET_TITLE = find("setTitle", MethodType.methodType(void.class, String.class));

    private InventoryViews() {
    }

    public static Inventory topInventory(HumanEntity entity) {
        return entity == null ? null : topInventory(entity.getOpenInventory());
    }

    public static Inventory topInventory(InventoryView view) {
        if (view == null) {
            return null;
        }
        try {
            return (Inventory) TOP_INVENTORY.invoke(view);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public static HumanEntity player(InventoryView view) {
        if (view == null) {
            return null;
        }
        try {
            return (HumanEntity) PLAYER.invoke(view);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public static void setTitle(InventoryView view, String title) {
        if (view == null || SET_TITLE == null) {
            return;
        }
        try {
            SET_TITLE.invoke(view, title);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    private static MethodHandle find(String name, MethodType type) {
        try {
            return MethodHandles.publicLookup().findVirtual(InventoryView.class, name, type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException runtime) {
            return runtime;
        }
        if (t instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(t);
    }
}
