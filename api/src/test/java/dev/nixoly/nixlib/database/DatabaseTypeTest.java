package dev.nixoly.nixlib.database;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseTypeTest {

    @Test
    void parsesIdsIgnoringCase() {
        assertThat(DatabaseType.parse("SQLite")).isEqualTo(DatabaseType.SQLITE);
        assertThat(DatabaseType.parse(" h2 ")).isEqualTo(DatabaseType.H2);
        assertThat(DatabaseType.parse("MySQL")).isEqualTo(DatabaseType.MYSQL);
        assertThat(DatabaseType.parse("mariadb")).isEqualTo(DatabaseType.MARIADB);
    }

    @Test
    void parsesPostgresAliases() {
        assertThat(DatabaseType.parse("postgres")).isEqualTo(DatabaseType.POSTGRESQL);
        assertThat(DatabaseType.parse("pgsql")).isEqualTo(DatabaseType.POSTGRESQL);
        assertThat(DatabaseType.parse("PostgreSQL")).isEqualTo(DatabaseType.POSTGRESQL);
    }

    @Test
    void parsesSqlServerSpellings() {
        assertThat(DatabaseType.parse("mssql")).isEqualTo(DatabaseType.SQL_SERVER);
        assertThat(DatabaseType.parse("sql-server")).isEqualTo(DatabaseType.SQL_SERVER);
        assertThat(DatabaseType.parse("SQL Server")).isEqualTo(DatabaseType.SQL_SERVER);
        assertThat(DatabaseType.parse("sql_server")).isEqualTo(DatabaseType.SQL_SERVER);
        assertThat(DatabaseType.parse("maria")).isEqualTo(DatabaseType.MARIADB);
    }

    @Test
    void rejectsUnknownTypeAndListsKnownOnes() {
        assertThatThrownBy(() -> DatabaseType.parse("oracle"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oracle")
                .hasMessageContaining("sqlite, h2, mysql, mariadb, postgresql, sqlserver");
    }

    @Test
    void recognisesTypeFromJdbcUrl() {
        assertThat(DatabaseType.fromJdbcUrl("jdbc:h2:mem:test")).isEqualTo(DatabaseType.H2);
        assertThat(DatabaseType.fromJdbcUrl("jdbc:sqlserver://db:1433;databaseName=mc")).isEqualTo(DatabaseType.SQL_SERVER);
        assertThat(DatabaseType.fromJdbcUrl("jdbc:mysql://localhost/mc")).isEqualTo(DatabaseType.MYSQL);
        assertThat(DatabaseType.fromJdbcUrl("jdbc:oracle:thin:@db:1521:mc")).isNull();
        assertThat(DatabaseType.fromJdbcUrl(null)).isNull();
    }

    @Test
    void buildsServerUrls() {
        assertThat(DatabaseType.POSTGRESQL.jdbcUrl("db.local", 5432, "survival"))
                .isEqualTo("jdbc:postgresql://db.local:5432/survival");
        assertThat(DatabaseType.MARIADB.jdbcUrl("127.0.0.1", 3307, "mc"))
                .isEqualTo("jdbc:mariadb://127.0.0.1:3307/mc");
        assertThat(DatabaseType.SQL_SERVER.jdbcUrl("sql", 1433, "mc"))
                .isEqualTo("jdbc:sqlserver://sql:1433;databaseName=mc");
    }

    @Test
    void stripsMvDbSuffixFromH2File() {
        Path file = Path.of("plugins", "Homes", "homes.mv.db");
        String expected = "jdbc:h2:file:" + Path.of("plugins", "Homes", "homes").toAbsolutePath();

        assertThat(DatabaseType.H2.jdbcUrl(file)).isEqualTo(expected);
        assertThat(DatabaseType.H2.jdbcUrl(Path.of("plugins", "Homes", "homes"))).isEqualTo(expected);
    }

    @Test
    void fileAndServerUrlsDoNotMix() {
        assertThatThrownBy(() -> DatabaseType.SQLITE.jdbcUrl("localhost", 1, "mc"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> DatabaseType.MYSQL.jdbcUrl(Path.of("data.db")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void onlyServerDatabasesHaveDefaultPorts() {
        assertThat(DatabaseType.SQLITE.defaultPort()).isEqualTo(-1);
        assertThat(DatabaseType.H2.fileBased()).isTrue();
        assertThat(DatabaseType.POSTGRESQL.defaultPort()).isEqualTo(5432);
        assertThat(DatabaseType.SQL_SERVER.defaultPort()).isEqualTo(1433);
        assertThat(DatabaseType.MARIADB.fileBased()).isFalse();
    }

    @Test
    void postgresBatchesAreRewritten() {
        assertThat(DatabaseType.POSTGRESQL.dataSourceProperties()).containsEntry("reWriteBatchedInserts", "true");
        assertThat(DatabaseType.MYSQL.dataSourceProperties()).containsEntry("cachePrepStmts", "true");
    }

    @Test
    void mysqlUpsertUpdatesNonKeyColumns() {
        String sql = DatabaseType.MYSQL.upsert("homes", List.of("owner", "name"), List.of("owner", "name", "world", "x"));

        assertThat(sql).isEqualTo("INSERT INTO homes (owner, name, world, x) VALUES (?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x)");
        assertThat(DatabaseType.MARIADB.upsert("homes", List.of("owner", "name"), List.of("owner", "name", "world", "x")))
                .isEqualTo(sql);
    }

    @Test
    void mysqlUpsertOfKeysOnlyLeavesExistingRowAlone() {
        assertThat(DatabaseType.MYSQL.upsert("seen", List.of("uuid"), List.of("uuid")))
                .isEqualTo("INSERT INTO seen (uuid) VALUES (?) ON DUPLICATE KEY UPDATE uuid = uuid");
    }

    @Test
    void postgresUpsertUsesOnConflict() {
        assertThat(DatabaseType.POSTGRESQL.upsert("stats", List.of("uuid"), List.of("uuid", "kills", "deaths")))
                .isEqualTo("INSERT INTO stats (uuid, kills, deaths) VALUES (?, ?, ?)"
                        + " ON CONFLICT (uuid) DO UPDATE SET kills = excluded.kills, deaths = excluded.deaths");
        assertThat(DatabaseType.POSTGRESQL.upsert("seen", List.of("uuid"), List.of("uuid")))
                .isEqualTo("INSERT INTO seen (uuid) VALUES (?) ON CONFLICT (uuid) DO NOTHING");
    }

    @Test
    void h2UpsertUsesMergeKey() {
        assertThat(DatabaseType.H2.upsert("stats", List.of("uuid"), List.of("uuid", "kills")))
                .isEqualTo("MERGE INTO stats (uuid, kills) KEY (uuid) VALUES (?, ?)");
    }

    @Test
    void sqlServerUpsertMergesFromValues() {
        assertThat(DatabaseType.SQL_SERVER.upsert("dbo.homes", List.of("owner", "name"), List.of("owner", "name", "world")))
                .isEqualTo("MERGE INTO dbo.homes WITH (HOLDLOCK) AS target"
                        + " USING (VALUES (?, ?, ?)) AS source (owner, name, world)"
                        + " ON target.owner = source.owner AND target.name = source.name"
                        + " WHEN MATCHED THEN UPDATE SET world = source.world"
                        + " WHEN NOT MATCHED THEN INSERT (owner, name, world)"
                        + " VALUES (source.owner, source.name, source.world);");
    }

    @Test
    void sqlServerUpsertOfKeysOnlyHasNoMatchedBranch() {
        assertThat(DatabaseType.SQL_SERVER.upsert("seen", List.of("uuid"), List.of("uuid")))
                .isEqualTo("MERGE INTO seen WITH (HOLDLOCK) AS target USING (VALUES (?)) AS source (uuid)"
                        + " ON target.uuid = source.uuid"
                        + " WHEN NOT MATCHED THEN INSERT (uuid) VALUES (source.uuid);");
    }

    @Test
    void everyDialectBindsOneParameterPerColumn() {
        List<String> columns = List.of("a", "b", "c", "d");
        for (DatabaseType type : DatabaseType.values()) {
            String sql = type.upsert("t", List.of("a", "b"), columns);
            assertThat(sql.chars().filter(ch -> ch == '?').count()).as(type.id()).isEqualTo(columns.size());
        }
    }

    @Test
    void acceptsSchemaQualifiedTable() {
        assertThat(DatabaseType.POSTGRESQL.upsert("game.stats", List.of("id"), List.of("id", "v")))
                .startsWith("INSERT INTO game.stats ");
    }

    @Test
    void rejectsInjectedTableName() {
        assertThatThrownBy(() -> DatabaseType.SQLITE.upsert("homes; DROP TABLE homes", List.of("id"), List.of("id")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("homes; DROP TABLE homes");
    }

    @Test
    void rejectsQuotedOrDottedColumns() {
        assertThatThrownBy(() -> DatabaseType.MYSQL.upsert("homes", List.of("id"), List.of("id", "`name`")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DatabaseType.MYSQL.upsert("homes", List.of("id"), List.of("id", "h.name")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DatabaseType.H2.upsert("1homes", List.of("id"), List.of("id")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsKeyMissingFromColumns() {
        assertThatThrownBy(() -> DatabaseType.H2.upsert("homes", List.of("owner"), List.of("name", "world")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner");
        assertThatThrownBy(() -> DatabaseType.H2.upsert("homes", List.of(), List.of("name")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
