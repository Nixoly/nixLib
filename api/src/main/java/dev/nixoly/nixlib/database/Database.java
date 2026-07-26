package dev.nixoly.nixlib.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface Database extends AutoCloseable {

    Connection borrow() throws SQLException;

    void execute(String sql, Object... params);

    int update(String sql, Object... params);

    <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params);

    <T> List<T> queryMany(String sql, RowMapper<T> mapper, Object... params);

    CompletableFuture<Void> executeAsync(String sql, Object... params);

    /** Runs a multi-statement unit on this database's asynchronous execution path. */
    default CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task);
    }

    /** Asynchronous counterpart of {@link #queryOne(String, RowMapper, Object...)}. */
    default <T> CompletableFuture<Optional<T>> queryOneAsync(
            String sql, RowMapper<T> mapper, Object... params) {
        return CompletableFuture.supplyAsync(() -> queryOne(sql, mapper, params));
    }

    <T> CompletableFuture<List<T>> queryManyAsync(String sql, RowMapper<T> mapper, Object... params);

    String dialect();

    /** Starts database shutdown without holding the calling thread. */
    default CompletableFuture<Boolean> closeAsync(Duration timeout) {
        return CompletableFuture.supplyAsync(() -> close(timeout));
    }

    /**
     * Requests shutdown within a deadline.
     *
     * @return true when queued work and resources closed before the deadline
     */
    default boolean close(Duration timeout) {
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        CompletableFuture<Void> close = CompletableFuture.runAsync(this::close);
        try {
            close.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
            return true;
        } catch (TimeoutException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }
    }

    @Override
    void close();
}
