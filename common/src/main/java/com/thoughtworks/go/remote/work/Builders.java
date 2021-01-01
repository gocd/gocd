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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.config.RunIfConfig;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.NullBuilder;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class Builders {
    private List<Builder> builders = new ArrayList<>();
    private final DefaultGoPublisher goPublisher;
    private TaskExtension taskExtension;
    private final ArtifactExtension artifactExtension;
    private final PluginRequestProcessorRegistry pluginRequestProcessorRegistry;
    private Builder currentBuilder = new NullBuilder();
    private transient boolean cancelStarted;
    private transient boolean cancelFinished;

    public Builders(List<Builder> builders, DefaultGoPublisher goPublisher, TaskExtension taskExtension, ArtifactExtension artifactExtension, PluginRequestProcessorRegistry pluginRequestProcessorRegistry) {
        this.builders = builders;
        this.goPublisher = goPublisher;
        this.taskExtension = taskExtension;
        this.artifactExtension = artifactExtension;
        this.pluginRequestProcessorRegistry = pluginRequestProcessorRegistry;
    }

    public JobResult build(EnvironmentVariableContext environmentVariableContext, String consoleLogCharset) {
        JobResult result = JobResult.Passed;

        for (Builder builder : builders) {
            if (cancelStarted) {
                return JobResult.Cancelled;
            }

            synchronized (this) {
                currentBuilder = builder;
            }

            if (builder.allowRun(RunIfConfig.fromJobResult(result.toLowerCase()))) {
                JobResult taskStatus = JobResult.Passed;
                Instant start = Instant.now();

                try {
                    String executeMessage = format("Task: %s", builder.getDescription());
                    goPublisher.taggedConsumeLineWithPrefix(DefaultGoPublisher.TASK_START, executeMessage);

                    builder.build(goPublisher, environmentVariableContext, taskExtension, artifactExtension, pluginRequestProcessorRegistry, consoleLogCharset);
                } catch (Exception e) {
                    result = taskStatus = JobResult.Failed;
                }

                Duration duration = Duration.between(start, Instant.now());
                String statusLine = format("Task status: %s (%d ms)", taskStatus.toLowerCase(), duration.toMillis());

                if (cancelStarted) {
                    result = taskStatus = JobResult.Cancelled;
                }

                String tag;

                if (taskStatus.isPassed()) {
                    tag = DefaultGoPublisher.TASK_PASS;
                } else {
                    if (Builder.UNSET_EXIT_CODE != builder.getExitCode()) {
                        statusLine = format("%s (exit code: %d)", statusLine, builder.getExitCode());
                    }

                    tag = taskStatus.isCancelled() ? DefaultGoPublisher.TASK_CANCELLED : DefaultGoPublisher.TASK_FAIL;
                }

                goPublisher.taggedConsumeLineWithPrefix(tag, statusLine);
            }

        }

        synchronized (this) {
            currentBuilder = new NullBuilder();
        }

        if (cancelStarted) {
            return JobResult.Cancelled;
        }
        return result;
    }

    public void cancel(EnvironmentVariableContext environmentVariableContext, String consoleLogCharset) {
        cancelStarted = true;
        synchronized (this) {
            currentBuilder.cancel(goPublisher, environmentVariableContext, taskExtension, artifactExtension, consoleLogCharset);
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

    @Override
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

    @Override
    public int hashCode() {
        int result;
        result = (builders != null ? builders.hashCode() : 0);
        result = 31 * result + (goPublisher != null ? goPublisher.hashCode() : 0);
        result = 31 * result + (currentBuilder != null ? currentBuilder.hashCode() : 0);
        result = 31 * result + (cancelStarted ? 1 : 0);
        return result;
    }
}
