package dev.nixoly.nixlib.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class QueryBuilder {

    private final StringBuilder sql = new StringBuilder();
    private final List<Object> params = new ArrayList<>();
    private boolean setStarted;

    private QueryBuilder() {}

    public static QueryBuilder select(String... columns) {
        QueryBuilder q = new QueryBuilder();
        q.sql.append("SELECT ");
        if (columns.length == 0) q.sql.append("*");
        else q.sql.append(String.join(", ", columns));
        return q;
    }

    public static QueryBuilder insertInto(String table, String... columns) {
        QueryBuilder q = new QueryBuilder();
        q.sql.append("INSERT INTO ").append(table)
                .append(" (").append(String.join(", ", columns)).append(") VALUES (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) q.sql.append(", ");
            q.sql.append('?');
        }
        q.sql.append(')');
        return q;
    }

    public static QueryBuilder update(String table) {
        QueryBuilder q = new QueryBuilder();
        q.sql.append("UPDATE ").append(table);
        return q;
    }

    public static QueryBuilder deleteFrom(String table) {
        QueryBuilder q = new QueryBuilder();
        q.sql.append("DELETE FROM ").append(table);
        return q;
    }

    public QueryBuilder from(String table) {
        sql.append(" FROM ").append(table);
        return this;
    }

    public QueryBuilder where(String condition, Object... values) {
        sql.append(" WHERE ").append(condition);
        params.addAll(Arrays.asList(values));
        return this;
    }

    public QueryBuilder and(String condition, Object... values) {
        sql.append(" AND ").append(condition);
        params.addAll(Arrays.asList(values));
        return this;
    }

    public QueryBuilder or(String condition, Object... values) {
        sql.append(" OR ").append(condition);
        params.addAll(Arrays.asList(values));
        return this;
    }

    public QueryBuilder set(String assignment, Object value) {
        sql.append(setStarted ? ", " : " SET ").append(assignment);
        setStarted = true;
        params.add(value);
        return this;
    }

    public QueryBuilder orderBy(String expr) {
        sql.append(" ORDER BY ").append(expr);
        return this;
    }

    public QueryBuilder limit(int n) {
        sql.append(" LIMIT ").append(n);
        return this;
    }

    public QueryBuilder bind(Object value) {
        params.add(value);
        return this;
    }

    public String sql() {
        return sql.toString();
    }

    public Object[] parameters() {
        return params.toArray();
    }
}
