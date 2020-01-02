/*
 * Copyright 2020 ThoughtWorks, Inc.
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
 * Adds ability to annotate lines received by {@link StreamConsumer} so that console
 * log lines can be meaningfully parsed.
 */
public interface TaggedStreamConsumer extends StreamConsumer {
    String NOTICE = "##";
    String PREP = "pr";
    String PREP_ERR = "pe";
    String CANCEL_TASK_START = "!x";
    String CANCEL_TASK_PASS = "x0";
    String CANCEL_TASK_FAIL = "x1";
    String TASK_START = "!!";
    String OUT = "&1";
    String ERR = "&2";
    String TASK_PASS = "?0";
    String TASK_FAIL = "?1";
    String TASK_CANCELLED = "^C";
    String JOB_PASS = "j0";
    String JOB_FAIL = "j1";
    String PUBLISH = "ar";
    String PUBLISH_ERR = "ae";
    String COMPLETED = "ex";

    void taggedConsumeLine(String tag, String line);
}
