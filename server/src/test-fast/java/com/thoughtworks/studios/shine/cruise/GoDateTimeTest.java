/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.studios.shine.cruise;

import static org.junit.Assert.assertEquals;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

public class GoDateTimeTest {

    @Test
    public void canParseCurrentTimestampFormat() throws Exception {
        String timestampString = "2008-05-29T09:45:37+05:30";
        DateTime expectedDateTime = new DateTime(2008, 05, 29, 4, 15, 37, 0, DateTimeZone.forID("UTC"));

        assertEquals(expectedDateTime, GoDateTime.parseToUTC(timestampString));
    }

    @Test
    public void canParseOldTimestampFormat() throws Exception {
        String timestampString = "2008-05-29 09:45:37 +0530";
        DateTime expectedDateTime = new DateTime(2008, 05, 29, 4, 15, 37, 0, DateTimeZone.forID("UTC"));

        assertEquals(expectedDateTime, GoDateTime.parseToUTC(timestampString));
    }

    @Test
    public void canConvertTimestampStringToZuluString() throws Exception {
        String timestampString = "2008-05-29 09:45:37 +0530";
        assertEquals("2008-05-29T04:15:37Z", GoDateTime.parseToZuluString(timestampString));
    }
}
