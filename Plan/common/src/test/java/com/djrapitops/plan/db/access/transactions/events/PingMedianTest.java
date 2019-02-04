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
package com.djrapitops.plan.db.access.transactions.events;

import com.djrapitops.plan.data.store.objects.DateObj;
import com.djrapitops.plan.utilities.analysis.Median;
import org.junit.Before;
import org.junit.Test;
import utilities.RandomData;
import utilities.TestConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link PingStoreTransaction#getMeanValue()}.
 *
 * @author Rsl1122
 */
public class PingMedianTest {

    private List<DateObj<Integer>> testPing;

    @Before
    public void setUp() {
        testPing = new ArrayList<>();

        for (int i = 0; i < TimeUnit.MINUTES.toMillis(1L); i += TimeUnit.SECONDS.toMillis(2L)) {
            testPing.add(new DateObj<>(i, RandomData.randomInt(1, 4000)));
        }
    }

    @Test
    public void medianCalculation() {
        List<Integer> collect = testPing.stream().map(DateObj::getValue).sorted().collect(Collectors.toList());

        int expected = (int) Median.forList(collect).calculate();
        int result = new PingStoreTransaction(TestConstants.PLAYER_ONE_UUID, TestConstants.SERVER_UUID, new ArrayList<>())
                .getMeanValue();

        assertEquals(expected, result);
    }

    @Test
    public void medianCalculationForSingleEntry() {
        int expected = 50;
        int result = new PingStoreTransaction(TestConstants.PLAYER_ONE_UUID, TestConstants.SERVER_UUID,
                Collections.singletonList(new DateObj<>(0, expected)))
                .getMeanValue();

        assertEquals(expected, result);
    }

    @Test
    public void medianCalculationForNoEntries() {
        int expected = -1;
        int result = new PingStoreTransaction(TestConstants.PLAYER_ONE_UUID, TestConstants.SERVER_UUID, new ArrayList<>())
                .getMeanValue();

        assertEquals(expected, result);
    }
}