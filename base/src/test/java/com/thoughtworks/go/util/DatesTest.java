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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatesTest {

    private Locale originalLocale;

    @BeforeEach
    public void setUp() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterEach
    public void tearDown() {
        Locale.setDefault(originalLocale);
    }

    @Test
    public void shouldBeAbleToParseRfc3339OrIso8601Dates() {
        assertThat(Dates.parseIso8601StrictOffset("2008-09-09T10:56:14.345Z"))
            .isEqualTo(Date.from(ZonedDateTime.parse("2008-09-09T18:56:14.345+08:00").toInstant()));
        assertThat(Dates.parseIso8601StrictOffset("2008-09-09T10:56:14Z"))
            .isEqualTo(Date.from(ZonedDateTime.parse("2008-09-09T18:56:14+08:00").toInstant()));

        assertThat(Dates.parseIso8601StrictOffset("2013-03-21T13:43:57+05:30"))
            .isEqualTo(Date.from(ZonedDateTime.parse("2013-03-21T13:43:57+05:30").toInstant()));
        assertThat(Dates.parseIso8601StrictOffset("2013-03-21T13:43:57.789+05:30"))
            .isEqualTo(Date.from(ZonedDateTime.parse("2013-03-21T13:43:57.789+05:30").toInstant()));
    }

    @Test
    public void shouldSerializeDateForCcTray() {
        Date date = Date.from(ZonedDateTime.parse("2008-12-09T18:56:14+08:00").toInstant());
        assertThat(Dates.formatIso8601ForCCTray(date)).isEqualTo("2008-12-09T10:56:14Z");
    }

    @Test
    public void shouldFormatIsoDatesToUtc() {
        assertThat(Dates.parseIso8601StrictOffset("2013-03-21T13:43:57Z"))
            .isEqualTo("2013-03-21T13:43:57Z");
        assertThat(Dates.parseIso8601StrictOffset("2013-03-21T13:43:57+05:30"))
            .isEqualTo("2013-03-21T08:13:57Z");
        assertThat(Dates.parseIso8601StrictOffset("2013-03-21T13:43:57.333+05:30"))
            .isEqualTo("2013-03-21T08:13:57.333Z");
    }

    @Test
    public void shouldFormatSimpleDisplayDatesLocaleAware() {
        assertThat(Dates.formatToSimpleDate(Date.from(ZonedDateTime.parse("2008-09-09T18:56:14+08:00").toInstant())))
            .isEqualTo("09 Sep 2008");

        Locale.setDefault(Locale.forLanguageTag("en-SG"));
        assertThat(Dates.formatToSimpleDate(Date.from(ZonedDateTime.parse("2008-09-09T18:56:14+08:00").toInstant())))
            .isEqualTo("09 Sept 2008");
    }

    @Test
    public void shouldAnswerIfTheProvidedDateIsToday() {
        assertTrue(Dates.isToday(new Date()));
        assertFalse(Dates.isToday(Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneOffset.systemDefault()).toInstant())));
        assertFalse(Dates.isToday(Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneOffset.systemDefault()).toInstant())));
    }
}
