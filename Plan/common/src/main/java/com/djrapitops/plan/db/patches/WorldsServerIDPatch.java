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

import com.djrapitops.plan.db.SQLDB;
import com.djrapitops.plan.db.access.ExecBatchStatement;
import com.djrapitops.plan.db.access.QueryAllStatement;
import com.djrapitops.plan.db.access.QueryStatement;
import com.djrapitops.plan.db.access.queries.LargeStoreQueries;
import com.djrapitops.plan.db.access.queries.objects.ServerQueries;
import com.djrapitops.plan.db.sql.tables.SessionsTable;
import com.djrapitops.plan.db.sql.tables.WorldTable;
import com.djrapitops.plan.db.sql.tables.WorldTimesTable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WorldsServerIDPatch extends Patch {

    public WorldsServerIDPatch(SQLDB db) {
        super(db);
    }

    @Override
    public boolean hasBeenApplied() {
        String tableName = WorldTable.TABLE_NAME;
        String columnName = "server_id";

        // WorldsOptimizationPatch makes this patch incompatible with newer patch versions.
        return hasColumn(tableName, "server_uuid")
                || hasColumn(tableName, columnName)
                && allValuesHaveServerID(tableName, columnName);
    }

    private Boolean allValuesHaveServerID(String tableName, String columnName) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + columnName + "=? LIMIT 1";
        return query(new QueryStatement<Boolean>(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setInt(1, 0);
            }

            @Override
            public Boolean processResults(ResultSet set) throws SQLException {
                return !set.next();
            }
        });
    }

    @Override
    protected void applyPatch() {
        Collection<UUID> serverUUIDs = db.query(ServerQueries.fetchPlanServerInformation()).keySet();

        Map<UUID, Collection<String>> worldsPerServer = new HashMap<>();
        for (UUID serverUUID : serverUUIDs) {
            worldsPerServer.put(serverUUID, getWorldNamesOld(serverUUID));
        }

        execute(LargeStoreQueries.storeAllWorldNames(worldsPerServer));

        updateWorldTimesTableWorldIDs();
        executeSwallowingExceptions("DELETE FROM " + WorldTable.TABLE_NAME + " WHERE server_id=0");
    }

    private Set<String> getWorldNamesOld(UUID serverUUID) {
        WorldTimesTable worldTimesTable = db.getWorldTimesTable();
        SessionsTable sessionsTable = db.getSessionsTable();

        String worldIDColumn = worldTimesTable + "." + WorldTimesTable.WORLD_ID;
        String worldSessionIDColumn = worldTimesTable + "." + WorldTimesTable.SESSION_ID;
        String sessionIDColumn = sessionsTable + "." + SessionsTable.ID;
        String sessionServerUUIDColumn = sessionsTable + "." + SessionsTable.SERVER_UUID;

        String sql = "SELECT DISTINCT " +
                WorldTable.NAME + " FROM " +
                WorldTable.TABLE_NAME +
                " INNER JOIN " + worldTimesTable + " on " + worldIDColumn + "=" + WorldTable.TABLE_NAME + "." + WorldTable.ID +
                " INNER JOIN " + sessionsTable + " on " + worldSessionIDColumn + "=" + sessionIDColumn +
                " WHERE " + sessionServerUUIDColumn + "=?";

        return query(new QueryStatement<Set<String>>(sql, 1000) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, serverUUID.toString());
            }

            @Override
            public Set<String> processResults(ResultSet set) throws SQLException {
                Set<String> worldNames = new HashSet<>();
                while (set.next()) {
                    worldNames.add(set.getString(WorldTable.NAME));
                }
                return worldNames;
            }
        });
    }

    private void updateWorldTimesTableWorldIDs() {
        List<WorldObj> worldObjects = getWorldObjects();
        Map<WorldObj, List<WorldObj>> oldToNewMap =
                worldObjects.stream()
                        .filter(worldObj -> worldObj.serverId == 0)
                        .collect(Collectors.toMap(
                                Function.identity(),
                                oldWorld -> worldObjects.stream()
                                        .filter(worldObj -> worldObj.serverId != 0)
                                        .filter(worldObj -> worldObj.equals(oldWorld))
                                        .collect(Collectors.toList()
                                        )));

        WorldTimesTable worldTimesTable = db.getWorldTimesTable();
        String sql = "UPDATE " + worldTimesTable + " SET " +
                WorldTimesTable.WORLD_ID + "=?" +
                " WHERE " + WorldTimesTable.WORLD_ID + "=?" +
                " AND " + "server_id=?";
        execute(new ExecBatchStatement(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                for (Map.Entry<WorldObj, List<WorldObj>> entry : oldToNewMap.entrySet()) {
                    WorldObj old = entry.getKey();
                    for (WorldObj newWorld : entry.getValue()) {
                        statement.setInt(1, newWorld.id);
                        statement.setInt(2, old.id);
                        statement.setInt(3, newWorld.serverId);
                        statement.addBatch();
                    }
                }
            }
        });
    }

    public List<WorldObj> getWorldObjects() {
        String sql = "SELECT * FROM " + WorldTable.TABLE_NAME;
        return query(new QueryAllStatement<List<WorldObj>>(sql, 100) {
            @Override
            public List<WorldObj> processResults(ResultSet set) throws SQLException {
                List<WorldObj> objects = new ArrayList<>();
                while (set.next()) {
                    int worldID = set.getInt(WorldTable.ID);
                    int serverID = set.getInt("server_id");
                    String worldName = set.getString(WorldTable.NAME);
                    objects.add(new WorldObj(worldID, serverID, worldName));
                }
                return objects;
            }
        });
    }
}

class WorldObj {
    final int id;
    final int serverId;
    final String name;

    public WorldObj(int id, int serverId, String name) {
        this.id = id;
        this.serverId = serverId;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldObj worldObj = (WorldObj) o;
        return Objects.equals(name, worldObj.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", name='" + name + '\'' +
                '}';
    }
}
