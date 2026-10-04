package dev.nixoly.nixlib.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

final class Statements {

    private Statements() {}

    static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
