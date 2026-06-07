package dev.nixoly.nixlib.gui;

import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public final class GuiItem {

    private final ItemStack item;
    private final Consumer<ClickContext> handler;

    private GuiItem(ItemStack item, Consumer<ClickContext> handler) {
        this.item = item;
        this.handler = handler;
    }

    public static GuiItem of(ItemStack item) {
        return new GuiItem(item, ctx -> {});
    }

    public static GuiItem of(ItemStack item, Consumer<ClickContext> handler) {
        return new GuiItem(item, handler == null ? ctx -> {} : handler);
    }

    public ItemStack stack() {
        return item;
    }

    public void onClick(ClickContext ctx) {
        handler.accept(ctx);
    }
}
