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

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import com.thoughtworks.go.plugin.api.task.TaskView;

import java.util.HashMap;

public class JsonBasedPluggableTask implements Task {
    private PluginRequestHelper pluginRequestHelper;
    private HashMap<String, JsonBasedTaskExtensionHandler> handlerMap;
    private String pluginId;

    public JsonBasedPluggableTask(String pluginId, PluginRequestHelper pluginRequestHelper, HashMap<String, JsonBasedTaskExtensionHandler> handlerMap) {
        this.pluginId = pluginId;
        this.pluginRequestHelper = pluginRequestHelper;
        this.handlerMap = handlerMap;
    }

    @Override
    public TaskConfig config() {
        return pluginRequestHelper.submitRequest(pluginId, JsonBasedTaskExtension.CONFIGURATION_REQUEST, new DefaultPluginInteractionCallback<TaskConfig>() {
            @Override
            public TaskConfig onSuccess(String responseBody, String resolvedExtensionVersion) {
                return handlerMap.get(resolvedExtensionVersion).convertJsonToTaskConfig(responseBody);
            }
        });
    }

    @Override
    public TaskExecutor executor() {
        return new JsonBasedTaskExecutor(pluginId, pluginRequestHelper, handlerMap);
    }

    @Override
    public TaskView view() {
        return pluginRequestHelper.submitRequest(pluginId, JsonBasedTaskExtension.TASK_VIEW_REQUEST, new DefaultPluginInteractionCallback<TaskView>() {


            @Override
            public TaskView onSuccess(String responseBody, String resolvedExtensionVersion) {
                return handlerMap.get(resolvedExtensionVersion).toTaskView(responseBody);
            }
        });
    }

    @Override
    public ValidationResult validate(final TaskConfig configuration) {
        return pluginRequestHelper.submitRequest(pluginId, JsonBasedTaskExtension.VALIDATION_REQUEST, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return handlerMap.get(resolvedExtensionVersion).convertTaskConfigToJson(configuration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return handlerMap.get(resolvedExtensionVersion).toValidationResult(responseBody);
            }
        });
    }
}
