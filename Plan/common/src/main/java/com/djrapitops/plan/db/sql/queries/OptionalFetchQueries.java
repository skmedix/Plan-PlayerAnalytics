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
package com.djrapitops.plan.db.sql.queries;

import com.djrapitops.plan.db.access.Query;
import com.djrapitops.plan.db.access.QueryStatement;
import com.djrapitops.plan.db.sql.tables.ServerTable;
import com.djrapitops.plan.system.info.server.Server;
import org.apache.commons.lang3.math.NumberUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Static method class for queries that return single item if found.
 *
 * @author Rsl1122
 */
public class OptionalFetchQueries {

    private OptionalFetchQueries() {
        /* Static method class */
    }

    public static Query<Optional<Server>> matchingServerIdentifier(String identifier) {
        String sql = "SELECT * FROM " + ServerTable.TABLE_NAME +
                " WHERE (" + ServerTable.Col.SERVER_ID + "=?" +
                " OR LOWER(" + ServerTable.Col.NAME + ") LIKE LOWER(?)" +
                " OR LOWER(" + ServerTable.Col.SERVER_UUID + ") LIKE LOWER(?))" +
                " AND " + ServerTable.Col.INSTALLED + "=?" +
                " LIMIT 1";
        return new QueryStatement<Optional<Server>>(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setInt(1, NumberUtils.isParsable(identifier) ? Integer.parseInt(identifier) : -1);
                statement.setString(2, identifier);
                statement.setString(3, identifier);
                statement.setBoolean(4, true);
            }

            @Override
            public Optional<Server> processResults(ResultSet set) throws SQLException {
                if (set.next()) {
                    return Optional.of(new Server(
                            set.getInt(ServerTable.Col.SERVER_ID.get()),
                            UUID.fromString(set.getString(ServerTable.Col.SERVER_UUID.get())),
                            set.getString(ServerTable.Col.NAME.get()),
                            set.getString(ServerTable.Col.WEBSERVER_ADDRESS.get()),
                            set.getInt(ServerTable.Col.MAX_PLAYERS.get())
                    ));
                }
                return Optional.empty();
            }
        };
    }

    public static Query<Optional<Server>> proxyServerInformation() {
        return db -> db.query(matchingServerIdentifier("BungeeCord"));
    }

}