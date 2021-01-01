/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;

public class JobConsoleLoggerInternal extends JobConsoleLogger {
    private JobConsoleLoggerInternal() {
        super();
    }

    public static void setContext(TaskExecutionContext taskExecutionContext) {
        JobConsoleLogger.context = taskExecutionContext;
    }

    public static void unsetContext() {
        JobConsoleLogger.context = null;
    }
}