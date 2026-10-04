package dev.nixoly.nixlib.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcDatabaseTest {

    @TempDir
    Path folder;

    private final List<HikariDatabase> opened = new ArrayList<>();

    @AfterEach
    void closeAll() {
        opened.forEach(HikariDatabase::close);
    }

    @Test
    void poolAndWorkersCarryThePluginChosenName() throws Exception {
        HikariDatabase db = track(JdbcDatabase.builder(DatabaseType.H2)
                .jdbcUrl(memoryUrl())
                .poolName("Homes")
                .build());

        String worker = db.queryOneAsync("SELECT 1", rs -> Thread.currentThread().getName())
                .get(2, TimeUnit.SECONDS).orElseThrow();

        assertThat(db.poolName()).isEqualTo("Homes");
        assertThat(worker).startsWith("Homes-worker-");
    }

    @Test
    void unnamedPoolNeverMentionsTheLibrary() {
        HikariDatabase sqlite = track(new SqliteDatabase(folder.resolve("data.db")));

        assertThat(sqlite.poolName()).isEqualTo("sqlite");
        assertThat(sqlite.poolName().toLowerCase()).doesNotContain("nixlib");
    }

    @Test
    void sqliteConstructorTakesAPoolName() {
        HikariDatabase sqlite = track(new SqliteDatabase(folder.resolve("named.db"), "Warps"));

        assertThat(sqlite.poolName()).isEqualTo("Warps");
        assertThat(sqlite.dialect()).isEqualTo("sqlite");
    }

    @Test
    void sqliteFileInMissingFolderIsCreated() {
        Path file = folder.resolve("plugins").resolve("Homes").resolve("homes.db");
        HikariDatabase db = track(JdbcDatabase.builder(DatabaseType.SQLITE).file(file).build());

        db.execute("CREATE TABLE t (id INTEGER)");

        assertThat(Files.exists(file)).isTrue();
    }

    @Test
    void buildsFromAConfigSection() {
        Map<String, Object> section = Map.of(
                "type", "SQLite",
                "file", "stats.db",
                "pool-name", "Stats",
                "pool", Map.of("maximum-size", 8, "connection-timeout-ms", "4000"));

        JdbcDatabase db = track(JdbcDatabase.fromConfig(section, folder).build());

        assertThat(db.type()).isEqualTo(DatabaseType.SQLITE);
        assertThat(db.poolName()).isEqualTo("Stats");
        assertThat(Files.exists(folder.resolve("stats.db"))).isTrue();
    }

    @Test
    void configWithoutTypeIsRejected() {
        assertThatThrownBy(() -> JdbcDatabase.fromConfig(Map.of("file", "x.db"), folder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }

    @Test
    void malformedPoolNumberNamesTheKey() {
        Map<String, Object> section = Map.of("type", "h2", "pool", Map.of("maximum-size", "lots"));

        assertThatThrownBy(() -> JdbcDatabase.fromConfig(section, folder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum-size");
    }

    @Test
    void missingDriverExplainsWhatToAdd() {
        JdbcDatabase.Builder builder = JdbcDatabase.builder(DatabaseType.POSTGRESQL).database("mc");

        assertThatThrownBy(builder::build)
                .isInstanceOf(DatabaseException.class)
                .hasMessageContaining("org.postgresql.Driver")
                .hasMessageContaining("org.postgresql:postgresql");
    }

    @Test
    void transactionCommitsWhenCallbackReturns() {
        HikariDatabase db = h2WithAccounts();

        db.transaction(c -> {
            transfer(c, "a", "b", 30);
            return null;
        });

        assertThat(balance(db, "a")).isEqualTo(70);
        assertThat(balance(db, "b")).isEqualTo(30);
    }

    @Test
    void rollsBackWhenCallbackThrows() {
        HikariDatabase db = h2WithAccounts();

        assertThatThrownBy(() -> db.transaction(c -> {
            transfer(c, "a", "b", 30);
            throw new IllegalStateException("insufficient funds");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(balance(db, "a")).isEqualTo(100);
        assertThat(balance(db, "b")).isZero();
    }

    @Test
    void sqlFailureInsideTransactionBecomesDatabaseException() {
        HikariDatabase db = h2WithAccounts();

        assertThatThrownBy(() -> db.transaction(c -> {
            transfer(c, "a", "b", 30);
            try (PreparedStatement ps = c.prepareStatement("UPDATE missing_table SET x = 1")) {
                return ps.executeUpdate();
            }
        })).isInstanceOf(DatabaseException.class);

        assertThat(balance(db, "a")).isEqualTo(100);
    }

    @Test
    void batchAppliesEveryRow() {
        HikariDatabase db = h2WithAccounts();

        int[] counts = db.batch("INSERT INTO accounts (id, balance) VALUES (?, ?)", List.of(
                new Object[]{"c", 1}, new Object[]{"d", 2}, new Object[]{"e", 3}));

        assertThat(counts).hasSize(3);
        assertThat(db.queryOne("SELECT COUNT(*) FROM accounts", rs -> rs.getInt(1))).contains(5);
    }

    @Test
    void failedBatchLeavesNoRowsBehind() {
        HikariDatabase db = h2WithAccounts();

        assertThatThrownBy(() -> db.batch("INSERT INTO accounts (id, balance) VALUES (?, ?)", List.of(
                new Object[]{"c", 1}, new Object[]{"a", 2})))
                .isInstanceOf(DatabaseException.class);

        assertThat(db.queryOne("SELECT COUNT(*) FROM accounts", rs -> rs.getInt(1))).contains(2);
    }

    @Test
    void insertReturnsGeneratedKey() throws Exception {
        HikariDatabase db = track(JdbcDatabase.builder(DatabaseType.H2).jdbcUrl(memoryUrl()).build());
        db.execute("CREATE TABLE notes (id BIGINT AUTO_INCREMENT PRIMARY KEY, body VARCHAR(64))");

        long first = db.insert("INSERT INTO notes (body) VALUES (?)", "hello");
        long second = db.insertAsync("INSERT INTO notes (body) VALUES (?)", "again").get(2, TimeUnit.SECONDS);

        assertThat(first).isEqualTo(1L);
        assertThat(second).isEqualTo(2L);
    }

    @Test
    void sqliteUpsertOverwritesExistingRow() {
        HikariDatabase db = track(JdbcDatabase.builder(DatabaseType.SQLITE).file(folder.resolve("up.db")).build());
        db.execute("CREATE TABLE stats (uuid TEXT PRIMARY KEY, kills INTEGER, deaths INTEGER)");
        List<String> columns = List.of("uuid", "kills", "deaths");

        db.upsert("stats", List.of("uuid"), columns, "p1", 1, 0);
        db.upsert("stats", List.of("uuid"), columns, "p1", 5, 2);

        assertThat(db.queryOne("SELECT kills FROM stats WHERE uuid = ?", rs -> rs.getInt(1), "p1")).contains(5);
        assertThat(db.queryOne("SELECT COUNT(*) FROM stats", rs -> rs.getInt(1))).contains(1);
    }

    @Test
    void h2UpsertOverwritesExistingRow() {
        HikariDatabase db = track(JdbcDatabase.builder(DatabaseType.H2).jdbcUrl(memoryUrl()).build());
        db.execute("CREATE TABLE homes (owner VARCHAR(36), name VARCHAR(16), world VARCHAR(32), PRIMARY KEY (owner, name))");
        List<String> columns = List.of("owner", "name", "world");

        db.upsert("homes", List.of("owner", "name"), columns, "p1", "base", "world");
        db.upsert("homes", List.of("owner", "name"), columns, "p1", "base", "nether");

        assertThat(db.queryOne("SELECT world FROM homes WHERE owner = ? AND name = ?",
                rs -> rs.getString(1), "p1", "base")).contains("nether");
    }

    @Test
    void upsertRejectsValueCountMismatch() {
        HikariDatabase db = track(JdbcDatabase.builder(DatabaseType.H2).jdbcUrl(memoryUrl()).build());

        assertThatThrownBy(() -> db.upsert("t", List.of("a"), List.of("a", "b"), "only-one"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateAsyncReportsAffectedRows() throws Exception {
        HikariDatabase db = h2WithAccounts();

        int rows = db.updateAsync("UPDATE accounts SET balance = balance + 1").get(2, TimeUnit.SECONDS);

        assertThat(rows).isEqualTo(2);
    }

    private HikariDatabase h2WithAccounts() {
        HikariDatabase db = track(JdbcDatabase.builder(DatabaseType.H2).jdbcUrl(memoryUrl()).build());
        db.execute("CREATE TABLE accounts (id VARCHAR(8) PRIMARY KEY, balance INT)");
        db.execute("INSERT INTO accounts VALUES ('a', 100), ('b', 0)");
        return db;
    }

    private static void transfer(Connection c, String from, String to, int amount) throws SQLException {
        try (PreparedStatement debit = c.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE id = ?");
             PreparedStatement credit = c.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE id = ?")) {
            debit.setInt(1, amount);
            debit.setString(2, from);
            debit.executeUpdate();
            credit.setInt(1, amount);
            credit.setString(2, to);
            credit.executeUpdate();
        }
    }

    private static int balance(HikariDatabase db, String id) {
        return db.queryOne("SELECT balance FROM accounts WHERE id = ?", rs -> rs.getInt(1), id).orElseThrow();
    }

    private static String memoryUrl() {
        return "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    }

    private <T extends HikariDatabase> T track(T db) {
        opened.add(db);
        return db;
    }
}
