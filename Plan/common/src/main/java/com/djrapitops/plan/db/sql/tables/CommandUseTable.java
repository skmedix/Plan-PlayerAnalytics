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
package com.djrapitops.plan.db.sql.tables;

import com.djrapitops.plan.api.exceptions.database.DBInitException;
import com.djrapitops.plan.db.SQLDB;
import com.djrapitops.plan.db.access.ExecStatement;
import com.djrapitops.plan.db.access.QueryAllStatement;
import com.djrapitops.plan.db.access.QueryStatement;
import com.djrapitops.plan.db.sql.parsing.Column;
import com.djrapitops.plan.db.sql.parsing.Select;
import com.djrapitops.plan.db.sql.parsing.Sql;
import com.djrapitops.plan.db.sql.parsing.TableSqlParser;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Table that is in charge of storing command data.
 * <p>
 * Table Name: plan_commandusages
 * <p>
 * For contained columns {@link Col}
 *
 * @author Rsl1122
 */
public class CommandUseTable extends Table {

    public CommandUseTable(SQLDB db) {
        super("plan_commandusages", db);
        serverTable = db.getServerTable();
        insertStatement = "INSERT INTO " + tableName + " ("
                + Col.COMMAND + ", "
                + Col.TIMES_USED + ", "
                + Col.SERVER_ID
                + ") VALUES (?, ?, " + serverTable.statementSelectServerID + ")";
    }

    private final ServerTable serverTable;
    private String insertStatement;

    @Override
    public void createTable() throws DBInitException {
        ServerTable serverTable = db.getServerTable();
        createTable(TableSqlParser.createTable(tableName)
                .primaryKeyIDColumn(supportsMySQLQueries, Col.COMMAND_ID)
                .column(Col.COMMAND, Sql.varchar(20)).notNull()
                .column(Col.TIMES_USED, Sql.INT).notNull()
                .column(Col.SERVER_ID, Sql.INT).notNull()
                .primaryKey(supportsMySQLQueries, Col.COMMAND_ID)
                .foreignKey(Col.SERVER_ID, serverTable.toString(), ServerTable.Col.SERVER_ID)
                .toString()
        );
    }

    /**
     * Used to get all commands used in a server.
     *
     * @param serverUUID UUID of the server.
     * @return command - times used Map
     */
    public Map<String, Integer> getCommandUse(UUID serverUUID) {
        String sql = Select.from(tableName,
                Col.COMMAND, Col.TIMES_USED)
                .where(Col.SERVER_ID + "=" + serverTable.statementSelectServerID)
                .toString();

        return query(new QueryStatement<Map<String, Integer>>(sql, 5000) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, serverUUID.toString());
            }

            @Override
            public Map<String, Integer> processResults(ResultSet set) throws SQLException {
                Map<String, Integer> commandUse = new HashMap<>();
                while (set.next()) {
                    String cmd = set.getString(Col.COMMAND.get()).toLowerCase();
                    int amountUsed = set.getInt(Col.TIMES_USED.get());
                    commandUse.put(cmd, amountUsed);
                }
                return commandUse;
            }
        });
    }

    /**
     * Used to get all commands used in this server.
     *
     * @return command - times used Map
     */
    public Map<String, Integer> getCommandUse() {
        return getCommandUse(getServerUUID());
    }

    public void commandUsed(String command) {
        if (command.length() > 20) {
            return;
        }

        String sql = "UPDATE " + tableName + " SET "
                + Col.TIMES_USED + "=" + Col.TIMES_USED + "+ 1" +
                " WHERE " + Col.SERVER_ID + "=" + serverTable.statementSelectServerID +
                " AND " + Col.COMMAND + "=?";

        boolean updated = execute(new ExecStatement(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, getServerUUID().toString());
                statement.setString(2, command);
            }
        });
        if (!updated) {
            insertCommand(command);
        }
    }

    public Optional<String> getCommandByID(int id) {
        String sql = Select.from(tableName, Col.COMMAND).where(Col.COMMAND_ID + "=?").toString();

        return query(new QueryStatement<Optional<String>>(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setInt(1, id);
            }

            @Override
            public Optional<String> processResults(ResultSet set) throws SQLException {
                if (set.next()) {
                    return Optional.of(set.getString(Col.COMMAND.get()));
                }
                return Optional.empty();
            }
        });
    }

    private void insertCommand(String command) {
        execute(new ExecStatement(insertStatement) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, command);
                statement.setInt(2, 1);
                statement.setString(3, getServerUUID().toString());
            }
        });
    }

    public Optional<Integer> getCommandID(String command) {
        String sql = Select.from(tableName, Col.COMMAND_ID).where(Col.COMMAND + "=?").toString();

        return query(new QueryStatement<Optional<Integer>>(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, command);
            }

            @Override
            public Optional<Integer> processResults(ResultSet set) throws SQLException {
                if (set.next()) {
                    return Optional.of(set.getInt(Col.COMMAND_ID.get()));
                }
                return Optional.empty();
            }
        });
    }

    public Map<UUID, Map<String, Integer>> getAllCommandUsages() {
        String serverIDColumn = serverTable + "." + ServerTable.Col.SERVER_ID;
        String serverUUIDColumn = serverTable + "." + ServerTable.Col.SERVER_UUID + " as s_uuid";
        String sql = "SELECT " +
                Col.COMMAND + ", " +
                Col.TIMES_USED + ", " +
                serverUUIDColumn +
                " FROM " + tableName +
                " INNER JOIN " + serverTable + " on " + serverIDColumn + "=" + Col.SERVER_ID;

        return query(new QueryAllStatement<Map<UUID, Map<String, Integer>>>(sql, 10000) {
            @Override
            public Map<UUID, Map<String, Integer>> processResults(ResultSet set) throws SQLException {
                Map<UUID, Map<String, Integer>> map = new HashMap<>();
                while (set.next()) {
                    UUID serverUUID = UUID.fromString(set.getString("s_uuid"));

                    Map<String, Integer> serverMap = map.getOrDefault(serverUUID, new HashMap<>());

                    String command = set.getString(Col.COMMAND.get());
                    int timesUsed = set.getInt(Col.TIMES_USED.get());

                    serverMap.put(command, timesUsed);
                    map.put(serverUUID, serverMap);
                }
                return map;
            }
        });
    }

    public enum Col implements Column {
        COMMAND_ID("id"),
        SERVER_ID("server_id"),
        COMMAND("command"),
        TIMES_USED("times_used");

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

    public void insertCommandUsage(Map<UUID, Map<String, Integer>> allCommandUsages) {
        if (allCommandUsages.isEmpty()) {
            return;
        }

        executeBatch(new ExecStatement(insertStatement) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                // Every Server
                for (UUID serverUUID : allCommandUsages.keySet()) {
                    // Every Command
                    for (Map.Entry<String, Integer> entry : allCommandUsages.get(serverUUID).entrySet()) {
                        String command = entry.getKey();
                        int timesUsed = entry.getValue();

                        statement.setString(1, command);
                        statement.setInt(2, timesUsed);
                        statement.setString(3, serverUUID.toString());
                        statement.addBatch();
                    }
                }
            }
        });
    }
}
