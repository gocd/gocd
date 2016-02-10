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

public class TaskPluginViewModel implements PluginViewModel {
    private String pluginId;
    private String version;
    private String message;
    private TaskConfig taskConfig;
    public static final String TYPE = "task";

    public TaskPluginViewModel() {
    }

    public TaskPluginViewModel(String pluginId, String version, TaskConfig taskConfig) {
        this.pluginId = pluginId;
        this.version = version;
        this.taskConfig = taskConfig;
    }

    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public TaskConfig getConfigurations() {
        return taskConfig;
    }

    @Override
    public void setViewModel(String id, String version, String message) {
        this.pluginId = id;
        this.version = version;
        this.taskConfig = PluggableTaskConfigStore.store().getMetaData(id);
        this.message = message;
    }
}
