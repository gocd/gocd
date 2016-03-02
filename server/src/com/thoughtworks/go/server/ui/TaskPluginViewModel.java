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

package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.api.task.TaskConfig;

public class TaskPluginViewModel extends PluginViewModel {
    private TaskConfig taskConfig;
    public static final String TYPE = "task";

    public TaskPluginViewModel() {
    }

    public TaskPluginViewModel(String pluginId, String version, String message) {
        super(pluginId, version, message);
    }

    public TaskPluginViewModel(String pluginId, String version, String message, TaskConfig taskConfig) {
        super(pluginId, version, message);
        this.taskConfig = taskConfig;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public TaskConfig getConfigurations() {
        if (taskConfig == null) {
            this.taskConfig = PluggableTaskConfigStore.store().getMetaData(getPluginId());
        }
        return taskConfig;
    }

    public Boolean hasPlugin(String pluginId){
        return PluggableTaskConfigStore.store().hasPlugin(pluginId);
    }
}
