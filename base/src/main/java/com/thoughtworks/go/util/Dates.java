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

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

public class Dates {
    private static final DateTimeFormatter ISO_FORMATTER_NO_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZZ");
    private static final DateTimeFormatter ISO_FORMATTER_UTC_NO_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter FORMATTER_SIMPLE_DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault());

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

    public static String formatIso8601StrictOffsetUtcWithoutMillis(Date date) {
        return ISO_FORMATTER_UTC_NO_MILLIS.format(date.toInstant());
    }

    public static String formatIso8601ForCCTray(Date date) {
        if (date == null) {
            return null;
        }
        return ISO_FORMATTER_UTC_NO_MILLIS.format(date.toInstant());
    }

    /**
     * Parses a date that is ISO8601-like, but has no milliseconds, and uses "compact" offsets, e.g +0200 instead of +02:00
     * @param date An ISO8601 compatible date
     * @return The parsed date
     */
    @TestOnly
    public static Date parseIso8601CompactOffset(String date) {
        return Date.from(ISO_FORMATTER_NO_MILLIS.parse(date, ZonedDateTime::from).toInstant());
    }

    /**
     * Parses the date using Java's standard parser, which should be broadly compatible with ISO 8601 and RFC 3339
     * despite them having some differences
     * @param date An ISO8601 or RFC3339 compatible date
     * @return The parsed date
     */
    public static Date parseIso8601StrictOffset(String date) {
        return Date.from(ISO_OFFSET_DATE_TIME.parse(date, ZonedDateTime::from).toInstant());
    }

    @SuppressWarnings("unused") // Used from Ruby stages_controller
    public static String formatToSimpleDate(Date date) {
        return FORMATTER_SIMPLE_DISPLAY_DATE.withLocale(Locale.getDefault()).format(date.toInstant());
    }

    public static boolean isToday(Date date) {
        return LocalDate.now().isEqual(LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault()));
    }
}
