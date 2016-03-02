/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluggableTaskConfigStoreTest {
    @Before
    public void setUp() throws Exception {
        SCMMetadataStore.getInstance().clear();
    }

    @After
    public void tearDown() throws Exception {
        SCMMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldPopulateMetaDataCorrectly() throws Exception {
        TaskConfig taskConfig = new TaskConfig();
        TaskView taskView = null;
        Task task = mock(Task.class);
        when(task.config()).thenReturn(taskConfig);
        when(task.view()).thenReturn(taskView);

        TaskPreference taskPreference = new TaskPreference(task);
        PluggableTaskConfigStore.store().setPreferenceFor("plugin-id", taskPreference);

        assertThat(PluggableTaskConfigStore.store().getMetaData("plugin-id"), is(taskConfig));
        assertThat(PluggableTaskConfigStore.store().getMetaData("invalid-plugin-id"), is(nullValue()));
    }

    @Test
    public void shouldBeAbleToCheckIfPluginExists() throws Exception {
        TaskConfig taskConfig = new TaskConfig();
        TaskView taskView = null;
        Task task = mock(Task.class);
        when(task.config()).thenReturn(taskConfig);
        when(task.view()).thenReturn(taskView);

        TaskPreference taskPreference = new TaskPreference(task);
        PluggableTaskConfigStore.store().setPreferenceFor("plugin-id", taskPreference);

        assertThat(PluggableTaskConfigStore.store().hasPlugin("plugin-id"), is(true));
        assertThat(PluggableTaskConfigStore.store().hasPlugin("some-plugin-which-does-not-exist"), is(false));

    }

}