package dev.nixoly.nixlib.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiNavigation {

    private static final Set<UUID> TRANSITIONING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> SUPPRESS_BACK = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> DISPATCHING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Runnable> DEFERRED = new ConcurrentHashMap<>();

    private GuiNavigation() {
    }

    public static void beginTransition(UUID playerId) {
        if (playerId != null) {
            TRANSITIONING.add(playerId);
        }
    }

    public static void endTransition(UUID playerId) {
        if (playerId != null) {
            TRANSITIONING.remove(playerId);
        }
    }

    public static boolean isTransitioning(UUID playerId) {
        return playerId != null && TRANSITIONING.contains(playerId);
    }

    public static void suppressBack(UUID playerId) {
        if (playerId != null) {
            SUPPRESS_BACK.add(playerId);
        }
    }

    public static boolean consumeSuppressBack(UUID playerId) {
        return playerId != null && SUPPRESS_BACK.remove(playerId);
    }

    /**
     * Closes the player's open menu. Inside a menu click handler the close runs on the next tick
     * instead, because closing while the server is still processing the click leaves the client's
     * inventory out of sync.
     */
    public static void close(Player player) {
        Inventory top = InventoryViews.topInventory(player);
        Runnable close = () -> {
            if (InventoryViews.topInventory(player) == top) {
                player.closeInventory();
            }
        };
        if (!deferIfDispatching(player.getUniqueId(), close)) {
            player.closeInventory();
        }
    }

    public static void clear(UUID playerId) {
        if (playerId != null) {
            TRANSITIONING.remove(playerId);
            SUPPRESS_BACK.remove(playerId);
            DISPATCHING.remove(playerId);
            DEFERRED.remove(playerId);
        }
    }

    static void beginDispatch(UUID playerId) {
        DISPATCHING.add(playerId);
    }

    /** Ends a click dispatch and hands back the menu change it requested, if any. */
    static Runnable endDispatch(UUID playerId) {
        DISPATCHING.remove(playerId);
        return DEFERRED.remove(playerId);
    }

    // Only the last request of a click survives: a handler that closes and then opens another menu
    // ends up with the new menu, and spam clicks cannot stack several opens into one tick.
    static boolean deferIfDispatching(UUID playerId, Runnable action) {
        if (!DISPATCHING.contains(playerId)) {
            return false;
        }
        DEFERRED.put(playerId, action);
        return true;
    }
}
