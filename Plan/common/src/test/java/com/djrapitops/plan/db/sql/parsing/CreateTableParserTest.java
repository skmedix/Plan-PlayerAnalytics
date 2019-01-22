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
package com.djrapitops.plan.db.sql.parsing;

import com.djrapitops.plan.db.DBType;
import com.djrapitops.plan.db.sql.tables.ServerTable;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link CreateTableParser}.
 *
 * @author Rsl1122
 */
@RunWith(JUnitPlatform.class)
class CreateTableParserTest {

    @Test
    void createsSameTablesAsOldParser() {
        String expected = "CREATE TABLE IF NOT EXISTS plan_servers (id integer NOT NULL AUTO_INCREMENT, uuid varchar(36) NOT NULL UNIQUE, name varchar(100), web_address varchar(100), is_installed boolean NOT NULL DEFAULT 1, max_players integer NOT NULL DEFAULT -1, PRIMARY KEY (id))";
        String result = CreateTableParser.create(ServerTable.TABLE_NAME, DBType.MYSQL)
                .column(ServerTable.Col.SERVER_ID.get(), Sql.INT)
                .primaryKey()
                .column(ServerTable.Col.SERVER_UUID.get(), Sql.varchar(36)).notNull().unique()
                .column(ServerTable.Col.NAME.get(), Sql.varchar(100))
                .column(ServerTable.Col.WEBSERVER_ADDRESS.get(), Sql.varchar(100))
                .column(ServerTable.Col.INSTALLED.get(), Sql.BOOL).notNull().defaultValue(true)
                .column(ServerTable.Col.MAX_PLAYERS.get(), Sql.INT).notNull().defaultValue("-1")
                .toString();
        assertEquals(expected, result);
    }

}