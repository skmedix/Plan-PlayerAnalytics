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
package com.djrapitops.plan.db.patches;

import com.djrapitops.plan.api.exceptions.database.DBOpException;
import com.djrapitops.plan.db.DBType;
import com.djrapitops.plan.db.SQLDB;
import com.djrapitops.plan.db.access.QueryAllStatement;
import com.djrapitops.plan.db.access.QueryStatement;
import com.djrapitops.plan.db.sql.parsing.TableSqlParser;
import com.djrapitops.plan.system.database.databases.sql.objects.ForeignKeyConstraint;
import com.djrapitops.plan.system.database.databases.sql.operation.Queries;
import com.djrapitops.plan.system.settings.paths.DatabaseSettings;
import com.djrapitops.plugin.utilities.Verify;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public abstract class Patch {

    protected final SQLDB db;
    protected final DBType dbType;

    public Patch(SQLDB db) {
        this.db = db;
        this.dbType = db.getType();
    }

    public abstract boolean hasBeenApplied();

    protected abstract void applyPatch();

    public void apply() {
        if (dbType == DBType.MYSQL) disableForeignKeyChecks();
        applyPatch();
        if (dbType == DBType.MYSQL) enableForeignKeyChecks();
    }

    private void enableForeignKeyChecks() {
        db.execute("SET FOREIGN_KEY_CHECKS=1");
    }

    private void disableForeignKeyChecks() {
        db.execute("SET FOREIGN_KEY_CHECKS=0");
    }

    public <T> T query(QueryStatement<T> query) {
        return db.query(query);
    }

    protected boolean hasTable(String tableName) {
        boolean secondParameter;

        String sql;
        if (dbType == DBType.H2) {
            sql = "SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME=?";
            secondParameter = false;
        } else if (dbType.supportsMySQLQueries()) {
            sql = "SELECT * FROM information_schema.TABLES WHERE table_name=? AND TABLE_SCHEMA=? LIMIT 1";
            secondParameter = true;
        } else {
            sql = "SELECT tbl_name FROM sqlite_master WHERE tbl_name=?";
            secondParameter = false;
        }

        return query(new QueryStatement<Boolean>(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, tableName);
                if (secondParameter) {
                    statement.setString(2, db.getConfig().get(DatabaseSettings.MYSQL_DATABASE));
                }
            }

            @Override
            public Boolean processResults(ResultSet set) throws SQLException {
                return set.next();
            }
        });
    }

    protected boolean hasColumn(String tableName, String columnName) {
        if (dbType.supportsMySQLQueries()) {
            String query;

            if (dbType == DBType.H2) {
                query = "SELECT * FROM INFORMATION_SCHEMA.COLUMNS" +
                        " WHERE TABLE_NAME=? AND COLUMN_NAME=?";
            } else {
                query = "SELECT * FROM information_schema.COLUMNS" +
                        " WHERE TABLE_NAME=? AND COLUMN_NAME=? AND TABLE_SCHEMA=?";
            }

            return query(new QueryStatement<Boolean>(query) {
                @Override
                public void prepare(PreparedStatement statement) throws SQLException {
                    statement.setString(1, tableName);
                    statement.setString(2, columnName);
                    if (dbType != DBType.H2) {
                        statement.setString(3, db.getConfig().get(DatabaseSettings.MYSQL_DATABASE));
                    }
                }

                @Override
                public Boolean processResults(ResultSet set) throws SQLException {
                    return set.next();
                }
            });
        } else {
            return query(new QueryAllStatement<Boolean>("PRAGMA table_info(" + tableName + ")") {
                @Override
                public Boolean processResults(ResultSet set) throws SQLException {
                    while (set.next()) {
                        if (columnName.equals(set.getString("name"))) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
    }

    protected void addColumn(String tableName, String columnInfo) {
        db.execute("ALTER TABLE " + tableName + " ADD " + (dbType.supportsMySQLQueries() ? "" : "COLUMN ") + columnInfo);
    }

    protected void dropTable(String name) {
        db.execute(TableSqlParser.dropTable(name));
    }

    protected void renameTable(String from, String to) {
        db.execute(getRenameTableSQL(from, to));
    }

    private String getRenameTableSQL(String from, String to) {
        switch (dbType) {
            case SQLITE:
                return "ALTER TABLE " + from + " RENAME TO " + to;
            case MYSQL:
                return "RENAME TABLE " + from + " TO " + to;
            case H2:
                return "ALTER TABLE " + from + " RENAME TO " + to;
            default:
                throw new IllegalArgumentException("DBType: " + dbType.getName() + " does not have rename table sql");
        }
    }

    protected void dropForeignKeys(String referencedTable) {
        if (dbType != DBType.MYSQL) {
            return;
        }

        String schema = db.getConfig().get(DatabaseSettings.MYSQL_DATABASE);
        List<ForeignKeyConstraint> constraints = query(Queries.foreignKeyConstraintsOf(schema, referencedTable));

        for (ForeignKeyConstraint constraint : constraints) {
            // Uses information from https://stackoverflow.com/a/34574758
            db.execute("ALTER TABLE " + constraint.getTable() +
                    " DROP FOREIGN KEY " + constraint.getConstraintName());
        }
    }

    protected void ensureNoForeignKeyConstraints(String table) {
        if (dbType != DBType.MYSQL) {
            return;
        }

        String schema = db.getConfig().get(DatabaseSettings.MYSQL_DATABASE);
        List<ForeignKeyConstraint> constraints = query(Queries.foreignKeyConstraintsOf(schema, table));

        Verify.isTrue(constraints.isEmpty(), () -> new DBOpException("Table '" + table + "' has constraints '" + constraints + "'"));
    }

    protected UUID getServerUUID() {
        return db.getServerUUIDSupplier().get();
    }
}
