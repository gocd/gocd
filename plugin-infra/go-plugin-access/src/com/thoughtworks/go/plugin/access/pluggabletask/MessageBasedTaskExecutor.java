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

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import com.thoughtworks.go.plugin.infra.PluginManager;

import java.util.List;
import java.util.Map;

public class MessageBasedTaskExecutor implements TaskExecutor {
    private String pluginId;
    private PluginManager pluginManager;

    public MessageBasedTaskExecutor(String pluginId, PluginManager pluginManager) {
        this.pluginId = pluginId;
        this.pluginManager = pluginManager;
    }

    @Override
    public ExecutionResult execute(TaskConfig config, TaskExecutionContext taskExecutionContext) {
        final DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(PluggableJsonBasedTask.TASK_EXTENSION, PluggableJsonBasedTask.VERSION_1, PluggableJsonBasedTask.EXECUTION_REQUEST);
        request.addRequestParameter("taskExecutionContext", taskExecutionContext);
        GoPluginApiResponse response = pluginManager.submitTo(pluginId, request);
        Map result = (Map) new GsonBuilder().create().fromJson(response.responseBody(), Object.class);
        if ((Boolean) result.get("success")) {
            ExecutionResult executionResult = new ExecutionResult();
            executionResult.withSuccessMessages((List<String>) result.get("messages"));
            return executionResult;
        } else {
            ExecutionResult executionResult = new ExecutionResult();
            executionResult.withErrorMessages((List<String>) result.get("messages"));
            return executionResult;
        }
    }
}
