package dev.nixoly.nixlib.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class BukkitScheduler implements Scheduler {

    private final Plugin plugin;
    private final Set<BukkitWrapper> tracked = ConcurrentHashMap.newKeySet();

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
            return new BukkitWrapper(Bukkit.getScheduler().runTask(plugin, task));
        } catch (Throwable ignored) {
            return cancelledDummy();
        }
    }

    @Override
    public ScheduledTask runGlobalLater(Runnable task, long delayTicks) {
        if (isDisabled()) return cancelledDummy();
        try {
            return new BukkitWrapper(Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(1, delayTicks)));
        } catch (Throwable ignored) {
            return cancelledDummy();
        }
    }

    @Override
    public ScheduledTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        if (isDisabled()) return cancelledDummy();
        try {
            return track(new BukkitWrapper(Bukkit.getScheduler().runTaskTimer(
                    plugin, task, Math.max(1, delayTicks), Math.max(1, periodTicks))));
        } catch (Throwable ignored) {
            return cancelledDummy();
        }
    }

    @Override
    public ScheduledTask runAsync(Runnable task) {
        if (isDisabled()) return cancelledDummy();
        try {
            return new BukkitWrapper(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
        } catch (Throwable ignored) {
            return cancelledDummy();
        }
    }

    @Override
    public ScheduledTask runAsyncLater(Runnable task, long delayTicks) {
        if (isDisabled()) return cancelledDummy();
        try {
            return new BukkitWrapper(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, Math.max(1, delayTicks)));
        } catch (Throwable ignored) {
            return cancelledDummy();
        }
    }

    @Override
    public ScheduledTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        if (isDisabled()) return cancelledDummy();
        try {
            return track(new BukkitWrapper(Bukkit.getScheduler().runTaskTimerAsynchronously(
                    plugin, task, Math.max(1, delayTicks), Math.max(1, periodTicks))));
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
            return track(handle);
        } catch (Throwable ignored) {
            return cancelledDummy();
        }
    }

    @Override
    public void cancelAll() {
        try {
            if (plugin != null) Bukkit.getScheduler().cancelTasks(plugin);
        } catch (Throwable ignored) {
        }
        tracked.clear();
    }

    int trackedCount() {
        return tracked.size();
    }

    private BukkitWrapper track(BukkitWrapper wrapper) {
        tracked.add(wrapper);
        return wrapper;
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

    private class BukkitWrapper implements ScheduledTask {
        protected BukkitTask task;
        protected volatile boolean cancelled;

        BukkitWrapper(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            cancelled = true;
            tracked.remove(this);
            if (task != null && !task.isCancelled()) task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return cancelled || (task != null && task.isCancelled());
        }

        @Override
        public boolean isRunning() {
            return task != null && !isCancelled();
        }
    }

    private final class BukkitTimerHandle extends BukkitWrapper {
        BukkitTimerHandle() {
            super(null);
        }

        void attach(BukkitTask task) {
            this.task = task;
        }
    }
}
