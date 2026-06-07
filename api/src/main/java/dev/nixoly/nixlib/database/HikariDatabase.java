package dev.nixoly.nixlib.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class HikariDatabase implements Database {

    protected final HikariDataSource ds;
    private final ExecutorService asyncExecutor;
    private volatile boolean closed;

    protected HikariDatabase(HikariConfig config) {
        this.ds = new HikariDataSource(config);
        this.asyncExecutor = Executors.newFixedThreadPool(
                Math.max(2, config.getMaximumPoolSize()),
                r -> {
                    Thread t = new Thread(r, "nixlib-db-" + System.nanoTime());
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
        return CompletableFuture.runAsync(() -> execute(sql, params), asyncExecutor);
    }

    @Override
    public <T> CompletableFuture<List<T>> queryManyAsync(String sql, RowMapper<T> mapper, Object... params) {
        return CompletableFuture.supplyAsync(() -> queryMany(sql, mapper, params), asyncExecutor);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        asyncExecutor.shutdown();
        ds.close();
    }

    public boolean isClosed() {
        return closed;
    }

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
