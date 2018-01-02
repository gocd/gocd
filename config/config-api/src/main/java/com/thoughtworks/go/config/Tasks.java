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

import java.util.Collections;
import java.util.Map;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.service.TaskFactory;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@ConfigTag("tasks")
@ConfigCollection(Task.class)
public class Tasks extends BaseCollection<Task> implements Validatable {
    public static final String TASK_OPTIONS = "taskOptions";

    private final ConfigErrors configErrors = new ConfigErrors();
    private final int INCREMENT_INDEX = 1;
    private final int DECREMENT_INDEX = -1;

    public Tasks() {
    }

    public Tasks(Task... items) {
        super(items);
    }

    public void validate(ValidationContext validationContext) {
    }

    public boolean validateTree(ValidationContext validationContext) {
        boolean isValid = true;
        for (Task task : this) {
            isValid = task.validateTree(validationContext) && isValid;
        }
        return isValid;
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public boolean isExecTask() {
        return first() instanceof ExecTask;
    }

    public Task execTask() {
        if (isExecTask()) {
            return first();
        }
        return new ExecTask();
    }

    public Tasks findByType(Class<? extends Task> type) {
        Tasks matchedTasks = new Tasks();
        for (Task t : this) {
            if (type.isInstance(t)) {
                matchedTasks.add(t);
            }
        }

        return matchedTasks;
    }

    public Task findFirstByType(Class<? extends Task> type) {
        Tasks tasks = findByType(type);
        if (tasks.size() > 0) {
            return tasks.first();
        } else {
            throw bomb("Unable to find task of type " + type);
        }
    }

    public void setConfigAttributes(Object attributes, TaskFactory taskFactory) {
        clear();
        if (attributes == null) {
            return;
        }
        if (taskFactory == null) throw new IllegalArgumentException("ConfigContext cannot be null");
        Map attributeMap = (Map) attributes;
        String taskType = (String) attributeMap.get(TASK_OPTIONS);
        Task task = taskFactory.taskInstanceFor(taskType);
        task.setConfigAttributes(attributeMap.get(taskType), taskFactory);
        add(task);
    }

    public void incrementIndex(int task_index) {


        moveTask(task_index, INCREMENT_INDEX);
    }

    public void decrementIndex(int taskIndex) {
        moveTask(taskIndex, DECREMENT_INDEX);
    }

     public String getTaskOptions() {
        return first() == null ? "" : first().getTaskType();
    }

    private void moveTask(int taskIndex, final int moveBy) {
        try {
            Collections.swap(this, taskIndex, taskIndex + moveBy);
        } catch (IndexOutOfBoundsException e) {
             throw new RuntimeException(String.format("There is not valid task at position %d.", taskIndex),e);
        }
    }

}
