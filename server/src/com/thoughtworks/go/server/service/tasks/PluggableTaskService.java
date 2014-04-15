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

package com.thoughtworks.go.server.service.tasks;

import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PluggableTaskService {

    private PluginManager pluginManager;

    @Autowired
    public PluggableTaskService(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void validate(final PluggableTask modifiedTask) {
        ValidationResult validationResult = (ValidationResult) pluginManager.doOn(Task.class, modifiedTask.getPluginConfiguration().getId(), new ActionWithReturn<Task, Object>() {
            @Override
            public Object execute(Task task, GoPluginDescriptor pluginDescriptor) {

                TaskConfig configuration = new TaskConfig();
                for (ConfigurationProperty configurationProperty : modifiedTask.getConfiguration()) {
                    configuration.add(new TaskConfigProperty(configurationProperty.getConfigurationKey().getName(), configurationProperty.getValue()));
                }
                return task.validate(configuration);
            }
        });
        for (ValidationError validationError : validationResult.getErrors()) {
            modifiedTask.getConfiguration().getProperty(validationError.getKey()).addError(validationError.getKey(), validationError.getMessage());
        }
    }
}
