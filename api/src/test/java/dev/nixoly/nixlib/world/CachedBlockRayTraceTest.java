package dev.nixoly.nixlib.world;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CachedBlockRayTraceTest {

    private static final long COOLDOWN = 250_000_000L;

    private static Location eye(double x, double y, double z, float yaw, float pitch) {
        return new Location(null, x, y, z, yaw, pitch);
    }

    @Test
    void probesWhenCacheMissing() {
        assertThat(CachedBlockRayTrace.needsProbe(null, eye(0, 64, 0, 0, 0), System.nanoTime())).isTrue();
    }

    @Test
    void unchangedViewSkipsProbeEvenAfterCooldown() {
        long now = System.nanoTime();
        Location eye = eye(100.5, 64.0, -20.25, 90.0f, -10.0f);
        CachedBlockRayTrace.CacheEntry entry = new CachedBlockRayTrace.CacheEntry(true, now, eye);

        assertThat(CachedBlockRayTrace.needsProbe(entry, eye, now + COOLDOWN * 4)).isFalse();
    }

    @Test
    void changedViewWaitsForCooldown() {
        long now = System.nanoTime();
        CachedBlockRayTrace.CacheEntry entry =
                new CachedBlockRayTrace.CacheEntry(false, now, eye(0, 64, 0, 0, 0));
        Location turned = eye(0, 64, 0, 45.0f, 0);

        assertThat(CachedBlockRayTrace.needsProbe(entry, turned, now + COOLDOWN / 2)).isFalse();
        assertThat(CachedBlockRayTrace.needsProbe(entry, turned, now + COOLDOWN + 1)).isTrue();
    }

    @Test
    void eyeShiftBreaksSnapshotMatch() {
        long now = System.nanoTime();
        CachedBlockRayTrace.CacheEntry entry =
                new CachedBlockRayTrace.CacheEntry(true, now, eye(0, 64, 0, 0, 0));

        assertThat(entry.matchesView(eye(0.2, 64, 0.2, 0, 0))).isTrue();
        assertThat(entry.matchesView(eye(1.0, 64, 0, 0, 0))).isFalse();
        assertThat(entry.matchesView(eye(0, 64, 0, 3.0f, 0))).isFalse();
        assertThat(entry.matchesView(eye(0, 64, 0, 0, -2.5f))).isFalse();
    }

    @Test
    void yawWrapAroundCountsAsSameView() {
        long now = System.nanoTime();
        CachedBlockRayTrace.CacheEntry entry =
                new CachedBlockRayTrace.CacheEntry(true, now, eye(0, 64, 0, 359.5f, 0));

        assertThat(entry.matchesView(eye(0, 64, 0, 0.5f, 0))).isTrue();
    }
}
