package dev.nixoly.nixlib.database;

import java.nio.file.Path;

public final class SqliteDatabase extends HikariDatabase {

    public SqliteDatabase(Path file) {
        this(file, null);
    }

    /** @param poolName what Hikari prints in the console; null falls back to the plugin name */
    public SqliteDatabase(Path file, String poolName) {
        super(JdbcDatabase.builder(DatabaseType.SQLITE).file(file).poolName(poolName).hikariConfig(), DatabaseType.SQLITE);
    }

    public SqliteDatabase(String jdbcUrl) {
        super(JdbcDatabase.builder(DatabaseType.SQLITE).jdbcUrl(jdbcUrl).hikariConfig(), DatabaseType.SQLITE);
    }

    @Override
    public String dialect() {
        return "sqlite";
    }
}
