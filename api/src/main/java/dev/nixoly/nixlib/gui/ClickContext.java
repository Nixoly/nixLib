package dev.nixoly.nixlib.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class ClickContext {

    private final InventoryClickEvent event;
    private final Gui gui;

    ClickContext(Gui gui, InventoryClickEvent event) {
        this.gui = gui;
        this.event = event;
    }

    public Player player() {
        return (Player) event.getWhoClicked();
    }

    public Gui gui() {
        return gui;
    }

    public InventoryClickEvent event() {
        return event;
    }

    public ClickType clickType() {
        return event.getClick();
    }

    public InventoryAction action() {
        return event.getAction();
    }

    public int slot() {
        return event.getRawSlot();
    }

    public ItemStack cursor() {
        return event.getCursor();
    }

    public boolean isLeftClick() {
        return event.getClick() == ClickType.LEFT;
    }

    public boolean isRightClick() {
        return event.getClick() == ClickType.RIGHT;
    }

    public boolean isShiftClick() {
        return event.getClick().isShiftClick();
    }

    public void close() {
        player().closeInventory();
    }
}
