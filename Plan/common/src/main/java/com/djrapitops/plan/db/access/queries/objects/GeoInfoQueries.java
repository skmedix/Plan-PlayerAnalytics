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

import com.djrapitops.plan.data.container.GeoInfo;
import com.djrapitops.plan.db.access.Query;
import com.djrapitops.plan.db.access.QueryAllStatement;
import com.djrapitops.plan.db.access.QueryStatement;
import com.djrapitops.plan.db.sql.tables.GeoInfoTable;
import com.djrapitops.plan.db.sql.tables.UserInfoTable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Queries for {@link com.djrapitops.plan.data.container.GeoInfo} objects.
 *
 * @author Rsl1122
 */
public class GeoInfoQueries {

    private GeoInfoQueries() {
        /* Static method class */
    }

    /**
     * Query database for all GeoInfo data.
     *
     * @return Map: Player UUID - List of GeoInfo
     */
    public static Query<Map<UUID, List<GeoInfo>>> fetchAllGeoInformation() {
        String sql = "SELECT " +
                GeoInfoTable.IP + ", " +
                GeoInfoTable.GEOLOCATION + ", " +
                GeoInfoTable.LAST_USED + ", " +
                GeoInfoTable.IP_HASH + ", " +
                GeoInfoTable.USER_UUID +
                " FROM " + GeoInfoTable.TABLE_NAME;

        return new QueryAllStatement<Map<UUID, List<GeoInfo>>>(sql, 50000) {
            @Override
            public Map<UUID, List<GeoInfo>> processResults(ResultSet set) throws SQLException {
                Map<UUID, List<GeoInfo>> geoInformation = new HashMap<>();
                while (set.next()) {
                    UUID uuid = UUID.fromString(set.getString(GeoInfoTable.USER_UUID));

                    List<GeoInfo> userGeoInfo = geoInformation.getOrDefault(uuid, new ArrayList<>());

                    String ip = set.getString(GeoInfoTable.IP);
                    String geolocation = set.getString(GeoInfoTable.GEOLOCATION);
                    String ipHash = set.getString(GeoInfoTable.IP_HASH);
                    long lastUsed = set.getLong(GeoInfoTable.LAST_USED);
                    userGeoInfo.add(new GeoInfo(ip, geolocation, lastUsed, ipHash));

                    geoInformation.put(uuid, userGeoInfo);
                }
                return geoInformation;
            }
        };
    }

    /**
     * Query Player's GeoInfo by player's UUID.
     *
     * @param playerUUID UUID of the player.
     * @return List of {@link GeoInfo}, empty if none are found.
     */
    public static Query<List<GeoInfo>> fetchPlayerGeoInformation(UUID playerUUID) {
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

    public static Query<Map<UUID, List<GeoInfo>>> fetchServerGeoInformation(UUID serverUUID) {
        String sql = "SELECT " + GeoInfoTable.TABLE_NAME + "." + GeoInfoTable.USER_UUID + ", " +
                GeoInfoTable.GEOLOCATION + ", " +
                GeoInfoTable.LAST_USED + ", " +
                GeoInfoTable.IP + ", " +
                GeoInfoTable.IP_HASH +
                " FROM " + GeoInfoTable.TABLE_NAME +
                " INNER JOIN " + UserInfoTable.TABLE_NAME + " on " +
                GeoInfoTable.TABLE_NAME + "." + GeoInfoTable.USER_UUID + "=" + UserInfoTable.TABLE_NAME + "." + UserInfoTable.USER_UUID +
                " WHERE " + UserInfoTable.SERVER_UUID + "=?";
        return new QueryStatement<Map<UUID, List<GeoInfo>>>(sql, 10000) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, serverUUID.toString());
            }

            @Override
            public Map<UUID, List<GeoInfo>> processResults(ResultSet set) throws SQLException {
                Map<UUID, List<GeoInfo>> geoInformation = new HashMap<>();
                while (set.next()) {
                    UUID uuid = UUID.fromString(set.getString(GeoInfoTable.USER_UUID));

                    List<GeoInfo> userGeoInfo = geoInformation.getOrDefault(uuid, new ArrayList<>());

                    String ip = set.getString(GeoInfoTable.IP);
                    String geolocation = set.getString(GeoInfoTable.GEOLOCATION);
                    String ipHash = set.getString(GeoInfoTable.IP_HASH);
                    long lastUsed = set.getLong(GeoInfoTable.LAST_USED);
                    userGeoInfo.add(new GeoInfo(ip, geolocation, lastUsed, ipHash));

                    geoInformation.put(uuid, userGeoInfo);
                }
                return geoInformation;
            }
        };
    }
}