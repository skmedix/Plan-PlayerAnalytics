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
package com.djrapitops.plan.db.sql.queries.containers;

import com.djrapitops.plan.data.container.Session;
import com.djrapitops.plan.data.store.containers.ServerContainer;
import com.djrapitops.plan.data.store.keys.ServerKeys;
import com.djrapitops.plan.data.store.mutators.PlayersMutator;
import com.djrapitops.plan.data.store.mutators.SessionsMutator;
import com.djrapitops.plan.db.SQLDB;
import com.djrapitops.plan.db.access.Query;
import com.djrapitops.plan.db.sql.queries.AggregateQueries;
import com.djrapitops.plan.db.sql.queries.OptionalFetchQueries;
import com.djrapitops.plan.system.cache.SessionCache;
import com.djrapitops.plan.system.info.server.Server;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Used to get a ServerContainer, some limitations apply to values returned by DataContainer keys.
 * <p>
 * Limitations:
 * - ServerKeys.PLAYERS PlayerContainers PlayerKeys.PER_SERVER only contains information about the queried server.
 * <p>
 * Blocking methods are not called until DataContainer getter methods are called.
 *
 * @author Rsl1122
 */
public class ServerContainerQuery implements Query<ServerContainer> {

    private final UUID serverUUID;

    public ServerContainerQuery(UUID serverUUID) {
        this.serverUUID = serverUUID;
    }

    @Override
    public ServerContainer executeQuery(SQLDB db) {
        ServerContainer container = new ServerContainer();

        Optional<Server> serverInfo = db.getServerTable().getServerInfo(serverUUID);
        if (!serverInfo.isPresent()) {
            return container;
        }

        container.putRawData(ServerKeys.SERVER_UUID, serverUUID);
        container.putRawData(ServerKeys.NAME, serverInfo.get().getName());
        container.putCachingSupplier(ServerKeys.PLAYERS, () -> db.query(new ServerPlayerContainersQuery(serverUUID)));
        container.putSupplier(ServerKeys.PLAYER_COUNT, () -> container.getUnsafe(ServerKeys.PLAYERS).size());

        container.putCachingSupplier(ServerKeys.TPS, () -> db.getTpsTable().getTPSData(serverUUID));
        container.putCachingSupplier(ServerKeys.PING, () -> PlayersMutator.forContainer(container).pings());
        container.putCachingSupplier(ServerKeys.ALL_TIME_PEAK_PLAYERS, () ->
                db.query(OptionalFetchQueries.fetchAllTimePeakPlayerCount(serverUUID)).orElse(null)
        );
        container.putCachingSupplier(ServerKeys.RECENT_PEAK_PLAYERS, () -> {
            long twoDaysAgo = System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(2L));
            return db.query(OptionalFetchQueries.fetchPeakPlayerCount(serverUUID, twoDaysAgo)).orElse(null);
        });

        container.putCachingSupplier(ServerKeys.COMMAND_USAGE, () -> db.query(AggregateQueries.commandUsageCounts(serverUUID)));
        container.putCachingSupplier(ServerKeys.WORLD_TIMES, () -> db.query(AggregateQueries.totalWorldTimes(serverUUID)));

        // Calculating getters
        container.putCachingSupplier(ServerKeys.OPERATORS, () -> PlayersMutator.forContainer(container).operators());
        container.putCachingSupplier(ServerKeys.SESSIONS, () -> {
            List<Session> sessions = PlayersMutator.forContainer(container).getSessions();
            if (serverUUID.equals(serverInfo.get().getUuid())) {
                sessions.addAll(SessionCache.getActiveSessions().values());
            }
            return sessions;
        });
        container.putCachingSupplier(ServerKeys.PLAYER_KILLS, () -> SessionsMutator.forContainer(container).toPlayerKillList());
        container.putCachingSupplier(ServerKeys.PLAYER_KILL_COUNT, () -> container.getUnsafe(ServerKeys.PLAYER_KILLS).size());
        container.putCachingSupplier(ServerKeys.MOB_KILL_COUNT, () -> SessionsMutator.forContainer(container).toMobKillCount());
        container.putCachingSupplier(ServerKeys.DEATH_COUNT, () -> SessionsMutator.forContainer(container).toDeathCount());

        return container;
    }
}