package dev.nixoly.nixlib.database;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseTest {

    private HikariDatabase db;

    @BeforeEach
    void setUp() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:h2:mem:nixlibtest-" + java.util.UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        cfg.setMaximumPoolSize(4);
        cfg.setPoolName("nixlib-h2-" + System.nanoTime());
        db = new HikariDatabase(cfg) {
            @Override
            public String dialect() { return "h2"; }
        };

        db.execute("CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(32), level INT)");
    }

    @AfterEach
    void tearDown() {
        if (db != null) db.close();
    }

    @Test
    void insertAndQueryRoundTrip() {
        db.execute("INSERT INTO players (uuid, name, level) VALUES (?, ?, ?)", "u1", "alice", 10);

        Optional<String> name = db.queryOne(
                "SELECT name FROM players WHERE uuid = ?",
                rs -> rs.getString(1),
                "u1"
        );

        assertThat(name).contains("alice");
    }

    @Test
    void queryManyReturnsAllRows() {
        db.execute("INSERT INTO players VALUES ('a', 'aa', 1)");
        db.execute("INSERT INTO players VALUES ('b', 'bb', 2)");
        db.execute("INSERT INTO players VALUES ('c', 'cc', 3)");

        List<Integer> levels = db.queryMany(
                "SELECT level FROM players ORDER BY level",
                rs -> rs.getInt(1)
        );

        assertThat(levels).containsExactly(1, 2, 3);
    }

    @Test
    void updateReturnsAffectedRows() {
        db.execute("INSERT INTO players VALUES ('z', 'zoe', 5)");
        int rows = db.update("UPDATE players SET level = ? WHERE uuid = ?", 99, "z");

        assertThat(rows).isEqualTo(1);
        Integer newLevel = db.queryOne("SELECT level FROM players WHERE uuid = ?", rs -> rs.getInt(1), "z").orElse(-1);
        assertThat(newLevel).isEqualTo(99);
    }

    @Test
    void queryOneEmptyOptionalWhenMissing() {
        Optional<String> result = db.queryOne(
                "SELECT name FROM players WHERE uuid = ?",
                rs -> rs.getString(1),
                "nope"
        );
        assertThat(result).isEmpty();
    }

    @Test
    void asyncExecutesOnBackgroundThread() throws Exception {
        db.executeAsync("INSERT INTO players VALUES ('async', 'a', 1)").get(2, TimeUnit.SECONDS);

        List<String> names = db.queryManyAsync(
                "SELECT name FROM players",
                rs -> rs.getString(1)
        ).get(2, TimeUnit.SECONDS);

        assertThat(names).contains("a");
    }

    @Test
    void queryOneAsyncUsesDatabaseExecutor() throws Exception {
        db.execute("INSERT INTO players VALUES ('async-one', 'one', 7)");

        Optional<Integer> level = db.queryOneAsync(
                "SELECT level FROM players WHERE uuid = ?",
                rs -> rs.getInt(1),
                "async-one"
        ).get(2, TimeUnit.SECONDS);

        assertThat(level).contains(7);
    }

    @Test
    void closeDrainsAcceptedWork() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var work = db.runAsync(() -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
        release.countDown();

        assertThat(db.close(Duration.ofSeconds(1))).isTrue();
        assertThat(work).isCompleted();
    }

    @Test
    void closeStopsWaitingAtDeadline() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        db.runAsync(() -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

        long start = System.nanoTime();
        boolean clean = db.close(Duration.ofMillis(50));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        release.countDown();

        assertThat(clean).isFalse();
        assertThat(elapsedMillis).isLessThan(500L);
    }

    @Test
    void asyncWorkIsRejectedAfterClose() {
        db.close();

        assertThat(db.executeAsync("SELECT 1")).isCompletedExceptionally();
    }

    @Test
    void closeAsyncDoesNotWaitOnCaller() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        db.runAsync(() -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

        var close = db.closeAsync(Duration.ofSeconds(1));
        assertThat(close).isNotDone();
        release.countDown();

        assertThat(close.get(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void closeMarksDatabaseClosed() {
        db.close();
        assertThat(db.isClosed()).isTrue();
    }
}
