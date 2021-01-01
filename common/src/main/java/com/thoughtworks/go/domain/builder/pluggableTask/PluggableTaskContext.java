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
package com.thoughtworks.go.domain.builder.pluggableTask;

import com.thoughtworks.go.plugin.api.task.Console;
import com.thoughtworks.go.plugin.api.task.EnvironmentVariables;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.SafeOutputStreamConsumer;

public class PluggableTaskContext implements TaskExecutionContext {
    private final SafeOutputStreamConsumer publisher;
    private final EnvironmentVariableContext environmentVariableContext;
    private final String workingDir;
    private final String consoleLogCharset;

    public PluggableTaskContext(SafeOutputStreamConsumer safeOutputStreamConsumer,
                                EnvironmentVariableContext environmentVariableContext, String workingDir, String consoleLogCharset) {
        this.publisher = safeOutputStreamConsumer;
        this.environmentVariableContext = environmentVariableContext;
        this.workingDir = workingDir;
        this.consoleLogCharset = consoleLogCharset;
    }

    @Override
    public EnvironmentVariables environment() {
        return new PluggableTaskEnvVars(environmentVariableContext);
    }

    @Override
    public Console console() {
        return new PluggableTaskConsole(publisher, consoleLogCharset);
    }

    @Override
    public String workingDir() {
        return workingDir;
    }
}
