/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import java.util.Map;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.KillAllChildProcessTask;
import com.thoughtworks.go.domain.NullTask;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.service.TaskFactory;

@ConfigTag(value = "oncancel", label = "OnCancel")
public class OnCancelConfig implements Validatable {

    public static final String EXEC_ON_CANCEL = "execOnCancel";
    public static final String ANT_ON_CANCEL = "antOnCancel";
    public static final String RAKE_ON_CANCEL = "rakeOnCancel";
    public static final String NANT_ON_CANCEL = "nantOnCancel";

    @ConfigSubtag(optional = true)
    private Task task;

    private final boolean shouldKillAll;
    public static final String ON_CANCEL_OPTIONS = "onCancelOption";
    private ConfigErrors configErrors = new ConfigErrors();

    public static OnCancelConfig killAllChildProcess() {
        return new OnCancelConfig(true);
    }

    private OnCancelConfig(boolean shouldKillAll) {
        this.shouldKillAll = shouldKillAll;
    }

    public OnCancelConfig() {
        this(false);
    }

    public OnCancelConfig(Task task) {
        this(false);
        this.task = task;
    }

    public Task getTask() {
        if (shouldKillAll) {
            return new KillAllChildProcessTask();
        }
        return task == null ? new NullTask() : task;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OnCancelConfig that = (OnCancelConfig) o;

        if (task != null ? !task.equals(that.task) : that.task != null) {
            return false;
        }

        if (this.shouldKillAll != that.shouldKillAll) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = task != null ? task.hashCode() : 0;
        result = 31 * result + (shouldKillAll ? 1 : 0);
        return result;
    }

    public boolean hasCancelTask() {
        return task != null;
    }

    private void setConfigAttributes(Object attributes, TaskFactory taskFactory) {
        Map attributeMap = (Map) attributes;
        Task task = taskFactory.taskInstanceFor((String) attributeMap.get(ON_CANCEL_OPTIONS));
        task.setConfigAttributes(attributeMap.get(task.getTaskType() + "OnCancel"));
        this.task = task;
    }

    public static OnCancelConfig create(Map attributeMap, TaskFactory taskFactory) {
        OnCancelConfig config = new OnCancelConfig(false);
        config.setConfigAttributes(attributeMap, taskFactory);
        return config;
    }

    public String onCancelOption() {
        return hasCancelTask() ? getTask().getTypeForDisplay() : "";
    }

    public boolean isExecTask() {
        return task instanceof ExecTask;
    }

    public Task execTask() {
        if (isExecTask()) {
            return task;
        }
        return new ExecTask();
    }

    public boolean isAntTask() {
        return task instanceof AntTask;
    }

    public Task antTask() {
        if (isAntTask()) {
            return task;
        }
        return new AntTask();
    }

    private boolean isRakeTask() {
        return task instanceof RakeTask;
    }

    public Task rakeTask() {
        if (isRakeTask()) {
            return task;
        }
        return new RakeTask();
    }

    private boolean isNantTask() {
        return task instanceof NantTask;
    }

    public Task nantTask() {
        if (isNantTask()) {
            return task;
        }
        return new NantTask();
    }

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public boolean validateTree(ValidationContext validationContext) {
        if(hasCancelTask()){
            return task.validateTree(validationContext);
        }
        return true;
    }
}
