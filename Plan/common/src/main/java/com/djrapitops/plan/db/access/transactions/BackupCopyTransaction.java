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

import com.djrapitops.plan.db.Database;
import com.djrapitops.plan.db.SQLDB;
import com.djrapitops.plan.db.access.Executable;
import com.djrapitops.plan.db.access.Query;
import com.djrapitops.plan.db.sql.queries.LargeFetchQueries;
import com.djrapitops.plan.db.sql.queries.LargeStoreQueries;
import com.djrapitops.plan.db.sql.tables.UsersTable;

import java.util.function.Function;

/**
 * Transaction that performs a clear + copy operation to duplicate a source database in the current one.
 *
 * @author Rsl1122
 */
public class BackupCopyTransaction extends RemoveEverythingTransaction {

    private final Database sourceDB;

    public BackupCopyTransaction(Database sourceDB) {
        this.sourceDB = sourceDB;
    }

    @Override
    protected boolean shouldBeExecuted() {
        return !sourceDB.equals(db) && sourceDB.isOpen();
    }

    @Override
    protected void performOperations() {
        super.performOperations();

        copyServers();
        copyUsers();
        copyWorlds();
        copyTPS();
        copyWebUsers();
        copyCommandUsageData();
        copyIPsAndGeolocs();
        copyNicknames();
        copySessions();
        copyUserInfo();
        copyPings();
    }

    private <T> void copy(Function<T, Executable> executableCreator, Query<T> dataQuery) {
        // Creates a new Executable from the queried data of the source database
        execute(executableCreator.apply(sourceDB.query(dataQuery)));
    }

    private void copyPings() {
        db.getPingTable().insertAllPings(sourceDB.query(LargeFetchQueries.fetchAllPingData()));
    }

    private void copyCommandUsageData() {
        copy(LargeStoreQueries::storeAllCommandUsageData, LargeFetchQueries.fetchAllCommandUsageData());
    }

    private void copyIPsAndGeolocs() {
        copy(LargeStoreQueries::storeAllGeoInfoData, LargeFetchQueries.fetchAllGeoInfoData());
    }

    private void copyNicknames() {
        copy(LargeStoreQueries::storeAllNicknameData, LargeFetchQueries.fetchAllNicknameData());
    }

    private void copyWebUsers() {
        copy(LargeStoreQueries::storeAllPlanWebUsers, LargeFetchQueries.fetchAllPlanWebUsers());
    }

    private void copyServers() {
        copy(LargeStoreQueries::storeAllPlanServerInformation, LargeFetchQueries.fetchPlanServerInformationCollection());
    }

    private void copyTPS() {
        copy(LargeStoreQueries::storeAllTPSData, LargeFetchQueries.fetchAllTPSData());
    }

    private void copyUserInfo() {
        copy(LargeStoreQueries::storePerServerUserInformation, LargeFetchQueries.fetchPerServerUserInformation());
    }

    private void copyWorlds() {
        copy(LargeStoreQueries::storeAllWorldNames, LargeFetchQueries.fetchAllWorldNames());
    }

    private void copyUsers() {
        UsersTable fromTable = ((SQLDB) sourceDB).getUsersTable();
        UsersTable toTable = db.getUsersTable();

        toTable.insertUsers(sourceDB.query(LargeFetchQueries.fetchAllCommonUserInformation()));
        toTable.updateKicked(fromTable.getAllTimesKicked());
    }

    private void copySessions() {
        db.getSessionsTable().insertSessions(sourceDB.query(LargeFetchQueries.fetchAllSessionsWithKillAndWorldData()), true);
    }
}