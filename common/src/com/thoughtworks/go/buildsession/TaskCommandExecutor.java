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

import java.time.Duration;
import java.time.Instant;

import static com.thoughtworks.go.util.command.ConsoleLogTags.*;
import static java.lang.String.format;

/**
 * Executes a task, reporting its description and result
 */
public class TaskCommandExecutor implements BuildCommandExecutor {
    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {
        printTaskLine(command, buildSession);

        boolean success = benchExec(command, buildSession);

        printTaskStatusLine(command, buildSession);

        return success;
    }

    /**
     * Executes the underlying (i.e. "real") command wrapped by the task command, gathering
     * metrics such as task result, exit code, and execution duration, and propagating those
     * data up to the top-level task command.
     *
     * @param command      the top-level task command
     * @param buildSession the build session
     * @return a boolean to determine whether or not the run was successful; not always the
     * same thing as {@link JobResult}.Passed
     */
    private boolean benchExec(BuildCommand command, BuildSession buildSession) {
        // task commands only wrap a single command
        BuildCommand realCommand = command.getSubCommands().get(0);

        Instant start = Instant.now();
        boolean success = buildSession.processCommand(realCommand);
        realCommand.setDuration(Duration.between(start, Instant.now()));

        bookKeeping(command, realCommand);
        return success;
    }

    private void printTaskLine(BuildCommand command, BuildSession buildSession) {
        buildSession.printlnWithPrefix(TASK_START, format("Task: %s", command.getStringArg("description")));
    }

    private void printTaskStatusLine(BuildCommand command, BuildSession buildSession) {
        JobResult result = command.result();

        String statusLine = format("Task status: %s (%d ms)", result.toLowerCase(), command.duration().toMillis());

        if (!result.isPassed() && BuildCommand.UNSET_EXIT_CODE != command.exitCode()) {
            statusLine = format("%s (exit code: %d)", statusLine, command.exitCode());
        }

        buildSession.printlnWithPrefix(statusTag(result), statusLine);
    }

    /**
     * Determine a console log tag for a task status line based on the task result
     *
     * @param taskResult the task result
     * @return a task status tag ({@link String})
     */
    private String statusTag(JobResult taskResult) {
        if (taskResult.isCancelled()) {
            return TASK_CANCELLED;
        }

        return taskResult.isPassed() ? TASK_PASS : TASK_FAIL;
    }

    /**
     * propagate the result from the unwrapped (i.e. "real" command) to the task command
     * @param command the task command
     * @param realCommand the unwrapped command
     */
    private void bookKeeping(BuildCommand command, BuildCommand realCommand) {
        command.recordResult(realCommand.result());
        command.setDuration(realCommand.duration());
        command.setExitCode(realCommand.exitCode());
    }
}
