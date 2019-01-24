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

import com.djrapitops.plan.api.exceptions.database.DBOpException;
import com.djrapitops.plan.db.SQLDB;
import com.djrapitops.plan.db.sql.tables.WorldTable;

public class WorldsOptimizationPatch extends Patch {

    private String tempTableName;
    private String tableName;

    public WorldsOptimizationPatch(SQLDB db) {
        super(db);
        tableName = WorldTable.TABLE_NAME;
        tempTableName = "temp_worlds";
    }

    @Override
    public boolean hasBeenApplied() {
        return hasColumn(tableName, WorldTable.ID)
                && hasColumn(tableName, WorldTable.SERVER_UUID)
                && !hasColumn(tableName, "server_id")
                && !hasTable(tempTableName); // If this table exists the patch has failed to finish.
    }

    @Override
    protected void applyPatch() {
        try {
            dropForeignKeys(tableName);
            ensureNoForeignKeyConstraints(tableName);

            tempOldTable();
            db.getWorldTable().createTable();

            execute("INSERT INTO " + tableName + " (" +
                    WorldTable.ID + ", " +
                    WorldTable.SERVER_UUID + ", " +
                    WorldTable.NAME +
                    ") SELECT " +
                    WorldTable.ID + ", " +
                    "(SELECT plan_servers.uuid FROM plan_servers WHERE plan_servers.id = " + tempTableName + ".server_id LIMIT 1), " +
                    WorldTable.NAME +
                    " FROM " + tempTableName
            );

            dropTable(tempTableName);
        } catch (Exception e) {
            throw new DBOpException(WorldsOptimizationPatch.class.getSimpleName() + " failed.", e);
        }
    }

    private void tempOldTable() {
        if (!hasTable(tempTableName)) {
            renameTable(tableName, tempTableName);
        }
    }
}
