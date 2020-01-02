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
package com.thoughtworks.go.domain.builder;

import com.thoughtworks.go.config.RunIfConfig;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.util.command.CruiseControlException;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public abstract class Builder implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);
    public static final int UNSET_EXIT_CODE = -1;
    static final int SUCCESS_EXIT_CODE = 0;

    private int exitCode = UNSET_EXIT_CODE;

    protected final RunIfConfigs conditions;
    private String description;
    private Builder cancelBuilder;

    public Builder(RunIfConfigs conditions, Builder cancelBuilder, String description) {
        this.conditions = conditions;
        this.cancelBuilder = cancelBuilder;
        this.description = description;
    }

    public boolean allowRun(RunIfConfig previousStatus) {
        return conditions.match(previousStatus);
    }

    public abstract void build(DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension, ArtifactExtension artifactExtension, PluginRequestProcessorRegistry pluginRequestProcessorRegistry, String consoleLogCharset) throws CruiseControlException;

    public String getDescription() {
        return description;
    }

    public void setCancelBuilder(Builder cancelBuilder) {
        this.cancelBuilder = cancelBuilder;
    }

    public Builder getCancelBuilder() {
        return cancelBuilder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Builder builder = (Builder) o;

        if (cancelBuilder != null ? !cancelBuilder.equals(builder.cancelBuilder) : builder.cancelBuilder != null) {
            return false;
        }
        if (conditions != null ? !conditions.equals(builder.conditions) : builder.conditions != null) {
            return false;
        }
        if (description != null ? !description.equals(builder.description) : builder.description != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (conditions != null ? conditions.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (cancelBuilder != null ? cancelBuilder.hashCode() : 0);
        return result;
    }

    public void cancel(DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension, ArtifactExtension artifactExtension, String consoleLogCharset) {
        publisher.taggedConsumeLineWithPrefix(DefaultGoPublisher.CANCEL_TASK_START, "On Cancel Task: " + cancelBuilder.getDescription()); // odd capitalization, but consistent with UI
        try {
            cancelBuilder.build(publisher, environmentVariableContext, taskExtension, artifactExtension, null, consoleLogCharset);
            // As this message will output before the running task outputs its task status, do not use the same
            // wording (i.e. "Task status: %s") as the order of outputted lines may be confusing
            publisher.taggedConsumeLineWithPrefix(DefaultGoPublisher.CANCEL_TASK_PASS, "On Cancel Task completed");
        } catch (Exception e) {
            publisher.taggedConsumeLineWithPrefix(DefaultGoPublisher.CANCEL_TASK_FAIL, "On Cancel Task failed");
            LOGGER.error("", e);
        }
    }

    protected void logException(DefaultGoPublisher publisher, Exception e) throws CruiseControlException {
        publisher.taggedConsumeLine(DefaultGoPublisher.ERR, String.format("Error: %s", e.getMessage()));
        LOGGER.error(e.getMessage(), e);
        throw new CruiseControlException(e);
    }

    protected void logError(DefaultGoPublisher publisher, String message) throws CruiseControlException {
        publisher.taggedConsumeLine(DefaultGoPublisher.ERR, message);
        LOGGER.error(message);
        throw new CruiseControlException(message);
    }

    public RunIfConfig resolvedRunIfConfig() {
        return this.conditions.resolveToSingle();
    }

    public int getExitCode() {
        return exitCode;
    }

    protected void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
}
