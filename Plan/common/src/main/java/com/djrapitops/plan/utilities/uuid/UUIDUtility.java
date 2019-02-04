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
package com.djrapitops.plan.utilities.uuid;

import com.djrapitops.plan.api.exceptions.database.DBOpException;
import com.djrapitops.plan.system.database.DBSystem;
import com.djrapitops.plugin.api.utility.UUIDFetcher;
import com.djrapitops.plugin.logging.L;
import com.djrapitops.plugin.logging.error.ErrorHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

/**
 * @author Rsl1122
 */
@Singleton
public class UUIDUtility {

    private final DBSystem dbSystem;
    private final ErrorHandler errorHandler;

    @Inject
    public UUIDUtility(DBSystem dbSystem, ErrorHandler errorHandler) {
        this.dbSystem = dbSystem;
        this.errorHandler = errorHandler;
    }

    /**
     * Get UUID of a player.
     *
     * @param playerName Player's name
     * @return UUID of the player
     */
    public UUID getUUIDOf(String playerName) {
        UUID uuid = getUUIDFromString(playerName);
        if (uuid != null) {
            return uuid;
        }

        uuid = getUUIDFromDB(playerName);
        if (uuid != null) {
            return uuid;
        }

        return getUUIDViaUUIDFetcher(playerName);
    }

    private UUID getUUIDFromString(String playerName) {
        try {
            return UUID.fromString(playerName);
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    private UUID getUUIDViaUUIDFetcher(String playerName) {
        try {
            return UUIDFetcher.getUUIDOf(playerName);
        } catch (Exception | NoClassDefFoundError ignored) {
            return null;
        }
    }

    private UUID getUUIDFromDB(String playerName) {
        try {
            return dbSystem.getDatabase().fetch().getUuidOf(playerName);
        } catch (DBOpException e) {
            errorHandler.log(L.ERROR, UUIDUtility.class, e);
            return null;
        }
    }
}
