/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.builder;

import java.io.Serializable;
import static java.lang.String.format;

import com.thoughtworks.go.config.RunIfConfig;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.BuildLogElement;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.work.DefaultGoPublisher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.CruiseControlException;
import org.apache.log4j.Logger;

public abstract class Builder implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(Builder.class);
    protected final RunIfConfigs conditions;
    private String description;
    private Builder cancelBuilder;

    public Builder(RunIfConfigs conditions, Builder cancelBuilder, String description) {
        this.conditions = conditions;
        this.cancelBuilder = cancelBuilder;
        this.description = description;
    }

    public void build(BuildLogElement buildLogElement, RunIfConfig currentStatus, DefaultGoPublisher publisher,
                      EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension)
            throws CruiseControlException {
        if (conditions.match(currentStatus)) {
            String statusMessage = format("Current job status: %s.\n", currentStatus);
            String executeMessage = format("Start to execute task: %s.", getDescription());
            publisher.consumeLineWithPrefix(statusMessage);
            publisher.consumeLineWithPrefix(executeMessage);
            build(buildLogElement, publisher, environmentVariableContext, taskExtension);
        }
    }

    public abstract void build(BuildLogElement buildLogElement, DefaultGoPublisher publisher,
                               EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension)
            throws CruiseControlException;

    public String getDescription() {
        return description;
    }

    public void setCancelBuilder(Builder cancelBuilder) {
        this.cancelBuilder = cancelBuilder;
    }

    public Builder getCancelBuilder() {
        return cancelBuilder;
    }

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

    public int hashCode() {
        int result;
        result = (conditions != null ? conditions.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (cancelBuilder != null ? cancelBuilder.hashCode() : 0);
        return result;
    }

    public void cancel(DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension) {
        publisher.consumeLineWithPrefix("Start to execute cancel task: " + cancelBuilder.getDescription());
        try {
            cancelBuilder.build(new BuildLogElement(), publisher, environmentVariableContext, taskExtension);
            publisher.consumeLineWithPrefix("Task is cancelled");
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    protected void logException(DefaultGoPublisher publisher, Exception e) throws CruiseControlException {
        publisher.consumeLine(String.format("Error: %s", e.getMessage()));
        LOGGER.error(e.getMessage(), e);
        throw new CruiseControlException(e);
    }

    protected void logError(DefaultGoPublisher publisher, String message) throws CruiseControlException {
        publisher.consumeLine(message);
        LOGGER.error(message);
        throw new CruiseControlException(message);
    }

    public BuildCommand buildCommand() {
        return BuildCommand.fail(format("\"%s\" does not support new build command agent", this.getClass().getName()));
    }

    public RunIfConfig resolvedRunIfConfig() {
        return this.conditions.resolveToSingle();
    }
}
