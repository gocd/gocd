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

package com.thoughtworks.go.domain;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.service.TaskFactory;


public class NullTask implements Task {
    private final ConfigErrors configErrors = new ConfigErrors();

    public RunIfConfigs getConditions() {
        return new RunIfConfigs();
    }

    public Task cancelTask() {
        return new NullTask();
    }

    public boolean hasCancelTask() {
        return false;
    }

    public String getTaskType() {
        return "null";
    }

    public String getTypeForDisplay() {
        return "null";
    }

    public List<TaskProperty> getPropertiesForDisplay() {
        return new ArrayList<>();
    }

    public void setConfigAttributes(Object attributes) {
        throw new UnsupportedOperationException("Not a configurable task");
    }

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


    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

}
