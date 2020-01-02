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
package com.thoughtworks.go.domain.builder.pluggableTask;

import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.JobConsoleLoggerInternal;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.util.command.*;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * This class is serialized and sent over wire to agents.
 * Please watch out what you store as fields.
 * Do not serialize PluggableTask
 */
public class PluggableTaskBuilder extends Builder implements Serializable {
    private final String workingDir;
    private String pluginId;
    private Map<String, Map<String, String>> pluginConfig;

    public PluggableTaskBuilder(RunIfConfigs conditions, Builder cancelBuilder, PluggableTask task, String description, String workingDir) {
        super(conditions, cancelBuilder, description);
        this.workingDir = workingDir;
        extractFrom(task);
    }

    private void extractFrom(PluggableTask task) {
        PluginConfiguration pluginConfiguration = task.getPluginConfiguration();
        pluginId = pluginConfiguration.getId();

        pluginConfig = task.configAsMap();
    }

    @Override
    public void build(final DefaultGoPublisher publisher,
                      final EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension, ArtifactExtension artifactExtension, PluginRequestProcessorRegistry pluginRequestProcessorRegistry, String consoleLogCharset) throws CruiseControlException {
        ExecutionResult executionResult = null;
        try {
            executionResult = taskExtension.execute(pluginId, (task, pluginDescriptor) -> executeTask(task, publisher, environmentVariableContext, consoleLogCharset));
        } catch (Exception e) {
            logException(publisher, e);
        } finally {
            JobConsoleLoggerInternal.unsetContext();
        }
        if (executionResult == null) {
            logError(publisher, "ExecutionResult cannot be null. Please return a success or a failure response.");
        }
        if (!executionResult.isSuccessful()) {
            logError(publisher, executionResult.getMessagesForDisplay());
        }
    }

    protected ExecutionResult executeTask(Task task,
                                          DefaultGoPublisher publisher,
                                          EnvironmentVariableContext environmentVariableContext, String consoleLogCharset) {
        final TaskExecutionContext taskExecutionContext = buildTaskContext(publisher, environmentVariableContext, consoleLogCharset);
        JobConsoleLoggerInternal.setContext(taskExecutionContext);

        TaskConfig config = buildTaskConfig(task.config());
        return task.executor().execute(config, taskExecutionContext);
    }

    protected TaskExecutionContext buildTaskContext(DefaultGoPublisher publisher,
                                                    EnvironmentVariableContext environmentVariableContext, String consoleLogCharset) {

        CompositeConsumer errorStreamConsumer = new CompositeConsumer(CompositeConsumer.ERR,  publisher);
        CompositeConsumer outputStreamConsumer = new CompositeConsumer(CompositeConsumer.OUT,  publisher);
        SafeOutputStreamConsumer safeOutputStreamConsumer = new SafeOutputStreamConsumer(new ProcessOutputStreamConsumer(errorStreamConsumer, outputStreamConsumer));
        safeOutputStreamConsumer.addSecrets(environmentVariableContext.secrets());
        return new PluggableTaskContext(safeOutputStreamConsumer, environmentVariableContext, workingDir, consoleLogCharset);
    }

    protected TaskConfig buildTaskConfig(TaskConfig config) {
        TaskConfig taskExecConfig = new TaskConfig();
        for (Property property : config.list()) {
            taskExecConfig.add(getExecProperty(config, property));
        }
        return taskExecConfig;
    }

    private Property getExecProperty(TaskConfig defaultConfig, Property property) {
        String key = property.getKey();
        String configValue = pluginConfig.get(key) == null ? null : pluginConfig.get(key).get(PluggableTask.VALUE_KEY);
        return StringUtils.isBlank(configValue) ? defaultConfig.get(key) : new TaskConfigProperty(key, configValue);
    }

}
