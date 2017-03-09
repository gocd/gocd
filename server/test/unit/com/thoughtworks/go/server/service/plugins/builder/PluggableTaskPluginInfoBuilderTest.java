/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.helper.MetadataStoreHelper;
import com.thoughtworks.go.plugin.access.pluggabletask.JsonBasedPluggableTask;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.plugins.PluggableInstanceSettings;
import com.thoughtworks.go.server.ui.plugins.PluggableTaskPluginInfo;
import com.thoughtworks.go.server.ui.plugins.PluginView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static com.thoughtworks.go.server.service.plugins.builder.PluggableTaskPluginInfoBuilder.configurations;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluggableTaskPluginInfoBuilderTest {

    @Before
    public void setUp() throws Exception {
        MetadataStoreHelper.clear();
    }

    @After
    public void tearDown() throws Exception {
        MetadataStoreHelper.clear();
    }

    @Test
    public void pluginInfoFor_ShouldProvidePluginInfoForAPlugin() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);

        PluginManager pluginManager = mock(PluginManager.class);
        PluggableTaskConfigStore packageMetadataStore = mock(PluggableTaskConfigStore.class);

        when(packageMetadataStore.pluginIds()).thenReturn(Collections.singleton(plugin.id()));
        when(pluginManager.getPluginDescriptorFor(plugin.id())).thenReturn(plugin);

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

        TaskPreference taskPreference = new TaskPreference(jsonBasedPluggableTask);

        when(packageMetadataStore.preferenceFor(plugin.id())).thenReturn(taskPreference);

        PluggableTaskPluginInfoBuilder builder = new PluggableTaskPluginInfoBuilder(pluginManager, packageMetadataStore);
        PluggableTaskPluginInfo pluginInfo = builder.pluginInfoFor(plugin.id());

        PluggableTaskPluginInfo expectedPluginInfo = new PluggableTaskPluginInfo(plugin, taskView.displayValue(), new PluggableInstanceSettings(configurations(taskConfig), new PluginView(taskView.template())));
        assertEquals(expectedPluginInfo, pluginInfo);
    }

    @Test
    public void pluginInfoFor_ShouldReturnNullWhenPluginIsNotFound() throws Exception {
        PluginManager pluginManager = mock(PluginManager.class);
        PluggableTaskConfigStore packageMetadataStore = mock(PluggableTaskConfigStore.class);

        PluggableTaskPluginInfoBuilder builder = new PluggableTaskPluginInfoBuilder(pluginManager, packageMetadataStore);
        PluggableTaskPluginInfo pluginInfo = builder.pluginInfoFor("docker-plugin");
        assertEquals(null, pluginInfo);
    }

    @Test
    public void allPluginInfos_ShouldReturnAListOfAllPluginInfos() throws Exception {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("Plugin Descriptor Validator", "1.0.1", "12.4", "Validates its own plugin descriptor",
                new GoPluginDescriptor.Vendor("ThoughtWorks Go Team", "www.thoughtworks.com"), Arrays.asList("Linux", "Windows", "Mac OS X"));
        GoPluginDescriptor plugin = new GoPluginDescriptor("docker-plugin", "1.0", about, null, null, false);


        PluginManager pluginManager = mock(PluginManager.class);
        PluggableTaskConfigStore packageMetadataStore = mock(PluggableTaskConfigStore.class);

        when(packageMetadataStore.pluginIds()).thenReturn(Collections.singleton(plugin.id()));
        when(pluginManager.getPluginDescriptorFor(plugin.id())).thenReturn(plugin);

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

        TaskPreference taskPreference = new TaskPreference(jsonBasedPluggableTask);

        when(packageMetadataStore.preferenceFor(plugin.id())).thenReturn(taskPreference);

        PluggableTaskPluginInfoBuilder builder = new PluggableTaskPluginInfoBuilder(pluginManager, packageMetadataStore);
        Collection<PluggableTaskPluginInfo> pluginInfos = builder.allPluginInfos();

        PluggableTaskPluginInfo expectedPluginInfo = new PluggableTaskPluginInfo(plugin, taskView.displayValue(), new PluggableInstanceSettings(configurations(taskConfig), new PluginView(taskView.template())));        assertEquals(Arrays.asList(expectedPluginInfo), pluginInfos);
    }

}