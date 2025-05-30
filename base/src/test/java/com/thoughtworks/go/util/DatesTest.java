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

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatesTest {

    @Test
    public void shouldBeAbleToParseRFC822Dates() {
        Date date = Dates.parseRFC822("Tue, 09 Dec 2008 18:56:14 +0800");
        assertThat(date).isEqualTo(Date.from(ZonedDateTime.parse("2008-12-09T18:56:14+08:00").toInstant()));
    }

    @Test
    public void shouldSerializeDateForCcTray() {
        Date date = Date.from(ZonedDateTime.parse("2008-12-09T18:56:14+08:00").toInstant());
        assertThat(Dates.formatIso8601ForCCTray(date)).isEqualTo("2008-12-09T10:56:14Z");
    }

    @Test
    public void shouldAnswerIfTheProvidedDateIsToday() {
        final Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        cal.add(Calendar.DATE, -1);
        Date yesterday = cal.getTime();

        assertTrue(Dates.isToday(today));
        assertFalse(Dates.isToday(yesterday));
    }
}
