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

import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

@Deprecated
class ApiBasedTaskExtension implements TaskExtensionContract {
    private PluginManager pluginManager;

    ApiBasedTaskExtension(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public PluginSettingsConfiguration getPluginSettingsConfiguration(String pluginId) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public String getPluginSettingsView(String pluginId) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ValidationResult validatePluginSettings(String pluginId, PluginSettingsConfiguration configuration) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ExecutionResult execute(String pluginId, ActionWithReturn<Task, ExecutionResult> actionWithReturn) {
        return pluginManager.doOn(Task.class, pluginId, actionWithReturn);
    }

    @Override
    public void doOnTask(String pluginId, Action<Task> action) {
        pluginManager.doOn(Task.class, pluginId, action);
    }

    @Override
    public ValidationResult validate(String pluginId, final TaskConfig taskConfig) {
        return (ValidationResult) pluginManager.doOn(Task.class, pluginId, new ActionWithReturn<Task, Object>() {
            @Override
            public Object execute(Task task, GoPluginDescriptor pluginDescriptor) {
                return task.validate(taskConfig);
            }
        });
    }
}