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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static java.lang.Math.min;
import static java.time.Duration.*;

public class TimeConverter {
    private static final DateTimeFormatter READABLE_PATTERN_WITH_SYSTEM_TIMEZONE = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm 'UTC' XXX", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    @FunctionalInterface
    private interface DurationFormatter {
        String toFuzzyDuration(Duration duration);
    }

    private static final Map<Duration, DurationFormatter> BOUNDARY_TO_FORMATTER = new LinkedHashMap<>();
    static {
        BOUNDARY_TO_FORMATTER.put(ofMinutes(1).minusSeconds(31),
            d -> "less than a minute ago");
        BOUNDARY_TO_FORMATTER.put(ofMinutes(2).minusSeconds(31),
            d -> "1 minute ago");
        BOUNDARY_TO_FORMATTER.put(ofMinutes(45).minusSeconds(31),
            d -> "%d minutes ago".formatted(d.plusSeconds(30).dividedBy(ofMinutes(1))));
        BOUNDARY_TO_FORMATTER.put(ofMinutes(90).minusSeconds(31),
            d -> "about 1 hour ago");
        BOUNDARY_TO_FORMATTER.put(ofDays(1).minusSeconds(31),
            d -> "about %d hours ago".formatted(min(23, d.plusMinutes(30).plusSeconds(30).dividedBy(ofHours(1)))));
        BOUNDARY_TO_FORMATTER.put(ofDays(2).minusSeconds(31),
            d -> "1 day ago");
        BOUNDARY_TO_FORMATTER.put(ofDays(30).minusSeconds(31),
            d -> "%d days ago".formatted(d.plusSeconds(30).dividedBy(ofDays(1))));
        BOUNDARY_TO_FORMATTER.put(ofDays(60).minusSeconds(31),
            d -> "about 1 month ago");
        BOUNDARY_TO_FORMATTER.put(ofDays(365).minusSeconds(31),
            d -> "%d months ago".formatted(d.plusSeconds(30).dividedBy(ofDays(30))));
        BOUNDARY_TO_FORMATTER.put(ofDays(730).minusSeconds(31),
            d -> "about 1 year ago");
    }

    @VisibleForTesting
    static String toHumanReadableFuzzyDuration(@NotNull Duration d) {
        return BOUNDARY_TO_FORMATTER.entrySet()
            .stream()
            .filter(entry -> d.compareTo(entry.getKey()) <= 0)
            .findFirst()
            .map(e -> e.getValue().toFuzzyDuration(d))
            .orElseGet(() -> "over %d years ago".formatted(d.plusSeconds(30).dividedBy(ofDays(365))));
    }

    @SuppressWarnings("unused") // Used from within VSM rendering Rails code
    public static String toHumanReadableFuzzyDuration(Date dateFrom) {
        return toHumanReadableFuzzyDuration(dateFrom, new Date());
    }

    @VisibleForTesting
    static String toHumanReadableFuzzyDuration(Date dateFrom, Date dateTo) {
        if (dateFrom == null) {
            return "N/A";
        }

        if (dateTo.getTime() < dateFrom.getTime()) {
            return toHumanReadableDate(dateFrom);
        } else {
            return toHumanReadableFuzzyDuration(ofMillis(dateTo.getTime() - dateFrom.getTime()));
        }
    }

    @VisibleForTesting
    static String toHumanReadableDate(@NotNull Date date) {
        return READABLE_PATTERN_WITH_SYSTEM_TIMEZONE.format(date.toInstant());
    }
}
