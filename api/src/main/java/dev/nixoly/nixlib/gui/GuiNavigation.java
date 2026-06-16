package dev.nixoly.nixlib.gui;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiNavigation {

    private static final Set<UUID> TRANSITIONING = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> SUPPRESS_BACK = ConcurrentHashMap.newKeySet();

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

    public static void clear(UUID playerId) {
        if (playerId != null) {
            TRANSITIONING.remove(playerId);
            SUPPRESS_BACK.remove(playerId);
        }
    }
}
