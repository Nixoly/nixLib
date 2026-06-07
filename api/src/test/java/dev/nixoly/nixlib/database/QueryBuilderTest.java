package dev.nixoly.nixlib.database;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryBuilderTest {

    @Test
    void selectWithWhereAndOrder() {
        QueryBuilder q = QueryBuilder.select("uuid", "name")
                .from("players")
                .where("level > ?", 10)
                .and("name LIKE ?", "a%")
                .orderBy("level DESC")
                .limit(5);

        assertThat(q.sql())
                .isEqualTo("SELECT uuid, name FROM players WHERE level > ? AND name LIKE ? ORDER BY level DESC LIMIT 5");
        assertThat(q.parameters()).containsExactly(10, "a%");
    }

    @Test
    void insertProducesPlaceholders() {
        QueryBuilder q = QueryBuilder.insertInto("players", "uuid", "name", "level")
                .bind("u1").bind("alice").bind(10);

        assertThat(q.sql()).isEqualTo("INSERT INTO players (uuid, name, level) VALUES (?, ?, ?)");
        assertThat(q.parameters()).containsExactly("u1", "alice", 10);
    }

    @Test
    void updateWithMultipleSets() {
        QueryBuilder q = QueryBuilder.update("players")
                .set("name = ?", "bob")
                .set("level = ?", 5)
                .where("uuid = ?", "u1");

        assertThat(q.sql()).isEqualTo("UPDATE players SET name = ?, level = ? WHERE uuid = ?");
        assertThat(q.parameters()).containsExactly("bob", 5, "u1");
    }

    @Test
    void deleteWithCondition() {
        QueryBuilder q = QueryBuilder.deleteFrom("players").where("level < ?", 1);
        assertThat(q.sql()).isEqualTo("DELETE FROM players WHERE level < ?");
        assertThat(q.parameters()).containsExactly(1);
    }

    @Test
    void selectStarWithoutColumns() {
        QueryBuilder q = QueryBuilder.select().from("players");
        assertThat(q.sql()).isEqualTo("SELECT * FROM players");
    }
}
