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
package com.djrapitops.plan.db.access.transactions;

import com.djrapitops.plan.db.DBType;
import com.djrapitops.plan.db.sql.tables.*;

/**
 * Transaction that creates the table schema of Plan database.
 *
 * @author Rsl1122
 */
public class CreateTablesTransaction extends OperationCriticalTransaction {

    @Override
    protected void execute() {
        // DBType is required for SQL parsing, as MySQL and SQLite primary key format differs.
        DBType dbType = getDBType();

        // Create statements are run in a specific order as some tables have foreign keys,
        // or had at some point in the past.
        execute(ServerTable.createTableSQL(dbType));
        execute(UsersTable.createTableSQL(dbType));
        execute(UserInfoTable.createTableSQL(dbType));
        execute(GeoInfoTable.createTableSQL(dbType));
        execute(NicknamesTable.createTableSQL(dbType));
        execute(SessionsTable.createTableSQL(dbType));
        execute(KillsTable.createTableSQL(dbType));
        execute(PingTable.createTableSQL(dbType));
        execute(CommandUseTable.createTableSQL(dbType));
        execute(TPSTable.createTableSQL(dbType));
        execute(WorldTable.createTableSQL(dbType));
        execute(WorldTimesTable.createTableSQL(dbType));
        execute(SecurityTable.createTableSQL(dbType));
        execute(SettingsTable.createTableSQL(dbType));
    }
}