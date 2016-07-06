/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import java.util.Date;

public class TimeReportingUtil {
    private Date begin;
    private final String key;

    public enum Key {
        CONFIG_READ_2000, CONFIG_WRITE_2000, NOT_APPLICABLE
    }

    private TimeReportingUtil(String key) {
        this.key = key;
    }

    public static void report(Key key, TestAction testAction) throws Exception {
        TimeReportingUtil t = new TimeReportingUtil(key.toString());
        t.begin();
        try {
            testAction.perform();
        } catch (Exception e) {
            throw e;
        }
    }

    public static void print(TestAction testAction) throws Exception {
        Date begin;
        Date end;
        begin = new Date();
        testAction.perform();
        end = new Date();
        System.out.println("Time Taken: " + (end.getTime() - begin.getTime()) / 1000.0 + "s");
    }

    public void begin() {
        begin = new Date();
    }

    public static interface TestAction {
        public void perform() throws Exception;
    }
}
