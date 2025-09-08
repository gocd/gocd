/*
 * Copyright Thoughtworks, Inc.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskConfigTest {

    private TaskConfig taskConfig;

    @BeforeEach
    public void setUp() {
        taskConfig = new TaskConfig();
    }

    @Test
    public void shouldAddPropertyWithGiveName() {
        String abcd = "Abcd";
        String abcdDefault = "first of alphabets";
        String wxyz = "wxyz";
        String wxyzDefault = "last of alphabets";

        taskConfig.addProperty(wxyz).withDefault(wxyzDefault);
        taskConfig.addProperty(abcd).withDefault(abcdDefault);
        List<? extends Property> properties = taskConfig.list();
        assertThat(properties.size()).isEqualTo(2);
        for (Property property : properties) {
            assertThat(property != null).isTrue();
            assertThat(property instanceof TaskConfigProperty).isTrue();
        }
        assertThat(taskConfig.get(abcd) != null).isTrue();
        assertThat(taskConfig.get(abcd).getValue()).isEqualTo(abcdDefault);
        assertThat(taskConfig.get(wxyz) != null).isTrue();
        assertThat(taskConfig.get(wxyz).getValue()).isEqualTo(wxyzDefault);
    }

    @Test
    public void shouldSortTheProperties() {
        TaskConfigProperty k1 = getTaskConfigProperty(3);
        TaskConfigProperty k2 = getTaskConfigProperty(0);
        TaskConfigProperty k3 = getTaskConfigProperty(2);
        TaskConfigProperty k4 = getTaskConfigProperty(1);
        taskConfig.add(k1);
        taskConfig.add(k2);
        taskConfig.add(k3);
        taskConfig.add(k4);
        assertThat(taskConfig.list().get(0)).isEqualTo(k2);
        assertThat(taskConfig.list().get(1)).isEqualTo(k4);
        assertThat(taskConfig.list().get(2)).isEqualTo(k3);
        assertThat(taskConfig.list().get(3)).isEqualTo(k1);
    }

    private TaskConfigProperty getTaskConfigProperty(int order) {
        TaskConfigProperty property = new TaskConfigProperty("Key" + order);
        property.with(Property.DISPLAY_ORDER, order);
        return property;
    }


}
