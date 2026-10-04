package dev.nixoly.nixlib.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    /**
     * Runs an insert and returns the first generated key, or -1 when the driver reports none.
     * PostgreSQL hands back the whole inserted row as "generated keys", so there the value comes
     * from the table's first column.
     */
    default long insert(String sql, Object... params) {
        try (Connection c = borrow(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Statements.bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    return -1L;
                }
                long key = keys.getLong(1);
                return keys.wasNull() ? -1L : key;
            }
        } catch (SQLException e) {
            throw new DatabaseException("insert failed: " + sql, e);
        }
    }

    /**
     * Sends {@code sql} once per row as one JDBC batch inside a single transaction: every row applies
     * or none does. Drivers that rewrite batches (MySQL, PostgreSQL) may report
     * {@link Statement#SUCCESS_NO_INFO} instead of per-row counts.
     */
    default int[] batch(String sql, List<Object[]> rows) {
        if (rows.isEmpty()) {
            return new int[0];
        }
        return inTransaction("batch failed: " + sql, c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (Object[] row : rows) {
                    Statements.bind(ps, row);
                    ps.addBatch();
                }
                return ps.executeBatch();
            }
        });
    }

    /**
     * Runs {@code work} on one connection with auto-commit off, commits when it returns and rolls back
     * when it throws. SQL failures surface as {@link DatabaseException}; unchecked exceptions thrown by
     * {@code work} are rethrown unchanged after the rollback.
     */
    default <T> T transaction(TransactionCallback<T> work) {
        return inTransaction("transaction failed", work);
    }

    private <T> T inTransaction(String failure, TransactionCallback<T> work) {
        try (Connection c = borrow()) {
            boolean autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            T result;
            try {
                result = work.run(c);
                c.commit();
            } catch (SQLException | RuntimeException | Error e) {
                try {
                    c.rollback();
                    c.setAutoCommit(autoCommit);
                } catch (SQLException cleanup) {
                    e.addSuppressed(cleanup);
                }
                throw e;
            }
            c.setAutoCommit(autoCommit);
            return result;
        } catch (SQLException e) {
            throw new DatabaseException(failure, e);
        }
    }

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

    default CompletableFuture<Integer> updateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> update(sql, params));
    }

    default CompletableFuture<Long> insertAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> insert(sql, params));
    }

    default CompletableFuture<int[]> batchAsync(String sql, List<Object[]> rows) {
        return CompletableFuture.supplyAsync(() -> batch(sql, rows));
    }

    default <T> CompletableFuture<T> transactionAsync(TransactionCallback<T> work) {
        return CompletableFuture.supplyAsync(() -> transaction(work));
    }

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
