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

import com.thoughtworks.go.plugin.api.config.Property;

/**
 * Specialization of {@link com.thoughtworks.go.plugin.api.config.Property} class, for task configuration.
 */
@Deprecated
//Will be moved to internal scope
public class TaskConfigProperty extends Property implements Comparable{
    protected TaskConfigProperty(String key) {
        super(key);
        updateDefaults();
    }

    public TaskConfigProperty(String propertyName, String value) {
        super(propertyName, value);
        updateDefaults();
    }


    private void updateDefaults() {
        with(REQUIRED, false);
        with(SECURE, false);
        with(DISPLAY_NAME, getKey());
        with(DISPLAY_ORDER, 0);
    }

    @Override
    public int compareTo(Object o) {
        return this.getOption(DISPLAY_ORDER) - ((TaskConfigProperty) o).getOption(DISPLAY_ORDER);
    }
}
