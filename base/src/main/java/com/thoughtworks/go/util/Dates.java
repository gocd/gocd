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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

public class Dates {
    private static final DateTimeFormatter ISO_FORMATTER_OFFSET_NO_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final DateTimeFormatter ISO_FORMATTER_OFFSET_MILLIS    = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static final DateTimeFormatter ISO_FORMATTER_UTC_NO_MILLIS    = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter ISO_FORMATTER_UTC_MILLIS       = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter FORMATTER_SIMPLE_DISPLAY_DATE  = DateTimeFormatter.ofPattern("dd MMM yyyy")
        .withLocale(Locale.ROOT).withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter FORMATTER_LONG_DATE_TIME       = DateTimeFormatter.ofPattern("dd MMM, yyyy 'at' HH:mm:ss '['Z']'")
        .withLocale(Locale.ROOT).withZone(ZoneId.systemDefault());

    @TestOnly
    public static @Nullable Date from(@Nullable LocalDateTime date) {
        return date == null ? null : Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static @Nullable Date from(@Nullable ZonedDateTime date) {
        return date == null ? null : Date.from(date.toInstant());
    }

    public static @NotNull String formatIso8601SystemCompactOffsetNoMillis(@NotNull Instant date) {
        return ISO_FORMATTER_OFFSET_NO_MILLIS.format(date.atZone(ZoneId.systemDefault()));
    }

    public static @NotNull String formatIso8601SystemCompactOffsetNoMillis(@NotNull Date date) {
        return formatIso8601SystemCompactOffsetNoMillis(date.toInstant());
    }

    public static @NotNull String formatIso8601UtcCompactOffsetWithMillis(@Nullable Instant date) {
        return date == null ? "" : ISO_FORMATTER_OFFSET_MILLIS.format(date.atZone(ZoneOffset.UTC));
    }

    public static @NotNull String formatIso8601UtcCompactOffsetWithMillis(@Nullable Date date) {
        return date == null ? "" : formatIso8601UtcCompactOffsetWithMillis(date.toInstant());
    }

    public static @NotNull String formatIso8601UtcWithMillis(@Nullable Instant date) {
        return date == null ? "" : ISO_FORMATTER_UTC_MILLIS.format(date);
    }

    public static @NotNull String formatIso8601UtcWithMillis(@Nullable Date date) {
        return date == null ? "" : formatIso8601UtcWithMillis(date.toInstant());
    }

    public static @NotNull String formatIso8601UtcNoMillis(@Nullable Date date) {
        return date == null ? "" : ISO_FORMATTER_UTC_NO_MILLIS.format(date.toInstant());
    }

    public static @Nullable String formatIso8601ForCCTray(@Nullable Date date) {
        return date == null ? null : ISO_FORMATTER_UTC_NO_MILLIS.format(date.toInstant());
    }

    /**
     * Parses a date that is ISO8601-like, but has no milliseconds, and uses "compact" offsets, e.g +0200 instead of +02:00
     * @param date An ISO8601 compatible date
     * @return The parsed date
     */
    @TestOnly
    public static @NotNull Date parseIso8601CompactOffsetNoMillis(@NotNull String date) {
        return Date.from(ISO_FORMATTER_OFFSET_NO_MILLIS.parse(date, ZonedDateTime::from).toInstant());
    }

    /**
     * Parses a date that is ISO8601-like, but has no milliseconds, and uses "compact" offsets, e.g +0200 instead of +02:00
     * @param date An ISO8601 compatible date
     * @return The parsed date
     */
    @TestOnly
    public static @NotNull Date parseIso8601CompactOffsetWithMillis(@NotNull String date) {
        return Date.from(ISO_FORMATTER_OFFSET_MILLIS.parse(date, ZonedDateTime::from).toInstant());
    }

    /**
     * Parses the date using Java's standard parser, which should be broadly compatible with ISO 8601 and RFC 3339
     * despite them having some differences
     * @param date An ISO8601 or RFC3339 compatible date
     * @return The parsed date
     */
    public static @NotNull Date parseIso8601StrictOffset(@NotNull String date) {
        return Date.from(ISO_OFFSET_DATE_TIME.parse(date, ZonedDateTime::from).toInstant());
    }

    @SuppressWarnings("unused") // Used from Ruby stages_controller
    public static @NotNull String formatToSimpleDate(@NotNull Date date) {
        return FORMATTER_SIMPLE_DISPLAY_DATE.format(date.toInstant());
    }

    @SuppressWarnings("unused") // Used from Ruby Java Util Date adapter
    public static @NotNull String formatToLongDateTime(@NotNull Date date) {
        return FORMATTER_LONG_DATE_TIME.format(date.toInstant());
    }
}
