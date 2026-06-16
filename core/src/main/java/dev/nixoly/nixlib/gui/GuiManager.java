package dev.nixoly.nixlib.gui;

import dev.nixoly.nixlib.NixLib;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.function.Consumer;

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
        AnvilGui anvil = AnvilGui.active(event.getWhoClicked().getUniqueId());
        if (anvil != null && anvil.matches(event.getInventory())) {
            anvil.handleClick(event);
            return;
        }
        Gui gui = resolve(event.getInventory());
        if (gui != null) gui.handleClick(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        AnvilGui anvil = AnvilGui.active(event.getWhoClicked().getUniqueId());
        if (anvil != null && anvil.matches(event.getInventory())) {
            event.setCancelled(true);
            return;
        }
        Gui gui = resolve(event.getInventory());
        if (gui != null) gui.handleDrag(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilGui anvil = AnvilGui.active(event.getView().getPlayer().getUniqueId());
        if (anvil != null && anvil.matches(event.getInventory())) {
            anvil.handlePrepare(event);
        }
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        Gui gui = resolve(event.getInventory());
        if (gui != null) gui.handleOpen(event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        AnvilGui anvil = AnvilGui.active(playerId);
        if (anvil != null && anvil.matches(event.getInventory())) {
            AnvilGui.remove(playerId);
            anvil.handleClose(event);
            return;
        }
        Gui gui = resolve(event.getInventory());
        if (gui == null) return;
        gui.handleClose(event);

        boolean transitioning = GuiNavigation.isTransitioning(playerId);
        boolean suppressed = GuiNavigation.consumeSuppressBack(playerId);
        Consumer<Player> back = gui.backHandler();
        if (transitioning || suppressed || back == null) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        scheduleBack(player, back);
    }

    private void scheduleBack(Player player, Consumer<Player> back) {
        Runnable task = () -> {
            if (player.isOnline()) back.accept(player);
        };
        try {
            NixLib.get().scheduler().runForLater(player, task, 1L);
        } catch (Throwable t) {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    private Gui resolve(Inventory inv) {
        if (inv == null) return null;
        return inv.getHolder() instanceof Gui holder ? holder : null;
    }
}
