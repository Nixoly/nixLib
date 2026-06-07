package dev.nixoly.nixlib.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public interface Scheduler {

    Plugin plugin();

    ScheduledTask runGlobal(Runnable task);

    ScheduledTask runGlobalLater(Runnable task, long delayTicks);

    ScheduledTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks);

    ScheduledTask runAsync(Runnable task);

    ScheduledTask runAsyncLater(Runnable task, long delayTicks);

    ScheduledTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks);

    ScheduledTask runAt(Location location, Runnable task);

    ScheduledTask runAtLater(Location location, Runnable task, long delayTicks);

    ScheduledTask runAtTimer(Location location, Runnable task, long delayTicks, long periodTicks);

    ScheduledTask runFor(Entity entity, Runnable task);

    ScheduledTask runForLater(Entity entity, Runnable task, long delayTicks);

    ScheduledTask runForTimer(Entity entity, Consumer<ScheduledTask> task, long delayTicks, long periodTicks);

    void cancelAll();

    boolean isRegionThreaded();
}
