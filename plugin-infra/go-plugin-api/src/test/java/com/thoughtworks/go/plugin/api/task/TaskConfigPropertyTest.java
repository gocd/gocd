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
package com.thoughtworks.go.plugin.api.task;

import com.thoughtworks.go.plugin.api.config.Property;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TaskConfigPropertyTest {
    @Test
    public void validateTaskPropertyDefaults() throws Exception {
        TaskConfigProperty taskConfigProperty = new TaskConfigProperty("Test-Property");
        assertThat(taskConfigProperty.getOptions().size(), is(4));
        assertThat(taskConfigProperty.getOption(Property.REQUIRED), is(false));
        assertThat(taskConfigProperty.getOption(Property.SECURE), is(false));
        taskConfigProperty = new TaskConfigProperty("Test-Property", "Dummy Value");
        taskConfigProperty.with(Property.REQUIRED, true);
        assertThat(taskConfigProperty.getOptions().size(), is(4));
        assertThat(taskConfigProperty.getOption(Property.REQUIRED), is(true));
        assertThat(taskConfigProperty.getOption(Property.SECURE), is(false));
    }

    @Test
    public void shouldAssignDefaults() {
        final TaskConfigProperty property = new TaskConfigProperty("key");
        assertThat(property.getOption(property.REQUIRED), is(false));
        assertThat(property.getOption(property.SECURE), is(false));
        assertThat(property.getOption(property.DISPLAY_NAME), is("key"));
        assertThat(property.getOption(property.DISPLAY_ORDER), is(0));
    }

    @Test
    public void shouldCompareTwoPropertiesBasedOnOrder() {
        TaskConfigProperty p1 = getTaskConfigProperty("Test-Property", 1);
        TaskConfigProperty p2 = getTaskConfigProperty("Test-Property", 0);
        assertThat(p1.compareTo(p2), is(1));
    }

    private TaskConfigProperty getTaskConfigProperty(String key, int order) {
        TaskConfigProperty property = new TaskConfigProperty(key);
        property.with(Property.DISPLAY_ORDER, order);
        return property;
    }

}
