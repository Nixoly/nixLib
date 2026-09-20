package dev.nixoly.nixlib.scheduler;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BukkitSchedulerTest {

    private ServerMock server;
    private MockPlugin plugin;
    private BukkitScheduler scheduler;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("nixlib-test");
        scheduler = new BukkitScheduler(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void runGlobalExecutesNextTick() {
        AtomicInteger n = new AtomicInteger();
        scheduler.runGlobal(n::incrementAndGet);
        server.getScheduler().performOneTick();
        assertThat(n).hasValue(1);
    }

    @Test
    void runGlobalLaterRespectsDelay() {
        AtomicInteger n = new AtomicInteger();
        scheduler.runGlobalLater(n::incrementAndGet, 5);

        for (int i = 0; i < 4; i++) server.getScheduler().performOneTick();
        assertThat(n).hasValue(0);

        server.getScheduler().performOneTick();
        assertThat(n).hasValue(1);
    }

    @Test
    void runGlobalTimerRepeats() {
        AtomicInteger n = new AtomicInteger();
        ScheduledTask t = scheduler.runGlobalTimer(n::incrementAndGet, 1, 2);

        for (int i = 0; i < 7; i++) server.getScheduler().performOneTick();

        t.cancel();
        int observed = n.get();
        assertThat(observed).isBetween(3, 4);
        assertThat(t.isCancelled()).isTrue();
    }

    @Test
    void runAtAndRunForFallBackToGlobal() {
        AtomicInteger n = new AtomicInteger();
        scheduler.runAt(null, n::incrementAndGet);
        scheduler.runFor(null, n::incrementAndGet);
        server.getScheduler().performOneTick();
        assertThat(n).hasValue(2);
        assertThat(scheduler.isRegionThreaded()).isFalse();
    }

    @Test
    void cancelAllStopsPendingTasks() {
        AtomicInteger n = new AtomicInteger();
        scheduler.runGlobalLater(n::incrementAndGet, 10);
        scheduler.cancelAll();
        for (int i = 0; i < 15; i++) server.getScheduler().performOneTick();
        assertThat(n).hasValue(0);
    }

    @Test
    void runGlobalWhenDisabledReturnsDummyAndNoException() {
        MockBukkit.getMock().getPluginManager().disablePlugin(plugin);
        assertThat(plugin.isEnabled()).isFalse();
        ScheduledTask task = scheduler.runGlobal(() -> {});
        assertThat(task.isCancelled()).isTrue();
        ScheduledTask task2 = scheduler.runAsync(() -> {});
        assertThat(task2.isCancelled()).isTrue();
        ScheduledTask task3 = scheduler.runGlobalLater(() -> {}, 5);
        assertThat(task3.isCancelled()).isTrue();
    }

    @Test
    void cancelAllAfterDisableDoesNotThrowAndNotDeadlock() {
        scheduler.runGlobalLater(() -> {}, 10);
        MockBukkit.getMock().getPluginManager().disablePlugin(plugin);
        scheduler.cancelAll();
        scheduler.cancelAll();
    }

    @Test
    void concurrentCancelAllDoesNotDeadlock() throws InterruptedException {
        for (int i = 0; i < 5; i++) scheduler.runGlobalLater(() -> {}, 100);
        Thread t1 = new Thread(scheduler::cancelAll);
        Thread t2 = new Thread(scheduler::cancelAll);
        t1.start();
        t2.start();
        t1.join(2000);
        t2.join(2000);
        assertThat(t1.isAlive()).isFalse();
        assertThat(t2.isAlive()).isFalse();
    }

    @Test
    void oneShotTasksAreNotRetained() {
        Runnable noop = () -> {};
        for (int i = 0; i < 1_000_000; i++) {
            scheduler.runGlobal(noop);
        }
        assertThat(scheduler.trackedCount()).isZero();

        server.getScheduler().performOneTick();
        assertThat(scheduler.trackedCount()).isZero();

        long started = System.nanoTime();
        scheduler.cancelAll();
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        assertThat(scheduler.trackedCount()).isZero();
        assertThat(elapsedMs).isLessThan(1000L);
    }

    @Test
    void repeatingTasksAreTrackedUntilCancelled() {
        ScheduledTask first = scheduler.runGlobalTimer(() -> {}, 1, 20);
        ScheduledTask second = scheduler.runGlobalTimer(() -> {}, 1, 20);
        assertThat(scheduler.trackedCount()).isEqualTo(2);

        first.cancel();
        assertThat(scheduler.trackedCount()).isEqualTo(1);
        assertThat(first.isCancelled()).isTrue();

        scheduler.cancelAll();
        assertThat(scheduler.trackedCount()).isZero();
        assertThat(second.isCancelled()).isTrue();
    }

    @Test
    void runForTimerHandleIsTrackedAndCancelled() {
        AtomicInteger ticks = new AtomicInteger();
        ScheduledTask handle = scheduler.runForTimer(null, task -> ticks.incrementAndGet(), 1, 1);
        assertThat(scheduler.trackedCount()).isEqualTo(1);

        server.getScheduler().performOneTick();
        assertThat(ticks).hasValue(1);

        handle.cancel();
        assertThat(scheduler.trackedCount()).isZero();
    }
}
