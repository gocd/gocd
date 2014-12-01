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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonBasedTaskExtensionHandler_V1 implements JsonBasedTaskExtensionHandler {
    public static final String VERSION = "1.0";

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public String convertTaskConfigToJson(TaskConfig taskConfig) {
        return new Gson().toJson(configPropertiesAsMap(taskConfig));
    }

    @Override
    public TaskConfig convertJsonToTaskConfig(String configJson) {
        Map<String, Object> configMap = (Map) new GsonBuilder().create().fromJson(configJson, Object.class);
        TaskConfig taskConfig = new TaskConfig();
        for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            TaskConfigProperty property = new TaskConfigProperty(entry.getKey(), null);
            Map propertyValue = (Map) entry.getValue();
            if (propertyValue != null) {
                property.withDefault((String) propertyValue.get("default-value"));
                property.with(Property.SECURE, (Boolean) propertyValue.get("secure"));
                property.with(Property.REQUIRED, (Boolean) propertyValue.get("required"));
                property.with(Property.DISPLAY_NAME, (String) propertyValue.get("display-name"));
                if (!StringUtil.isBlank((String) propertyValue.get("display-order"))) {
                    property.with(Property.DISPLAY_ORDER, Integer.parseInt((String) propertyValue.get("display-order")));
                }
            }
            taskConfig.add(property);
        }
        return taskConfig;
    }

    @Override
    public ValidationResult toValidationResult(GoPluginApiResponse response) {
        ValidationResult validationResult = new ValidationResult();
        Map result = (Map) new GsonBuilder().create().fromJson(response.responseBody(), Object.class);
        final Map<String, String> errors = (Map<String, String>) result.get("errors");
        if (errors != null) {
            for (Map.Entry<String, String> entry : errors.entrySet()) {
                validationResult.addError(new ValidationError(entry.getKey(), entry.getValue()));
            }
        }
        return validationResult;
    }

    @Override
    public TaskView toTaskView(GoPluginApiResponse response) {
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
    public ExecutionResult toExecutionResult(GoPluginApiResponse response) {
        Map result = (Map) new GsonBuilder().create().fromJson(response.responseBody(), Object.class);
        if ((Boolean) result.get("success")) {
            ExecutionResult executionResult = new ExecutionResult();
            executionResult.withSuccessMessages((String) result.get("message"));
            return executionResult;
        } else {
            ExecutionResult executionResult = new ExecutionResult();
            executionResult.withErrorMessages((String) result.get("message"));
            return executionResult;
        }
    }

    @Override
    public String getTaskExecutionBody(TaskConfig config, TaskExecutionContext taskExecutionContext) {
        Map requestBody = new HashMap();
        Map contextMap = new HashMap();
        contextMap.put("environmentVariables", taskExecutionContext.environment().asMap());
        contextMap.put("workingDirectory", taskExecutionContext.workingDir());
        requestBody.put("context", contextMap);
        requestBody.put("config", configPropertiesAsMap(config));
        return new Gson().toJson(requestBody);

    }

    private Map configPropertiesAsMap(TaskConfig taskConfig) {
        HashMap properties = new HashMap();
        for (Property property : taskConfig.list()) {
            final HashMap propertyValue = new HashMap();
            propertyValue.put("value", property.getValue());
            propertyValue.put("secure", property.getOption(Property.SECURE));
            propertyValue.put("required", property.getOption(Property.REQUIRED));
            propertyValue.put("display-name", property.getOption(Property.DISPLAY_NAME));
            propertyValue.put("display-order", property.getOption(Property.DISPLAY_ORDER).toString());
            properties.put(property.getKey(), propertyValue);
        }
        return properties;
    }
}