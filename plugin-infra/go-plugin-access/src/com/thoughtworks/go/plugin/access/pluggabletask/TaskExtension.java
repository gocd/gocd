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

package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class TaskExtension extends AbstractExtension {

    final HashMap<String, TaskMessageConverter> messageHandlerMap = new HashMap<>();

    @Autowired
    public TaskExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, TaskExtensionConstants.SUPPORTED_VERSIONS, TaskExtensionConstants.TASK_EXTENSION), TaskExtensionConstants.TASK_EXTENSION);
        addHandler(TaskMessageConverter_V1.VERSION, new PluginSettingsJsonMessageHandler1_0(), new TaskMessageConverter_V1());
    }

    private void addHandler(String version, PluginSettingsJsonMessageHandler handler, TaskMessageConverter value) {
        registerHandler(version, handler);
        messageHandlerMap.put(TaskMessageConverter_V1.VERSION, value);
    }

    public ExecutionResult execute(String pluginId, ActionWithReturn<Task, ExecutionResult> actionWithReturn) {
        JsonBasedPluggableTask task = new JsonBasedPluggableTask(pluginId, pluginRequestHelper, messageHandlerMap);
        return actionWithReturn.execute(task, pluginManager.getPluginDescriptorFor(pluginId));
    }

    public void doOnTask(String pluginId, Action<Task> action) {
        JsonBasedPluggableTask task = new JsonBasedPluggableTask(pluginId, pluginRequestHelper, messageHandlerMap);
        action.execute(task, pluginManager.getPluginDescriptorFor(pluginId));
    }

    public ValidationResult validate(String pluginId, TaskConfig taskConfig) {
        JsonBasedPluggableTask task = new JsonBasedPluggableTask(pluginId, pluginRequestHelper, messageHandlerMap);
        return task.validate(taskConfig);
    }

}
