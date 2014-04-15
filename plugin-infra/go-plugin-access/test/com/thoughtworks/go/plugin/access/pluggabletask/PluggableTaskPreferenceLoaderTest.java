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

package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PluggableTaskPreferenceLoaderTest {
    @Test
    public void shouldRegisterPluginListenerWithPluginManager() throws Exception {
        PluginManager pluginManager=mock(PluginManager.class);
        PluggableTaskPreferenceLoader pluggableTaskPreferenceLoader = new PluggableTaskPreferenceLoader(pluginManager);
        verify(pluginManager).addPluginChangeListener(pluggableTaskPreferenceLoader, Task.class);
    }

    @Test
    public void shouldSetConfigForTheTaskCorrespondingToGivenPluginId() throws Exception {
        final GoPluginDescriptor descriptor = mock(GoPluginDescriptor.class);
        String pluginId = "test-plugin-id";
        when(descriptor.id()).thenReturn(pluginId);
        final Task task = mock(Task.class);
        TaskConfig config = new TaskConfig();
        TaskView taskView = mock(TaskView.class);
        when(task.config()).thenReturn(config);
        when(task.view()).thenReturn(taskView);
        PluginManager pluginManager=mock(PluginManager.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] arguments = invocationOnMock.getArguments();
                Action<Task> action= (Action<Task>) arguments[2];
                action.execute(task,descriptor);
                return null;
            }
        }).when(pluginManager).doOnIfHasReference(eq(Task.class),eq(pluginId), Matchers.<Action<Task>>anyObject());
        PluggableTaskPreferenceLoader pluggableTaskPreferenceLoader = new PluggableTaskPreferenceLoader(pluginManager);
        pluggableTaskPreferenceLoader.pluginLoaded(descriptor);
        assertThat(PluggableTaskConfigStore.store().hasPreferenceFor(pluginId), is(true));
        assertThat(PluggableTaskConfigStore.store().preferenceFor(pluginId), is(new TaskPreference(task)));
        verify(pluginManager).addPluginChangeListener(pluggableTaskPreferenceLoader, Task.class);
    }

    @Test
    public void shouldRemoveConfigForTheTaskCorrespondingToGivenPluginId() throws Exception {
        final GoPluginDescriptor descriptor = mock(GoPluginDescriptor.class);
        String pluginId = "test-plugin-id";
        when(descriptor.id()).thenReturn(pluginId);
        final Task task = mock(Task.class);
        TaskConfig config = new TaskConfig();
        TaskView taskView = mock(TaskView.class);
        when(task.config()).thenReturn(config);
        when(task.view()).thenReturn(taskView);
        PluggableTaskConfigStore.store().setPreferenceFor(pluginId, new TaskPreference(task));
        PluginManager pluginManager=mock(PluginManager.class);
        PluggableTaskPreferenceLoader pluggableTaskPreferenceLoader = new PluggableTaskPreferenceLoader(pluginManager);
        assertThat(PluggableTaskConfigStore.store().hasPreferenceFor(pluginId), is(true));
        pluggableTaskPreferenceLoader.pluginUnLoaded(descriptor);
        assertThat(PluggableTaskConfigStore.store().hasPreferenceFor(pluginId), is(false));
        verify(pluginManager).addPluginChangeListener(pluggableTaskPreferenceLoader, Task.class);
    }
}
