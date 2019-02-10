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

import com.djrapitops.plan.data.store.objects.Nickname;
import com.djrapitops.plan.db.access.Query;
import com.djrapitops.plan.db.access.QueryAllStatement;
import com.djrapitops.plan.db.access.QueryStatement;
import com.djrapitops.plan.db.sql.tables.NicknamesTable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Queries for {@link com.djrapitops.plan.data.store.objects.Nickname} objects.
 *
 * @author Rsl1122
 */
public class NicknameQueries {

    private NicknameQueries() {
        /* Static method class */
    }

    /**
     * Query database for all nickname data.
     *
     * @return Multimap: Server UUID - (Player UUID - List of nicknames)
     */
    public static Query<Map<UUID, Map<UUID, List<Nickname>>>> fetchAllNicknameData() {
        String sql = "SELECT " +
                NicknamesTable.NICKNAME + ", " +
                NicknamesTable.LAST_USED + ", " +
                NicknamesTable.USER_UUID + ", " +
                NicknamesTable.SERVER_UUID +
                " FROM " + NicknamesTable.TABLE_NAME;

        return new QueryAllStatement<Map<UUID, Map<UUID, List<Nickname>>>>(sql, 5000) {
            @Override
            public Map<UUID, Map<UUID, List<Nickname>>> processResults(ResultSet set) throws SQLException {
                Map<UUID, Map<UUID, List<Nickname>>> map = new HashMap<>();
                while (set.next()) {
                    UUID serverUUID = UUID.fromString(set.getString(NicknamesTable.SERVER_UUID));
                    UUID uuid = UUID.fromString(set.getString(NicknamesTable.USER_UUID));

                    Map<UUID, List<Nickname>> serverMap = map.getOrDefault(serverUUID, new HashMap<>());
                    List<Nickname> nicknames = serverMap.getOrDefault(uuid, new ArrayList<>());

                    nicknames.add(new Nickname(
                            set.getString(NicknamesTable.NICKNAME),
                            set.getLong(NicknamesTable.LAST_USED),
                            serverUUID
                    ));

                    serverMap.put(uuid, nicknames);
                    map.put(serverUUID, serverMap);
                }
                return map;
            }
        };
    }

    public static Query<Optional<Nickname>> fetchPlayersLastSeenNickname(UUID playerUUID, UUID serverUUID) {
        String subQuery = "SELECT MAX(" + NicknamesTable.LAST_USED + ") FROM " + NicknamesTable.TABLE_NAME +
                " WHERE " + NicknamesTable.USER_UUID + "=?" +
                " AND " + NicknamesTable.SERVER_UUID + "=?" +
                " GROUP BY " + NicknamesTable.USER_UUID;
        String sql = "SELECT " + NicknamesTable.LAST_USED + ", " +
                NicknamesTable.NICKNAME + " FROM " + NicknamesTable.TABLE_NAME +
                " WHERE " + NicknamesTable.USER_UUID + "=?" +
                " AND " + NicknamesTable.SERVER_UUID + "=?" +
                " AND " + NicknamesTable.LAST_USED + "=(" + subQuery + ")";
        return new QueryStatement<Optional<Nickname>>(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, playerUUID.toString());
                statement.setString(2, serverUUID.toString());
                statement.setString(3, playerUUID.toString());
                statement.setString(4, serverUUID.toString());
            }

            @Override
            public Optional<Nickname> processResults(ResultSet set) throws SQLException {
                if (set.next()) {
                    return Optional.of(new Nickname(
                            set.getString(NicknamesTable.NICKNAME),
                            set.getLong(NicknamesTable.LAST_USED),
                            serverUUID
                    ));
                }
                return Optional.empty();
            }
        };
    }

    public static Query<List<Nickname>> fetchPlayersNicknameData(UUID playerUUID) {
        String sql = "SELECT " +
                NicknamesTable.NICKNAME + ", " +
                NicknamesTable.LAST_USED + ", " +
                NicknamesTable.SERVER_UUID +
                " FROM " + NicknamesTable.TABLE_NAME +
                " WHERE (" + NicknamesTable.USER_UUID + "=?)";

        return new QueryStatement<List<Nickname>>(sql, 5000) {

            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setString(1, playerUUID.toString());
            }

            @Override
            public List<Nickname> processResults(ResultSet set) throws SQLException {
                List<Nickname> nicknames = new ArrayList<>();
                while (set.next()) {
                    UUID serverUUID = UUID.fromString(set.getString(NicknamesTable.SERVER_UUID));
                    String nickname = set.getString(NicknamesTable.NICKNAME);
                    nicknames.add(new Nickname(nickname, set.getLong(NicknamesTable.LAST_USED), serverUUID));
                }
                return nicknames;
            }
        };
    }

    /**
     * Query database for all nickname data.
     *
     * @return Map: Player UUID - List of nicknames.
     */
    public static Query<Map<UUID, List<Nickname>>> fetchAllNicknameDataByPlayerUUIDs() {
        String sql = "SELECT " +
                NicknamesTable.NICKNAME + ", " +
                NicknamesTable.LAST_USED + ", " +
                NicknamesTable.USER_UUID + ", " +
                NicknamesTable.SERVER_UUID +
                " FROM " + NicknamesTable.TABLE_NAME;
        return new QueryAllStatement<Map<UUID, List<Nickname>>>(sql, 5000) {
            @Override
            public Map<UUID, List<Nickname>> processResults(ResultSet set) throws SQLException {
                Map<UUID, List<Nickname>> map = new HashMap<>();
                while (set.next()) {
                    UUID uuid = UUID.fromString(set.getString(NicknamesTable.USER_UUID));
                    UUID serverUUID = UUID.fromString(set.getString(NicknamesTable.SERVER_UUID));
                    List<Nickname> nicknames = map.computeIfAbsent(uuid, x -> new ArrayList<>());
                    nicknames.add(new Nickname(
                            set.getString(NicknamesTable.NICKNAME), set.getLong(NicknamesTable.LAST_USED), serverUUID
                    ));
                }
                return map;
            }
        };
    }
}