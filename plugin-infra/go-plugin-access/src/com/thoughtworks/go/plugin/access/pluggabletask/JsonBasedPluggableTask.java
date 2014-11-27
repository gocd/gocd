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

package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.infra.PluginManager;

public class JsonBasedPluggableTask implements Task {
    private final JsonBasedTaskExtensionHandler extensionVersionHandler;
    private PluginManager pluginManager;
    private String pluginId;

    public JsonBasedPluggableTask(PluginManager pluginManager, String pluginId, JsonBasedTaskExtensionHandler extensionVersionHandler) {
        this.pluginManager = pluginManager;
        this.pluginId = pluginId;
        this.extensionVersionHandler = extensionVersionHandler;
    }

    @Override
    public TaskConfig config() {
        GoPluginApiResponse response = pluginManager.submitTo(pluginId, new DefaultGoPluginApiRequest(JsonBasedTaskExtension.TASK_EXTENSION, extensionVersionHandler.version(), JsonBasedTaskExtension.CONFIGURATION_REQUEST));
        return extensionVersionHandler.convertJsonToTaskConfig(response.responseBody());
    }

    @Override
    public TaskExecutor executor() {
        return new JsonBasedTaskExecutor(pluginId, pluginManager, extensionVersionHandler);
    }

    @Override
    public TaskView view() {
        GoPluginApiResponse response = pluginManager.submitTo(pluginId, new DefaultGoPluginApiRequest(JsonBasedTaskExtension.TASK_EXTENSION, extensionVersionHandler.version(), JsonBasedTaskExtension.TASK_VIEW_REQUEST));
        return extensionVersionHandler.toTaskView(response);
    }

    @Override
    public ValidationResult validate(TaskConfig configuration) {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(JsonBasedTaskExtension.TASK_EXTENSION, extensionVersionHandler.version(), JsonBasedTaskExtension.VALIDATION_REQUEST);
        request.setRequestBody(extensionVersionHandler.convertTaskConfigToJson(configuration));
        GoPluginApiResponse response = pluginManager.submitTo(pluginId, request);
        return extensionVersionHandler.toValidationResult(response);
    }
}
