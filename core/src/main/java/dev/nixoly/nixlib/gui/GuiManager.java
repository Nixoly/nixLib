package dev.nixoly.nixlib.gui;

import dev.nixoly.nixlib.NixLib;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class GuiManager implements Listener {

    private static GuiManager instance;

    private final Plugin plugin;
    private final Map<UUID, Long> lastHandledClick = new ConcurrentHashMap<>();

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
        instance.lastHandledClick.clear();
        instance = null;
    }

    public Plugin plugin() {
        return plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        HumanEntity who = event.getWhoClicked();
        AnvilGui anvil = AnvilGui.active(who.getUniqueId());
        if (anvil != null && anvil.matches(event.getInventory())) {
            anvil.handleClick(event);
            return;
        }
        Gui gui = Gui.from(event.getInventory());
        if (gui == null) return;
        if (!(who instanceof Player player) || throttled(gui, event, player.getUniqueId())) {
            gui.cancelClick(event);
            return;
        }

        UUID playerId = player.getUniqueId();
        GuiNavigation.beginDispatch(playerId);
        try {
            gui.handleClick(event);
        } finally {
            Runnable deferred = GuiNavigation.endDispatch(playerId);
            if (deferred != null) runNextTick(player, deferred);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        AnvilGui anvil = AnvilGui.active(event.getWhoClicked().getUniqueId());
        if (anvil != null && anvil.matches(event.getInventory())) {
            event.setCancelled(true);
            return;
        }
        Gui gui = Gui.from(event.getInventory());
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
        Gui gui = Gui.from(event.getInventory());
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
        Gui gui = Gui.from(event.getInventory());
        if (gui == null) return;
        gui.handleClose(event);

        boolean suppressed = GuiNavigation.consumeSuppressBack(playerId);
        if (GuiNavigation.isTransitioning(playerId) || !(event.getPlayer() instanceof Player player)) return;

        Consumer<Player> back = gui.backHandler();
        if (back != null && !suppressed) {
            runNextTick(player, () -> back.accept(player));
            return;
        }
        // The client predicts every click it made in the menu. Once the menu is gone, send the real
        // inventory and cursor so nothing predicted survives into the player's own inventory.
        runNextTick(player, player::updateInventory);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastHandledClick.remove(playerId);
        GuiNavigation.clear(playerId);
        AnvilGui.remove(playerId);
    }

    // A double click always follows a regular click on the same slot, which already ran the handler.
    private boolean throttled(Gui gui, InventoryClickEvent event, UUID playerId) {
        if (event.getClick() == ClickType.DOUBLE_CLICK) return true;
        long cooldown = gui.clickCooldownNanos();
        int slot = event.getRawSlot();
        if (cooldown <= 0L || slot < 0 || slot >= gui.size()) return false;

        long now = System.nanoTime();
        Long last = lastHandledClick.get(playerId);
        if (last != null && now - last < cooldown) return true;
        lastHandledClick.put(playerId, now);
        return false;
    }

    private void runNextTick(Player player, Runnable task) {
        Runnable guarded = () -> {
            if (player.isOnline()) task.run();
        };
        if (NixLib.isBootstrapped()) {
            NixLib.get().scheduler().runForLater(player, guarded, 1L);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, guarded);
        }
    }
}
