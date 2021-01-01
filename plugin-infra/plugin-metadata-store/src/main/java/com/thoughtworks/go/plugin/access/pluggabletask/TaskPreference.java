/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.api.config.PluginPreference;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskView;

public class TaskPreference implements PluginPreference {
    private TaskConfig taskConfig;
    private TaskView taskView;

    public TaskPreference(Task task) {
        this.taskConfig = task.config();
        this.taskView = task.view();
    }

    public TaskConfig getConfig() {
        return taskConfig;
    }

    public TaskView getView() {
        return taskView;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskPreference that = (TaskPreference) o;

        if (!taskConfig.equals(that.taskConfig)) return false;
        if (!taskView.equals(that.taskView)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = taskConfig.hashCode();
        result = 31 * result + taskView.hashCode();
        return result;
    }
}
