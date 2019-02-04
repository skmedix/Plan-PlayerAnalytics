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
import com.djrapitops.plan.db.sql.tables.TPSTable;

public class DiskUsagePatch extends Patch {

    public DiskUsagePatch(SQLDB db) {
        super(db);
    }

    @Override
    public boolean hasBeenApplied() {
        return hasColumn(TPSTable.TABLE_NAME, TPSTable.Col.FREE_DISK.get());
    }

    @Override
    protected void applyPatch() {
        addColumn(TPSTable.TABLE_NAME,
                TPSTable.Col.FREE_DISK + " bigint NOT NULL DEFAULT -1"
        );
    }
}
