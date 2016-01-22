/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
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
public class TaskExtension implements TaskExtensionContract, GoPluginExtension {

    private final String API_BASED = "API_BASED";
    private final String MESSAGE_BASED = "MESSAGE_BASED";
    private final HashMap<String, TaskExtensionContract> map;
    private PluginManager pluginManager;

    @Autowired
    public TaskExtension(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        map = new HashMap<>();
        map.put(API_BASED, new ApiBasedTaskExtension(pluginManager));
        map.put(MESSAGE_BASED, new JsonBasedTaskExtension(pluginManager));
    }

    TaskExtensionContract getExtension(String pluginId) {
        if (pluginManager.getPluginDescriptorFor(pluginId) == null) {
            throw new RuntimeException(String.format("Associated plugin '%s' not found. Please contact the Go admin to install the plugin.", pluginId));
        }
        return getTaskExtensionContract(pluginId);
    }

    TaskExtensionContract getTaskExtensionContract(String pluginId) {
        TaskExtensionContract extension = null;
        if (pluginManager.hasReferenceFor(Task.class, pluginId)) {
            extension = map.get(API_BASED);
        } else if (pluginManager.isPluginOfType(JsonBasedTaskExtension.TASK_EXTENSION, pluginId)) {
            extension = map.get(MESSAGE_BASED);
        }
        if (extension != null) return extension;
        throw new RuntimeException(String.format("Plugin should use either message-based or api-based extension. Plugin-id: %s", pluginId));
    }

    @Override
    public PluginSettingsConfiguration getPluginSettingsConfiguration(String pluginId) {
        return getExtension(pluginId).getPluginSettingsConfiguration(pluginId);
    }

    @Override
    public String getPluginSettingsView(String pluginId) {
        return getExtension(pluginId).getPluginSettingsView(pluginId);
    }

    @Override
    public ValidationResult validatePluginSettings(String pluginId, PluginSettingsConfiguration configuration) {
        return getExtension(pluginId).validatePluginSettings(pluginId, configuration);
    }

    public ExecutionResult execute(String pluginId, ActionWithReturn<Task, ExecutionResult> actionWithReturn) {
        return getExtension(pluginId).execute(pluginId, actionWithReturn);
    }

    @Override
    public void doOnTask(String pluginId, Action<Task> action) {
        getTaskExtensionContract(pluginId).doOnTask(pluginId, action);
    }

    @Override
    public ValidationResult validate(String pluginId, TaskConfig taskConfig) {
        return getExtension(pluginId).validate(pluginId, taskConfig);
    }


    @Override
    public boolean canHandlePlugin(String pluginId) {
        return pluginManager.hasReferenceFor(Task.class, pluginId) || pluginManager.isPluginOfType(JsonBasedTaskExtension.TASK_EXTENSION, pluginId);
    }
}
