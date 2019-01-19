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
import com.djrapitops.plan.db.access.QueryAllStatement;
import com.djrapitops.plan.db.sql.tables.SessionsTable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Patch that resets AFK time of sessions with afk time of length of the session to 0.
 * <p>
 * This is a bug remedy patch that attempts to turn "bad" afk data to good.
 * In 4.5.2 there was a bug that caused some config setting defaults not being copied, along those
 * AFKThreshold setting, which lead to AFK threshold being read as 0.
 * This in turn lead to full sessions being regarded as having been AFK.
 *
 * @author Rsl1122
 */
public class BadAFKThresholdValuePatch extends Patch {

    public BadAFKThresholdValuePatch(SQLDB db) {
        super(db);
    }

    @Override
    public boolean hasBeenApplied() {
        return !containsSessionsWithFullAFK();
    }

    private boolean containsSessionsWithFullAFK() {
        // where |afk - session_length| < 5
        String sql = "SELECT COUNT(1) as found FROM " + SessionsTable.TABLE_NAME +
                " WHERE ABS(" +
                SessionsTable.Col.AFK_TIME +
                " - (" + SessionsTable.Col.SESSION_END + "-" + SessionsTable.Col.SESSION_START +
                ")) < 5 AND " + SessionsTable.Col.AFK_TIME + "!=0";
        return query(new QueryAllStatement<Boolean>(sql) {
            @Override
            public Boolean processResults(ResultSet set) throws SQLException {
                return set.next() && set.getInt("found") > 0;
            }
        });
    }

    @Override
    protected void applyPatch() {
        // where |afk - session_length| < 5
        String sql = "UPDATE " + SessionsTable.TABLE_NAME + " SET " + SessionsTable.Col.AFK_TIME + "=0 WHERE ABS(" +
                SessionsTable.Col.AFK_TIME +
                " - (" + SessionsTable.Col.SESSION_END + "-" + SessionsTable.Col.SESSION_START +
                ")) < 5 AND " + SessionsTable.Col.AFK_TIME + "!=0";
        db.execute(sql);
    }
}