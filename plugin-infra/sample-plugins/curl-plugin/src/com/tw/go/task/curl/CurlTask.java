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

package com.tw.go.task.curl;

import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import com.thoughtworks.go.plugin.api.task.TaskView;
import org.apache.commons.io.IOUtils;

@Extension
public class CurlTask implements Task {

    public static final String URL_PROPERTY = "Url";
    public static final String ADDITIONAL_OPTIONS = "AdditionalOptions";
    public static final String SECURE_CONNECTION = "yes";
    public static final String SECURE_CONNECTION_PROPERTY = "SecureConnection";
    public static final String REQUEST_TYPE = "-G";
    public static final String REQUEST_PROPERTY = "RequestType";

    @Override
    public TaskConfig config() {
        TaskConfig config = new TaskConfig();
        config.addProperty(URL_PROPERTY);
        config.addProperty(SECURE_CONNECTION_PROPERTY).withDefault(SECURE_CONNECTION);
        config.addProperty(REQUEST_PROPERTY).withDefault(REQUEST_TYPE);
        config.addProperty(ADDITIONAL_OPTIONS);
        return config;
    }

    @Override
    public TaskExecutor executor() {
        return new CurlTaskExecutor();
    }

    @Override
    public TaskView view() {
        TaskView taskView = new TaskView() {
            @Override
            public String displayValue() {
                return "Curl";
            }

            @Override
            public String template() {
                try {
                    return IOUtils.toString(getClass().getResourceAsStream("/views/task.template.html"), "UTF-8");
                } catch (Exception e) {
                    return "Failed to find template: " + e.getMessage();
                }
            }
        };
        return taskView;
    }

    @Override
    public ValidationResult validate(TaskConfig configuration) {
        ValidationResult validationResult = new ValidationResult();
        if (configuration.getValue(URL_PROPERTY) == null || configuration.getValue(URL_PROPERTY).trim().isEmpty()) {
            validationResult.addError(new ValidationError(URL_PROPERTY, "URL cannot be empty"));
        }
        return validationResult;
    }
}
