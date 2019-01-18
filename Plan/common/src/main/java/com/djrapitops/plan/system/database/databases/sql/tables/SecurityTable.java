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
package com.djrapitops.plan.system.database.databases.sql.tables;

import com.djrapitops.plan.api.exceptions.database.DBInitException;
import com.djrapitops.plan.data.WebUser;
import com.djrapitops.plan.db.SQLDB;
import com.djrapitops.plan.system.database.databases.sql.processing.ExecStatement;
import com.djrapitops.plan.system.database.databases.sql.processing.QueryAllStatement;
import com.djrapitops.plan.system.database.databases.sql.processing.QueryStatement;
import com.djrapitops.plan.system.database.databases.sql.statements.*;
import com.djrapitops.plugin.utilities.Verify;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Table that is in charge of storing WebUser data.
 * <p>
 * Table Name: plan_security
 * <p>
 * For contained columns {@link Col}
 *
 * @author Rsl1122
 * @see WebUser
 */
public class SecurityTable extends Table {

    public SecurityTable(SQLDB db) {
        super("plan_security", db);
        insertStatement = Insert.values(tableName,
                Col.USERNAME,
                Col.SALT_PASSWORD_HASH,
                Col.PERMISSION_LEVEL);
    }

    private String insertStatement;

    @Override
    public void createTable() throws DBInitException {
        createTable(TableSqlParser.createTable(tableName)
                .column(Col.USERNAME, Sql.varchar(100)).notNull().unique()
                .column(Col.SALT_PASSWORD_HASH, Sql.varchar(100)).notNull().unique()
                .column(Col.PERMISSION_LEVEL, Sql.INT).notNull()
                .toString()
        );
    }

    public void removeUser(String user) {
        String sql = "DELETE FROM " + tableName + " WHERE (" + Col.USERNAME + "=?)";

        execute(new ExecStatement(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, user);
            }
        });
    }

    public WebUser getWebUser(String user) {
        String sql = Select.all(tableName).where(Col.USERNAME + "=?").toString();

        return query(new QueryStatement<WebUser>(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, user);
            }

            @Override
            public WebUser processResults(ResultSet set) throws SQLException {
                if (set.next()) {
                    String saltedPassHash = set.getString(Col.SALT_PASSWORD_HASH.get());
                    int permissionLevel = set.getInt(Col.PERMISSION_LEVEL.get());
                    return new WebUser(user, saltedPassHash, permissionLevel);
                }
                return null;
            }
        });
    }

    public void addNewUser(WebUser info) {
        addNewUser(info.getName(), info.getSaltedPassHash(), info.getPermLevel());
    }

    public void addNewUser(String user, String saltPassHash, int permLevel) {
        execute(new ExecStatement(insertStatement) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, user);
                statement.setString(2, saltPassHash);
                statement.setInt(3, permLevel);
            }
        });
    }

    public boolean userExists(String user) {
        return getWebUser(user) != null;
    }

    public List<WebUser> getUsers() {
        String sql = Select.all(tableName).toString();

        return query(new QueryAllStatement<List<WebUser>>(sql, 5000) {
            @Override
            public List<WebUser> processResults(ResultSet set) throws SQLException {
                List<WebUser> list = new ArrayList<>();
                while (set.next()) {
                    String user = set.getString(Col.USERNAME.get());
                    String saltedPassHash = set.getString(Col.SALT_PASSWORD_HASH.get());
                    int permissionLevel = set.getInt(Col.PERMISSION_LEVEL.get());
                    WebUser info = new WebUser(user, saltedPassHash, permissionLevel);
                    list.add(info);
                }
                return list;
            }
        });
    }

    public enum Col implements Column {
        USERNAME("username"),
        SALT_PASSWORD_HASH("salted_pass_hash"),
        PERMISSION_LEVEL("permission_level");

        private final String column;

        Col(String column) {
            this.column = column;
        }

        @Override
        public String get() {
            return toString();
        }

        @Override
        public String toString() {
            return column;
        }
    }

    public void addUsers(List<WebUser> users) {
        if (Verify.isEmpty(users)) {
            return;
        }

        executeBatch(new ExecStatement(insertStatement) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                for (WebUser user : users) {
                    String userName = user.getName();
                    String pass = user.getSaltedPassHash();
                    int permLvl = user.getPermLevel();

                    statement.setString(1, userName);
                    statement.setString(2, pass);
                    statement.setInt(3, permLvl);
                    statement.addBatch();
                }
            }
        });
    }
}
