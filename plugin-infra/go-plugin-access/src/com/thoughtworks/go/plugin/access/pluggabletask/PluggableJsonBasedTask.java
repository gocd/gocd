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
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.infra.PluginManager;
import java.util.List;
import java.util.Map;

public class PluggableJsonBasedTask implements Task {
    public final static String TASK_EXTENSION = "task-plugin";
    public final static String VERSION_1 = "1.0";
    public final static String CONFIGURATION_REQUEST = "configuration";
    public final static String VALIDATION_REQUEST = "validate";
    public final static String EXECUTION_REQUEST = "execute";
    public final static String TASK_VIEW_REQUEST = "view";
    private PluginManager pluginManager;
    private String pluginId;

    public PluggableJsonBasedTask(PluginManager pluginManager, String pluginId) {
        this.pluginManager = pluginManager;
        this.pluginId = pluginId;
    }

    @Override
    public TaskConfig config() {
        GoPluginApiResponse response = pluginManager.submitTo(pluginId, new DefaultGoPluginApiRequest(TASK_EXTENSION, VERSION_1, CONFIGURATION_REQUEST));
        return new TaskConfigForApi_Version1().fromJson(response.responseBody());
    }

    @Override
    public TaskExecutor executor() {
        return new MessageBasedTaskExecutor(pluginId, pluginManager);
    }

    @Override
    public TaskView view() {
        final GoPluginApiResponse response = pluginManager.submitTo(pluginId, new DefaultGoPluginApiRequest(TASK_EXTENSION, VERSION_1, TASK_VIEW_REQUEST));
        final Map map = (Map) new GsonBuilder().create().fromJson(response.responseBody(), Object.class);
        return new TaskView() {
            @Override
            public String displayValue() {
                return (String) map.get("displayValue");
            }

            @Override
            public String template() {
                return (String) map.get("template");
            }
        };
    }

    @Override
    public ValidationResult validate(TaskConfig configuration) {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(TASK_EXTENSION, VERSION_1, VALIDATION_REQUEST);
        request.addRequestParameter("config", new TaskConfigForApi_Version1().toJson(configuration));
        ValidationResult validationResult = new ValidationResult();
        GoPluginApiResponse response = pluginManager.submitTo(pluginId, request);
        Map result = (Map) new GsonBuilder().create().fromJson(response.responseBody(), Object.class);
        List<Map> errors = (List<Map>) result.get("errors");
        for (Map error : errors) {
            validationResult.addError(new ValidationError((String) error.get("key"), (String) error.get("message")));
        }
        return validationResult;
    }
}
