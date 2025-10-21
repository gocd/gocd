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

import com.thoughtworks.go.junit5.SystemDefaultIsAsiaSingaporeTimeZoneExtension;
import com.thoughtworks.go.util.Dates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.*;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemDefaultIsAsiaSingaporeTimeZoneExtension.class)
public class TimeConverterTest {
    private TimeConverter timeConverter;

    @BeforeEach
    public void setUp() {
        this.timeConverter = new TimeConverter();
    }

    @Test
    public void convertedTimeShouldBeNullSafe() {
        assertThat(new TimeConverter().getConvertedTime(null, new Date()).toString())
            .isEqualTo("N/A");
    }

    @Test
    public void shouldConvertTimeFromDates() {
        Instant start = ZonedDateTime.of(2025, 11, 3, 15, 6, 8, 0, ZoneOffset.UTC).toInstant();
        Instant now = ZonedDateTime.of(2026, 6, 3, 15, 6, 8, 0, ZoneOffset.UTC).toInstant();
        assertThat(new TimeConverter().getConvertedTime(Date.from(start), Date.from(now)).toString())
            .isEqualTo("7 months ago");
    }

    @Test
    public void shouldDefaultToHumanReadableStartTimeWhenEndIsBeforeStart() {
        Instant start = ZonedDateTime.of(2025, 11, 3, 15, 6, 8, 0, ZoneOffset.UTC).toInstant();
        Instant now = start.minusSeconds(60);
        assertThat(new TimeConverter().getConvertedTime(Date.from(start), Date.from(now)).toString())
            .isEqualTo("3 Nov 2025 23:06 GMT +08:00");
    }

    @Test
    public void humanReadableDateShouldBeNullSafe() {
        assertThat(new TimeConverter().getHumanReadableStringWithTimeZone((Instant) null))
            .isEqualTo("N/A");
    }

    @Test
    public void humanReadableDateShouldReturnInSystemDefaultTimeZone() {
        Instant date = ZonedDateTime.of(2025, 11, 3, 15, 6, 8, 0, ZoneOffset.UTC).toInstant();
        assertThat(new TimeConverter().getHumanReadableStringWithTimeZone(date))
            .isEqualTo("03 Nov, 2025 at 23:06:08 +0800");
    }

    @Test
    public void testShouldReturn() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        assertThat(timeConverter.getConvertedTime(Dates.from(now), Dates.from(yesterday)))
            .isEqualTo(new TimeConverter.ConvertedTime(TimeConverter.getHumanReadableDate(Dates.from(now))));
    }

    @Test
    public void testShouldReportLessThanOneMinutesFor0To29Seconds() {
        assertThat(timeConverter.getConvertedTime(29))
            .isEqualTo(TimeConverter.LESS_THAN_A_MINUTE_AGO)
            .satisfies(t -> assertThat(t.toString()).isEqualTo("less than a minute ago"));
    }

    @Test
    public void testShouldReportOneMinuteFor30Seconds() {
        assertThat(timeConverter.getConvertedTime(30))
            .isEqualTo(TimeConverter.ABOUT_1_MINUTE_AGO)
            .satisfies(t -> assertThat(t.toString()).isEqualTo("1 minute ago"));
    }

    @Test
    public void testShouldReportOneMinuteFor89Seconds() {
        assertThat(timeConverter.getConvertedTime(89))
            .isEqualTo(TimeConverter.ABOUT_1_MINUTE_AGO)
            .satisfies(t -> assertThat(t.toString()).isEqualTo("1 minute ago"));
    }

    @Test
    public void testShouldReport2To44MinutesFor90Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofSeconds(90).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_X_MINUTES_AGO.argument(2))
            .satisfies(t -> assertThat(t.toString()).isEqualTo("2 minutes ago"));
    }

    @Test
    public void testShouldReport1HourFor45Minutes() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofMinutes(45).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_1_HOUR_AGO)
            .satisfies(t -> assertThat(t.toString()).isEqualTo("about 1 hour ago"));
    }

    @Test
    public void testShouldReport44MinutesFor44Minutes29Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofMinutes(45).minusSeconds(31).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_X_MINUTES_AGO.argument(44))
            .satisfies(t -> assertThat(t.toString()).isEqualTo("44 minutes ago"));;
    }

    @Test
    public void testShouldReportAbout1HourFor44Minutes30Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofMinutes(45).minusSeconds(30).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_1_HOUR_AGO)
            .satisfies(t -> assertThat(t.toString()).isEqualTo("about 1 hour ago"));
    }

    @Test
    public void testShouldReportAbout1HourFor89Minutes29Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofHours(1).plusMinutes(30).minusSeconds(31).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_1_HOUR_AGO)
            .satisfies(t -> assertThat(t.toString()).isEqualTo("about 1 hour ago"));
    }

    @Test
    public void testShouldReportAbout2HoursHourFor89Minutes30Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofHours(1).plusMinutes(30).minusSeconds(30).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_X_HOURS_AGO.argument(2))
            .satisfies(t -> assertThat(t.toString()).isEqualTo("about 2 hours ago"));
    }

    @Test
    public void testShouldReport23HoursFor23Hours59Minutes29Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofDays(1).minusSeconds(31).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_X_HOURS_AGO.argument(23))
            .satisfies(t -> assertThat(t.toString()).isEqualTo("about 23 hours ago"));
    }

    @Test
    public void testShouldReportAbout1DayFor23Hours59Minutes30Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofDays(1).minusSeconds(30).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_1_DAY_AGO)
            .satisfies(t -> assertThat(t.toString()).isEqualTo("1 day ago"));
    }

    @Test
    public void testShouldReportAbout1DayFor47Hours59Minutes29Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofDays(2).minusSeconds(31).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_1_DAY_AGO)
            .satisfies(t -> assertThat(t.toString()).isEqualTo("1 day ago"));
    }

    @Test
    public void testShouldReport2DaysFor47Hours59Minutes29Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofDays(2).minusSeconds(30).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_X_DAYS_AGO.argument(2))
            .satisfies(t -> assertThat(t.toString()).isEqualTo("2 days ago"));
    }

    @Test
    public void testShouldReport29DaysFor29Days23Hours59Minutes29Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofDays(30).minusSeconds(31).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_X_DAYS_AGO.argument(29))
            .satisfies(t -> assertThat(t.toString()).isEqualTo("29 days ago"));
    }

    @Test
    public void testShouldReportAbout1MonthFor29Days23Hours59Minutes30Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofDays(30).minusSeconds(30).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_1_MONTH_AGO)
            .satisfies(t -> assertThat(t.toString()).isEqualTo("about 1 month ago"));
    }

    @Test
    public void testShouldReportAbout1MonthFor59Days23Hours59Minutes29Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofDays(60).minusSeconds(31).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_1_MONTH_AGO)
            .satisfies(t -> assertThat(t.toString()).isEqualTo("about 1 month ago"));
    }

    @Test
    public void testShouldReport2MonthsFor59Days23Hours59Minutes30Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofDays(60).minusSeconds(30).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_X_MONTHS_AGO.argument(2))
            .satisfies(t -> assertThat(t.toString()).isEqualTo("2 months ago"));
    }

    @Test
    public void testShouldReport12MonthsFor59Days23Hours59Minutes30Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofDays(365).minusSeconds(31).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_X_MONTHS_AGO.argument(12))
            .satisfies(t -> assertThat(t.toString()).isEqualTo("12 months ago"));
    }

    @Test
    public void testShouldReportAbout1YearFor1YearMinus30Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofDays(365).minusSeconds(30).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_1_YEAR_AGO)
            .satisfies(t -> assertThat(t.toString()).isEqualTo("about 1 year ago"));
    }

    @Test
    public void testShouldReportAbout1YearFor2YearsMinus31Seconds() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofDays(2 * 365).minusSeconds(31).toSeconds()))
            .isEqualTo(TimeConverter.ABOUT_1_YEAR_AGO)
            .satisfies(t -> assertThat(t.toString()).isEqualTo("about 1 year ago"));
    }

    @Test
    public void testShouldReturnTimeUnitAsYearsWhenDurationIsLargerThan2Years() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofDays(2 * 365).minusSeconds(30).toSeconds()))
            .isEqualTo(TimeConverter.OVER_X_YEARS_AGO.argument(2))
            .satisfies(t -> assertThat(t.toString()).isEqualTo("over 2 years ago"));
    }

    @Test
    public void testShouldReturnTimeUnitAsYearsWhenDurationIsLargerThan3Years() {
        assertThat(timeConverter
            .getConvertedTime(Duration.ofDays(3 * 365).plusDays(2).toSeconds()))
            .isEqualTo(TimeConverter.OVER_X_YEARS_AGO.argument(3))
            .satisfies(t -> assertThat(t.toString()).isEqualTo("over 3 years ago"));

    }

    @Test
    public void testShouldReturnNotAvailableWhenInputDateIsNull() {
        assertThat(timeConverter.getConvertedTime(null))
            .isEqualTo(TimeConverter.NOT_AVAILABLE)
            .satisfies(t -> assertThat(t.toString()).isEqualTo("N/A"));
    }
}
