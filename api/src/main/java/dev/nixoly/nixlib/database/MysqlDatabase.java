package dev.nixoly.nixlib.database;

import com.zaxxer.hikari.HikariConfig;

public final class MysqlDatabase extends HikariDatabase {

    public MysqlDatabase(String host, int port, String database, String username, String password) {
        this(host, port, database, username, password, null);
    }

    /** @param poolName what Hikari prints in the console; null falls back to the plugin name */
    public MysqlDatabase(String host, int port, String database, String username, String password, String poolName) {
        super(JdbcDatabase.builder(DatabaseType.MYSQL)
                .host(host)
                .port(port)
                .database(database)
                .credentials(username, password)
                .poolName(poolName)
                // This class has always connected without TLS and existing servers are set up for that.
                .property("useSSL", "false")
                .hikariConfig(), DatabaseType.MYSQL);
    }

    public MysqlDatabase(HikariConfig preset) {
        super(preset, DatabaseType.MYSQL);
    }

    @Override
    public String dialect() {
        return "mysql";
    }
}
