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

package com.thoughtworks.go.plugin.api.task;

import com.thoughtworks.go.plugin.api.config.Configuration;
import com.thoughtworks.go.plugin.api.config.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Allows the plugin to specify information about the configuration it accepts and expects.
 *
 * This class, at other times, is used to hold information about the value provided by the
 * user, for the configuration.
 */
@Deprecated
//Will be moved to internal scope
public class TaskConfig extends Configuration {
    /**
     * Adds a property to the configuration of this task.
     *
     * @param propertyName Name of the property (or key) in the configuration.
     *
     * @return an instance of {@link com.thoughtworks.go.plugin.api.task.TaskConfigProperty}
     */
    public TaskConfigProperty addProperty(String propertyName) {
        TaskConfigProperty property = new TaskConfigProperty(propertyName);
        add(property);
        return property;
    }

    /**
     * Retrieves the value of a property.
     * @param propertyName Name of the property (or key) in the configuration.
     *
     * @return the value of the specified property, or null if not found.
     */
    public String getValue(String propertyName) {
        Property property = super.get(propertyName);
        if (property == null) {
            return null;
        }
        return property.getValue();
    }

    public List<? extends  Property> list() {
        ArrayList<TaskConfigProperty> list = new ArrayList<>();
        for (Property property : super.list()) {
            list.add((TaskConfigProperty) property);
        }
        Collections.sort(list);
        return list;
    }
}
