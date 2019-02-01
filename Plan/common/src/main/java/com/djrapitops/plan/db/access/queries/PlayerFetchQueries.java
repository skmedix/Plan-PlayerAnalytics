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

import com.djrapitops.plan.data.container.GeoInfo;
import com.djrapitops.plan.db.access.Query;
import com.djrapitops.plan.db.access.QueryStatement;
import com.djrapitops.plan.db.sql.tables.GeoInfoTable;
import com.djrapitops.plan.db.sql.tables.UsersTable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Static method class for queries that return information related to a single player.
 *
 * @author Rsl1122
 */
public class PlayerFetchQueries {

    private PlayerFetchQueries() {
        /* static method class */
    }

    /**
     * Query Player's name by player's UUID.
     *
     * @param playerUUID UUID of the player.
     * @return Optional, Name if found.
     */
    public static Query<Optional<String>> playerUserName(UUID playerUUID) {
        String sql = "SELECT " + UsersTable.USER_NAME +
                " FROM " + UsersTable.TABLE_NAME +
                " WHERE " + UsersTable.USER_UUID + "=?";
        return new QueryStatement<Optional<String>>(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, playerUUID.toString());
            }

            @Override
            public Optional<String> processResults(ResultSet set) throws SQLException {
                if (set.next()) {
                    return Optional.of(set.getString(UsersTable.USER_NAME));
                }
                return Optional.empty();
            }
        };
    }

    /**
     * Query Player's GeoInfo by player's UUID.
     *
     * @param playerUUID UUID of the player.
     * @return List of {@link GeoInfo}, empty if none are found.
     */
    public static Query<List<GeoInfo>> playerGeoInfo(UUID playerUUID) {
        String sql = "SELECT DISTINCT * FROM " + GeoInfoTable.TABLE_NAME +
                " WHERE " + GeoInfoTable.USER_UUID + "=?";

        return new QueryStatement<List<GeoInfo>>(sql, 100) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, playerUUID.toString());
            }

            @Override
            public List<GeoInfo> processResults(ResultSet set) throws SQLException {
                List<GeoInfo> geoInfo = new ArrayList<>();
                while (set.next()) {
                    String ip = set.getString(GeoInfoTable.IP);
                    String geolocation = set.getString(GeoInfoTable.GEOLOCATION);
                    String ipHash = set.getString(GeoInfoTable.IP_HASH);
                    long lastUsed = set.getLong(GeoInfoTable.LAST_USED);
                    geoInfo.add(new GeoInfo(ip, geolocation, lastUsed, ipHash));
                }
                return geoInfo;
            }
        };
    }
}