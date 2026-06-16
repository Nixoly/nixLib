package dev.nixoly.nixlib.menu;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

final class Warnings {

    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();
    private static volatile Logger logger;

    private Warnings() {
    }

    static void logger(Logger value) {
        logger = value;
    }

    static void once(String key, String message) {
        if (!SEEN.add(key)) {
            return;
        }
        Logger target = logger;
        if (target != null) {
            target.warning(message);
        }
    }
}
