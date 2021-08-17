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

import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.common.PluginView;
import com.thoughtworks.go.plugin.domain.pluggabletask.PluggableTaskPluginInfo;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class PluggableTaskPluginInfoBuilderTest {
    private TaskExtension extension;

    @BeforeEach
    public void setUp() throws Exception {
        extension = mock(TaskExtension.class);

        TaskConfig taskConfig = new TaskConfig();
        taskConfig.add(new TaskConfigProperty("username", null).with(Property.REQUIRED, true).with(Property.SECURE, false));
        taskConfig.add(new TaskConfigProperty("password", null).with(Property.REQUIRED, true).with(Property.SECURE, true));

        TaskView taskView = new TaskView() {

            @Override
            public String displayValue() {
                return "my task plugin";
            }

            @Override
            public String template() {
                return "some html";
            }
        };


        final Task task = mock(Task.class);
        when(task.config()).thenReturn(taskConfig);
        when(task.view()).thenReturn(taskView);

        final GoPluginDescriptor descriptor = mock(GoPluginDescriptor.class);
        String pluginId = "plugin1";
        when(descriptor.id()).thenReturn(pluginId);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final Action<Task> action = (Action<Task>) invocation.getArguments()[1];
                action.execute(task, descriptor);
                return null;
            }
        }).when(extension).doOnTask(eq("plugin1"), any(Action.class));
    }

    @Test
    public void shouldBuildPluginInfo() throws Exception {
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin1").build();

        PluggableTaskPluginInfo pluginInfo = new PluggableTaskPluginInfoBuilder(extension) .pluginInfoFor(descriptor);

        List<PluginConfiguration> pluginConfigurations = Arrays.asList(
                new PluginConfiguration("username", new Metadata(true, false)),
                new PluginConfiguration("password", new Metadata(true, true))
        );
        PluginView pluginView = new PluginView("some html");

        assertThat(pluginInfo.getDescriptor(), is(descriptor));
        assertThat(pluginInfo.getExtensionName(), is("task"));
        assertThat(pluginInfo.getDisplayName(), is("my task plugin"));
        assertThat(pluginInfo.getTaskSettings(), is(new PluggableInstanceSettings(pluginConfigurations, pluginView)));
        assertNull(pluginInfo.getPluginSettings());
    }
}
