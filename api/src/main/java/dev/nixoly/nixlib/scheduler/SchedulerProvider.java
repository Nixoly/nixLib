package dev.nixoly.nixlib.scheduler;

import dev.nixoly.nixlib.version.ServerType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;

public final class SchedulerProvider {

    private SchedulerProvider() {}

    public static Scheduler create(Plugin plugin) {
        ServerType type = ServerType.detect();
        if (type.isMultithreaded()) {
            return tryInstantiate("dev.nixoly.nixlib.scheduler.FoliaScheduler", plugin)
                    .orElseGet(() -> instantiateOrThrow("dev.nixoly.nixlib.scheduler.BukkitScheduler", plugin));
        }
        return instantiateOrThrow("dev.nixoly.nixlib.scheduler.BukkitScheduler", plugin);
    }

    private static java.util.Optional<Scheduler> tryInstantiate(String className, Plugin plugin) {
        try {
            Class<?> cls = Class.forName(className);
            Scheduler s = (Scheduler) cls.getConstructor(Plugin.class).newInstance(plugin);
            return java.util.Optional.of(s);
        } catch (ClassNotFoundException ignored) {
            return java.util.Optional.empty();
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new IllegalStateException("failed to construct scheduler " + className, e);
        }
    }

    private static Scheduler instantiateOrThrow(String className, Plugin plugin) {
        return tryInstantiate(className, plugin)
                .orElseThrow(() -> new IllegalStateException("scheduler implementation not on classpath: " + className));
    }
}
