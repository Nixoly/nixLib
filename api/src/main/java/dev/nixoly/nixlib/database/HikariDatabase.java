package dev.nixoly.nixlib.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public abstract class HikariDatabase implements Database {

    private static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(2);

    protected final HikariDataSource ds;
    private final ExecutorService asyncExecutor;
    private final Set<CompletableFuture<?>> pending = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean dataSourceCloseStarted = new AtomicBoolean();
    private final CountDownLatch dataSourceClosed = new CountDownLatch(1);
    private final AtomicReference<CompletableFuture<Boolean>> asyncClose = new AtomicReference<>();
    private final String poolName;

    protected HikariDatabase(HikariConfig config) {
        this.ds = new HikariDataSource(config);
        this.poolName = config.getPoolName() == null ? "database" : config.getPoolName();
        int workerCount = Math.max(1, Math.min(4, config.getMaximumPoolSize()));
        AtomicInteger threadId = new AtomicInteger();
        this.asyncExecutor = Executors.newFixedThreadPool(
                workerCount,
                r -> {
                    Thread t = new Thread(r, "nixlib-db-" + poolName + "-" + threadId.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    @Override
    public Connection borrow() throws SQLException {
        return ds.getConnection();
    }

    @Override
    public void execute(String sql, Object... params) {
        try (Connection c = borrow(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            ps.execute();
        } catch (SQLException e) {
            throw new DatabaseException("execute failed: " + sql, e);
        }
    }

    @Override
    public int update(String sql, Object... params) {
        try (Connection c = borrow(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("update failed: " + sql, e);
        }
    }

    @Override
    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = borrow(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(mapper.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("queryOne failed: " + sql, e);
        }
    }

    @Override
    public <T> List<T> queryMany(String sql, RowMapper<T> mapper, Object... params) {
        List<T> out = new ArrayList<>();
        try (Connection c = borrow(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapper.map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("queryMany failed: " + sql, e);
        }
        return out;
    }

    @Override
    public CompletableFuture<Void> executeAsync(String sql, Object... params) {
        return submitAsync(() -> {
            execute(sql, params);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable task) {
        return submitAsync(() -> {
            task.run();
            return null;
        });
    }

    @Override
    public <T> CompletableFuture<Optional<T>> queryOneAsync(
            String sql, RowMapper<T> mapper, Object... params) {
        return submitAsync(() -> queryOne(sql, mapper, params));
    }

    @Override
    public <T> CompletableFuture<List<T>> queryManyAsync(String sql, RowMapper<T> mapper, Object... params) {
        return submitAsync(() -> queryMany(sql, mapper, params));
    }

    @Override
    public void close() {
        close(DEFAULT_CLOSE_TIMEOUT);
    }

    @Override
    public CompletableFuture<Boolean> closeAsync(Duration timeout) {
        CompletableFuture<Boolean> existing = asyncClose.get();
        if (existing != null) {
            return existing;
        }
        CompletableFuture<Boolean> created = new CompletableFuture<>();
        if (!asyncClose.compareAndSet(null, created)) {
            return asyncClose.get();
        }
        Thread closeThread = new Thread(() -> {
            try {
                created.complete(close(timeout));
            } catch (Throwable t) {
                created.completeExceptionally(t);
            }
        }, "nixlib-db-shutdown");
        closeThread.setDaemon(true);
        closeThread.start();
        return created;
    }

    @Override
    public boolean close(Duration timeout) {
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        long timeoutNanos = timeout.toNanos();
        long deadline = deadlineAfter(timeoutNanos);
        if (closed.compareAndSet(false, true)) {
            asyncExecutor.shutdown();
        }

        boolean drained = awaitExecutor(deadline);
        if (!drained) {
            asyncExecutor.shutdownNow();
            RejectedExecutionException failure = new RejectedExecutionException(
                    "Database closed before queued work completed: " + poolName);
            for (CompletableFuture<?> future : pending) {
                future.completeExceptionally(failure);
            }
        }

        startDataSourceClose();
        return drained && awaitDataSource(deadline);
    }

    public boolean isClosed() {
        return closed.get();
    }

    private <T> CompletableFuture<T> submitAsync(Supplier<T> task) {
        if (closed.get()) {
            return CompletableFuture.failedFuture(
                    new RejectedExecutionException("Database is closed: " + poolName));
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        pending.add(future);
        try {
            asyncExecutor.execute(() -> {
                try {
                    future.complete(task.get());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                } finally {
                    pending.remove(future);
                }
            });
        } catch (RejectedExecutionException e) {
            pending.remove(future);
            future.completeExceptionally(e);
        }
        return future;
    }

    private boolean awaitExecutor(long deadline) {
        if (asyncExecutor.isTerminated()) {
            return true;
        }
        long remaining = remainingNanos(deadline);
        if (remaining <= 0L) {
            return false;
        }
        try {
            return asyncExecutor.awaitTermination(remaining, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void startDataSourceClose() {
        if (!dataSourceCloseStarted.compareAndSet(false, true)) {
            return;
        }
        Thread closeThread = new Thread(() -> {
            try {
                ds.close();
            } finally {
                dataSourceClosed.countDown();
            }
        }, "nixlib-db-close");
        closeThread.setDaemon(true);
        closeThread.start();
    }

    private boolean awaitDataSource(long deadline) {
        if (dataSourceClosed.getCount() == 0L) {
            return true;
        }
        long remaining = remainingNanos(deadline);
        if (remaining <= 0L) {
            return false;
        }
        try {
            return dataSourceClosed.await(remaining, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static long deadlineAfter(long timeoutNanos) {
        long now = System.nanoTime();
        long deadline = now + timeoutNanos;
        return deadline < now ? Long.MAX_VALUE : deadline;
    }

    private static long remainingNanos(long deadline) {
        if (deadline == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, deadline - System.nanoTime());
    }

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
