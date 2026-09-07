package dev.nixoly.nixlib.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class BukkitScheduler implements Scheduler {

    private final Plugin plugin;
    private final Set<BukkitWrapper> tracked = Collections.synchronizedSet(new HashSet<>());

    public BukkitScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Plugin plugin() {
        return plugin;
    }

    @Override
    public boolean isRegionThreaded() {
        return false;
    }

    @Override
    public ScheduledTask runGlobal(Runnable task) {
        if (isDisabled()) return cancelledDummy();
        try {
            return wrap(Bukkit.getScheduler().runTask(plugin, task));
        } catch (Throwable ignored) {
            return cancelledDummy();
        }
    }

    @Override
    public ScheduledTask runGlobalLater(Runnable task, long delayTicks) {
        if (isDisabled()) return cancelledDummy();
        try {
            return wrap(Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(1, delayTicks)));
        } catch (Throwable ignored) {
            return cancelledDummy();
        }
    }

    @Override
    public ScheduledTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        if (isDisabled()) return cancelledDummy();
        try {
            return wrap(Bukkit.getScheduler().runTaskTimer(plugin, task, Math.max(1, delayTicks), Math.max(1, periodTicks)));
        } catch (Throwable ignored) {
            return cancelledDummy();
        }
    }

    @Override
    public ScheduledTask runAsync(Runnable task) {
        if (isDisabled()) return cancelledDummy();
        try {
            return wrap(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
        } catch (Throwable ignored) {
            return cancelledDummy();
        }
    }

    @Override
    public ScheduledTask runAsyncLater(Runnable task, long delayTicks) {
        if (isDisabled()) return cancelledDummy();
        try {
            return wrap(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, Math.max(1, delayTicks)));
        } catch (Throwable ignored) {
            return cancelledDummy();
        }
    }

    @Override
    public ScheduledTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        if (isDisabled()) return cancelledDummy();
        try {
            return wrap(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, Math.max(1, delayTicks), Math.max(1, periodTicks)));
        } catch (Throwable ignored) {
            return cancelledDummy();
        }
    }

    @Override
    public ScheduledTask runAt(Location location, Runnable task) {
        return runGlobal(task);
    }

    @Override
    public ScheduledTask runAtLater(Location location, Runnable task, long delayTicks) {
        return runGlobalLater(task, delayTicks);
    }

    @Override
    public ScheduledTask runAtTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
        return runGlobalTimer(task, delayTicks, periodTicks);
    }

    @Override
    public ScheduledTask runFor(Entity entity, Runnable task) {
        return runGlobal(task);
    }

    @Override
    public ScheduledTask runForLater(Entity entity, Runnable task, long delayTicks) {
        return runGlobalLater(task, delayTicks);
    }

    @Override
    public ScheduledTask runForTimer(Entity entity, Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        if (isDisabled()) return cancelledDummy();
        try {
            BukkitTimerHandle handle = new BukkitTimerHandle();
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    () -> task.accept(handle),
                    Math.max(1, delayTicks),
                    Math.max(1, periodTicks)
            );
            handle.attach(bukkitTask);
            tracked.add(handle);
            return handle;
        } catch (Throwable ignored) {
            return cancelledDummy();
        }
    }

    @Override
    public void cancelAll() {
        Set<BukkitWrapper> copy;
        synchronized (tracked) {
            copy = new HashSet<>(tracked);
            tracked.clear();
        }
        // Fix: deadlock engelle - HazeBox 20:52:26 Watchdog Locked on cancelAll, tracked lock'u Bukkit scheduler call oncesi birak
        for (BukkitWrapper w : copy) {
            try {
                w.cancel();
            } catch (Throwable ignored) {}
        }
        try {
            if (plugin != null) {
                Bukkit.getScheduler().cancelTasks(plugin);
            }
        } catch (Throwable ignored) {
            // Plugin already disabled - IllegalPluginAccessException yutulur, spam engellenir
        }
    }

    private BukkitWrapper wrap(BukkitTask task) {
        BukkitWrapper w = new BukkitWrapper(task);
        tracked.add(w);
        return w;
    }

    private boolean isDisabled() {
        try {
            return plugin == null || !plugin.isEnabled();
        } catch (Throwable ignored) {
            return true;
        }
    }

    private ScheduledTask cancelledDummy() {
        BukkitWrapper dummy = new BukkitWrapper(null);
        dummy.cancel();
        return dummy;
    }

    private static class BukkitWrapper implements ScheduledTask {
        protected BukkitTask task;
        protected volatile boolean cancelled;

        BukkitWrapper(BukkitTask task) { this.task = task; }

        @Override
        public void cancel() {
            cancelled = true;
            if (task != null && !task.isCancelled()) task.cancel();
        }

        @Override
        public boolean isCancelled() { return cancelled || (task != null && task.isCancelled()); }

        @Override
        public boolean isRunning() { return task != null && !isCancelled(); }
    }

    private static final class BukkitTimerHandle extends BukkitWrapper {
        BukkitTimerHandle() { super(null); }
        void attach(BukkitTask task) { this.task = task; }
    }
}
