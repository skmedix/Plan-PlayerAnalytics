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
package com.djrapitops.plan.utilities.formatting.time;

import com.djrapitops.plan.system.locale.Locale;
import com.djrapitops.plan.system.locale.lang.GenericLang;
import com.djrapitops.plan.system.settings.config.PlanConfig;
import com.djrapitops.plan.system.settings.paths.FormatSettings;
import com.djrapitops.plan.system.settings.paths.PluginSettings;
import com.djrapitops.plan.system.settings.paths.TimeSettings;
import com.djrapitops.plan.utilities.formatting.Formatter;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Abstract formatter for a timestamp.
 *
 * @author Rsl1122
 */
public abstract class DateFormatter implements Formatter<Long> {

    protected final PlanConfig config;
    protected final Locale locale;

    public DateFormatter(PlanConfig config, Locale locale) {
        this.config = config;
        this.locale = locale;
    }

    @Override
    public abstract String apply(Long value);

    protected String format(long epochMs, String format) {
        boolean useServerTime = config.isTrue(TimeSettings.USE_SERVER_TIME);
        String localeSetting = config.get(PluginSettings.LOCALE);
        java.util.Locale usedLocale = "default".equalsIgnoreCase(localeSetting)
                ? java.util.Locale.ENGLISH
                : java.util.Locale.forLanguageTag(localeSetting);
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, usedLocale);
        TimeZone timeZone = useServerTime ? TimeZone.getDefault() : TimeZone.getTimeZone("GMT");
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(epochMs);
    }

    protected String replaceRecentDays(long epochMs, String format) {
        return replaceRecentDays(epochMs, format, config.get(FormatSettings.DATE_RECENT_DAYS_PATTERN));
    }

    protected String replaceRecentDays(long epochMs, String format, String pattern) {
        long now = System.currentTimeMillis();

        boolean useServerTime = config.isTrue(TimeSettings.USE_SERVER_TIME);
        TimeZone timeZone = useServerTime ? TimeZone.getDefault() : TimeZone.getTimeZone("GMT");
        int offset = timeZone.getOffset(epochMs);

        // Time since Start of day: UTC + Timezone % 24 hours
        long fromStartOfDay = (now + offset) % TimeUnit.DAYS.toMillis(1L);
        if (epochMs > now - fromStartOfDay) {
            format = format.replace(pattern, locale.getString(GenericLang.TODAY));
        } else if (epochMs > now - TimeUnit.DAYS.toMillis(1L) - fromStartOfDay) {
            format = format.replace(pattern, locale.getString(GenericLang.YESTERDAY));
        } else if (epochMs > now - TimeUnit.DAYS.toMillis(5L)) {
            format = format.replace(pattern, "EEEE");
        }
        return format;
    }

}