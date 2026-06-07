package dev.nixoly.nixlib.world;

import dev.nixoly.nixlib.version.ServerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public final class WorldThreadAccess {

    private static final boolean MULTITHREADED = ServerType.detect().isMultithreaded();
    private static final MethodHandle TICK_THREAD_FOR_ENTITY;
    private static final MethodHandle TICK_THREAD_FOR_CHUNK;

    static {
        MethodHandle entityHandle = null;
        MethodHandle chunkHandle = null;
        if (MULTITHREADED) {
            try {
                Class<?> tickThread = Class.forName("ca.spottedleaf.moonrise.common.util.TickThread");
                entityHandle = MethodHandles.publicLookup().unreflect(
                        tickThread.getMethod("isTickThreadFor", Entity.class));
                chunkHandle = MethodHandles.publicLookup().unreflect(
                        tickThread.getMethod("isTickThreadFor", World.class, int.class, int.class));
            } catch (Throwable ignored) {
            }
        }
        TICK_THREAD_FOR_ENTITY = entityHandle;
        TICK_THREAD_FOR_CHUNK = chunkHandle;
    }

    private WorldThreadAccess() {
    }

    public static boolean canReadBlocks(@NotNull Entity entity) {
        if (!MULTITHREADED) {
            return Bukkit.isPrimaryThread();
        }
        return tickThreadForEntity(entity);
    }

    public static boolean canReadBlocks(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        if (!MULTITHREADED) {
            return Bukkit.isPrimaryThread();
        }
        return tickThreadForChunk(world, location.getBlockX(), location.getBlockZ());
    }

    public static boolean isMultithreadedServer() {
        return MULTITHREADED;
    }

    private static boolean tickThreadForEntity(@NotNull Entity entity) {
        MethodHandle handle = TICK_THREAD_FOR_ENTITY;
        if (handle == null) {
            return false;
        }
        try {
            return (boolean) handle.invoke(entity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tickThreadForChunk(@NotNull World world, int blockX, int blockZ) {
        MethodHandle handle = TICK_THREAD_FOR_CHUNK;
        if (handle == null) {
            return false;
        }
        try {
            return (boolean) handle.invoke(world, blockX, blockZ);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
