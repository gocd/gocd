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

package com.thoughtworks.studios.shine.time;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class Clock {

  private static DateTime fakeNow;

  public static void fakeNowUTC(int year, int month, int dayOfMonth, int hour, int minute, int second) {
    fakeNow = new DateTime(year, month, dayOfMonth, hour, minute, second, 0, DateTimeZone.UTC);
  }


  public static DateTime nowUTC() {
    if (fakeNow != null) {
      return fakeNow;
    } else {
      return new DateTime(DateTimeZone.UTC);
    }
  }

  public static void reset() {
    fakeNow = null;
  }
}