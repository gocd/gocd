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

import java.util.List;

import com.thoughtworks.go.plugin.api.config.Property;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TaskConfigTest {

    private TaskConfig taskConfig;

    @Before
    public void setUp() throws Exception {
        taskConfig = new TaskConfig();
    }

    @Test
    public void shouldAddPropertyWithGiveName() throws Exception {
        String abcd = "Abcd";
        String abcdDefault = "first of alphabets";
        String wxyz = "wxyz";
        String wxyzDefault = "last of alphabets";

        taskConfig.addProperty(wxyz).withDefault(wxyzDefault);
        taskConfig.addProperty(abcd).withDefault(abcdDefault);
        List<? extends Property> properties = taskConfig.list();
        assertThat(properties.size(), is(2));
        for (Property property : properties) {
            assertThat(property != null, is(true));
            assertThat(property instanceof TaskConfigProperty, is(true));
        }
        assertThat(taskConfig.get(abcd) != null, is(true));
        assertThat(taskConfig.get(abcd).getValue(), is(abcdDefault));
        assertThat(taskConfig.get(wxyz) != null, is(true));
        assertThat(taskConfig.get(wxyz).getValue(), is(wxyzDefault));
    }

}
