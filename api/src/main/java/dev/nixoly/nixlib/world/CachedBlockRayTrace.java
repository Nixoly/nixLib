package dev.nixoly.nixlib.world;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import dev.nixoly.nixlib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CachedBlockRayTrace {

    private static final long FRESH_NANOS = 100_000_000L;
    private static final long REFRESH_COOLDOWN_NANOS = 75_000_000L;

    private static final Map<UUID, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, AtomicBoolean> REFRESH_PENDING = new ConcurrentHashMap<>();

    private CachedBlockRayTrace() {
    }

    public static boolean solidBlockInReach(@NotNull LivingEntity entity, double reach) {
        CacheEntry entry = CACHE.get(entity.getUniqueId());
        if (entry != null && entry.isFresh(FRESH_NANOS)) {
            return entry.blockInReach;
        }
        return true;
    }

    public static void refreshIfStale(@NotNull Scheduler scheduler, @NotNull Entity entity, double reach) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        UUID uuid = entity.getUniqueId();
        if (WorldThreadAccess.canReadBlocks(entity)) {
            putCache(uuid, probe(living, reach));
            return;
        }
        CacheEntry entry = CACHE.get(uuid);
        long now = System.nanoTime();
        if (entry != null && entry.isFresh(REFRESH_COOLDOWN_NANOS)) {
            return;
        }
        AtomicBoolean pending = REFRESH_PENDING.computeIfAbsent(uuid, ignored -> new AtomicBoolean());
        if (!pending.compareAndSet(false, true)) {
            return;
        }
        scheduler.runFor(entity, () -> {
            try {
                if (entity.isValid() && entity instanceof LivingEntity validLiving) {
                    putCache(uuid, probe(validLiving, reach));
                }
            } finally {
                pending.set(false);
            }
        });
    }

    public static void clear(@NotNull UUID uuid) {
        CACHE.remove(uuid);
        REFRESH_PENDING.remove(uuid);
    }

    private static void putCache(@NotNull UUID uuid, boolean blockInReach) {
        CACHE.put(uuid, new CacheEntry(blockInReach, System.nanoTime()));
    }

    private static boolean probe(@NotNull LivingEntity entity, double reach) {
        Location eye = entity.getEyeLocation();
        World world = eye.getWorld();
        if (world == null) {
            return false;
        }
        Vector direction = eye.getDirection();
        try {
            RayTraceResult hit = world.rayTraceBlocks(eye, direction, reach,
                    FluidCollisionMode.NEVER, true);
            if (hit == null) {
                return false;
            }
            Block block = hit.getHitBlock();
            return block != null && !block.getType().isAir();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class CacheEntry {
        private final boolean blockInReach;
        private final long updatedAt;

        private CacheEntry(boolean blockInReach, long updatedAt) {
            this.blockInReach = blockInReach;
            this.updatedAt = updatedAt;
        }

        private boolean isFresh(long maxAgeNanos) {
            return (System.nanoTime() - updatedAt) <= maxAgeNanos;
        }
    }
}
