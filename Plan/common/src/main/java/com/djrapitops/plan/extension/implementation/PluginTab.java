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
package com.djrapitops.plan.extension.implementation;

import com.djrapitops.plan.extension.ElementOrder;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plugin.utilities.ArrayUtil;

import java.util.Arrays;
import java.util.Optional;

/**
 * Represents a tab of {@link com.djrapitops.plan.extension.DataExtension} defined by {@link com.djrapitops.plan.extension.annotation.Tab} and
 * {@link com.djrapitops.plan.extension.annotation.TabInfo} annotations.
 *
 * @author Rsl1122
 */
public class PluginTab {

    private final String tabName;
    private final Icon icon; // can be null
    private ElementOrder[] elementOrder; // can be null / miss values

    public PluginTab(String tabName, Icon icon, ElementOrder[] elementOrder) {
        this.tabName = tabName;
        this.icon = icon;
        this.elementOrder = elementOrder;
    }

    public String getTabName() {
        return tabName;
    }

    public Optional<Icon> getTabIcon() {
        return Optional.ofNullable(icon);
    }

    public Optional<ElementOrder[]> getTabElementOrder() {
        if (elementOrder == null) {
            return Optional.empty();
        }

        ElementOrder[] possibleValues = ElementOrder.values();
        if (elementOrder.length < possibleValues.length) {
            addMissingElements(possibleValues);
        }

        return Optional.of(elementOrder);
    }

    private void addMissingElements(ElementOrder[] possibleValues) {
        for (ElementOrder possibleValue : possibleValues) {
            if (Arrays.binarySearch(elementOrder, possibleValue) < 0) {
                elementOrder = ArrayUtil.merge(elementOrder, new ElementOrder[]{possibleValue});
            }
        }
    }
}