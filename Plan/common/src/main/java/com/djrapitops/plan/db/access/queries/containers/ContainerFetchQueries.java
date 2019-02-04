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
package com.djrapitops.plan.db.access.queries.containers;

import com.djrapitops.plan.data.store.containers.NetworkContainer;
import com.djrapitops.plan.data.store.containers.PlayerContainer;
import com.djrapitops.plan.data.store.containers.ServerContainer;
import com.djrapitops.plan.db.access.Query;

import java.util.List;
import java.util.UUID;

/**
 * Static method class for queries that return some kind of {@link com.djrapitops.plan.data.store.containers.DataContainer}.
 *
 * @author Rsl1122
 */
public class ContainerFetchQueries {

    private ContainerFetchQueries() {
        /* Static method class */
    }

    /**
     * Used to get a NetworkContainer, some limitations apply to values returned by DataContainer keys.
     *
     * @return a new NetworkContainer.
     * @see NetworkContainerQuery
     */
    public static Query<NetworkContainer> fetchNetworkContainer() {
        return new NetworkContainerQuery();
    }

    /**
     * Used to get a ServerContainer, some limitations apply to values returned by DataContainer keys.
     *
     * @param serverUUID UUID of the Server.
     * @return a new ServerContainer.
     * @see ServerContainerQuery
     */
    public static Query<ServerContainer> fetchServerContainer(UUID serverUUID) {
        return new ServerContainerQuery(serverUUID);
    }

    /**
     * Used to get a PlayerContainer of a specific player.
     * <p>
     * Blocking methods are not called until DataContainer getter methods are called.
     *
     * @param playerUUID UUID of the player.
     * @return a new PlayerContainer.
     * @see PlayerContainerQuery
     */
    public static Query<PlayerContainer> fetchPlayerContainer(UUID playerUUID) {
        return new PlayerContainerQuery(playerUUID);
    }

    /**
     * Used to get PlayerContainers of all players on the network, some limitations apply to DataContainer keys.
     * <p>
     * Limitations:
     * - PlayerContainers do not support: PlayerKeys WORLD_TIMES, PLAYER_KILLS, PLAYER_KILL_COUNT
     * - PlayerContainers PlayerKeys.PER_SERVER does not support: PerServerKeys WORLD_TIMES, PLAYER_KILLS, PLAYER_KILL_COUNT
     * <p>
     * Blocking methods are not called until DataContainer getter methods are called.
     *
     * @return a list of PlayerContainers in Plan database.
     */
    public static Query<List<PlayerContainer>> fetchAllPlayerContainers() {
        return new AllPlayerContainersQuery();
    }

}