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
package com.djrapitops.plan.utilities.comparators;

import com.djrapitops.plan.data.WebUser;
import com.djrapitops.plan.data.container.GeoInfo;
import com.djrapitops.plan.data.container.Session;
import com.djrapitops.plan.data.container.TPS;
import com.djrapitops.plan.data.store.keys.SessionKeys;
import com.djrapitops.plan.system.locale.Message;
import com.djrapitops.plan.system.locale.lang.CmdHelpLang;
import com.djrapitops.plan.system.locale.lang.Lang;
import com.djrapitops.plan.utilities.PassEncryptUtil;
import com.djrapitops.plan.utilities.html.graphs.line.Point;
import org.junit.Test;
import utilities.RandomData;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class ComparatorTest {

    @Test
    public void pointComparator() {
        List<Point> points = RandomData.randomPoints();

        List<Long> expected = points.stream().map(Point::getX).map(i -> (long) (double) i)
                .sorted(Long::compare).collect(Collectors.toList());

        points.sort(new PointComparator());

        List<Long> result = points.stream().map(Point::getX).map(i -> (long) (double) i).collect(Collectors.toList());
        assertEquals(expected, result);
    }

    @Test
    public void sessionDataComparator() {
        List<Session> sessions = RandomData.randomSessions();

        List<Long> expected = sessions.stream().map(s -> s.getUnsafe(SessionKeys.START))
                .sorted(Long::compare).collect(Collectors.toList());
        Collections.reverse(expected);

        sessions.sort(new SessionStartComparator());
        List<Long> result = sessions.stream().map(s -> s.getUnsafe(SessionKeys.START)).collect(Collectors.toList());

        assertEquals(expected, result);
    }

    @Test
    public void tpsComparator() {
        List<TPS> tpsList = RandomData.randomTPS();

        List<Long> expected = tpsList.stream().map(TPS::getDate)
                .sorted(Long::compare).collect(Collectors.toList());

        tpsList.sort(new TPSComparator());
        List<Long> result = tpsList.stream().map(TPS::getDate).collect(Collectors.toList());

        assertEquals(expected, result);
    }

    @Test
    public void webUserComparator() throws PassEncryptUtil.CannotPerformOperationException {
        List<WebUser> webUsers = RandomData.randomWebUsers();

        List<Integer> expected = webUsers.stream().map(WebUser::getPermLevel)
                .sorted(Integer::compare).collect(Collectors.toList());
        Collections.reverse(expected);

        webUsers.sort(new WebUserComparator());
        List<Integer> result = webUsers.stream().map(WebUser::getPermLevel).collect(Collectors.toList());

        assertEquals(expected, result);
    }

    @Test
    public void stringLengthComparator() {
        List<Integer> result = Stream.of(
                RandomData.randomString(10),
                RandomData.randomString(3),
                RandomData.randomString(4),
                RandomData.randomString(20),
                RandomData.randomString(7),
                RandomData.randomString(4),
                RandomData.randomString(86),
                RandomData.randomString(6)
        )
                .sorted(new StringLengthComparator())
                .map(String::length)
                .collect(Collectors.toList());

        List<Integer> expected = Arrays.asList(86, 20, 10, 7, 6, 4, 4, 3);
        assertEquals(expected, result);
    }

    @Test
    public void localeEntryComparator() {
        Map<Lang, Message> messageMap = new HashMap<>();
        messageMap.put(CmdHelpLang.SERVERS, new Message(RandomData.randomString(10)));
        messageMap.put(CmdHelpLang.ANALYZE, new Message(RandomData.randomString(10)));
        messageMap.put(CmdHelpLang.MANAGE_RESTORE, new Message(RandomData.randomString(10)));

        List<Lang> result = messageMap.entrySet().stream()
                .sorted(new LocaleEntryComparator())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Lang> expected = Arrays.asList(
                CmdHelpLang.ANALYZE,
                CmdHelpLang.MANAGE_RESTORE,
                CmdHelpLang.SERVERS
        );
        assertEquals(expected, result);
    }

    @Test
    public void geoInfoComparator() {
        List<GeoInfo> geoInfos = RandomData.randomGeoInfo();

        List<Long> expected = geoInfos.stream().map(GeoInfo::getDate)
                .sorted(Long::compare).collect(Collectors.toList());
        Collections.reverse(expected);

        geoInfos.sort(new GeoInfoComparator());
        List<Long> result = geoInfos.stream().map(GeoInfo::getDate).collect(Collectors.toList());
        assertEquals(expected, result);
    }
}
