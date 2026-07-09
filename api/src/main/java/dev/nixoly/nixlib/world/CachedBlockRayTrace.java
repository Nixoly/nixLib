package dev.nixoly.nixlib.world;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import dev.nixoly.nixlib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CachedBlockRayTrace {

    private static final long FRESH_NANOS = 300_000_000L;
    private static final long REFRESH_COOLDOWN_NANOS = 250_000_000L;
    private static final float VIEW_ANGLE_EPSILON_DEG = 2.0f;
    private static final double EYE_SHIFT_EPSILON_SQ = 0.25;

    private static final Map<UUID, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, AtomicBoolean> REFRESH_PENDING = new ConcurrentHashMap<>();

    private CachedBlockRayTrace() {
    }

    public static boolean solidBlockInReach(@NotNull LivingEntity entity, double reach) {
        CacheEntry entry = CACHE.get(entity.getUniqueId());
        if (entry == null) {
            return true;
        }
        if (entry.isFresh(FRESH_NANOS)) {
            return entry.blockInReach;
        }
        // A stale entry is still valid while the player keeps looking the same way.
        if (entry.matchesView(entity.getEyeLocation())) {
            return entry.blockInReach;
        }
        return true;
    }

    public static void refreshIfStale(@NotNull Scheduler scheduler, @NotNull Entity entity, double reach) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        UUID uuid = entity.getUniqueId();
        if (!needsProbe(CACHE.get(uuid), living.getEyeLocation(), System.nanoTime())) {
            return;
        }
        if (WorldThreadAccess.canReadBlocks(entity)) {
            probeAndCache(uuid, living, reach);
            return;
        }
        AtomicBoolean pending = REFRESH_PENDING.computeIfAbsent(uuid, ignored -> new AtomicBoolean());
        if (!pending.compareAndSet(false, true)) {
            return;
        }
        scheduler.runFor(entity, () -> {
            try {
                if (entity.isValid() && entity instanceof LivingEntity validLiving
                        && needsProbe(CACHE.get(uuid), validLiving.getEyeLocation(), System.nanoTime())) {
                    probeAndCache(uuid, validLiving, reach);
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

    static boolean needsProbe(CacheEntry entry, @NotNull Location eye, long now) {
        if (entry == null) {
            return true;
        }
        if (entry.matchesView(eye)) {
            return false;
        }
        return (now - entry.updatedAt) > REFRESH_COOLDOWN_NANOS;
    }

    private static void probeAndCache(@NotNull UUID uuid, @NotNull LivingEntity entity, double reach) {
        Location eye = entity.getEyeLocation();
        World world = eye.getWorld();
        boolean blockInReach = world != null && rayTraceHitsSolid(world, eye, reach);
        CACHE.put(uuid, new CacheEntry(blockInReach, System.nanoTime(), eye));
    }

    private static boolean rayTraceHitsSolid(@NotNull World world, @NotNull Location eye, double reach) {
        try {
            RayTraceResult hit = world.rayTraceBlocks(eye, eye.getDirection(), reach,
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

    private static float angleDelta(float a, float b) {
        float delta = Math.abs(a - b) % 360.0f;
        return delta > 180.0f ? 360.0f - delta : delta;
    }

    static final class CacheEntry {
        private final boolean blockInReach;
        private final long updatedAt;
        private final float yaw;
        private final float pitch;
        private final double eyeX;
        private final double eyeY;
        private final double eyeZ;

        CacheEntry(boolean blockInReach, long updatedAt, @NotNull Location eye) {
            this.blockInReach = blockInReach;
            this.updatedAt = updatedAt;
            this.yaw = eye.getYaw();
            this.pitch = eye.getPitch();
            this.eyeX = eye.getX();
            this.eyeY = eye.getY();
            this.eyeZ = eye.getZ();
        }

        private boolean isFresh(long maxAgeNanos) {
            return (System.nanoTime() - updatedAt) <= maxAgeNanos;
        }

        boolean matchesView(@NotNull Location eye) {
            if (angleDelta(yaw, eye.getYaw()) >= VIEW_ANGLE_EPSILON_DEG
                    || Math.abs(pitch - eye.getPitch()) >= VIEW_ANGLE_EPSILON_DEG) {
                return false;
            }
            double dx = eye.getX() - eyeX;
            double dy = eye.getY() - eyeY;
            double dz = eye.getZ() - eyeZ;
            return dx * dx + dy * dy + dz * dz < EYE_SHIFT_EPSILON_SQ;
        }
    }
}
