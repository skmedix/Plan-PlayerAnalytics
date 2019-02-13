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
package com.djrapitops.plan.data.store.mutators;

import com.djrapitops.plan.data.container.PlayerDeath;
import com.djrapitops.plan.data.container.PlayerKill;
import com.djrapitops.plan.data.container.Session;
import com.djrapitops.plan.data.store.containers.DataContainer;
import com.djrapitops.plan.data.store.keys.CommonKeys;
import com.djrapitops.plan.data.store.keys.SessionKeys;
import com.djrapitops.plan.data.time.WorldTimes;
import com.djrapitops.plan.utilities.analysis.Median;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Mutator for a list of Sessions.
 * <p>
 * Can be used to get properties of a large number of sessions easily.
 *
 * @author Rsl1122
 */
public class SessionsMutator {

    private List<Session> sessions;

    public static SessionsMutator forContainer(DataContainer container) {
        return new SessionsMutator(container.getValue(CommonKeys.SESSIONS).orElse(new ArrayList<>()));
    }

    public SessionsMutator(List<Session> sessions) {
        this.sessions = sessions;
    }

    public List<Session> all() {
        return sessions;
    }

    public SessionsMutator filterBy(Predicate<Session> predicate) {
        return new SessionsMutator(sessions.stream()
                .filter(predicate)
                .collect(Collectors.toList()));
    }

    public SessionsMutator filterSessionsBetween(long after, long before) {
        return filterBy(getBetweenPredicate(after, before));
    }

    public SessionsMutator filterPlayedOnServer(UUID serverUUID) {
        return filterBy(session ->
                session.getValue(SessionKeys.SERVER_UUID)
                        .map(uuid -> uuid.equals(serverUUID))
                        .orElse(false)
        );
    }

    public DateHoldersMutator<Session> toDateHoldersMutator() {
        return new DateHoldersMutator<>(sessions);
    }

    public WorldTimes toTotalWorldTimes() {
        WorldTimes total = new WorldTimes(new HashMap<>());

        for (Session session : sessions) {
            session.getValue(SessionKeys.WORLD_TIMES).ifPresent(total::add);
        }

        return total;
    }

    public List<PlayerKill> toPlayerKillList() {
        return sessions.stream()
                .map(session -> session.getValue(SessionKeys.PLAYER_KILLS).orElse(new ArrayList<>()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<PlayerDeath> toPlayerDeathList() {
        return sessions.stream()
                .map(session -> session.getValue(SessionKeys.PLAYER_DEATHS).orElse(new ArrayList<>()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public int toMobKillCount() {
        return sessions.stream()
                .mapToInt(session -> session.getValue(SessionKeys.MOB_KILL_COUNT).orElse(0))
                .sum();
    }

    public int toDeathCount() {
        return sessions.stream()
                .mapToInt(session -> session.getValue(SessionKeys.DEATH_COUNT).orElse(0))
                .sum();
    }

    public long toPlaytime() {
        return sessions.stream()
                .mapToLong(Session::getLength)
                .sum();
    }

    public long toAfkTime() {
        return sessions.stream()
                .mapToLong(session -> session.getValue(SessionKeys.AFK_TIME).orElse(0L))
                .sum();
    }

    public long toActivePlaytime() {
        return sessions.stream()
                .mapToLong(session -> session.getValue(SessionKeys.ACTIVE_TIME).orElse(0L))
                .sum();
    }

    public long toLastSeen() {
        return sessions.stream()
                .mapToLong(session -> Math.max(session.getUnsafe(
                        SessionKeys.START),
                        session.getValue(SessionKeys.END).orElse(System.currentTimeMillis()))
                ).max().orElse(-1);
    }

    public long toLongestSessionLength() {
        OptionalLong longestSession = sessions.stream().mapToLong(Session::getLength).max();
        if (longestSession.isPresent()) {
            return longestSession.getAsLong();
        }
        return -1;
    }

    public long toAverageSessionLength() {
        OptionalDouble average = sessions.stream().map(Session::getLength)
                .mapToLong(i -> i)
                .average();
        if (average.isPresent()) {
            return (long) average.getAsDouble();
        }
        return 0L;
    }

    public long toMedianSessionLength() {
        List<Long> sessionLengths = sessions.stream().map(Session::getLength).collect(Collectors.toList());
        return (long) Median.forList(sessionLengths).calculate();
    }

    public int toAverageUniqueJoinsPerDay() {
        return MutatorFunctions.average(uniqueJoinsPerDay());
    }

    public TreeMap<Long, Integer> uniqueJoinsPerDay() {
        SortedMap<Long, List<Session>> byStartOfDay = toDateHoldersMutator().groupByStartOfDay();

        TreeMap<Long, Integer> uniqueJoins = new TreeMap<>();
        for (Map.Entry<Long, List<Session>> entry : byStartOfDay.entrySet()) {
            uniqueJoins.put(
                    entry.getKey(),
                    new SessionsMutator(entry.getValue()).toUniquePlayers()
            );
        }

        return uniqueJoins;
    }

    public int toUniquePlayers() {
        return (int) sessions.stream()
                .map(session -> session.getUnsafe(SessionKeys.UUID))
                .distinct()
                .count();
    }

    public int count() {
        return sessions.size();
    }

    public int toPlayerKillCount() {
        return toPlayerKillList().size();
    }

    public boolean playedBetween(long after, long before) {
        return sessions.stream().anyMatch(getBetweenPredicate(after, before));
    }

    private Predicate<Session> getBetweenPredicate(long after, long before) {
        return session -> {
            Long start = session.getUnsafe(SessionKeys.START);
            Long end = session.getValue(SessionKeys.END).orElse(System.currentTimeMillis());
            return (after <= start && start <= before) || (after <= end && end <= before);
        };
    }

    public static Map<UUID, List<Session>> sortByPlayers(List<Session> sessions) {
        Map<UUID, List<Session>> sorted = new HashMap<>();
        for (Session session : sessions) {
            UUID playerUUID = session.getUnsafe(SessionKeys.UUID);
            List<Session> playerSessions = sorted.getOrDefault(playerUUID, new ArrayList<>());
            playerSessions.add(session);
            sorted.put(playerUUID, playerSessions);
        }
        return sorted;
    }

    public static Map<UUID, List<Session>> sortByServers(List<Session> sessions) {
        Map<UUID, List<Session>> sorted = new HashMap<>();
        for (Session session : sessions) {
            UUID serverUUID = session.getUnsafe(SessionKeys.SERVER_UUID);
            List<Session> serverSessions = sorted.getOrDefault(serverUUID, new ArrayList<>());
            serverSessions.add(session);
            sorted.put(serverUUID, serverSessions);
        }
        return sorted;
    }

    public int toPlayerDeathCount() {
        return toPlayerDeathList().size();
    }

}