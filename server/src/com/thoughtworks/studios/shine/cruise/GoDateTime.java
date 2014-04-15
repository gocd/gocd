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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class GoDateTime {

    public static DateTime parseToUTC(String timestampString) throws GoDateTimeException {

        DateTimeFormatter format = ISODateTimeFormat.dateTimeNoMillis();
        DateTime dateTime;
        try {
            dateTime = format.parseDateTime(timestampString);
        } catch (java.lang.IllegalArgumentException e) {
            // sigh. handle old cruise timestamp format, e.g. 2008-09-19 02:18:39 +0800
            format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z");
            try {
                dateTime = format.parseDateTime(timestampString);
            } catch (java.lang.IllegalArgumentException e2) {
                // give up !!
                throw new GoDateTimeException("Could not parse datetime " + timestampString, e2);
            }
        }
        return dateTime.toDateTime(DateTimeZone.forID("UTC"));
    }

    public static String parseToZuluString(String timestampString) throws GoDateTimeException {
        return ZuluDateTimeFormatter.toZuluString(parseToUTC(timestampString));
    }

    // this is called from within xslt, return an empty string if the timestamp cannot be parsed
    // let the xslt figure out what to do with such a case...
    public static String parseToZuluStringSwallowException(String timestampString) {
        try {
            return ZuluDateTimeFormatter.toZuluString(parseToUTC(timestampString));
        } catch (GoDateTimeException e) {
            e.printStackTrace();
            return "";
        }
    }
}
