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
package com.djrapitops.plan.db.access.queries.objects;

import com.djrapitops.plan.data.WebUser;
import com.djrapitops.plan.data.container.Ping;
import com.djrapitops.plan.db.access.Query;
import com.djrapitops.plan.db.access.QueryAllStatement;
import com.djrapitops.plan.db.access.QueryStatement;
import com.djrapitops.plan.db.sql.tables.PingTable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Queries for {@link WebUser} objects.
 *
 * @author Rsl1122
 */
public class PingQueries {

    private PingQueries() {
        /* Static method class */
    }

    /**
     * Query database for all Ping data.
     *
     * @return Map: Player UUID - List of ping data.
     */
    public static Query<Map<UUID, List<Ping>>> fetchAllPingData() {
        String sql = "SELECT " +
                PingTable.DATE + ", " +
                PingTable.MAX_PING + ", " +
                PingTable.MIN_PING + ", " +
                PingTable.AVG_PING + ", " +
                PingTable.USER_UUID + ", " +
                PingTable.SERVER_UUID +
                " FROM " + PingTable.TABLE_NAME;
        return new QueryAllStatement<Map<UUID, List<Ping>>>(sql, 100000) {
            @Override
            public Map<UUID, List<Ping>> processResults(ResultSet set) throws SQLException {
                Map<UUID, List<Ping>> userPings = new HashMap<>();

                while (set.next()) {
                    UUID uuid = UUID.fromString(set.getString(PingTable.USER_UUID));
                    UUID serverUUID = UUID.fromString(set.getString(PingTable.SERVER_UUID));
                    long date = set.getLong(PingTable.DATE);
                    double avgPing = set.getDouble(PingTable.AVG_PING);
                    int minPing = set.getInt(PingTable.MIN_PING);
                    int maxPing = set.getInt(PingTable.MAX_PING);

                    List<Ping> pings = userPings.getOrDefault(uuid, new ArrayList<>());
                    pings.add(new Ping(date, serverUUID,
                            minPing,
                            maxPing,
                            avgPing));
                    userPings.put(uuid, pings);
                }

                return userPings;
            }
        };
    }

    /**
     * Query database for Ping data of a specific player.
     *
     * @param playerUUID UUID of the player.
     * @return List of Ping entries for this player.
     */
    public static Query<List<Ping>> fetchPingDataOfPlayer(UUID playerUUID) {
        String sql = "SELECT * FROM " + PingTable.TABLE_NAME +
                " WHERE " + PingTable.USER_UUID + "=?";

        return new QueryStatement<List<Ping>>(sql, 10000) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, playerUUID.toString());
            }

            @Override
            public List<Ping> processResults(ResultSet set) throws SQLException {
                List<Ping> pings = new ArrayList<>();

                while (set.next()) {
                    pings.add(new Ping(
                                    set.getLong(PingTable.DATE),
                                    UUID.fromString(set.getString(PingTable.SERVER_UUID)),
                                    set.getInt(PingTable.MIN_PING),
                                    set.getInt(PingTable.MAX_PING),
                                    set.getDouble(PingTable.AVG_PING)
                            )
                    );
                }

                return pings;
            }
        };
    }

    public static Query<Map<UUID, List<Ping>>> fetchPingDataOfServer(UUID serverUUID) {
        String sql = "SELECT " +
                PingTable.DATE + ", " +
                PingTable.MAX_PING + ", " +
                PingTable.MIN_PING + ", " +
                PingTable.AVG_PING + ", " +
                PingTable.USER_UUID + ", " +
                PingTable.SERVER_UUID +
                " FROM " + PingTable.TABLE_NAME +
                " WHERE " + PingTable.SERVER_UUID + "=?";
        return new QueryStatement<Map<UUID, List<Ping>>>(sql, 100000) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, serverUUID.toString());
            }

            @Override
            public Map<UUID, List<Ping>> processResults(ResultSet set) throws SQLException {
                Map<UUID, List<Ping>> userPings = new HashMap<>();

                while (set.next()) {
                    UUID uuid = UUID.fromString(set.getString(PingTable.USER_UUID));
                    UUID serverUUID = UUID.fromString(set.getString(PingTable.SERVER_UUID));
                    long date = set.getLong(PingTable.DATE);
                    double avgPing = set.getDouble(PingTable.AVG_PING);
                    int minPing = set.getInt(PingTable.MIN_PING);
                    int maxPing = set.getInt(PingTable.MAX_PING);

                    List<Ping> pings = userPings.getOrDefault(uuid, new ArrayList<>());
                    pings.add(new Ping(date, serverUUID,
                            minPing,
                            maxPing,
                            avgPing));
                    userPings.put(uuid, pings);
                }

                return userPings;
            }
        };
    }
}