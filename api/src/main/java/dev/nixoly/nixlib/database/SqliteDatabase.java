package dev.nixoly.nixlib.database;

import com.zaxxer.hikari.HikariConfig;

import java.nio.file.Path;

public final class SqliteDatabase extends HikariDatabase {

    public SqliteDatabase(Path file) {
        super(buildConfig(file));
    }

    public SqliteDatabase(String jdbcUrl) {
        super(buildConfig(jdbcUrl));
    }

    @Override
    public String dialect() {
        return "sqlite";
    }

    private static HikariConfig buildConfig(Path file) {
        return buildConfig("jdbc:sqlite:" + file.toAbsolutePath());
    }

    private static HikariConfig buildConfig(String jdbcUrl) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setDriverClassName("org.sqlite.JDBC");
        cfg.setMaximumPoolSize(1);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTestQuery("SELECT 1");
        cfg.setPoolName("nixlib-sqlite");
        return cfg;
    }
}
