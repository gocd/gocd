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

import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;

class JsonBasedTaskExtension implements TaskExtensionContract {
    private PluginManager pluginManager;

    JsonBasedTaskExtension(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public ExecutionResult execute(String pluginId, ActionWithReturn<Task, ExecutionResult> actionWithReturn) {
        final PluggableJsonBasedTask task = new PluggableJsonBasedTask(pluginManager, pluginId);
        return actionWithReturn.execute(task, pluginManager.getPluginDescriptorFor(pluginId));
    }

    @Override
    public void doOnTask(String pluginId, Action<Task> action) {
        final PluggableJsonBasedTask task = new PluggableJsonBasedTask(pluginManager, pluginId);
        action.execute(task, pluginManager.getPluginDescriptorFor(pluginId));
    }
}