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

package com.thoughtworks.go.util;

import java.util.Calendar;
import java.util.Date;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DateUtilsTest {

    @Test public void shouldBeAbleToParseRFC822Dates() throws Exception {
        Date date = DateUtils.parseRFC822("Tue, 09 Dec 2008 18:56:14 +0800");
        assertThat(date, is(new DateTime("2008-12-09T18:56:14+08:00").toDate()));
    }

    @Test
    public void shouldFormatDateToDisplayOnUI() {
        Calendar instance = Calendar.getInstance();
        instance.set(2009, Calendar.NOVEMBER, 5);
        Date date = instance.getTime();
        String formattedDate = DateUtils.formatToSimpleDate(date);
        assertThat(formattedDate, is("05 Nov 2009"));
    }
}
