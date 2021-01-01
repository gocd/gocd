/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class DateUtils {
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
    private static final DateTimeFormatter formatterUtc = formatter.withZoneUTC();

    public static String formatISO8601(Date from) {
        return formatter.print(from.getTime());
    }

    public static String formatIso8601ForCCTray(Date date) {
        if (date == null) {
            return null;
        }
        return formatterUtc.print(date.getTime());
    }

    public static String formatRFC822(Date date) {
        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZZ").format(date);
    }

    public static Date parseYYYYMMDD(String yyyyDashmmDashdd) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd").parse(yyyyDashmmDashdd);
    }

    public static Date parseRFC822(String date) {
        try {
            return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZZ").parse(date.trim());
        } catch (ParseException e) {
            throw bomb(e);
        }
    }

    public static Date parseISO8601(String date) {
        try {
            DateTime dateTime = formatter.parseDateTime(date);
            return dateTime.toDate();
        } catch (Exception e) {
            //fall through and try and parse other ISO standard formats
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZZZZ").parse(date);
        } catch (ParseException e) {
            //fall through and try and parse other ISO standard formats
        }
        try {
            return dateFormatFor("yyyy-MM-dd'T'HH:mm:ss").parse(date);
        } catch (ParseException e) {
            throw bomb(e);
        }
    }

    public static String formatToSimpleDate(Date date) {
        SimpleDateFormat simpleDate = new SimpleDateFormat("dd MMM yyyy");
        return simpleDate.format(date);
    }

    public static String formatTime(long time) {
        long seconds = time / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours != 0) {
            sb.append(hours).append(" hours ");
        }
        if (minutes != 0) {
            sb.append(minutes).append(" minutes ");
        }
        if (seconds != 0) {
            sb.append(seconds).append(" seconds");
        }

        return sb.toString();
    }

    public static boolean isToday(Date date) {
        Calendar today = Calendar.getInstance();
        Calendar otherDay = Calendar.getInstance();
        otherDay.setTime(date);

        return (today.get(Calendar.YEAR) == otherDay.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == otherDay.get(Calendar.DAY_OF_YEAR));
    }

    private static SimpleDateFormat dateFormatFor(String simpleDateFormat) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(simpleDateFormat);
        dateFormat.setTimeZone(UTC);
        return dateFormat;
    }
}
