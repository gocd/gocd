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

import java.util.List;

import com.thoughtworks.go.config.ConfigInterface;
import com.thoughtworks.go.config.ParamsAttributeAware;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.service.TaskFactory;

@ConfigInterface
public interface Task extends ParamsAttributeAware, Validatable {
    public static final String TASK_TYPE = "task_type";

    RunIfConfigs getConditions();

    Task cancelTask();

    boolean hasCancelTask();

    String getTaskType();

    String getTypeForDisplay();

    List<TaskProperty> getPropertiesForDisplay();

    void setConfigAttributes(Object attributes, TaskFactory taskFactory);

    boolean hasSameTypeAs(Task task);
    boolean validateTree(ValidationContext validationContext);
}
