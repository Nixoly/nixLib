package dev.nixoly.nixlib.gui;

import dev.nixoly.nixlib.NixLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class AnvilGui {

    private static final int RESULT_SLOT = 2;

    private static final Map<UUID, AnvilGui> ACTIVE = new ConcurrentHashMap<>();

    static AnvilGui active(UUID playerId) {
        return ACTIVE.get(playerId);
    }

    static void remove(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    private String title;
    private ItemStack inputTemplate;
    private String initialText = "";
    private Function<String, ItemStack> outputProvider;
    private BiConsumer<Player, String> confirmHandler;
    private BiConsumer<Player, String> closeHandler;

    private Player player;
    private AnvilInventory inventory;
    private String currentText = "";
    private boolean confirmed;

    public AnvilGui title(String title) {
        this.title = title;
        return this;
    }

    public AnvilGui initialText(String initialText) {
        this.initialText = initialText == null ? "" : initialText;
        return this;
    }

    public AnvilGui inputItem(ItemStack inputTemplate) {
        this.inputTemplate = inputTemplate;
        return this;
    }

    public AnvilGui output(Function<String, ItemStack> outputProvider) {
        this.outputProvider = outputProvider;
        return this;
    }

    public AnvilGui onConfirm(BiConsumer<Player, String> confirmHandler) {
        this.confirmHandler = confirmHandler;
        return this;
    }

    public AnvilGui onClose(BiConsumer<Player, String> closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

    public String currentText() {
        return currentText;
    }

    public void open(Player player) {
        this.player = player;
        this.currentText = initialText;
        this.confirmed = false;

        GuiNavigation.beginTransition(player.getUniqueId());
        InventoryView view;
        try {
            view = player.openAnvil(player.getLocation(), true);
        } finally {
            GuiNavigation.endTransition(player.getUniqueId());
        }
        if (view == null || !(view.getTopInventory() instanceof AnvilInventory anvil)) {
            return;
        }
        this.inventory = anvil;
        if (title != null && !title.isEmpty()) {
            applyTitle(view, title);
        }
        ACTIVE.put(player.getUniqueId(), this);
        applyItems();
        scheduleApply(player);
    }

    void handlePrepare(PrepareAnvilEvent event) {
        String text = event.getInventory().getRenameText();
        currentText = text == null ? "" : text;
        if (outputProvider != null) {
            event.setResult(outputProvider.apply(currentText));
        }
        event.getInventory().setRepairCost(0);
    }

    void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() != RESULT_SLOT) {
            return;
        }
        confirmed = true;
        if (confirmHandler != null) {
            confirmHandler.accept((Player) event.getWhoClicked(), currentText);
        }
    }

    void handleClose(InventoryCloseEvent event) {
        clearItems();
        if (confirmed) {
            return;
        }
        if (closeHandler != null) {
            closeHandler.accept((Player) event.getPlayer(), currentText);
        }
    }

    private void clearItems() {
        if (inventory == null) {
            return;
        }
        try {
            inventory.setItem(0, null);
            inventory.setItem(1, null);
            inventory.setItem(RESULT_SLOT, null);
        } catch (Throwable ignored) {
        }
    }

    boolean matches(Inventory other) {
        return inventory != null && inventory == other;
    }

    private void applyItems() {
        if (inventory == null) {
            return;
        }
        inventory.setItem(0, buildInput());
        if (outputProvider != null) {
            inventory.setItem(RESULT_SLOT, outputProvider.apply(currentText));
        }
        try {
            inventory.setRepairCost(0);
        } catch (Throwable ignored) {
        }
    }

    private void scheduleApply(Player player) {
        try {
            NixLib.get().scheduler().runForLater(player, this::applyItems, 1L);
        } catch (Throwable ignored) {
        }
    }

    private ItemStack buildInput() {
        ItemStack base = inputTemplate != null ? inputTemplate.clone() : new ItemStack(Material.PAPER);
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(initialText).decoration(TextDecoration.ITALIC, false));
            base.setItemMeta(meta);
        }
        return base;
    }

    private static void applyTitle(InventoryView view, String title) {
        try {
            view.setTitle(title);
        } catch (Throwable ignored) {
        }
    }
}
