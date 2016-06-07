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

package com.thoughtworks.go.server.service;


import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.access.pluggabletask.JsonBasedPluggableTask;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.access.scm.*;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.PluginViewModel;
import com.thoughtworks.go.server.ui.PluginConfigurationViewModel;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginBuilderTest {
    @Test
    public void shouldBuildPluginViewModel() {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("plugin_name", "plugin_version", null, null, null, null);
        PluginViewModel model = PluginBuilder.getByExtension("scm").build("plugin_id", about, false);

        assertThat(model.getId(), is("plugin_id"));
        assertThat(model.getVersion(), is("plugin_version"));
        assertThat(model.getType(), is("scm"));
        assertNull(model.getConfigurations());
    }

    @Test
    public void shouldBuildPluginViewModelForSCMPluginWithConfigurations() {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("plugin_name", "plugin_version", null, null, null, null);
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        scmConfigurations.add(new SCMConfiguration("key1"));
        scmConfigurations.add(new SCMConfiguration("key2"));

        SCMMetadataStore.getInstance().setPreferenceFor("scm-plugin-id", new SCMPreference(scmConfigurations, new SCMView() {
            @Override
            public String displayValue() {
                return null;
            }

            @Override
            public String template() {
                return "scm view template";
            }
        }));

        PluginViewModel model = PluginBuilder.getByExtension("scm").build("scm-plugin-id", about, true);

        HashMap expectedMetadata = new HashMap<String, Object>() {{
            put("required",true);
            put("secure",false);
            put("part_of_identity",true);
        }};

        assertThat(model.getConfigurations().size(), is(2));
        PluginConfigurationViewModel configuration1 = model.getConfigurations().get(0);
        assertThat(configuration1.getKey(), is("key1"));
        assertNull(configuration1.getType());
        assertThat(configuration1.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));

        PluginConfigurationViewModel configuration2 = model.getConfigurations().get(1);
        assertThat(configuration2.getKey(), is("key2"));
        assertNull(configuration2.getType());
        assertThat(configuration1.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));
    }

    @Test
    public void shouldBuildPluginViewModelForSCMPluginWithViewTemplate() {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("plugin_name", "plugin_version", null, null, null, null);

        SCMMetadataStore.getInstance().setPreferenceFor("scm-plugin-id", new SCMPreference(new SCMConfigurations(), new SCMView() {
            @Override
            public String displayValue() {
                return null;
            }

            @Override
            public String template() {
                return "scm view template";
            }
        }));

        PluginViewModel model = PluginBuilder.getByExtension("scm").build("scm-plugin-id", about, true);

        assertThat(model.getViewTemplate(), is("scm view template"));
    }

    @Test
    public void shouldBuildPluginViewModelForTaskPluginWithConfigurations() {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("plugin_name", "plugin_version", null, null, null, null);
        JsonBasedPluggableTask jsonBasedPluggableTask = mock(JsonBasedPluggableTask.class);
        TaskConfig taskConfig = new TaskConfig();
        taskConfig.add(new TaskConfigProperty("key1", null));
        taskConfig.add(new TaskConfigProperty("key2", null));

        when(jsonBasedPluggableTask.config()).thenReturn(taskConfig);
        when(jsonBasedPluggableTask.view()).thenReturn(mock(TaskView.class));


        TaskPreference taskPreference = new TaskPreference(jsonBasedPluggableTask);
        PluggableTaskConfigStore.store().setPreferenceFor("task_plugin_id", taskPreference);

        PluginViewModel model = PluginBuilder.getByExtension("task").build("task_plugin_id", about, true);

        HashMap expectedMetadata = new HashMap<String, Object>() {{
            put("required",false);
            put("secure",false);
        }};

        assertThat(model.getConfigurations().size(), is(2));

        PluginConfigurationViewModel configuration1 = model.getConfigurations().get(0);
        assertThat(configuration1.getKey(), is("key1"));
        assertNull(configuration1.getType());
        assertThat(configuration1.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));


        PluginConfigurationViewModel configuration2 = model.getConfigurations().get(1);
        assertThat(configuration2.getKey(), is("key2"));
        assertNull(configuration2.getType());
        assertThat(configuration1.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));
    }

    @Test
    public void shouldBuildPluginViewModelForTaskPluginWithViewTemplate() {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("plugin_name", "plugin_version", null, null, null, null);

        JsonBasedPluggableTask jsonBasedPluggableTask = mock(JsonBasedPluggableTask.class);
        TaskView taskView = mock(TaskView.class);
        TaskConfig taskConfig = new TaskConfig();

        when(jsonBasedPluggableTask.config()).thenReturn(taskConfig);
        when(jsonBasedPluggableTask.view()).thenReturn(taskView);
        when(taskView.template()).thenReturn("task view template");

        TaskPreference taskPreference = new TaskPreference(jsonBasedPluggableTask);
        PluggableTaskConfigStore.store().setPreferenceFor("task_plugin_id", taskPreference);

        PluginViewModel model = PluginBuilder.getByExtension("task").build("task_plugin_id", about, true);

        assertThat(model.getViewTemplate(), is("task view template"));
    }

    @Test
    public void shouldBuildPluginViewModelForPackagePluginWithConfigurations() {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("plugin_name", "plugin_version", null, null, null, null);
        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.add(new PackageConfiguration("key1"));
        packageConfigurations.add(new PackageConfiguration("key2"));

        PackageConfigurations repositoryConfigurations = new PackageConfigurations();
        repositoryConfigurations.add(new PackageConfiguration("key1"));

        RepositoryMetadataStore.getInstance().addMetadataFor("package-plugin-id", repositoryConfigurations);
        PackageMetadataStore.getInstance().addMetadataFor("package-plugin-id", packageConfigurations);

        PluginViewModel model = PluginBuilder.getByExtension("package-repository").build("package-plugin-id", about, true);

        HashMap expectedMetadata = new HashMap<String, Object>() {{
            put("required",true);
            put("secure",false);
            put("part_of_identity",true);
        }};

        assertThat(model.getConfigurations().size(), is(3));
        PluginConfigurationViewModel configuration1 = model.getConfigurations().get(0);
        assertThat(configuration1.getKey(), is("key1"));
        assertThat(configuration1.getType(), is("repository"));
        assertThat(configuration1.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));

        PluginConfigurationViewModel configuration2 = model.getConfigurations().get(1);
        assertThat(configuration2.getKey(), is("key1"));
        assertThat(configuration2.getType(), is("package"));
        assertThat(configuration2.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));

        PluginConfigurationViewModel configuration3 = model.getConfigurations().get(2);
        assertThat(configuration3.getKey(), is("key2"));
        assertThat(configuration3.getType(), is("package"));
        assertThat(configuration3.getMetadata(), Is.<Map<String, Object>>is(expectedMetadata));
    }

    @Test
    public void shouldBuildPluginViewModelForAuthenticationPlugin() {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("plugin_name", "plugin_version", null, null, null, null);
        PluginViewModel model = PluginBuilder.getByExtension("authentication").build("auth-plugin-id", about, true);

        assertThat(model.getId(), is("auth-plugin-id"));
        assertThat(model.getVersion(), is("plugin_version"));
        assertThat(model.getType(), is("authentication"));
        assertNull(model.getConfigurations());
    }

    @Test
    public void shouldBuildPluginViewModelForNotificationPlugin() {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("plugin_name", "plugin_version", null, null, null, null);
        PluginViewModel model = PluginBuilder.getByExtension("notification").build("notification-plugin-id", about, true);

        assertThat(model.getId(), is("notification-plugin-id"));
        assertThat(model.getVersion(), is("plugin_version"));
        assertThat(model.getType(), is("notification"));
        assertNull(model.getConfigurations());
    }
}
