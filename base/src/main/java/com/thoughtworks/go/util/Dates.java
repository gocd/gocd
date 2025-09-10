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
package com.thoughtworks.go.util;

import org.jetbrains.annotations.TestOnly;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

public class Dates {
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final DateTimeFormatter ISO_FORMATTER_NO_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZZ");
    private static final DateTimeFormatter ISO_FORMATTER_UTC_NO_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    @TestOnly
    public static Date from(LocalDateTime date) {
        return date == null ? null : Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date from(ZonedDateTime date) {
        return date == null ? null : Date.from(date.toInstant());
    }

    public static String formatIso8601CompactOffset(Date date) {
        return formatIso8601CompactOffset(date.toInstant());
    }

    public static String formatIso8601CompactOffset(Instant date) {
        return ISO_FORMATTER_NO_MILLIS.format(date.atZone(ZoneId.systemDefault()));
    }

    public static String formatIso8601ForCCTray(Date date) {
        if (date == null) {
            return null;
        }
        return ISO_FORMATTER_UTC_NO_MILLIS.format(date.toInstant());
    }

    public static Date parseRFC822(String date) {
        try {
            return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZZ").parse(date.trim());
        } catch (ParseException e) {
            throw bomb(e);
        }
    }

    public static Date parseIso8601CompactOffset(String date) {
        return Date.from(ISO_FORMATTER_NO_MILLIS.parse(date, ZonedDateTime::from).toInstant());
    }

    public static Date parseIso8601StrictOffset(String date) {
        return Date.from(ISO_OFFSET_DATE_TIME.parse(date, ZonedDateTime::from).toInstant());
    }

    @SuppressWarnings("unused") // Used from Ruby stages_controller
    public static String formatToSimpleDate(Date date) {
        SimpleDateFormat simpleDate = new SimpleDateFormat("dd MMM yyyy");
        return simpleDate.format(date);
    }

    public static boolean isToday(Date date) {
        Calendar today = Calendar.getInstance();
        Calendar otherDay = Calendar.getInstance();
        otherDay.setTime(date);

        return (today.get(Calendar.YEAR) == otherDay.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == otherDay.get(Calendar.DAY_OF_YEAR));
    }
}
