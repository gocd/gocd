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

package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.config.RunIfConfig;
import com.thoughtworks.go.domain.BuildLogElement;
import com.thoughtworks.go.domain.GoControlLog;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.NullBuilder;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.util.command.ConsoleLogTags.*;
import static java.lang.String.format;

public class Builders {
    private List<Builder> builders = new ArrayList<>();
    private final DefaultGoPublisher goPublisher;
    private final GoControlLog buildLog;
    private TaskExtension taskExtension;
    private Builder currentBuilder = new NullBuilder();
    private transient boolean cancelStarted;
    private transient boolean cancelFinished;

    public Builders(List<Builder> builders, DefaultGoPublisher goPublisher,
                    GoControlLog buildLog, TaskExtension taskExtension) {
        this.builders = builders;
        this.goPublisher = goPublisher;
        this.buildLog = buildLog;
        this.taskExtension = taskExtension;
    }

    public JobResult build(EnvironmentVariableContext environmentVariableContext) {
        JobResult jobResult = JobResult.Passed;

        for (Builder builder : builders) {
            if (cancelStarted) {
                return builder.recordResult(JobResult.Cancelled);
            }

            synchronized (this) {
                currentBuilder = builder;
            }

            BuildLogElement buildLogElement = new BuildLogElement();

            if (builder.allowRun(RunIfConfig.fromJobResult(jobResult.toLowerCase()))) {
                printTaskLine(builder);

                JobResult taskResult = benchExec(builder, environmentVariableContext, buildLogElement);

                if (!taskResult.isPassed()) {
                    jobResult = taskResult;
                }

                if (cancelStarted) {
                    jobResult = builder.recordResult(JobResult.Cancelled);
                }

                printTaskStatusLine(builder);
            }

            buildLog.addContent(buildLogElement.getElement());
        }

        synchronized (this) {
            currentBuilder = new NullBuilder();
        }

        if (cancelStarted) {
            return JobResult.Cancelled;
        }
        return jobResult;
    }

    /**
     * Executes the task, gathering metrics such as task result, exit code, and execution duration
     *
     * @param builder                    the task builder instance
     * @param environmentVariableContext a set of environment variables for execution
     * @param buildLogElement            the build log element
     * @return the task result
     */
    private JobResult benchExec(Builder builder, EnvironmentVariableContext environmentVariableContext, BuildLogElement buildLogElement) {
        builder.recordResult(JobResult.Passed);

        Instant start = Instant.now();

        try {
            builder.build(buildLogElement, goPublisher, environmentVariableContext, taskExtension);
        } catch (Exception e) {
            builder.recordResult(JobResult.Failed);
        }

        builder.setDuration(Duration.between(start, Instant.now()));

        return builder.result();
    }

    private void printTaskLine(Builder builder) {
        String executeMessage = format("Task: %s", builder.getDescription());
        goPublisher.taggedConsumeLineWithPrefix(TASK_START, executeMessage);
    }

    private void printTaskStatusLine(Builder builder) {
        JobResult taskResult = builder.result();

        String statusLine = format("Task status: %s (%d ms)", taskResult.toLowerCase(), builder.duration().toMillis());

        if (!taskResult.isPassed() && Builder.UNSET_EXIT_CODE != builder.exitCode()) {
            statusLine = format("%s (exit code: %d)", statusLine, builder.exitCode());
        }

        goPublisher.taggedConsumeLineWithPrefix(statusTag(taskResult), statusLine);
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

    public void cancel(EnvironmentVariableContext environmentVariableContext) {
        cancelStarted = true;
        synchronized (this) {
            currentBuilder.cancel(goPublisher, environmentVariableContext, taskExtension);
            cancelFinished = true;
        }
    }

    public void waitForCancelTasks() {
        if (cancelStarted) {
            while (!cancelFinished) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    //Used only for tests
    void setIsCancelled(boolean isCancelled) {
        this.cancelStarted = isCancelled;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Builders builders1 = (Builders) o;

        if (cancelStarted != builders1.cancelStarted) {
            return false;
        }

        if (builders != null ? !builders.equals(builders1.builders) : builders1.builders != null) {
            return false;
        }

        if (currentBuilder != null ? !currentBuilder.equals(
                builders1.currentBuilder) : builders1.currentBuilder != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (builders != null ? builders.hashCode() : 0);
        result = 31 * result + (goPublisher != null ? goPublisher.hashCode() : 0);
        result = 31 * result + (buildLog != null ? buildLog.hashCode() : 0);
        result = 31 * result + (currentBuilder != null ? currentBuilder.hashCode() : 0);
        result = 31 * result + (cancelStarted ? 1 : 0);
        return result;
    }
}
