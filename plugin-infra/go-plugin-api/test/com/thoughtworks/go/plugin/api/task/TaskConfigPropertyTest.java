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
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TaskConfigPropertyTest {
    @Test
    public void validateTaskPropertyDefaults() throws Exception {
        TaskConfigProperty taskConfigProperty = new TaskConfigProperty("Test-Property");
        assertThat(taskConfigProperty.getOptions().size(), is(2));
        assertThat(taskConfigProperty.getOption(Property.REQUIRED), is(true));
        assertThat(taskConfigProperty.getOption(Property.SECURE), is(false));
        taskConfigProperty = new TaskConfigProperty("Test-Property", "Dummy Value");
        assertThat(taskConfigProperty.getOptions().size(), is(2));
        assertThat(taskConfigProperty.getOption(Property.REQUIRED), is(true));
        assertThat(taskConfigProperty.getOption(Property.SECURE), is(false));
    }

    @Test
    public void shouldAssignDefaults() {
        final TaskConfigProperty property = new TaskConfigProperty("key");
        assertThat(property.getOption(property.REQUIRED), is(true));
        assertThat(property.getOption(property.SECURE), is(false));
    }
}
