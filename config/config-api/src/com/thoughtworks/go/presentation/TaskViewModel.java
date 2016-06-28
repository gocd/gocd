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

package com.thoughtworks.go.presentation;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.plugins.presentation.PluggableViewModel;

/**
 * @understands information required to display a task
 */
public class TaskViewModel implements PluggableViewModel {
    private final String renderingFramework;
    private String templatePath;
    private Task task;

    public TaskViewModel(Task task, String viewTemplate, String renderingFramework) {
        this.task = task;
        this.renderingFramework = renderingFramework;
        this.templatePath = viewTemplate;
    }

    public String getRenderingFramework() {
        return renderingFramework;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public Map<String, Object> getParameters() {
        return new HashMap<>();
    }

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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TaskViewModel that = (TaskViewModel) o;

        if (renderingFramework != null ? !renderingFramework.equals(that.renderingFramework) : that.renderingFramework != null) {
            return false;
        }
        if (task != null ? !task.equals(that.task) : that.task != null) {
            return false;
        }
        if (templatePath != null ? !templatePath.equals(that.templatePath) : that.templatePath != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = renderingFramework != null ? renderingFramework.hashCode() : 0;
        result = 31 * result + (templatePath != null ? templatePath.hashCode() : 0);
        result = 31 * result + (task != null ? task.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "TaskViewModel{" +
                ", renderingFramework='" + renderingFramework + '\'' +
                ", templatePath='" + templatePath + '\'' +
                ", task=" + task +
                '}';
    }
}
