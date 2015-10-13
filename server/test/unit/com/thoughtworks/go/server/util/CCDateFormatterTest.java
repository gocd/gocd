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

package com.thoughtworks.go.server.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;

import static org.junit.Assert.assertEquals;

public class CCDateFormatterTest {
    @Test
    public void testShouldGetDateStringInHumanBeingReadingStyleWithGMTOffset() throws Throwable {
        Method getDateFormatterWithTimeZoneMethod = CCDateFormatter.class.getDeclaredMethod("getDateFormatterWithTimeZone");
        getDateFormatterWithTimeZoneMethod.setAccessible(true);
        SimpleDateFormat dateFormatter= (SimpleDateFormat) getDateFormatterWithTimeZoneMethod.invoke(CCDateFormatter.class);
        assertEquals("d MMM yyyy HH:mm 'GMT' Z", dateFormatter.toPattern());
    }

    @Test
    public void testFormatISO8601PatternDateTime() throws Exception {
        DateTime dateTime = new DateTime(2007, 11, 12, 13, 10, 10, 00, DateTimeZone.forOffsetHours(0));
        assertEquals(dateTime, CCDateFormatter.iso8601("2007-11-12T13:10:10"));
    }
}
