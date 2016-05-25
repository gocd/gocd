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

import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.settings.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonBasedTaskExtension extends AbstractExtension implements TaskExtensionContract {
    public final static String TASK_EXTENSION = "task";
    private final static List<String> supportedVersions = Arrays.asList(JsonBasedTaskExtensionHandler_V1.VERSION);

    public final static String CONFIGURATION_REQUEST = "configuration";
    public final static String VALIDATION_REQUEST = "validate";
    public final static String EXECUTION_REQUEST = "execute";
    public final static String TASK_VIEW_REQUEST = "view";

    private final HashMap<String, JsonBasedTaskExtensionHandler> handlerHashMap = new HashMap<>();

    JsonBasedTaskExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, supportedVersions, TASK_EXTENSION), TASK_EXTENSION);
        pluginSettingsMessageHandlerMap.put(JsonBasedTaskExtensionHandler_V1.VERSION, new PluginSettingsJsonMessageHandler1_0());
        handlerHashMap.put(JsonBasedTaskExtensionHandler_V1.VERSION, new JsonBasedTaskExtensionHandler_V1());
    }

    @Override
    public ExecutionResult execute(String pluginId, ActionWithReturn<Task, ExecutionResult> actionWithReturn) {
        JsonBasedPluggableTask task = new JsonBasedPluggableTask(pluginId, pluginRequestHelper, handlerHashMap);
        return actionWithReturn.execute(task, pluginManager.getPluginDescriptorFor(pluginId));
    }

    @Override
    public void doOnTask(String pluginId, Action<Task> action) {
        JsonBasedPluggableTask task = new JsonBasedPluggableTask(pluginId, pluginRequestHelper, handlerHashMap);
        action.execute(task, pluginManager.getPluginDescriptorFor(pluginId));
    }

    @Override
    public ValidationResult validate(String pluginId, TaskConfig taskConfig) {
        JsonBasedPluggableTask task = new JsonBasedPluggableTask(pluginId, pluginRequestHelper, handlerHashMap);
        return task.validate(taskConfig);
    }

    Map<String, PluginSettingsJsonMessageHandler> getPluginSettingsMessageHandlerMap() {
        return pluginSettingsMessageHandlerMap;
    }

    Map<String, JsonBasedTaskExtensionHandler> getMessageHandlerMap() {
        return handlerHashMap;
    }
}
