package dev.nixoly.nixlib.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;

public class Gui implements InventoryHolder {

    private static final long DEFAULT_CLICK_COOLDOWN_NANOS = Duration.ofMillis(150).toNanos();
    private static final boolean SNAPSHOT_FREE_HOLDER = hasSnapshotFreeHolder();

    private final String title;
    private final int rows;
    private final GuiItem[] items;
    private final Inventory inventory;

    private boolean cancelByDefault = true;
    private long clickCooldownNanos = DEFAULT_CLICK_COOLDOWN_NANOS;
    private Consumer<ClickContext> globalClickHandler;
    private Consumer<InventoryOpenEvent> openHandler;
    private Consumer<InventoryCloseEvent> closeHandler;
    private Consumer<Player> backHandler;

    public Gui(String title, int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("rows must be between 1 and 6, got " + rows);
        }
        this.title = title;
        this.rows = rows;
        this.items = new GuiItem[rows * 9];
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    /**
     * The menu behind {@code inventory}, or null. Unlike {@code Inventory#getHolder()} on Paper this
     * never copies a block state, so it is cheap to call for every chest click on the server.
     */
    public static Gui from(Inventory inventory) {
        if (inventory == null || inventory.getType() != InventoryType.CHEST) {
            return null;
        }
        InventoryHolder holder = SNAPSHOT_FREE_HOLDER ? inventory.getHolder(false) : inventory.getHolder();
        return holder instanceof Gui gui ? gui : null;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public int size() {
        return items.length;
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
        items[slot] = item;
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
        GuiItem filler = GuiItem.of(stack);
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null) setItem(i, filler);
        }
        return this;
    }

    public Gui border(ItemStack stack) {
        GuiItem edge = GuiItem.of(stack);
        int lastRow = items.length - 9;
        for (int i = 0; i < 9; i++) {
            setItem(i, edge);
            setItem(lastRow + i, edge);
        }
        for (int r = 1; r < rows - 1; r++) {
            setItem(r * 9, edge);
            setItem(r * 9 + 8, edge);
        }
        return this;
    }

    public Gui clear(int slot) {
        ensureSlot(slot);
        items[slot] = null;
        inventory.setItem(slot, null);
        return this;
    }

    public Gui clearAll() {
        Arrays.fill(items, null);
        inventory.clear();
        return this;
    }

    public Gui cancelByDefault(boolean cancel) {
        this.cancelByDefault = cancel;
        return this;
    }

    /**
     * Minimum time between two handled clicks from the same player, 150 ms by default. Clicks inside
     * the window are still cancelled but run no handlers, so spam clicking a button cannot queue up
     * work. {@link Duration#ZERO} turns the limit off.
     */
    public Gui clickCooldown(Duration cooldown) {
        if (cooldown.isNegative()) {
            throw new IllegalArgumentException("click cooldown must not be negative");
        }
        this.clickCooldownNanos = cooldown.toNanos();
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

    public Gui onBack(Consumer<Player> handler) {
        this.backHandler = handler;
        return this;
    }

    Consumer<Player> backHandler() {
        return backHandler;
    }

    long clickCooldownNanos() {
        return clickCooldownNanos;
    }

    /**
     * Shows the menu. Called from a click handler, the open happens on the next tick; several opens
     * requested by the same click collapse into the last one.
     */
    public void open(Player player) {
        if (!GuiNavigation.deferIfDispatching(player.getUniqueId(), () -> openNow(player))) {
            openNow(player);
        }
    }

    private void openNow(Player player) {
        if (!player.isOnline() || InventoryViews.topInventory(player) == inventory) {
            return;
        }
        UUID playerId = player.getUniqueId();
        GuiNavigation.beginTransition(playerId);
        try {
            player.openInventory(inventory);
        } finally {
            GuiNavigation.endTransition(playerId);
        }
    }

    void handleClick(InventoryClickEvent event) {
        cancelClick(event);
        int slot = event.getRawSlot();
        GuiItem item = slot >= 0 && slot < items.length ? items[slot] : null;
        boolean itemClickable = item != null && item.clickable();
        if (globalClickHandler == null && !itemClickable) {
            return;
        }
        ClickContext ctx = new ClickContext(this, event);
        if (globalClickHandler != null) {
            globalClickHandler.accept(ctx);
        }
        if (itemClickable) {
            item.onClick(ctx);
        }
    }

    void cancelClick(InventoryClickEvent event) {
        if (cancelByDefault) event.setCancelled(true);
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
        if (slot < 0 || slot >= items.length) {
            throw new IndexOutOfBoundsException("slot " + slot + " not in [0, " + items.length + ")");
        }
    }

    // Paper's getHolder(false) skips the block state copy; Spigot only has the copying variant.
    private static boolean hasSnapshotFreeHolder() {
        try {
            Inventory.class.getMethod("getHolder", boolean.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
