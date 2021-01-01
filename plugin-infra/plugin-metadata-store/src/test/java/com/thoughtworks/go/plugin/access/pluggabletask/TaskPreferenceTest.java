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
package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskView;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaskPreferenceTest {
    @Test
    public void shouldTestEquals() throws Exception {
        Task task1 = mock(Task.class);
        TaskConfig config1 = new TaskConfig();
        TaskView taskView1 = mock(TaskView.class);
        when(task1.config()).thenReturn(config1);
        when(task1.view()).thenReturn(taskView1);
        TaskPreference taskPreference1 = new TaskPreference(task1);

        Task task2 = mock(Task.class);
        TaskConfig config2 = new TaskConfig();
        TaskView taskView2 = mock(TaskView.class);
        when(task2.config()).thenReturn(config2);
        when(task2.view()).thenReturn(taskView2);
        TaskPreference taskPreference2 = new TaskPreference(task2);

        TaskPreference taskPreference3 = new TaskPreference(task1);

        Task task3 = mock(Task.class);
        when(task3.config()).thenReturn(config1);
        when(task3.view()).thenReturn(taskView1);

        TaskPreference taskPreference4 = new TaskPreference(task3);

        Task task5 = mock(Task.class);
        TaskView taskView5 = mock(TaskView.class);
        when(task5.config()).thenReturn(config1);
        when(task5.view()).thenReturn(taskView5);
        TaskPreference taskPreference5 = new TaskPreference(task5);

        assertThat(taskPreference1.equals(taskPreference2), is(false));
        assertThat(taskPreference1.equals(taskPreference3), is(true));
        assertThat(taskPreference1.equals(taskPreference4), is(true));
        assertThat(taskPreference1.equals(taskPreference5), is(false));
    }
}
