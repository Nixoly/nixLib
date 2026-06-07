package dev.nixoly.nixlib;

import dev.nixoly.nixlib.scheduler.Scheduler;
import dev.nixoly.nixlib.scheduler.SchedulerProvider;
import dev.nixoly.nixlib.version.ServerCapabilities;
import dev.nixoly.nixlib.version.ServerType;
import dev.nixoly.nixlib.version.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

public final class NixLib {

    private static volatile NixLib instance;

    private final Plugin plugin;
    private final Scheduler scheduler;
    private final ServerCapabilities capabilities;

    private NixLib(Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = SchedulerProvider.create(plugin);
        this.capabilities = detectCapabilities();
    }

    public static synchronized NixLib bootstrap(Plugin plugin) {
        if (instance != null) {
            throw new IllegalStateException("nixLib already bootstrapped by " + instance.plugin.getName());
        }
        instance = new NixLib(plugin);
        instance.scheduler.plugin().getLogger().info(
                "nixLib started on " + instance.capabilities + " (regionThreaded=" + instance.scheduler.isRegionThreaded() + ")"
        );
        return instance;
    }

    public static synchronized void shutdown() {
        if (instance == null) return;
        instance.scheduler.cancelAll();
        instance = null;
    }

    public static NixLib get() {
        NixLib current = instance;
        if (current == null) {
            throw new IllegalStateException("nixLib not bootstrapped; call NixLib.bootstrap(plugin) in onEnable()");
        }
        return current;
    }

    public static Logger logger() {
        return get().plugin.getLogger();
    }

    public Plugin plugin() {
        return plugin;
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    public ServerCapabilities capabilities() {
        return capabilities;
    }

    private static ServerCapabilities detectCapabilities() {
        ServerType type = ServerType.detect();
        ServerVersion version;
        try {
            version = ServerVersion.parse(Bukkit.getBukkitVersion());
        } catch (Throwable t) {
            version = ServerVersion.V1_20_6;
        }
        return new ServerCapabilities(type, version);
    }
}
