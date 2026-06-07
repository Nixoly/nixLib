package dev.nixoly.nixlib.database;

import com.zaxxer.hikari.HikariConfig;

public final class MysqlDatabase extends HikariDatabase {

    public MysqlDatabase(String host, int port, String database, String username, String password) {
        super(build(host, port, database, username, password));
    }

    public MysqlDatabase(HikariConfig preset) {
        super(preset);
    }

    @Override
    public String dialect() {
        return "mysql";
    }

    private static HikariConfig build(String host, int port, String database, String user, String pass) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&autoReconnect=true&characterEncoding=UTF-8");
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(2);
        cfg.setPoolName("nixlib-mysql");
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        cfg.addDataSourceProperty("useServerPrepStmts", "true");
        return cfg;
    }
}
