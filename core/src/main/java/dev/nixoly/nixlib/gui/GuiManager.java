package dev.nixoly.nixlib.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public final class GuiManager implements Listener {

    private static GuiManager instance;

    private final Plugin plugin;

    private GuiManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public static synchronized GuiManager register(Plugin plugin) {
        if (instance != null) return instance;
        instance = new GuiManager(plugin);
        plugin.getServer().getPluginManager().registerEvents(instance, plugin);
        return instance;
    }

    public static synchronized void unregister() {
        if (instance == null) return;
        HandlerList.unregisterAll(instance);
        instance = null;
    }

    public Plugin plugin() {
        return plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        Gui gui = resolve(event.getInventory());
        if (gui != null) gui.handleClick(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        Gui gui = resolve(event.getInventory());
        if (gui != null) gui.handleDrag(event);
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        Gui gui = resolve(event.getInventory());
        if (gui != null) gui.handleOpen(event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Gui gui = resolve(event.getInventory());
        if (gui != null) gui.handleClose(event);
    }

    private Gui resolve(Inventory inv) {
        if (inv == null) return null;
        return inv.getHolder() instanceof Gui holder ? holder : null;
    }
}
