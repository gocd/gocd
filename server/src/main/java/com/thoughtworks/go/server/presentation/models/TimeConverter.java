/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.server.presentation.models;

import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.VisibleForTesting;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.time.temporal.ChronoUnit.*;

public class TimeConverter {
    static final ConvertedTime OVER_X_YEARS_AGO = new ConvertedTime("label.over.x.years", "over $time years ago");
    static final ConvertedTime ABOUT_1_YEAR_AGO = new ConvertedTime("label.about.1.year", "about 1 year ago");
    static final ConvertedTime ABOUT_X_MONTHS_AGO = new ConvertedTime("label.x.months", "$time months ago");
    static final ConvertedTime ABOUT_1_MONTH_AGO = new ConvertedTime("label.1.month", "about 1 month ago");
    static final ConvertedTime ABOUT_X_DAYS_AGO = new ConvertedTime("label.x.days", "$time days ago");
    static final ConvertedTime ABOUT_1_DAY_AGO = new ConvertedTime("label.1.day", "1 day ago");
    static final ConvertedTime ABOUT_X_HOURS_AGO = new ConvertedTime("label.x.hours", "about $time hours ago");
    static final ConvertedTime ABOUT_1_HOUR_AGO = new ConvertedTime("label.1.hour", "about 1 hour ago");
    static final ConvertedTime ABOUT_X_MINUTES_AGO = new ConvertedTime("label.x.minutes", "$time minutes ago");
    static final ConvertedTime ABOUT_1_MINUTE_AGO = new ConvertedTime("label.1.minute", "1 minute ago");
    static final ConvertedTime LESS_THAN_A_MINUTE_AGO = new ConvertedTime("label.less.1.minute", "less than a minute ago");
    static final ConvertedTime NOT_AVAILABLE = new TimeConverter.ConvertedTime("N/A");

    private static final DateTimeFormatter DATE_FORMATTER_WITH_TIME_ZONE = DateTimeFormatter.ofPattern("dd MMM, yyyy 'at' HH:mm:ss [Z]");

    private static final Map<Duration, ConvertableTime> RULES = new LinkedHashMap<>();
    static {
        RULES.put(Duration.ofMinutes(1).minusSeconds(31), new TimeConverter.LessThanAMinute());
        RULES.put(Duration.ofMinutes(2).minusSeconds(31), new TimeConverter.AboutOneMinute());
        RULES.put(Duration.ofMinutes(45).minusSeconds(31), new TimeConverter.From2To44Minutes());
        RULES.put(Duration.ofMinutes(90).minusSeconds(31), new TimeConverter.AboutOneHour());
        RULES.put(Duration.ofDays(1).minusSeconds(31), new TimeConverter.About2To24Hours());
        RULES.put(Duration.ofDays(2).minusSeconds(31), new TimeConverter.AboutOneDay());
        RULES.put(Duration.ofDays(30).minusSeconds(31), new TimeConverter.From2To29Days());
        RULES.put(Duration.ofDays(60).minusSeconds(31), new TimeConverter.AboutOneMonth());
        RULES.put(Duration.ofDays(365).minusSeconds(31), new TimeConverter.From2To12Month());
        RULES.put(Duration.ofDays(730).minusSeconds(31), new TimeConverter.AboutOneYear());
    }

    public ConvertedTime getConvertedTime(long durationSeconds) {
        return RULES.entrySet()
            .stream()
            .filter(entry -> durationSeconds <= entry.getKey().toSeconds())
            .findFirst()
            .map(Map.Entry::getValue)
            .orElseGet(OverTwoYears::new)
            .getConvertedTime(durationSeconds);
    }

    public ConvertedTime getConvertedTime(Date dateFrom) {
        return getConvertedTime(dateFrom, new Date());
    }

    public static String getHumanReadableDate(Date date) {
        String dateString = getDateFormatterWithTimeZone().format(date);
        int colonPlace = dateString.length() - 2;
        return dateString.substring(0, colonPlace) + ":" + dateString.substring(colonPlace);
    }

    private static SimpleDateFormat getDateFormatterWithTimeZone() {
        return new SimpleDateFormat("d MMM yyyy HH:mm 'GMT' Z", Locale.ENGLISH);
    }

    public String getHumanReadableStringWithTimeZone(Date date) {
        return date == null ? null : getHumanReadableStringWithTimeZone(date.toInstant());
    }

    public String getHumanReadableStringWithTimeZone(Instant instant) {
        return instant == null ? NOT_AVAILABLE.toString() : DATE_FORMATTER_WITH_TIME_ZONE.format(instant.atZone(ZoneId.systemDefault()));
    }

    @VisibleForTesting
    ConvertedTime getConvertedTime(Date dateFrom, Date dateTo) {
        if (dateFrom == null) {
            return NOT_AVAILABLE;
        }

        if (dateTo.getTime() < dateFrom.getTime()) {
            return new ConvertedTime(getHumanReadableDate(dateFrom));
        } else {
            return getConvertedTime((dateTo.getTime() - dateFrom.getTime()) / 1000);
        }
    }

    public static class ConvertedTime {
        private final String message;
        private final String code;
        private final long arguments;

        public ConvertedTime(String message, String code, long time) {
            this.message = message;
            this.arguments = time;
            this.code = code;
        }

        public ConvertedTime(String message, String code) {
            this(message, code, 0);
        }

        public ConvertedTime(String message) {
           this(message, null, 0);
        }

        @SuppressWarnings("unused") // May be needed for JSON serialization?
        public Object[] getArguments() {
            return new Long[]{arguments};
        }

        /**
         * Create a new ConvertedTime instance based on this with new time value.
         */
        public ConvertedTime argument(long time) {
            String newMessage = Strings.CS.replace(message, "$time", String.valueOf(time));
            return new ConvertedTime(newMessage, code, time);
        }

        @SuppressWarnings("unused") // May be needed for JSON serialization?
        public String getDefaultMessage() {
            return message;
        }

        @Override
        public int hashCode() {
            return Objects.hash(message, code, arguments);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ConvertedTime that = (ConvertedTime) o;
            return arguments == that.arguments && Objects.equals(message, that.message) && Objects.equals(code, that.code);
        }

        @Override
        public String toString() {
            return getDefaultMessage();
        }
    }

    interface ConvertableTime {
        ConvertedTime getConvertedTime(long durationSeconds);
    }

    static class LessThanAMinute implements ConvertableTime {

        @Override
        public ConvertedTime getConvertedTime(long durationSeconds) {
            return LESS_THAN_A_MINUTE_AGO;
        }
    }

    static class AboutOneMinute implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long durationSeconds) {
            return ABOUT_1_MINUTE_AGO;
        }
    }

    static class From2To44Minutes implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long durationSeconds) {
            long time = (durationSeconds + 30) / 60;
            return ABOUT_X_MINUTES_AGO.argument(time);
        }
    }

    static class AboutOneHour implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long durationSeconds) {
            return ABOUT_1_HOUR_AGO;
        }
    }

    static class About2To24Hours implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long durationSeconds) {
            long hours = (durationSeconds + 30 * 60 + 30) / Duration.ofHours(1).toSeconds();
            long time = hours >= 23 ? 23 : hours;
            return ABOUT_X_HOURS_AGO.argument(time);

        }
    }

    static class AboutOneDay implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long durationSeconds) {
            return ABOUT_1_DAY_AGO;
        }
    }

    static class From2To29Days implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long durationSeconds) {
            long time = (durationSeconds + 30) / Duration.ofDays(1).toSeconds();
            return ABOUT_X_DAYS_AGO.argument(time);
        }
    }

    static class AboutOneMonth implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long durationSeconds) {
            return ABOUT_1_MONTH_AGO;
        }
    }

    static class From2To12Month implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long durationSeconds) {
            long time = (durationSeconds + 30) / Duration.of(30, DAYS).toSeconds();
            return ABOUT_X_MONTHS_AGO.argument(time);
        }
    }

    static class AboutOneYear implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long durationSeconds) {
            return ABOUT_1_YEAR_AGO;
        }
    }

    static class OverTwoYears implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long durationSeconds) {
            long time = (durationSeconds + 30) / Duration.of(365, DAYS).toSeconds();
            return OVER_X_YEARS_AGO.argument(time);
        }
    }
}
