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
        return wrap(Bukkit.getScheduler().runTask(plugin, task));
    }

    @Override
    public ScheduledTask runGlobalLater(Runnable task, long delayTicks) {
        return wrap(Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(1, delayTicks)));
    }

    @Override
    public ScheduledTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        return wrap(Bukkit.getScheduler().runTaskTimer(plugin, task, Math.max(1, delayTicks), Math.max(1, periodTicks)));
    }

    @Override
    public ScheduledTask runAsync(Runnable task) {
        return wrap(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
    }

    @Override
    public ScheduledTask runAsyncLater(Runnable task, long delayTicks) {
        return wrap(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, Math.max(1, delayTicks)));
    }

    @Override
    public ScheduledTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return wrap(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, Math.max(1, delayTicks), Math.max(1, periodTicks)));
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
    }

    @Override
    public void cancelAll() {
        Bukkit.getScheduler().cancelTasks(plugin);
        synchronized (tracked) {
            tracked.forEach(BukkitWrapper::cancel);
            tracked.clear();
        }
    }

    private BukkitWrapper wrap(BukkitTask task) {
        BukkitWrapper w = new BukkitWrapper(task);
        tracked.add(w);
        return w;
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
