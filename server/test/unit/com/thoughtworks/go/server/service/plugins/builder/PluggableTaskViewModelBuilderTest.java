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

package com.thoughtworks.go.server.service.plugins.builder;

import com.thoughtworks.go.plugin.access.pluggabletask.JsonBasedPluggableTask;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluginConfiguration;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluginView;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluggableTaskViewModelBuilderTest {
    @Mock
    PluginManager manager;

    private PluggableTaskViewModelBuilder builder;
    private GoPluginDescriptor xunitConvertor;
    private GoPluginDescriptor powershellTask;
    private TaskPreference taskPreference;

    @Before
    public void setUp() {
        initMocks(this);
        builder = new PluggableTaskViewModelBuilder(manager);

        xunitConvertor = new GoPluginDescriptor("xunit.convertor", "version1",
                new GoPluginDescriptor.About("Xunit Convertor", "1.0", null, null, null,null),
                null, null, false);

        powershellTask = new GoPluginDescriptor("powershell.task", "version1",
                new GoPluginDescriptor.About("Powershell Task", "2.0", null, null, null,null),
                null, null, false);

        JsonBasedPluggableTask jsonBasedPluggableTask = mock(JsonBasedPluggableTask.class);
        TaskView taskView = new TaskView() {
            @Override
            public String displayValue() {
                return "task display value";
            }

            @Override
            public String template() {
                return "pluggable task view template";
            }
        };

        TaskConfig taskConfig = new TaskConfig();
        taskConfig.add(new TaskConfigProperty("key1", null));
        taskConfig.add(new TaskConfigProperty("key2", null));

        when(jsonBasedPluggableTask.config()).thenReturn(taskConfig);
        when(jsonBasedPluggableTask.view()).thenReturn(taskView);

        taskPreference = new TaskPreference(jsonBasedPluggableTask);

        PluggableTaskConfigStore.store().setPreferenceFor("xunit.convertor", taskPreference);
        PluggableTaskConfigStore.store().setPreferenceFor("powershell.task", taskPreference);
    }

    @Test
    public void shouldBeAbleToFetchAllPluginInfos() {
        when(manager.getPluginDescriptorFor("xunit.convertor")).thenReturn(xunitConvertor);
        when(manager.getPluginDescriptorFor("powershell.task")).thenReturn(powershellTask);

        List<PluginInfo> pluginInfos = builder.allPluginInfos();

        assertThat(pluginInfos.size(), is(2));
        PluginInfo pluginInfo = pluginInfos.get(0).getId() == "xunit.convertor" ? pluginInfos.get(0) : pluginInfos.get(1);
        assertThat(pluginInfo.getId(), is("xunit.convertor"));
        assertThat(pluginInfo.getType(), is("task"));
        assertThat(pluginInfo.getName(), is(xunitConvertor.about().name()));
        assertThat(pluginInfo.getDisplayName(), is("task display value"));
        assertThat(pluginInfo.getVersion(), is(xunitConvertor.about().version()));
        assertNull(pluginInfo.getPluggableInstanceSettings());
    }

    @Test
    public void shouldBeAbleToFetchAPluginInfoForAGivenId() {
        when(manager.getPluginDescriptorFor("xunit.convertor")).thenReturn(xunitConvertor);

        PluginInfo pluginInfo = builder.pluginInfoFor("xunit.convertor");

        assertThat(pluginInfo.getId(), is("xunit.convertor"));
        assertThat(pluginInfo.getType(), is("task"));
        assertThat(pluginInfo.getName(), is(xunitConvertor.about().name()));
        assertThat(pluginInfo.getDisplayName(), is("task display value"));
        assertThat(pluginInfo.getVersion(), is(xunitConvertor.about().version()));
    }

    @Test
    public void shouldBeAbleToFetchAPluginInfoForAGivenIdWithView() {
        when(manager.getPluginDescriptorFor("xunit.convertor")).thenReturn(xunitConvertor);

        PluginInfo pluginInfo = builder.pluginInfoFor("xunit.convertor");

        PluginView view = pluginInfo.getPluggableInstanceSettings().getView();
        assertThat(view.getTemplate(), is("pluggable task view template"));
    }

    @Test
    public void shouldBeAbleToFetchAPluginInfoForAGivenIdWithConfigurations() {
        when(manager.getPluginDescriptorFor("xunit.convertor")).thenReturn(xunitConvertor);

        PluginInfo pluginInfo = builder.pluginInfoFor("xunit.convertor");

        HashMap expectedMetadata = new HashMap<String, Object>() {{
            put("required",false);
            put("secure",false);
        }};

        List<PluginConfiguration> configurations = pluginInfo.getPluggableInstanceSettings().getConfigurations();
        assertThat(configurations.size(), is(2));

        PluginConfiguration configuration1 = configurations.get(0);
        assertThat(configuration1.getKey(), is("key1"));
        assertNull(configuration1.getType());
        assertThat(configuration1.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));


        PluginConfiguration configuration2 = configurations.get(1);
        assertThat(configuration2.getKey(), is("key2"));
        assertNull(configuration2.getType());
        assertThat(configuration1.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));
    }

    @Test
    public void shouldBeNullIfPluginNotRegistered() {
        assertNull(builder.pluginInfoFor("unregistered_plugin"));
    }
}