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

package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.JobResult;

import static com.thoughtworks.go.util.command.ConsoleLogTags.JOB_FAIL;
import static com.thoughtworks.go.util.command.ConsoleLogTags.JOB_PASS;
import static java.lang.String.format;

/**
 * Reports the current job result
 */
public class JobResultCommandExecutor implements BuildCommandExecutor {
    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {
        JobResult jobResult = buildSession.getBuildResult();
        String tag = jobResult.isPassed() ? JOB_PASS : JOB_FAIL;
        buildSession.printlnWithPrefix(tag, format("Current job status: %s", jobResult.toLowerCase()));
        return true;
    }
}
