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
package com.djrapitops.plan.system.tasks.sponge;

import com.djrapitops.plan.PlanSponge;
import com.djrapitops.plan.data.container.TPS;
import com.djrapitops.plan.data.container.builders.TPSBuilder;
import com.djrapitops.plan.system.database.DBSystem;
import com.djrapitops.plan.system.info.server.ServerInfo;
import com.djrapitops.plan.system.info.server.properties.ServerProperties;
import com.djrapitops.plan.system.tasks.TPSCountTimer;
import com.djrapitops.plugin.logging.console.PluginLogger;
import com.djrapitops.plugin.logging.error.ErrorHandler;
import org.spongepowered.api.world.World;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SpongeTPSCountTimer extends TPSCountTimer {

    private long lastCheckNano;
    private final PlanSponge plugin;
    private ServerProperties serverProperties;

    @Inject
    public SpongeTPSCountTimer(
            PlanSponge plugin,
            DBSystem dbSystem,
            ServerInfo serverInfo,
            ServerProperties serverProperties,
            PluginLogger logger,
            ErrorHandler errorHandler
    ) {
        super(dbSystem, serverInfo, logger, errorHandler);
        this.plugin = plugin;
        this.serverProperties = serverProperties;
        lastCheckNano = -1;
    }

    @Override
    public void addNewTPSEntry(long nanoTime, long now) {
        long diff = nanoTime - lastCheckNano;

        lastCheckNano = nanoTime;

        if (diff > nanoTime) { // First run's diff = nanoTime + 1, no calc possible.
            logger.debug("First run of TPSCountTimer Task.");
            return;
        }

        history.add(calculateTPS(now));
    }

    /**
     * Calculates the TPS
     *
     * @param now The time right now
     * @return the TPS
     */
    private TPS calculateTPS(long now) {
        double averageCPUUsage = getCPUUsage();

        long usedMemory = getUsedMemory();

        double tps = plugin.getGame().getServer().getTicksPerSecond();
        int playersOnline = serverProperties.getOnlinePlayers();
        latestPlayersOnline = playersOnline;
        int loadedChunks = -1; // getLoadedChunks();
        int entityCount = getEntityCount();
        long freeDiskSpace = getFreeDiskSpace();

        return TPSBuilder.get()
                .date(now)
                .tps(tps)
                .playersOnline(playersOnline)
                .usedCPU(averageCPUUsage)
                .usedMemory(usedMemory)
                .entities(entityCount)
                .chunksLoaded(loadedChunks)
                .freeDiskSpace(freeDiskSpace)
                .toTPS();
    }

    /**
     * Gets the amount of loaded chunks
     *
     * @return amount of loaded chunks
     */
    private int getLoadedChunks() {
        // DISABLED
        int loaded = 0;
        for (World world : plugin.getGame().getServer().getWorlds()) {
            loaded += world.getLoadedChunks().spliterator().estimateSize();
        }
        return loaded;
    }

    /**
     * Gets the amount of entities on the server
     *
     * @return amount of entities
     */
    private int getEntityCount() {
        return plugin.getGame().getServer().getWorlds().stream().mapToInt(world -> world.getEntities().size()).sum();
    }
}
