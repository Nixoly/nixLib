package dev.nixoly.nixlib.database;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface TransactionCallback<T> {

    T run(Connection connection) throws SQLException;
}
