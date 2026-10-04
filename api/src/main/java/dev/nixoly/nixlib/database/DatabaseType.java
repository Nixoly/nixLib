package dev.nixoly.nixlib.database;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum DatabaseType {

    SQLITE("sqlite", -1, 1, "org.xerial:sqlite-jdbc", "org.sqlite.JDBC"),
    H2("h2", -1, 4, "com.h2database:h2", "org.h2.Driver"),
    // Older Spigot/Paper builds bundle Connector/J 5.1, which only has the legacy class name.
    MYSQL("mysql", 3306, 10, "com.mysql:mysql-connector-j", "com.mysql.cj.jdbc.Driver", "com.mysql.jdbc.Driver"),
    MARIADB("mariadb", 3306, 10, "org.mariadb.jdbc:mariadb-java-client", "org.mariadb.jdbc.Driver"),
    POSTGRESQL("postgresql", 5432, 10, "org.postgresql:postgresql", "org.postgresql.Driver"),
    SQL_SERVER("sqlserver", 1433, 10, "com.microsoft.sqlserver:mssql-jdbc", "com.microsoft.sqlserver.jdbc.SQLServerDriver");

    private static final Pattern TABLE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");
    private static final Pattern COLUMN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final String id;
    private final int defaultPort;
    private final int defaultPoolSize;
    private final String driverArtifact;
    private final String[] driverClassNames;

    DatabaseType(String id, int defaultPort, int defaultPoolSize, String driverArtifact, String... driverClassNames) {
        this.id = id;
        this.defaultPort = defaultPort;
        this.defaultPoolSize = defaultPoolSize;
        this.driverArtifact = driverArtifact;
        this.driverClassNames = driverClassNames;
    }

    /**
     * Resolves a config value such as {@code postgres}, {@code MSSQL} or {@code sql-server}.
     *
     * @throws IllegalArgumentException for anything that is not a known type or alias
     */
    public static DatabaseType parse(String name) {
        String key = name.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s_-]", "");
        return switch (key) {
            case "sqlite", "sqlite3" -> SQLITE;
            case "h2" -> H2;
            case "mysql" -> MYSQL;
            case "mariadb", "maria" -> MARIADB;
            case "postgresql", "postgres", "pgsql", "pg" -> POSTGRESQL;
            case "sqlserver", "mssql" -> SQL_SERVER;
            default -> throw new IllegalArgumentException("Unknown database type '" + name + "', expected one of "
                    + Arrays.stream(values()).map(DatabaseType::id).collect(Collectors.joining(", ")));
        };
    }

    // Config ids double as the JDBC sub-protocol, so "jdbc:<id>:" identifies the type.
    static DatabaseType fromJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        for (DatabaseType type : values()) {
            if (jdbcUrl.startsWith("jdbc:" + type.id + ":")) {
                return type;
            }
        }
        return null;
    }

    public String id() {
        return id;
    }

    public String driverClassName() {
        return driverClassNames[0];
    }

    List<String> driverClassNames() {
        return List.of(driverClassNames);
    }

    /** Maven coordinate of the JDBC driver, for plugin.yml {@code libraries:} or shading. */
    public String driverArtifact() {
        return driverArtifact;
    }

    /** Default server port, or -1 for file databases. */
    public int defaultPort() {
        return defaultPort;
    }

    public boolean fileBased() {
        return defaultPort < 0;
    }

    public int defaultPoolSize() {
        return defaultPoolSize;
    }

    public String jdbcUrl(String host, int port, String database) {
        return switch (this) {
            case SQLITE, H2 -> throw new UnsupportedOperationException(id + " is file based, build its URL from a path");
            case SQL_SERVER -> "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + database;
            default -> "jdbc:" + id + "://" + host + ":" + port + "/" + database;
        };
    }

    /** URL for a file database. H2 wants the path without its {@code .mv.db} suffix, which is stripped if present. */
    public String jdbcUrl(Path file) {
        String path = file.toAbsolutePath().toString();
        return switch (this) {
            case SQLITE -> "jdbc:sqlite:" + path;
            case H2 -> "jdbc:h2:file:" + (path.endsWith(".mv.db") ? path.substring(0, path.length() - 6) : path);
            default -> throw new UnsupportedOperationException(id + " is a network database, build its URL from host, port and database");
        };
    }

    /** Driver properties worth setting on every pool of this type. Builder properties override them. */
    public Map<String, String> dataSourceProperties() {
        return switch (this) {
            case MYSQL -> Map.of(
                    "cachePrepStmts", "true",
                    "prepStmtCacheSize", "250",
                    "prepStmtCacheSqlLimit", "2048",
                    "useServerPrepStmts", "true",
                    "rewriteBatchedStatements", "true",
                    "characterEncoding", "UTF-8",
                    "connectTimeout", "5000",
                    "socketTimeout", "10000",
                    "tcpKeepAlive", "true");
            case MARIADB -> Map.of(
                    "connectTimeout", "5000",
                    "socketTimeout", "10000",
                    "tcpKeepAlive", "true");
            // pgjdbc reads both timeouts in seconds.
            case POSTGRESQL -> Map.of(
                    "reWriteBatchedInserts", "true",
                    "connectTimeout", "5",
                    "socketTimeout", "10",
                    "tcpKeepAlive", "true");
            case SQL_SERVER -> Map.of(
                    "loginTimeout", "5",
                    "socketTimeout", "10000");
            case SQLITE, H2 -> Map.of();
        };
    }

    /**
     * Builds an insert-or-update statement with one {@code ?} per entry of {@code columns}, in that order.
     * Rows matching on {@code keyColumns} get their other columns overwritten. When every column is a key
     * there is nothing to overwrite, so the statement only inserts rows that are missing.
     *
     * @throws IllegalArgumentException if a name is not a plain identifier or a key is missing from {@code columns}
     */
    public String upsert(String table, List<String> keyColumns, List<String> columns) {
        checkIdentifier(table, TABLE);
        if (columns.isEmpty() || keyColumns.isEmpty()) {
            throw new IllegalArgumentException("upsert needs at least one column and one key column");
        }
        for (String column : columns) {
            checkIdentifier(column, COLUMN);
        }
        if (!columns.containsAll(keyColumns)) {
            throw new IllegalArgumentException("key columns " + keyColumns + " must all appear in " + columns);
        }
        List<String> updated = new ArrayList<>(columns);
        updated.removeAll(keyColumns);

        String columnList = String.join(", ", columns);
        String keyList = String.join(", ", keyColumns);
        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
        String insert = "INSERT INTO " + table + " (" + columnList + ") VALUES (" + placeholders + ")";

        return switch (this) {
            case MYSQL, MARIADB -> {
                // A self-assignment leaves duplicates alone; INSERT IGNORE would also hide truncation
                // and foreign key errors.
                String assignments = updated.isEmpty()
                        ? keyColumns.get(0) + " = " + keyColumns.get(0)
                        : join(updated, ", ", c -> c + " = VALUES(" + c + ")");
                yield insert + " ON DUPLICATE KEY UPDATE " + assignments;
            }
            case SQLITE, POSTGRESQL -> insert + " ON CONFLICT (" + keyList + ") DO "
                    + (updated.isEmpty() ? "NOTHING" : "UPDATE SET " + join(updated, ", ", c -> c + " = excluded." + c));
            case H2 -> "MERGE INTO " + table + " (" + columnList + ") KEY (" + keyList + ") VALUES (" + placeholders + ")";
            case SQL_SERVER -> {
                // HOLDLOCK keeps two concurrent merges from both taking the NOT MATCHED branch.
                String merge = "MERGE INTO " + table + " WITH (HOLDLOCK) AS target"
                        + " USING (VALUES (" + placeholders + ")) AS source (" + columnList + ")"
                        + " ON " + join(keyColumns, " AND ", c -> "target." + c + " = source." + c);
                if (!updated.isEmpty()) {
                    merge += " WHEN MATCHED THEN UPDATE SET " + join(updated, ", ", c -> c + " = source." + c);
                }
                yield merge + " WHEN NOT MATCHED THEN INSERT (" + columnList + ")"
                        + " VALUES (" + join(columns, ", ", c -> "source." + c) + ");";
            }
        };
    }

    private static String join(List<String> names, String delimiter, UnaryOperator<String> format) {
        return names.stream().map(format).collect(Collectors.joining(delimiter));
    }

    private static void checkIdentifier(String name, Pattern pattern) {
        if (name == null || !pattern.matcher(name).matches()) {
            throw new IllegalArgumentException("Not a plain SQL identifier: " + name);
        }
    }
}
