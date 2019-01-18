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
package com.djrapitops.plan.system.info.server;

import com.djrapitops.plan.api.exceptions.EnableException;
import com.djrapitops.plan.api.exceptions.database.DBOpException;
import com.djrapitops.plan.db.Database;
import com.djrapitops.plan.system.database.DBSystem;
import com.djrapitops.plan.system.info.server.properties.ServerProperties;
import com.djrapitops.plan.system.webserver.WebServer;
import com.djrapitops.plugin.logging.console.PluginLogger;
import dagger.Lazy;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages Server information on the Bungee instance.
 *
 * @author Rsl1122
 */
@Singleton
public class VelocityServerInfo extends ServerInfo {

    private final DBSystem dbSystem;
    private final Lazy<WebServer> webServer;
    private final PluginLogger logger;

    @Inject
    public VelocityServerInfo(
            ServerProperties serverProperties,
            DBSystem dbSystem,
            Lazy<WebServer> webServer,
            PluginLogger logger
    ) {
        super(serverProperties);
        this.dbSystem = dbSystem;
        this.webServer = webServer;
        this.logger = logger;
    }

    @Override
    public Server loadServerInfo() throws EnableException {
        checkIfDefaultIP();

        try {
            Database database = dbSystem.getDatabase();
            Optional<Server> bungeeInfo = database.fetch().getBungeeInformation();
            if (bungeeInfo.isPresent()) {
                server = bungeeInfo.get();
                updateServerInfo(database);
            } else {
                server = registerVelocityInfo(database);
            }
        } catch (DBOpException e) {
            throw new EnableException("Failed to read Server information from Database.");
        }
        return server;
    }

    private void updateServerInfo(Database db) {
        String accessAddress = webServer.get().getAccessAddress();
        if (!accessAddress.equals(server.getWebAddress())) {
            server.setWebAddress(accessAddress);
            db.save().serverInfoForThisServer(server);
        }
    }

    private void checkIfDefaultIP() throws EnableException {
        String ip = serverProperties.getIp();
        if ("0.0.0.0".equals(ip)) {
            logger.error("IP setting still 0.0.0.0 - Configure AlternativeIP/IP that connects to the Proxy server.");
            logger.info("Player Analytics partially enabled (Use /planbungee to reload config)");
            throw new EnableException("IP setting still 0.0.0.0 - Configure AlternativeIP/IP that connects to the Proxy server.");
        }
    }

    private Server registerVelocityInfo(Database db) throws EnableException {
        UUID serverUUID = generateNewUUID();
        String accessAddress = webServer.get().getAccessAddress();

        // TODO Rework to allow Velocity as name.
        Server bungeeCord = new Server(-1, serverUUID, "BungeeCord", accessAddress, serverProperties.getMaxPlayers());
        db.save().serverInfoForThisServer(bungeeCord);

        Optional<Server> bungeeInfo = db.fetch().getBungeeInformation();
        if (bungeeInfo.isPresent()) {
            return bungeeInfo.get();
        }
        throw new EnableException("Velocity registration failed (DB)");
    }

    private UUID generateNewUUID() {
        String seed = serverProperties.getName() +
                serverProperties.getIp() +
                serverProperties.getPort() +
                serverProperties.getVersion() +
                serverProperties.getImplVersion();
        return UUID.nameUUIDFromBytes(seed.getBytes());
    }
}
