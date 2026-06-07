package dev.nixoly.nixlib.version;

public enum ServerType {
    BUKKIT,
    SPIGOT,
    PAPER,
    FOLIA,
    CANVAS,
    UNKNOWN;

    public boolean isPaperBased() {
        return this == PAPER || this == FOLIA || this == CANVAS;
    }

    public boolean isMultithreaded() {
        return this == FOLIA || this == CANVAS;
    }

    public static ServerType detect() {
        if (classPresent("io.canvasmc.canvas.Config")) {
            return CANVAS;
        }
        if (classPresent("io.papermc.paper.threadedregions.RegionizedServer")) {
            return FOLIA;
        }
        if (classPresent("com.destroystokyo.paper.PaperConfig") || classPresent("io.papermc.paper.configuration.Configuration")) {
            return PAPER;
        }
        if (classPresent("org.spigotmc.SpigotConfig")) {
            return SPIGOT;
        }
        if (classPresent("org.bukkit.Bukkit")) {
            return BUKKIT;
        }
        return UNKNOWN;
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name, false, ServerType.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
