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

import java.text.SimpleDateFormat;
import java.util.Locale;

import com.thoughtworks.go.util.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public final class CCDateFormatter {

    private static final int DATE_START = 3;

    private static final int DATE_END = 17;

    private static final int MILLONS_OF_DAY = 1000 * 3600 * 24;

    private static DateTimeFormatter yyyyMMddHHmmssPattern = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    private static DateTimeFormatter iso8601Pattern = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static SimpleDateFormat yyyyMMddHHmmssSimpleDateFormat =
        new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);

    private static SimpleDateFormat iso8601SimpleDateFormat =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);

    private CCDateFormatter() {
    }

    public static String duration(long timeSpan) {
        String when = timeSpan > 0 ? "ago" : "later";
        long days = timeSpan / CCDateFormatter.MILLONS_OF_DAY;
        String daysString = days == 0 ? "" : days + " days ";
        String remaining = DateUtils.formatTime(timeSpan - days * CCDateFormatter.MILLONS_OF_DAY);
        String space = remaining.endsWith(" ") ? "" : " ";
        return daysString + remaining + space + when;
    }

    public static String getHumanReadableDate(DateTime date) {
        String dateString = getDateFormatterWithTimeZone().format(date.toDate());
        int colonPlace = dateString.length() - 2;
        return dateString.substring(0, colonPlace) + ":" + dateString.substring(colonPlace);
    }

    private static SimpleDateFormat getDateFormatterWithTimeZone() {
        return new SimpleDateFormat("d MMM yyyy HH:mm 'GMT' Z", Locale.ENGLISH);
    }

    public static String yyyyMMddHHmmss(DateTime date) {
        return yyyyMMddHHmmssSimpleDateFormat.format(date.toDate());
    }

    public static DateTime format(String datetime, String dateFormat) {
        return DateTimeFormat.forPattern(dateFormat).parseDateTime(datetime);
    }

    public static String format(DateTime datetime, String format) {
        return new SimpleDateFormat(format, Locale.ENGLISH).format(datetime.toDate());
    }

    public static DateTime formatLogName(String logFileName) {
        return yyyyMMddHHmmssPattern.parseDateTime(getBuildDateFromLogFileName(logFileName));
    }

    public static String getBuildDateFromLogFileName(String logFileName) {
        return logFileName.substring(DATE_START, DATE_END);
    }

    public static DateTime iso8601(String datetime) {
        try {
            //TODO: ChrisS: This seems wierd - surely JodaTime has already got a way to do this?
            return new DateTime(DateUtils.parseISO8601(datetime).getTime(),
                    DateTimeZone.forOffsetHours(0));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
