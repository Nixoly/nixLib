package dev.nixoly.nixlib.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Database extends AutoCloseable {

    Connection borrow() throws SQLException;

    void execute(String sql, Object... params);

    int update(String sql, Object... params);

    <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params);

    <T> List<T> queryMany(String sql, RowMapper<T> mapper, Object... params);

    CompletableFuture<Void> executeAsync(String sql, Object... params);

    <T> CompletableFuture<List<T>> queryManyAsync(String sql, RowMapper<T> mapper, Object... params);

    String dialect();

    @Override
    void close();
}
