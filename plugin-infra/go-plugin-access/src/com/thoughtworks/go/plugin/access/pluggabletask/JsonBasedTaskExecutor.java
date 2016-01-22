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
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;

import java.util.HashMap;

public class JsonBasedTaskExecutor implements TaskExecutor {
    private String pluginId;
    private PluginRequestHelper pluginRequestHelper;
    private HashMap<String, JsonBasedTaskExtensionHandler> handlerMap;

    public JsonBasedTaskExecutor(String pluginId, PluginRequestHelper pluginRequestHelper, HashMap<String, JsonBasedTaskExtensionHandler> handlerMap) {
        this.pluginId = pluginId;
        this.pluginRequestHelper = pluginRequestHelper;
        this.handlerMap = handlerMap;
    }

    @Override
    public ExecutionResult execute(final TaskConfig config, final TaskExecutionContext taskExecutionContext) {
        return pluginRequestHelper.submitRequest(pluginId, JsonBasedTaskExtension.EXECUTION_REQUEST, new DefaultPluginInteractionCallback<ExecutionResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return handlerMap.get(resolvedExtensionVersion).getTaskExecutionBody(config, taskExecutionContext);
            }

            @Override
            public ExecutionResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return handlerMap.get(resolvedExtensionVersion).toExecutionResult(responseBody);
            }
        });
    }
}

