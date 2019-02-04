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
package com.djrapitops.plan.db;

import com.djrapitops.plan.api.exceptions.database.DBException;
import com.djrapitops.plan.api.exceptions.database.DBInitException;
import com.djrapitops.plan.system.database.databases.operation.*;

/**
 * Abstract class representing a Database.
 * <p>
 * All Operations methods should be only called from an asynchronous thread.
 *
 * @author Rsl1122
 */
public abstract class Database {

    protected volatile boolean open = false;

    public abstract void init() throws DBInitException;

    public abstract BackupOperations backup();

    public abstract CheckOperations check();

    public abstract FetchOperations fetch();

    public abstract RemoveOperations remove();

    public abstract SearchOperations search();

    public abstract CountOperations count();

    public abstract SaveOperations save();

    /**
     * Used to get the {@code DBType} of the Database
     *
     * @return the {@code DBType}
     * @see DBType
     */
    public abstract DBType getType();

    public abstract void close() throws DBException;

    public boolean isOpen() {
        return open;
    }

    public abstract void scheduleClean(long delay);

}
