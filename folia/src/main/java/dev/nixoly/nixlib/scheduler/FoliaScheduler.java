package dev.nixoly.nixlib.scheduler;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class FoliaScheduler implements Scheduler {

    private static final long MS_PER_TICK = 50L;

    private final Plugin plugin;
    private final GlobalRegionScheduler global;
    private final RegionScheduler region;
    private final AsyncScheduler async;
    private final Set<FoliaTask> tracked = ConcurrentHashMap.newKeySet();

    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.global = Bukkit.getGlobalRegionScheduler();
        this.region = Bukkit.getRegionScheduler();
        this.async  = Bukkit.getAsyncScheduler();
    }

    @Override
    public Plugin plugin() { return plugin; }

    @Override
    public boolean isRegionThreaded() { return true; }

    @Override
    public ScheduledTask runGlobal(Runnable task) {
        var raw = global.run(plugin, t -> task.run());
        return new FoliaTask(raw);
    }

    @Override
    public ScheduledTask runGlobalLater(Runnable task, long delayTicks) {
        var raw = global.runDelayed(plugin, t -> task.run(), Math.max(1, delayTicks));
        return new FoliaTask(raw);
    }

    @Override
    public ScheduledTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        var raw = global.runAtFixedRate(plugin, t -> task.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
        return track(new FoliaTask(raw));
    }

    @Override
    public ScheduledTask runAsync(Runnable task) {
        var raw = async.runNow(plugin, t -> task.run());
        return new FoliaTask(raw);
    }

    @Override
    public ScheduledTask runAsyncLater(Runnable task, long delayTicks) {
        var raw = async.runDelayed(plugin, t -> task.run(), Math.max(1, delayTicks) * MS_PER_TICK, TimeUnit.MILLISECONDS);
        return new FoliaTask(raw);
    }

    @Override
    public ScheduledTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        var raw = async.runAtFixedRate(plugin, t -> task.run(),
                Math.max(1, delayTicks) * MS_PER_TICK,
                Math.max(1, periodTicks) * MS_PER_TICK,
                TimeUnit.MILLISECONDS);
        return track(new FoliaTask(raw));
    }

    @Override
    public ScheduledTask runAt(Location location, Runnable task) {
        var raw = region.run(plugin, location, t -> task.run());
        return new FoliaTask(raw);
    }

    @Override
    public ScheduledTask runAtLater(Location location, Runnable task, long delayTicks) {
        var raw = region.runDelayed(plugin, location, t -> task.run(), Math.max(1, delayTicks));
        return new FoliaTask(raw);
    }

    @Override
    public ScheduledTask runAtTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
        var raw = region.runAtFixedRate(plugin, location, t -> task.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
        return track(new FoliaTask(raw));
    }

    @Override
    public ScheduledTask runFor(Entity entity, Runnable task) {
        EntityScheduler es = entity.getScheduler();
        var raw = es.run(plugin, t -> task.run(), null);
        return raw == null ? cancelled() : new FoliaTask(raw);
    }

    @Override
    public ScheduledTask runForLater(Entity entity, Runnable task, long delayTicks) {
        EntityScheduler es = entity.getScheduler();
        var raw = es.runDelayed(plugin, t -> task.run(), null, Math.max(1, delayTicks));
        return raw == null ? cancelled() : new FoliaTask(raw);
    }

    @Override
    public ScheduledTask runForTimer(Entity entity, Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        EntityScheduler es = entity.getScheduler();
        FoliaTask handle = new FoliaTask(null);
        var raw = es.runAtFixedRate(plugin, t -> task.accept(handle), null,
                Math.max(1, delayTicks), Math.max(1, periodTicks));
        if (raw == null) return cancelled();
        handle.bind(raw);
        return track(handle);
    }

    @Override
    public void cancelAll() {
        try {
            global.cancelTasks(plugin);
        } catch (Throwable ignored) {
        }
        try {
            async.cancelTasks(plugin);
        } catch (Throwable ignored) {
        }
        for (FoliaTask task : tracked) {
            task.cancel();
        }
        tracked.clear();
    }

    private FoliaTask track(FoliaTask task) {
        tracked.add(task);
        return task;
    }

    private static ScheduledTask cancelled() {
        return new ScheduledTask() {
            public void cancel() {}
            public boolean isCancelled() { return true; }
            public boolean isRunning() { return false; }
        };
    }

    private final class FoliaTask implements ScheduledTask {
        private volatile io.papermc.paper.threadedregions.scheduler.ScheduledTask raw;
        private volatile boolean cancelled;

        FoliaTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask raw) {
            this.raw = raw;
        }

        void bind(io.papermc.paper.threadedregions.scheduler.ScheduledTask raw) {
            this.raw = raw;
        }

        @Override
        public void cancel() {
            cancelled = true;
            tracked.remove(this);
            if (raw != null) raw.cancel();
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isRunning() {
            return raw != null && !cancelled;
        }
    }
}
