/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util.command;

/**
 * Created by marqueslee on 2/15/17.
 */
public interface TaggedStreamConsumer extends StreamConsumer {
    public static final String NONE = "  ";
    public static final String NOTICE = "##"; // Go information output
    public static final String ALERT = "@@"; // Go alert output
    public static final String PREP = "pr";
    public static final String TASK_START = "!!";
    public static final String OUT = "&1";
    public static final String ERR = "&2";
    public static final String TASK_PASS = "?0";
    public static final String TASK_FAIL = "?1";

    public void taggedConsumeLine(String tag, String line);
}
