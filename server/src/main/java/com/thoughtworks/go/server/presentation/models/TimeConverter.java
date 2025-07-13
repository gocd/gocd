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
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Seconds;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TimeConverter {
    static final int HOUR_IN_SECONDS = 60 * 60;

    static final int DAY_IN_SECONDS = 24 * 60 * 60;

    static final int MONTH_IN_SECONDS = 30 * DAY_IN_SECONDS;

    static final int YEAR_IN_SECONDS = 365 * DAY_IN_SECONDS;

    public static final ConvertedTime OVER_X_YEARS_AGO = new ConvertedTime("label.over.x.years",
            "over $time years ago");

    public static final ConvertedTime ABOUT_1_YEAR_AGO = new ConvertedTime("label.about.1.year", "about 1 year ago");

    public static final ConvertedTime ABOUT_X_MONTHS_AGO = new ConvertedTime("label.x.months",
            "$time months ago");

    public static final ConvertedTime ABOUT_1_MONTH_AGO =
            new ConvertedTime("label.1.month", "about 1 month ago");

    public static final ConvertedTime ABOUT_X_DAYS_AGO = new ConvertedTime("label.x.days",
            "$time days ago");

    public static final ConvertedTime ABOUT_1_DAY_AGO = new ConvertedTime("label.1.day", "1 day ago");

    public static final ConvertedTime ABOUT_X_HOURS_AGO = new ConvertedTime("label.x.hours",
            "about $time hours ago");

    public static final ConvertedTime ABOUT_1_HOUR_AGO = new ConvertedTime("label.1.hour", "about 1 hour ago");

    public static final ConvertedTime ABOUT_X_MINUTES_AGO = new ConvertedTime("label.x.minutes",
            "$time minutes ago");

    public static final ConvertedTime ABOUT_1_MINUTE_AGO = new ConvertedTime("label.1.minute", "1 minute ago");

    public static final ConvertedTime LESS_THAN_A_MINUTE_AGO = new ConvertedTime("label.less.1.minute",
            "less than a minute ago");

    public static final DateTimeFormatter dateFormatterWithTimeZone = DateTimeFormatter.ofPattern("dd MMM, yyyy 'at' HH:mm:ss [Z]");

    private static final Map<Seconds, ConvertableTime> RULES = new LinkedHashMap<>();

    static {
        RULES.put(Seconds.seconds(29), new TimeConverter.LessThanAMinute());
        RULES.put(Minutes.minutes(1).toStandardSeconds().plus(Seconds.seconds(29)),
                new TimeConverter.AboutOneMinute());
        RULES.put(Minutes.minutes(44).toStandardSeconds().plus(Seconds.seconds(29)),
                new TimeConverter.From2To44Minutes());
        RULES.put(Minutes.minutes(89).toStandardSeconds().plus(Seconds.seconds(29)),
                new TimeConverter.AboutOneHour());
        RULES.put(Hours.hours(23).toStandardMinutes().plus(Minutes.minutes(59)).toStandardSeconds().plus(
                Seconds.seconds(29)), new TimeConverter.About2To24Hours());
        RULES.put(Hours.hours(47).toStandardMinutes().plus(Minutes.minutes(59)).toStandardSeconds().plus(
                Seconds.seconds(29)), new TimeConverter.AboutOneDay());
        RULES.put(Days.days(29).toStandardHours().plus(Hours.hours(23)).toStandardMinutes().plus(
                Minutes.minutes(59)).toStandardSeconds().plus(Seconds.seconds(29)),
                new TimeConverter.From2To29Days());
        RULES.put(Days.days(59).toStandardHours().plus(Hours.hours(23)).toStandardMinutes().plus(
                Minutes.minutes(59)).toStandardSeconds().plus(Seconds.seconds(29)),
                new TimeConverter.AboutOneMonth());
        RULES.put(Days.days(365).toStandardSeconds().minus(Seconds.seconds(31)),
                new TimeConverter.From2To12Month());
        RULES.put(Days.days(730).toStandardSeconds().minus(Seconds.seconds(31)),
                new TimeConverter.AboutOneYear());
    }

    public ConvertedTime getConvertedTime(long duration) {
        Set<Seconds> keys = RULES.keySet();
        for (Seconds seconds : keys) {
            if (duration <= seconds.getSeconds()) {
                return RULES.get(seconds).getConvertedTime(duration);
            }
        }
        return new TimeConverter.OverTwoYears().getConvertedTime(duration);
    }

    public ConvertedTime getConvertedTime(Date dateFrom) {
        return dateFrom == null ? ConvertedTime.NOT_AVAILABLE : getConvertedTime(dateFrom, new Date());
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
        return date == null ? ConvertedTime.NOT_AVAILABLE.toString() : dateFormatterWithTimeZone.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }

    public ConvertedTime getConvertedTime(Date dateLogFileGenerated, Date dateCheckTheDuration) {
        if (dateCheckTheDuration.getTime() < dateLogFileGenerated.getTime()) {
            String dateString = getHumanReadableDate(dateLogFileGenerated);
            return new ConvertedTime(dateString);
        } else {
            return getConvertedTime((dateCheckTheDuration.getTime() - dateLogFileGenerated.getTime()) / 1000);
        }
    }

    public static class ConvertedTime {
        public static final ConvertedTime NOT_AVAILABLE = new TimeConverter.ConvertedTime("N/A");
        private final String message;
        private String code;
        private long arguments;

        public ConvertedTime(String code, long time, String message) {
            this.message = message;
            this.arguments = time;
            this.code = code;
        }

        public ConvertedTime(String code, String message) {
            this.message = message;
            this.code = code;
        }

        public ConvertedTime(String message) {
            this.message = message;
        }

        public Object[] getArguments() {
            return new Long[]{arguments};
        }

        /**
         * Create a new ConvertedTime instance based on this with new time value.
         */
        public ConvertedTime argument(long time) {
            String newMessage = Strings.CS.replace(message, "$time", String.valueOf(time));
            return new ConvertedTime(code, time, newMessage);
        }

        public String getDefaultMessage() {
            return message;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ConvertedTime other)) {
                return false;
            }

            boolean eq = true;
            eq &= Objects.equals(code, other.code);
            eq &= Objects.equals(arguments, other.arguments);
            eq &= Objects.equals(message, other.message);

            return eq;
        }

        @Override
        public String toString() {
            return getDefaultMessage();
        }
    }

    interface ConvertableTime {
        ConvertedTime getConvertedTime(long duration);
    }

    static class LessThanAMinute implements ConvertableTime {

        @Override
        public ConvertedTime getConvertedTime(long duration) {
            return LESS_THAN_A_MINUTE_AGO;
        }
    }

    static class AboutOneMinute implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long duration) {
            return ABOUT_1_MINUTE_AGO;
        }
    }

    static class From2To44Minutes implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long duration) {
            long time = (duration + 30) / 60;
            return ABOUT_X_MINUTES_AGO.argument(time);
        }
    }

    static class AboutOneHour implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long duration) {
            return ABOUT_1_HOUR_AGO;
        }
    }

    static class About2To24Hours implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long duration) {
            long hours = (duration + 30 * 60 + 30) / TimeConverter.HOUR_IN_SECONDS;
            long time = hours >= 23 ? 23 : hours;
            return ABOUT_X_HOURS_AGO.argument(time);

        }
    }

    static class AboutOneDay implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long duration) {
            return ABOUT_1_DAY_AGO;
        }
    }

    static class From2To29Days implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long duration) {
            long time = (duration + 30) / TimeConverter.DAY_IN_SECONDS;
            return ABOUT_X_DAYS_AGO.argument(time);
        }
    }

    static class AboutOneMonth implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long duration) {
            return ABOUT_1_MONTH_AGO;
        }
    }

    static class From2To12Month implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long duration) {
            long time = (duration + 30) / TimeConverter.MONTH_IN_SECONDS;
            return ABOUT_X_MONTHS_AGO.argument(time);
        }
    }

    static class AboutOneYear implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long duration) {
            return ABOUT_1_YEAR_AGO;
        }

    }

    static class OverTwoYears implements ConvertableTime {
        @Override
        public ConvertedTime getConvertedTime(long duration) {
            long time = (duration + 30)
                    / TimeConverter.YEAR_IN_SECONDS;
            return OVER_X_YEARS_AGO.argument(time);
        }
    }
}
