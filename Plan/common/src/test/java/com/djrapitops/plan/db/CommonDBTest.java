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

import com.djrapitops.plan.api.exceptions.database.DBInitException;
import com.djrapitops.plan.data.WebUser;
import com.djrapitops.plan.data.container.*;
import com.djrapitops.plan.data.store.Key;
import com.djrapitops.plan.data.store.containers.AnalysisContainer;
import com.djrapitops.plan.data.store.containers.NetworkContainer;
import com.djrapitops.plan.data.store.containers.PlayerContainer;
import com.djrapitops.plan.data.store.containers.ServerContainer;
import com.djrapitops.plan.data.store.keys.*;
import com.djrapitops.plan.data.store.objects.Nickname;
import com.djrapitops.plan.data.time.GMTimes;
import com.djrapitops.plan.data.time.WorldTimes;
import com.djrapitops.plan.db.access.Query;
import com.djrapitops.plan.db.access.transactions.RemovePlayerTransaction;
import com.djrapitops.plan.db.patches.Patch;
import com.djrapitops.plan.db.sql.queries.LargeFetchQueries;
import com.djrapitops.plan.db.sql.tables.*;
import com.djrapitops.plan.db.tasks.CreateIndexTask;
import com.djrapitops.plan.system.PlanSystem;
import com.djrapitops.plan.system.database.DBSystem;
import com.djrapitops.plan.system.info.server.Server;
import com.djrapitops.plan.system.settings.config.Config;
import com.djrapitops.plan.system.settings.config.PlanConfig;
import com.djrapitops.plan.system.settings.paths.WebserverSettings;
import com.djrapitops.plan.utilities.SHA256Hash;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import rules.ComponentMocker;
import rules.PluginComponentMocker;
import utilities.FieldFetcher;
import utilities.OptionalAssert;
import utilities.RandomData;
import utilities.TestConstants;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Contains all common Database Tests for all Database Types
 *
 * @author Rsl1122 (Refactored into this class by Fuzzlemann)
 */
public abstract class CommonDBTest {

    private static final int TEST_PORT_NUMBER = RandomData.randomInt(9005, 9500);

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    @ClassRule
    public static ComponentMocker component = new PluginComponentMocker(temporaryFolder);

    public static UUID serverUUID;

    public static DBSystem dbSystem;
    public static SQLDB db;
    public static PlanSystem system;

    public final List<String> worlds = Arrays.asList("TestWorld", "TestWorld2");
    public final UUID playerUUID = TestConstants.PLAYER_ONE_UUID;
    public final UUID player2UUID = TestConstants.PLAYER_TWO_UUID;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(5);

    static void handleSetup(String dbName) throws Exception {
        system = component.getPlanSystem();
        system.getConfigSystem().getConfig().set(WebserverSettings.PORT, TEST_PORT_NUMBER);
        system.enable();

        dbSystem = system.getDatabaseSystem();
        db = (SQLDB) dbSystem.getActiveDatabaseByName(dbName);

        db.init();

        serverUUID = system.getServerInfo().getServerUUID();
    }

    @AfterClass
    public static void tearDownClass() {
        if (system != null) system.disable();
    }

    @Before
    public void setUp() throws DBInitException {
        new Patch(db) {
            @Override
            public boolean hasBeenApplied() {
                return false;
            }

            @Override
            public void applyPatch() {
                dropTable("plan_world_times");
                dropTable("plan_kills");
                dropTable("plan_sessions");
                dropTable("plan_worlds");
                dropTable("plan_users");
            }
        }.apply();
        db.createTables();
        db.remove().everything();
        ServerTable serverTable = db.getServerTable();
        serverTable.saveCurrentServerInfo(new Server(-1, serverUUID, "ServerName", "", 20));
        assertEquals(serverUUID, db.getServerUUIDSupplier().get());
    }

    public void commitTest() throws DBInitException {
        db.close();
        db.init();
    }

    @Test
    public void testNoExceptionWhenCommitEmpty() throws Exception {
        db.commit(db.getConnection());
        db.commit(db.getConnection());
        db.commit(db.getConnection());
    }

    @Test
    public void testSaveCommandUse() throws DBInitException {
        CommandUseTable commandUseTable = db.getCommandUseTable();
        Map<String, Integer> expected = new HashMap<>();

        expected.put("plan", 1);
        expected.put("tp", 4);
        expected.put("pla", 7);
        expected.put("help", 21);

        commandUseTable.commandUsed("plan");

        for (int i = 0; i < 4; i++) {
            commandUseTable.commandUsed("tp");
        }

        for (int i = 0; i < 7; i++) {
            commandUseTable.commandUsed("pla");
        }

        for (int i = 0; i < 21; i++) {
            commandUseTable.commandUsed("help");
        }

        for (int i = 0; i < 3; i++) {
            commandUseTable.commandUsed("roiergbnougbierubieugbeigubeigubgierbgeugeg");
        }

        commitTest();

        Map<String, Integer> commandUse = db.getCommandUseTable().getCommandUse();
        assertEquals(expected, commandUse);

        for (int i = 0; i < 3; i++) {
            commandUseTable.commandUsed("test");
        }

        for (int i = 0; i < 2; i++) {
            commandUseTable.commandUsed("tp");
        }

        expected.put("test", 3);
        expected.put("tp", 6);

        commandUse = db.getCommandUseTable().getCommandUse();

        assertEquals(expected, commandUse);
    }

    @Test
    public void testCommandUseTableIDSystem() {
        CommandUseTable commandUseTable = db.getCommandUseTable();
        commandUseTable.commandUsed("plan");

        for (int i = 0; i < 4; i++) {
            commandUseTable.commandUsed("tp");
        }

        for (int i = 0; i < 7; i++) {
            commandUseTable.commandUsed("pla");
        }

        for (int i = 0; i < 21; i++) {
            commandUseTable.commandUsed("help");
        }

        for (int i = 0; i < 3; i++) {
            commandUseTable.commandUsed("roiergbnougbierubieugbeigubeigubgierbgeugeg");
        }

        Optional<Integer> id = commandUseTable.getCommandID("plan");

        assertTrue(id.isPresent());

        Optional<String> commandByID = commandUseTable.getCommandByID(id.get());

        assertTrue(commandByID.isPresent());
        assertEquals("plan", commandByID.get());
        assertFalse(commandUseTable.getCommandID("roiergbnougbierubieugbeigubeigubgierbgeugeg").isPresent());
    }

    @Test
    public void testTPSSaving() throws Exception {
        TPSTable tpsTable = db.getTpsTable();
        Random r = new Random();

        List<TPS> expected = new ArrayList<>();

        for (int i = 0; i < RandomData.randomInt(1, 5); i++) {
            expected.add(new TPS(r.nextLong(), r.nextDouble(), r.nextInt(100000000), r.nextDouble(), r.nextLong(), r.nextInt(), r.nextInt(), r.nextLong()));
        }

        for (TPS tps : expected) {
            tpsTable.insertTPS(tps);
        }

        commitTest();

        assertEquals(expected, tpsTable.getTPSData());
    }

    private void saveUserOne() {
        saveUserOne(db);
    }

    private void saveUserOne(SQLDB database) {
        database.getUsersTable().registerUser(playerUUID, 123456789L, "Test");
        database.getUsersTable().kicked(playerUUID);
    }

    private void saveUserTwo() {
        saveUserTwo(db);
    }

    private void saveUserTwo(SQLDB database) {
        database.getUsersTable().registerUser(player2UUID, 123456789L, "Test");
    }

    @Test
    public void testIPTable() throws DBInitException {
        saveUserOne();
        GeoInfoTable geoInfoTable = db.getGeoInfoTable();

        String expectedIP = "1.2.3.4";
        String expectedGeoLoc = "TestLocation";
        long time = System.currentTimeMillis();

        GeoInfo expected = new GeoInfo(expectedIP, expectedGeoLoc, time, "3");
        geoInfoTable.saveGeoInfo(playerUUID, expected);
        geoInfoTable.saveGeoInfo(playerUUID, expected);
        commitTest();

        List<GeoInfo> getInfo = geoInfoTable.getGeoInfo(playerUUID);
        assertEquals(1, getInfo.size());
        GeoInfo actual = getInfo.get(0);
        assertEquals(expected, actual);
        assertEquals(time, actual.getDate());

        Optional<String> result = geoInfoTable.getGeolocation(expectedIP);
        assertTrue(result.isPresent());
        assertEquals(expectedGeoLoc, result.get());
    }

    @Test
    public void testNicknamesTable() throws DBInitException {
        saveUserOne();
        NicknamesTable nickTable = db.getNicknamesTable();

        Nickname expected = new Nickname("TestNickname", System.currentTimeMillis(), serverUUID);
        nickTable.saveUserName(playerUUID, expected);
        nickTable.saveUserName(playerUUID, expected);
        commitTest();

        List<Nickname> nicknames = nickTable.getNicknameInformation(playerUUID);
        assertEquals(1, nicknames.size());
        assertEquals(expected, nicknames.get(0));
    }

    @Test
    public void testSecurityTable() throws DBInitException {
        SecurityTable securityTable = db.getSecurityTable();
        WebUser expected = new WebUser("Test", "RandomGarbageBlah", 0);
        securityTable.addNewUser(expected);
        commitTest();

        assertTrue(securityTable.userExists("Test"));
        WebUser test = securityTable.getWebUser("Test");
        assertEquals(expected, test);

        assertFalse(securityTable.userExists("NotExist"));
        assertNull(securityTable.getWebUser("NotExist"));

        assertEquals(1, db.query(LargeFetchQueries.fetchAllPlanWebUsers()).size());

        securityTable.removeUser("Test");
        assertFalse(securityTable.userExists("Test"));
        assertNull(securityTable.getWebUser("Test"));

        assertEquals(0, db.query(LargeFetchQueries.fetchAllPlanWebUsers()).size());
    }

    @Test
    public void testWorldTable() throws DBInitException {
        WorldTable worldTable = db.getWorldTable();
        List<String> worlds = Arrays.asList("Test", "Test2", "Test3");
        worldTable.saveWorlds(worlds);

        commitTest();

        List<String> saved = worldTable.getWorlds(serverUUID);
        assertEquals(new HashSet<>(worlds), new HashSet<>(saved));
    }

    private void saveTwoWorlds() {
        saveTwoWorlds(db);
    }

    private void saveTwoWorlds(SQLDB database) {
        database.getWorldTable().saveWorlds(worlds);
    }

    private WorldTimes createWorldTimes() {
        Map<String, GMTimes> times = new HashMap<>();
        Map<String, Long> gm = new HashMap<>();
        String[] gms = GMTimes.getGMKeyArray();
        gm.put(gms[0], 1000L);
        gm.put(gms[1], 2000L);
        gm.put(gms[2], 3000L);
        gm.put(gms[3], 4000L);
        times.put(worlds.get(0), new GMTimes(gm));

        return new WorldTimes(times);
    }

    private List<PlayerKill> createKills() {
        List<PlayerKill> kills = new ArrayList<>();
        kills.add(new PlayerKill(TestConstants.PLAYER_TWO_UUID, "Iron Sword", 4321L));
        kills.add(new PlayerKill(TestConstants.PLAYER_TWO_UUID, "Gold Sword", 5321L));
        return kills;
    }

    @Test
    public void testSessionPlaytimeSaving() throws DBInitException {
        saveTwoWorlds();
        saveUserOne();
        saveUserTwo();
        Session session = new Session(playerUUID, serverUUID, 12345L, "", "");
        session.endSession(22345L);
        session.setWorldTimes(createWorldTimes());
        session.setPlayerKills(createKills());

        long expectedLength = 10000L;
        assertEquals(expectedLength, session.getLength());
        assertEquals(expectedLength, session.getUnsafe(SessionKeys.WORLD_TIMES).getTotal());

        SessionsTable sessionsTable = db.getSessionsTable();
        sessionsTable.saveSession(playerUUID, session);

        commitTest();

        assertEquals(expectedLength, sessionsTable.getPlaytime(playerUUID));
        assertEquals(0L, sessionsTable.getPlaytime(playerUUID, 30000L));

        long playtimeOfServer = sessionsTable.getPlaytimeOfServer(serverUUID);
        assertEquals(expectedLength, playtimeOfServer);
        assertEquals(0L, sessionsTable.getPlaytimeOfServer(serverUUID, 30000L));

        assertEquals(1, sessionsTable.getSessionCount(playerUUID));
        assertEquals(0, sessionsTable.getSessionCount(playerUUID, 30000L));
    }

    @Test
    public void testSessionSaving() throws DBInitException {
        saveUserOne();
        saveUserTwo();

        Session session = new Session(playerUUID, serverUUID, 12345L, "", "");
        session.endSession(22345L);
        session.setWorldTimes(createWorldTimes());
        session.setPlayerKills(createKills());

        SessionsTable sessionsTable = db.getSessionsTable();
        sessionsTable.saveSession(playerUUID, session);

        commitTest();

        Map<UUID, List<Session>> sessions = sessionsTable.getSessions(playerUUID);
        List<Session> savedSessions = sessions.get(serverUUID);

        assertNotNull(savedSessions);
        assertEquals(1, savedSessions.size());
        assertNull(sessions.get(UUID.randomUUID()));

        assertEquals(session, savedSessions.get(0));

        Map<UUID, Long> lastSeen = sessionsTable.getLastSeenForAllPlayers();
        assertTrue(lastSeen.containsKey(playerUUID));
        assertFalse(lastSeen.containsKey(TestConstants.PLAYER_TWO_UUID));
        assertEquals(22345L, (long) lastSeen.get(playerUUID));
    }

    @Test
    public void testUserInfoTableRegisterRegistered() throws DBInitException {
        saveUserOne();
        UsersTable usersTable = db.getUsersTable();
        assertTrue(usersTable.isRegistered(playerUUID));

        UserInfoTable userInfoTable = db.getUserInfoTable();
        assertFalse(userInfoTable.isRegistered(playerUUID));

        userInfoTable.registerUserInfo(playerUUID, 223456789L);
        commitTest();

        assertTrue(usersTable.isRegistered(playerUUID));
        assertTrue(userInfoTable.isRegistered(playerUUID));

        UserInfo userInfo = userInfoTable.getUserInfo(playerUUID);
        assertEquals(playerUUID, userInfo.getUuid());
        assertEquals(123456789L, (long) usersTable.getRegisterDates().get(0));
        assertEquals(223456789L, userInfo.getRegistered());
        assertEquals("Test", userInfo.getName());
        assertFalse(userInfo.isBanned());
        assertFalse(userInfo.isOperator());

        assertEquals(userInfo, userInfoTable.getServerUserInfo().get(0));
    }

    @Test
    public void testUserInfoTableUpdateBannedOpped() throws DBInitException {
        UsersTable usersTable = db.getUsersTable();
        usersTable.registerUser(playerUUID, 223456789L, "Test_name");
        UserInfoTable userInfoTable = db.getUserInfoTable();
        userInfoTable.registerUserInfo(playerUUID, 223456789L);
        assertTrue(userInfoTable.isRegistered(playerUUID));

        userInfoTable.updateOpStatus(playerUUID, true);
        userInfoTable.updateBanStatus(playerUUID, true);
        commitTest();

        UserInfo userInfo = userInfoTable.getUserInfo(playerUUID);
        assertTrue(userInfo.isBanned());
        assertTrue(userInfo.isOperator());

        userInfoTable.updateOpStatus(playerUUID, false);
        userInfoTable.updateBanStatus(playerUUID, true);
        commitTest();

        userInfo = userInfoTable.getUserInfo(playerUUID);

        assertTrue(userInfo.isBanned());
        assertFalse(userInfo.isOperator());

        userInfoTable.updateOpStatus(playerUUID, true);
        userInfoTable.updateBanStatus(playerUUID, false);
        commitTest();

        userInfo = userInfoTable.getUserInfo(playerUUID);

        assertFalse(userInfo.isBanned());
        assertTrue(userInfo.isOperator());
    }

    @Test
    public void testUsersTableUpdateName() throws DBInitException {
        saveUserOne();

        UsersTable usersTable = db.getUsersTable();

        assertEquals(playerUUID, usersTable.getUuidOf("Test"));
        usersTable.updateName(playerUUID, "NewName");

        commitTest();

        assertNull(usersTable.getUuidOf("Test"));

        assertEquals("NewName", usersTable.getPlayerName(playerUUID));
        assertEquals(playerUUID, usersTable.getUuidOf("NewName"));
    }

    @Test
    public void testUsersTableKickSaving() throws DBInitException {
        saveUserOne();
        UsersTable usersTable = db.getUsersTable();
        assertEquals(1, usersTable.getTimesKicked(playerUUID));

        int random = new Random().nextInt(20);

        for (int i = 0; i < random + 1; i++) {
            usersTable.kicked(playerUUID);
        }
        commitTest();
        assertEquals(random + 2, usersTable.getTimesKicked(playerUUID));
    }

    @Test
    public void testRemovalSingleUser() {
        saveUserTwo();

        UserInfoTable userInfoTable = db.getUserInfoTable();
        UsersTable usersTable = db.getUsersTable();
        SessionsTable sessionsTable = db.getSessionsTable();
        NicknamesTable nicknamesTable = db.getNicknamesTable();
        GeoInfoTable geoInfoTable = db.getGeoInfoTable();

        usersTable.registerUser(playerUUID, 223456789L, "Test_name");
        userInfoTable.registerUserInfo(playerUUID, 223456789L);
        saveTwoWorlds();

        Session session = new Session(playerUUID, serverUUID, 12345L, "", "");
        session.endSession(22345L);
        session.setWorldTimes(createWorldTimes());
        session.setPlayerKills(createKills());

        sessionsTable.saveSession(playerUUID, session);
        nicknamesTable.saveUserName(playerUUID, new Nickname("TestNick", System.currentTimeMillis(), serverUUID));
        geoInfoTable.saveGeoInfo(playerUUID, new GeoInfo("1.2.3.4", "TestLoc", 223456789L, "3"));

        assertTrue(usersTable.isRegistered(playerUUID));

        db.executeTransaction(new RemovePlayerTransaction(playerUUID));

        assertFalse(usersTable.isRegistered(playerUUID));
        assertFalse(userInfoTable.isRegistered(playerUUID));
        assertTrue(nicknamesTable.getNicknames(playerUUID).isEmpty());
        assertTrue(geoInfoTable.getGeoInfo(playerUUID).isEmpty());
        assertTrue(sessionsTable.getSessions(playerUUID).isEmpty());
    }

    @Test
    public void testRemovalEverything() throws NoSuchAlgorithmException {
        saveAllData(db);

        db.remove().everything();

        assertQueryIsEmpty(db, LargeFetchQueries.fetchAllCommonUserInformation());
        assertQueryIsEmpty(db, LargeFetchQueries.fetchPerServerUserInformation());
        assertQueryIsEmpty(db, LargeFetchQueries.fetchAllNicknameData());
        assertQueryIsEmpty(db, LargeFetchQueries.fetchAllGeoInfoData());
        assertQueryIsEmpty(db, LargeFetchQueries.fetchAllSessionsWithoutKillOrWorldData());
        assertQueryIsEmpty(db, LargeFetchQueries.fetchAllCommandUsageData());
        assertQueryIsEmpty(db, LargeFetchQueries.fetchAllWorldNames());
        assertQueryIsEmpty(db, LargeFetchQueries.fetchAllTPSData());
        assertQueryIsEmpty(db, LargeFetchQueries.fetchPlanServerInformation());
        assertQueryIsEmpty(db, LargeFetchQueries.fetchAllPingData());
        assertTrue(db.query(LargeFetchQueries.fetchAllPlanWebUsers()).isEmpty());
    }

    private <T extends Map> void assertQueryIsEmpty(Database database, Query<T> query) {
        assertTrue(database.query(query).isEmpty());
    }

    private void saveAllData(SQLDB database) throws NoSuchAlgorithmException {
        UserInfoTable userInfoTable = database.getUserInfoTable();
        UsersTable usersTable = database.getUsersTable();
        SessionsTable sessionsTable = database.getSessionsTable();
        NicknamesTable nicknamesTable = database.getNicknamesTable();
        GeoInfoTable geoInfoTable = database.getGeoInfoTable();
        TPSTable tpsTable = database.getTpsTable();
        SecurityTable securityTable = database.getSecurityTable();
        PingTable pingTable = database.getPingTable();

        saveUserOne(database);
        saveUserTwo(database);

        userInfoTable.registerUserInfo(playerUUID, 223456789L);
        saveTwoWorlds(database);

        Session session = new Session(playerUUID, serverUUID, 12345L, "", "");
        session.endSession(22345L);
        session.setWorldTimes(createWorldTimes());
        session.setPlayerKills(createKills());

        sessionsTable.saveSession(playerUUID, session);
        nicknamesTable.saveUserName(playerUUID, new Nickname("TestNick", System.currentTimeMillis(), serverUUID));
        geoInfoTable.saveGeoInfo(playerUUID, new GeoInfo("1.2.3.4", "TestLoc", 223456789L,
                new SHA256Hash("1.2.3.4").create()));

        assertTrue(usersTable.isRegistered(playerUUID));

        CommandUseTable commandUseTable = database.getCommandUseTable();
        commandUseTable.commandUsed("plan");
        commandUseTable.commandUsed("plan");
        commandUseTable.commandUsed("tp");
        commandUseTable.commandUsed("help");
        commandUseTable.commandUsed("help");
        commandUseTable.commandUsed("help");

        List<TPS> expected = new ArrayList<>();
        Random r = new Random();
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        int availableProcessors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
        double averageCPUUsage = operatingSystemMXBean.getSystemLoadAverage() / availableProcessors * 100.0;
        long usedMemory = 51231251254L;
        int entityCount = 6123;
        int chunksLoaded = 2134;
        long freeDiskSpace = new File("").getUsableSpace();
        expected.add(new TPS(r.nextLong(), r.nextDouble(), r.nextInt(100000000), averageCPUUsage, usedMemory, entityCount, chunksLoaded, freeDiskSpace));
        expected.add(new TPS(r.nextLong(), r.nextDouble(), r.nextInt(100000000), averageCPUUsage, usedMemory, entityCount, chunksLoaded, freeDiskSpace));
        expected.add(new TPS(r.nextLong(), r.nextDouble(), r.nextInt(100000000), averageCPUUsage, usedMemory, entityCount, chunksLoaded, freeDiskSpace));
        expected.add(new TPS(r.nextLong(), r.nextDouble(), r.nextInt(100000000), averageCPUUsage, usedMemory, entityCount, chunksLoaded, freeDiskSpace));
        for (TPS tps : expected) {
            tpsTable.insertTPS(tps);
        }

        pingTable.insertPing(playerUUID, new Ping(
                System.currentTimeMillis(), serverUUID,
                r.nextInt(), r.nextInt(), r.nextDouble()
        ));

        securityTable.addNewUser(new WebUser("Test", "RandomGarbageBlah", 0));
    }

    @Test
    public void testSessionTableNPEWhenNoPlayers() {
        Map<UUID, Long> lastSeen = db.getSessionsTable().getLastSeenForAllPlayers();
        assertTrue(lastSeen.isEmpty());
    }

    @Test
    public void testSessionTableGetInfoOfServer() throws DBInitException {
        saveUserOne();
        saveUserTwo();

        Session session = new Session(playerUUID, serverUUID, 12345L, "", "");
        session.endSession(22345L);
        session.setWorldTimes(createWorldTimes());
        session.setPlayerKills(createKills());

        SessionsTable sessionsTable = db.getSessionsTable();
        sessionsTable.saveSession(playerUUID, session);

        commitTest();

        Map<UUID, List<Session>> sessions = sessionsTable.getSessionInfoOfServer();

        session.setPlayerKills(new ArrayList<>());
        session.setWorldTimes(new WorldTimes(new HashMap<>()));

        List<Session> sSessions = sessions.get(playerUUID);
        assertFalse(sessions.isEmpty());
        assertNotNull(sSessions);
        assertFalse(sSessions.isEmpty());
        assertEquals(session, sSessions.get(0));
    }

    @Test
    public void testKillTableGetKillsOfServer() throws DBInitException {
        saveUserOne();
        saveUserTwo();

        Session session = createSession();
        List<PlayerKill> expected = createKills();
        session.setPlayerKills(expected);
        db.getSessionsTable().saveSession(playerUUID, session);

        commitTest();

        Map<UUID, List<Session>> sessions = db.getSessionsTable().getSessions(playerUUID);
        List<Session> savedSessions = sessions.get(serverUUID);
        assertNotNull(savedSessions);
        assertFalse(savedSessions.isEmpty());

        Session savedSession = savedSessions.get(0);
        assertNotNull(savedSession);

        List<PlayerKill> kills = savedSession.getPlayerKills();
        assertNotNull(kills);
        assertFalse(kills.isEmpty());
        assertEquals(expected, kills);
    }

    private Session createSession() {
        Session session = new Session(
                playerUUID,
                serverUUID,
                System.currentTimeMillis(),
                "world",
                GMTimes.getGMKeyArray()[0]
        );
        session.endSession(System.currentTimeMillis() + 1L);
        return session;
    }

    @Test
    public void testBackupAndRestore() throws Exception {
        H2DB backup = dbSystem.getH2Factory().usingFile(temporaryFolder.newFile("backup.db"));
        backup.init();

        saveAllData(db);

        db.backup().backup(backup);

        assertQueryResultIsEqual(db, backup, LargeFetchQueries.fetchAllCommonUserInformation());
        assertQueryResultIsEqual(db, backup, LargeFetchQueries.fetchPerServerUserInformation());
        assertQueryResultIsEqual(db, backup, LargeFetchQueries.fetchAllNicknameData());
        assertQueryResultIsEqual(db, backup, LargeFetchQueries.fetchAllGeoInfoData());
        assertQueryResultIsEqual(db, backup, LargeFetchQueries.fetchAllSessionsWithKillAndWorldData());
        assertQueryResultIsEqual(db, backup, LargeFetchQueries.fetchAllCommandUsageData());
        assertQueryResultIsEqual(db, backup, LargeFetchQueries.fetchAllWorldNames());
        assertQueryResultIsEqual(db, backup, LargeFetchQueries.fetchAllTPSData());
        assertQueryResultIsEqual(db, backup, LargeFetchQueries.fetchPlanServerInformation());
        assertQueryResultIsEqual(db, backup, LargeFetchQueries.fetchAllPlanWebUsers());
    }

    private <T> void assertQueryResultIsEqual(Database one, Database two, Query<T> query) {
        assertEquals(one.query(query), two.query(query));
    }

    @Test
    public void testSaveWorldTimes() {
        saveUserOne();
        WorldTimes worldTimes = createWorldTimes();
        WorldTimesTable worldTimesTable = db.getWorldTimesTable();

        Session session = new Session(1, playerUUID, serverUUID, 12345L, 23456L, 0, 0, 0);
        session.setWorldTimes(worldTimes);
        db.getSessionsTable().saveSession(playerUUID, session);

        Map<Integer, Session> sessions = new HashMap<>();
        sessions.put(1, session);
        worldTimesTable.addWorldTimesToSessions(playerUUID, sessions);

        assertEquals(worldTimes, session.getUnsafe(SessionKeys.WORLD_TIMES));
    }

    @Test
    public void testSaveAllWorldTimes() {
        saveUserOne();
        WorldTimes worldTimes = createWorldTimes();

        SessionsTable sessionsTable = db.getSessionsTable();
        WorldTimesTable worldTimesTable = db.getWorldTimesTable();

        Session session = createSession();
        session.setWorldTimes(worldTimes);
        Map<UUID, Map<UUID, List<Session>>> map = new HashMap<>();
        Map<UUID, List<Session>> sessionMap = new HashMap<>();
        List<Session> sessions = new ArrayList<>();
        sessions.add(session);
        sessionMap.put(playerUUID, sessions);
        map.put(serverUUID, sessionMap);

        sessionsTable.insertSessions(map, true);

        Map<Integer, WorldTimes> worldTimesBySessionID = worldTimesTable.getAllWorldTimesBySessionID();
        System.out.println(worldTimesBySessionID);
        assertEquals(worldTimes, worldTimesBySessionID.get(1));
    }

    @Test
    public void testSaveSessionsWorldTimes() {
        SessionsTable sessionsTable = db.getSessionsTable();

        saveUserOne();
        WorldTimes worldTimes = createWorldTimes();
        Session session = createSession();
        session.setWorldTimes(worldTimes);

        Map<UUID, Map<UUID, List<Session>>> map = new HashMap<>();
        Map<UUID, List<Session>> sessionMap = new HashMap<>();
        List<Session> sessions = new ArrayList<>();
        sessions.add(session);
        sessionMap.put(playerUUID, sessions);
        map.put(serverUUID, sessionMap);

        sessionsTable.insertSessions(map, true);

        Map<UUID, Map<UUID, List<Session>>> allSessions = db.query(LargeFetchQueries.fetchAllSessionsWithKillAndWorldData());

        assertEquals(worldTimes, allSessions.get(serverUUID).get(playerUUID).get(0).getUnsafe(SessionKeys.WORLD_TIMES));
    }

    @Test
    public void testGetUserWorldTimes() {
        testSaveSessionsWorldTimes();
        WorldTimes worldTimesOfUser = db.getWorldTimesTable().getWorldTimesOfUser(playerUUID);
        assertEquals(createWorldTimes(), worldTimesOfUser);
    }

    @Test
    public void testGetServerWorldTimes() {
        testSaveSessionsWorldTimes();
        WorldTimes worldTimesOfServer = db.getWorldTimesTable().getWorldTimesOfServer(serverUUID);
        assertEquals(createWorldTimes(), worldTimesOfServer);
    }

    @Test
    public void testRegister() {
        assertFalse(db.check().isPlayerRegistered(playerUUID));
        assertFalse(db.check().isPlayerRegisteredOnThisServer(playerUUID));
        db.save().registerNewUser(playerUUID, 1000L, "name");
        db.save().registerNewUserOnThisServer(playerUUID, 500L);
        assertTrue(db.check().isPlayerRegistered(playerUUID));
        assertTrue(db.check().isPlayerRegisteredOnThisServer(playerUUID));
    }

    @Test
    public void testWorldTableGetWorldNamesNoException() throws NoSuchAlgorithmException {
        saveAllData(db);
        Set<String> worldNames = db.getWorldTable().getWorldNames(serverUUID);
        assertEquals(new HashSet<>(worlds), worldNames);
    }

    @Test
    public void testGetNetworkGeolocations() {
        GeoInfoTable geoInfoTable = db.getGeoInfoTable();
        UUID firstUuid = UUID.randomUUID();
        UUID secondUuid = UUID.randomUUID();
        UUID thirdUuid = UUID.randomUUID();

        UsersTable usersTable = db.getUsersTable();
        usersTable.registerUser(firstUuid, 0, "");
        usersTable.registerUser(secondUuid, 0, "");
        usersTable.registerUser(thirdUuid, 0, "");

        geoInfoTable.saveGeoInfo(firstUuid, new GeoInfo("-", "Test1", 0, "3"));
        GeoInfo secondInfo = new GeoInfo("-", "Test2", 5, "3");
        geoInfoTable.saveGeoInfo(firstUuid, secondInfo);
        geoInfoTable.saveGeoInfo(secondUuid, new GeoInfo("-", "Test3", 0, "3"));
        geoInfoTable.saveGeoInfo(thirdUuid, new GeoInfo("-", "Test4", 0, "3"));

        List<String> geolocations = geoInfoTable.getNetworkGeolocations();

        assertNotNull(geolocations);
        assertFalse(geolocations.isEmpty());
        assertEquals(3, geolocations.size());
        assertTrue(geolocations.contains(secondInfo.getGeolocation()));
    }

    @Test
    public void testNewContainerForPlayer() throws NoSuchAlgorithmException {
        saveAllData(db);

        long start = System.nanoTime();

        PlayerContainer container = db.fetch().getPlayerContainer(playerUUID);

        assertTrue(container.supports(PlayerKeys.UUID));
        assertTrue(container.supports(PlayerKeys.REGISTERED));
        assertTrue(container.supports(PlayerKeys.NAME));
        assertTrue(container.supports(PlayerKeys.KICK_COUNT));

        assertTrue(container.supports(PlayerKeys.GEO_INFO));
        assertTrue(container.supports(PlayerKeys.NICKNAMES));

        assertTrue(container.supports(PlayerKeys.PER_SERVER));

        assertTrue(container.supports(PlayerKeys.OPERATOR));
        assertTrue(container.supports(PlayerKeys.BANNED));

        assertTrue(container.supports(PlayerKeys.SESSIONS));
        assertTrue(container.supports(PlayerKeys.WORLD_TIMES));
        assertTrue(container.supports(PlayerKeys.LAST_SEEN));
        assertTrue(container.supports(PlayerKeys.DEATH_COUNT));
        assertTrue(container.supports(PlayerKeys.MOB_KILL_COUNT));
        assertTrue(container.supports(PlayerKeys.PLAYER_KILLS));
        assertTrue(container.supports(PlayerKeys.PLAYER_KILL_COUNT));

        assertFalse(container.supports(PlayerKeys.ACTIVE_SESSION));
        container.putRawData(PlayerKeys.ACTIVE_SESSION, new Session(playerUUID, serverUUID, System.currentTimeMillis(), "TestWorld", "SURVIVAL"));
        assertTrue(container.supports(PlayerKeys.ACTIVE_SESSION));

        long end = System.nanoTime();

        assertFalse("Took too long: " + ((end - start) / 1000000.0) + "ms", end - start > TimeUnit.SECONDS.toNanos(1L));

        OptionalAssert.equals(playerUUID, container.getValue(PlayerKeys.UUID));
        OptionalAssert.equals(123456789L, container.getValue(PlayerKeys.REGISTERED));
        OptionalAssert.equals("Test", container.getValue(PlayerKeys.NAME));
        OptionalAssert.equals(1, container.getValue(PlayerKeys.KICK_COUNT));

        List<GeoInfo> expectedGeoInfo =
                Collections.singletonList(new GeoInfo("1.2.3.4", "TestLoc", 223456789, "ZpT4PJ9HbaMfXfa8xSADTn5X1CHSR7nTT0ntv8hKdkw="));
        OptionalAssert.equals(expectedGeoInfo, container.getValue(PlayerKeys.GEO_INFO));

        List<Nickname> expectedNicknames = Collections.singletonList(new Nickname("TestNick", -1, serverUUID));
        OptionalAssert.equals(expectedNicknames, container.getValue(PlayerKeys.NICKNAMES));

        OptionalAssert.equals(false, container.getValue(PlayerKeys.OPERATOR));
        OptionalAssert.equals(false, container.getValue(PlayerKeys.BANNED));

        // TODO Test rest
    }

    @Test
    public void playerContainerSupportsAllPlayerKeys() throws NoSuchAlgorithmException, IllegalAccessException {
        saveAllData(db);

        PlayerContainer playerContainer = db.fetch().getPlayerContainer(playerUUID);
        // Active sessions are added after fetching
        playerContainer.putRawData(PlayerKeys.ACTIVE_SESSION, RandomData.randomSession());

        List<String> unsupported = new ArrayList<>();
        List<Key> keys = FieldFetcher.getPublicStaticFields(PlayerKeys.class, Key.class);
        for (Key key : keys) {
            if (!playerContainer.supports(key)) {
                unsupported.add(key.getKeyName());
            }
        }

        assertTrue("Some keys are not supported by PlayerContainer: PlayerKeys." + unsupported.toString(), unsupported.isEmpty());
    }

    @Test
    public void serverContainerSupportsAllServerKeys() throws NoSuchAlgorithmException, IllegalAccessException {
        saveAllData(db);

        ServerContainer serverContainer = db.fetch().getServerContainer(serverUUID);

        List<String> unsupported = new ArrayList<>();
        List<Key> keys = FieldFetcher.getPublicStaticFields(ServerKeys.class, Key.class);
        for (Key key : keys) {
            if (!serverContainer.supports(key)) {
                unsupported.add(key.getKeyName());
            }
        }

        assertTrue("Some keys are not supported by ServerContainer: ServerKeys." + unsupported.toString(), unsupported.isEmpty());
    }

    @Test
    public void analysisContainerSupportsAllAnalysisKeys() throws IllegalAccessException, NoSuchAlgorithmException {
        serverContainerSupportsAllServerKeys();
        AnalysisContainer.Factory factory = constructAnalysisContainerFactory();
        AnalysisContainer analysisContainer = factory.forServerContainer(
                db.fetch().getServerContainer(serverUUID)
        );
        Collection<String> unsupported = new ArrayList<>();
        List<Key> keys = FieldFetcher.getPublicStaticFields(AnalysisKeys.class, Key.class);
        for (Key key : keys) {
            if (!analysisContainer.supports(key)) {
                unsupported.add(key.getKeyName());
            }
        }

        assertTrue("Some keys are not supported by AnalysisContainer: AnalysisKeys." + unsupported.toString(), unsupported.isEmpty());
    }

    private AnalysisContainer.Factory constructAnalysisContainerFactory() {
        return new AnalysisContainer.Factory(
                "1.0.0",
                system.getConfigSystem().getConfig(),
                system.getLocaleSystem().getLocale(),
                system.getConfigSystem().getTheme(),
                dbSystem,
                system.getServerInfo().getServerProperties(),
                system.getHtmlUtilities().getFormatters(),
                system.getHtmlUtilities().getGraphs(),
                system.getHtmlUtilities().getHtmlTables(),
                system.getHtmlUtilities().getAccordions(),
                system.getHtmlUtilities().getAnalysisPluginsTabContentCreator()
        );
    }

    @Test
    public void networkContainerSupportsAllNetworkKeys() throws IllegalAccessException, NoSuchAlgorithmException {
        serverContainerSupportsAllServerKeys();
        NetworkContainer networkContainer = db.fetch().getNetworkContainer();

        List<String> unsupported = new ArrayList<>();
        List<Key> keys = FieldFetcher.getPublicStaticFields(NetworkKeys.class, Key.class);
        for (Key key : keys) {
            if (!networkContainer.supports(key)) {
                unsupported.add(key.getKeyName());
            }
        }

        assertTrue("Some keys are not supported by NetworkContainer: NetworkKeys." + unsupported.toString(), unsupported.isEmpty());
    }

    @Test
    public void testGetMatchingNames() {
        String exp1 = "TestName";
        String exp2 = "TestName2";

        UsersTable usersTable = db.getUsersTable();
        UUID uuid1 = UUID.randomUUID();
        usersTable.registerUser(uuid1, 0L, exp1);
        usersTable.registerUser(UUID.randomUUID(), 0L, exp2);

        String search = "testname";

        List<String> result = db.search().matchingPlayers(search);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(exp1, result.get(0));
        assertEquals(exp2, result.get(1));
    }

    @Test
    public void testGetMatchingNickNames() {
        UUID uuid = UUID.randomUUID();
        String userName = RandomData.randomString(10);
        db.getUsersTable().registerUser(uuid, 0L, userName);
        db.getUsersTable().registerUser(playerUUID, 1L, "Not random");

        String nickname = "2" + RandomData.randomString(10);
        db.getNicknamesTable().saveUserName(uuid, new Nickname(nickname, System.currentTimeMillis(), serverUUID));
        db.getNicknamesTable().saveUserName(playerUUID, new Nickname("No nick", System.currentTimeMillis(), serverUUID));

        String search = "2";

        List<String> result = db.search().matchingPlayers(search);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userName, result.get(0));
    }

    @Test
    public void configIsStoredInTheDatabase() {
        PlanConfig config = system.getConfigSystem().getConfig();

        SettingsTable settingsTable = db.getSettingsTable();
        settingsTable.storeConfig(serverUUID, config, System.currentTimeMillis());

        Optional<Config> foundConfig = settingsTable.fetchNewerConfig(0, serverUUID);
        assertTrue(foundConfig.isPresent());
        assertEquals(config, foundConfig.get());
    }

    @Test
    public void unchangedConfigDoesNotUpdateInDatabase() {
        configIsStoredInTheDatabase();
        long savedMs = System.currentTimeMillis();

        PlanConfig config = system.getConfigSystem().getConfig();

        SettingsTable settingsTable = db.getSettingsTable();
        settingsTable.storeConfig(serverUUID, config, System.currentTimeMillis());

        assertFalse(settingsTable.fetchNewerConfig(savedMs, serverUUID).isPresent());
    }

    @Test
    public void indexCreationWorksWithoutErrors() {
        new CreateIndexTask(db).run();
    }

}
