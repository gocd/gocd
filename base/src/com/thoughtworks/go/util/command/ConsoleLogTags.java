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

import com.thoughtworks.go.util.Pair;

/**
 * The authoritative list of console log metadata tags/annotations. One should be
 * careful to not change the values of existing tags, as these are now public/released.
 * Adding new tags is fine though, but once it's put into a release, the value should
 * never change and it's not feasible (or at least easy) to migrate console log files.
 */
public class ConsoleLogTags {

    public static final String NOTICE = "##";
    public static final String PREP = "pr";
    public static final String PREP_ERR = "pe";
    public static final String CANCEL_TASK_START = "!x";
    public static final String CANCEL_TASK_PASS = "x0";
    public static final String CANCEL_TASK_FAIL = "x1";
    public static final String TASK_START = "!!";
    public static final String OUT = "&1";
    public static final String ERR = "&2";
    public static final String TASK_PASS = "?0";
    public static final String TASK_FAIL = "?1";
    public static final String TASK_CANCELLED = "^C";
    public static final String JOB_PASS = "j0";
    public static final String JOB_FAIL = "j1";
    public static final String PUBLISH = "ar";
    public static final String PUBLISH_ERR = "ae";
    public static final String COMPLETED = "ex";

    // These tag pairs can be used to remap stdout/stderr to new values
    public static final Pair<String, String> STD_TAGS = tagpair(OUT, ERR);
    public static final Pair<String, String> PREP_TAGS = tagpair(PREP, PREP_ERR);

    private ConsoleLogTags() {
    }

    public static Pair<String, String> tagpair(String stdout, String stderr) {
        if (null == stdout && null == stderr) {
            return null;
        }

        return new Pair<>(stdout, stderr);
    }
}
