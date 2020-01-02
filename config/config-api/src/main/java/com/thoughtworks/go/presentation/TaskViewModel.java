/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.presentation;

import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.plugins.presentation.PluggableViewModel;

import java.util.HashMap;
import java.util.Map;

/**
 * @understands information required to display a task
 */
public class TaskViewModel implements PluggableViewModel {
    private String templatePath;
    private Task task;

    public TaskViewModel(Task task, String viewTemplate) {
        this.task = task;
        this.templatePath = viewTemplate;
    }

    @Override
    public String getTemplatePath() {
        return templatePath;
    }

    @Override
    public Map<String, Object> getParameters() {
        return new HashMap<>();
    }

    @Override
    public Object getModel() {
        return task;
    }

    @Override
    public void setModel(Object model) {
        this.task = (Task) model;
    }

    @Override
    public String getTypeForDisplay() {
        return task.getTypeForDisplay();
    }

    @Override
    public String getTaskType() {
        return task.getTaskType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskViewModel that = (TaskViewModel) o;

        if (templatePath != null ? !templatePath.equals(that.templatePath) : that.templatePath != null) return false;
        return task != null ? task.equals(that.task) : that.task == null;
    }

    @Override
    public int hashCode() {
        int result = templatePath != null ? templatePath.hashCode() : 0;
        result = 31 * result + (task != null ? task.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "TaskViewModel{" +
                ", templatePath='" + templatePath + '\'' +
                ", task=" + task +
                '}';
    }
}
