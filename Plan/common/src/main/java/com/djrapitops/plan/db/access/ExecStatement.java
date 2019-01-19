/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.db.access;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * SQL executing statement that closes appropriate elements.
 *
 * @author Rsl1122
 */
public abstract class ExecStatement {

    private final String sql;

    public ExecStatement(String sql) {
        this.sql = sql;
    }

    public boolean execute(PreparedStatement statement) throws SQLException {
        try {
            prepare(statement);
            return callExecute(statement);
        } finally {
            statement.close();
        }
    }

    private boolean callExecute(PreparedStatement statement) throws SQLException {
        if (sql.startsWith("UPDATE") || sql.startsWith("INSERT") || sql.startsWith("DELETE") || sql.startsWith("REPLACE")) {
            return statement.executeUpdate() > 0;
        } else {
            statement.execute();
            return false;
        }
    }

    public void executeBatch(PreparedStatement statement) throws SQLException {
        try {
            prepare(statement);
            statement.executeBatch();
        } finally {
            statement.close();
        }
    }

    public abstract void prepare(PreparedStatement statement) throws SQLException;

    public String getSql() {
        return sql;
    }
}