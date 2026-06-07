package dev.nixoly.nixlib.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Gui implements InventoryHolder {

    private final String title;
    private final int rows;
    private final Map<Integer, GuiItem> items = new HashMap<>();
    private final Inventory inventory;

    private boolean cancelByDefault = true;
    private Consumer<ClickContext> globalClickHandler;
    private Consumer<InventoryOpenEvent> openHandler;
    private Consumer<InventoryCloseEvent> closeHandler;

    public Gui(String title, int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("rows must be between 1 and 6, got " + rows);
        }
        this.title = title;
        this.rows = rows;
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public int size() {
        return rows * 9;
    }

    public int rows() {
        return rows;
    }

    public String title() {
        return title;
    }

    public Inventory inventory() {
        return inventory;
    }

    public Gui setItem(int slot, GuiItem item) {
        ensureSlot(slot);
        items.put(slot, item);
        inventory.setItem(slot, item.stack());
        return this;
    }

    public Gui setItem(int slot, ItemStack stack) {
        return setItem(slot, GuiItem.of(stack));
    }

    public Gui setItem(int slot, ItemStack stack, Consumer<ClickContext> handler) {
        return setItem(slot, GuiItem.of(stack, handler));
    }

    public Gui fill(ItemStack stack) {
        for (int i = 0; i < size(); i++) {
            if (!items.containsKey(i)) setItem(i, stack);
        }
        return this;
    }

    public Gui border(ItemStack stack) {
        for (int i = 0; i < 9; i++) setItem(i, stack);
        for (int i = size() - 9; i < size(); i++) setItem(i, stack);
        for (int r = 1; r < rows - 1; r++) {
            setItem(r * 9, stack);
            setItem(r * 9 + 8, stack);
        }
        return this;
    }

    public Gui clear(int slot) {
        ensureSlot(slot);
        items.remove(slot);
        inventory.setItem(slot, null);
        return this;
    }

    public Gui clearAll() {
        items.clear();
        inventory.clear();
        return this;
    }

    public Gui cancelByDefault(boolean cancel) {
        this.cancelByDefault = cancel;
        return this;
    }

    public Gui onAnyClick(Consumer<ClickContext> handler) {
        this.globalClickHandler = handler;
        return this;
    }

    public Gui onOpen(Consumer<InventoryOpenEvent> handler) {
        this.openHandler = handler;
        return this;
    }

    public Gui onClose(Consumer<InventoryCloseEvent> handler) {
        this.closeHandler = handler;
        return this;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    void handleClick(InventoryClickEvent event) {
        if (cancelByDefault) event.setCancelled(true);

        ClickContext ctx = new ClickContext(this, event);
        if (globalClickHandler != null) {
            globalClickHandler.accept(ctx);
        }
        GuiItem item = items.get(event.getRawSlot());
        if (item != null) item.onClick(ctx);
    }

    void handleDrag(InventoryDragEvent event) {
        if (cancelByDefault) event.setCancelled(true);
    }

    void handleOpen(InventoryOpenEvent event) {
        if (openHandler != null) openHandler.accept(event);
    }

    void handleClose(InventoryCloseEvent event) {
        if (closeHandler != null) closeHandler.accept(event);
    }

    public boolean isViewing(HumanEntity entity) {
        return inventory.getViewers().contains(entity);
    }

    private void ensureSlot(int slot) {
        if (slot < 0 || slot >= size()) {
            throw new IndexOutOfBoundsException("slot " + slot + " not in [0, " + size() + ")");
        }
    }
}
