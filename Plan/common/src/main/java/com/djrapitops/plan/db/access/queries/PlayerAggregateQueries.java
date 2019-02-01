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
package com.djrapitops.plan.db.access.queries;

import com.djrapitops.plan.data.time.GMTimes;
import com.djrapitops.plan.data.time.WorldTimes;
import com.djrapitops.plan.db.access.Query;
import com.djrapitops.plan.db.access.QueryStatement;
import com.djrapitops.plan.db.sql.tables.WorldTable;
import com.djrapitops.plan.db.sql.tables.WorldTimesTable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Static method class for queries that count how many entries of particular kinds there are for a player.
 *
 * @author Rsl1122
 */
public class PlayerAggregateQueries {

    private PlayerAggregateQueries() {
        /* Static method class */
    }

    /**
     * Sum total playtime per world on all servers.
     *
     * @param playerUUID UUID of the player.
     * @return WorldTimes with world name - playtime ms information.
     */
    public static Query<WorldTimes> totalWorldTimes(UUID playerUUID) {
        String worldIDColumn = WorldTable.TABLE_NAME + "." + WorldTable.ID;
        String worldNameColumn = WorldTable.TABLE_NAME + "." + WorldTable.NAME + " as world";
        String sql = "SELECT " +
                "SUM(" + WorldTimesTable.SURVIVAL + ") as survival, " +
                "SUM(" + WorldTimesTable.CREATIVE + ") as creative, " +
                "SUM(" + WorldTimesTable.ADVENTURE + ") as adventure, " +
                "SUM(" + WorldTimesTable.SPECTATOR + ") as spectator, " +
                worldNameColumn +
                " FROM " + WorldTimesTable.TABLE_NAME +
                " INNER JOIN " + WorldTable.TABLE_NAME + " on " + worldIDColumn + "=" + WorldTimesTable.WORLD_ID +
                " WHERE " + WorldTimesTable.USER_UUID + "=?" +
                " GROUP BY world";

        return new QueryStatement<WorldTimes>(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, playerUUID.toString());
            }

            @Override
            public WorldTimes processResults(ResultSet set) throws SQLException {
                String[] gms = GMTimes.getGMKeyArray();

                WorldTimes worldTimes = new WorldTimes(new HashMap<>());
                while (set.next()) {
                    String worldName = set.getString("world");

                    Map<String, Long> gmMap = new HashMap<>();
                    gmMap.put(gms[0], set.getLong("survival"));
                    gmMap.put(gms[1], set.getLong("creative"));
                    gmMap.put(gms[2], set.getLong("adventure"));
                    gmMap.put(gms[3], set.getLong("spectator"));
                    GMTimes gmTimes = new GMTimes(gmMap);

                    worldTimes.setGMTimesForWorld(worldName, gmTimes);
                }
                return worldTimes;
            }
        };
    }

}