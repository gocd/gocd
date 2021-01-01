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
package com.thoughtworks.go.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.service.TaskFactory;

/**
 * @understands how to get the builder to kill all child processes of the current process
 */
public class KillAllChildProcessTask implements Task, Serializable {
    private final ConfigErrors configErrors = new ConfigErrors();

    @Override
    public RunIfConfigs getConditions() {
        return new RunIfConfigs();
    }

    @Override
    public Task cancelTask() {
        return new NullTask();
    }

    @Override
    public boolean hasCancelTask() {
        return false;
    }

    @Override
    public String getTaskType() {
        return "killallchildprocess";
    }

    @Override
    public String getTypeForDisplay() {
        return "kill all child process";
    }

    @Override
    public List<TaskProperty> getPropertiesForDisplay() {
        return new ArrayList<>();
    }

    @Override
    public void setConfigAttributes(Object attributes, TaskFactory taskFactory) {
        throw new UnsupportedOperationException("Not a configurable task");
    }

    @Override
    public boolean hasSameTypeAs(Task task) {
        return this.getTaskType().equals(task.getTaskType());
    }

    @Override
    public boolean validateTree(ValidationContext validationContext) {
        return true;
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        throw new UnsupportedOperationException("Not a configurable task");
    }

    @Override
    public void validate(ValidationContext validationContext) {
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

}
