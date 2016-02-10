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

package com.thoughtworks.go.server.ui;


import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.infra.DefaultPluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.PluginStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginViewModelFactoryTest {
    private DefaultPluginManager defaultPluginManager;
    private GoPluginDescriptor goPluginDescriptor1;
    private GoPluginDescriptor goPluginDescriptor2;
    private PluginViewModelFactory pluginViewModelFactory;
    private Task task;
    private TaskConfig taskConfig;
    private PackageConfigurations packageConfigurations;
    private PackageConfigurations repositoryConfigurations;
    private SCMConfigurations scmConfigurations;
    private GoPluginDescriptor invalidGoPluginDescriptor;


    @Before
    public void setUp() throws Exception {
        defaultPluginManager = mock(DefaultPluginManager.class);

        goPluginDescriptor1 = new GoPluginDescriptor("plugin-id1", null, new GoPluginDescriptor.About("plugin", "version", null, null, null, null), null, null, false);
        goPluginDescriptor2 = new GoPluginDescriptor("plugin-id2", null, new GoPluginDescriptor.About("plugin", "version", null, null, null, null), null, null, false);
        invalidGoPluginDescriptor = mock(GoPluginDescriptor.class);
        List<GoPluginDescriptor> pluginDescriptors = Arrays.asList(goPluginDescriptor1, goPluginDescriptor2, invalidGoPluginDescriptor);
        when(defaultPluginManager.plugins()).thenReturn(pluginDescriptors);

        when(defaultPluginManager.getPluginDescriptorFor(anyString())).thenReturn(goPluginDescriptor1);
        when(defaultPluginManager.hasReferenceFor((Class) anyObject(), anyString())).thenReturn(true);

        scmConfigurations = new SCMConfigurations();
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id1", scmConfigurations, null);
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id2", scmConfigurations, null);

        taskConfig = new TaskConfig();
        taskConfig.addProperty("k1").withDefault("v1");
        TaskView taskView = mock(TaskView.class);
        task = mock(Task.class);
        when(task.config()).thenReturn(taskConfig);
        when(task.view()).thenReturn(taskView);
        PluggableTaskConfigStore.store().setPreferenceFor("plugin-id", new TaskPreference(task));

        packageConfigurations = new PackageConfigurations();
        repositoryConfigurations = new PackageConfigurations();
        RepositoryMetadataStore.getInstance().addMetadataFor("plugin-id1", repositoryConfigurations);
        RepositoryMetadataStore.getInstance().addMetadataFor("plugin-id2", repositoryConfigurations);
        PackageMetadataStore.getInstance().addMetadataFor("plugin-id1", packageConfigurations);
        PackageMetadataStore.getInstance().addMetadataFor("plugin-id2", packageConfigurations);

        pluginViewModelFactory = new PluginViewModelFactory(defaultPluginManager);
    }

    @Test
    public void shouldPopulatePluginViewModelOfTypeSCM() throws Exception {
        when(defaultPluginManager.isPluginOfType("scm", "plugin-id1")).thenReturn(true);
        when(defaultPluginManager.isPluginOfType("scm", "plugin-id2")).thenReturn(true);


        List<PluginViewModel> scmViewModels = pluginViewModelFactory.getPluginViewModelsOfType("scm");
        SCMPluginViewModel scmPluginViewModel1 = (SCMPluginViewModel) scmViewModels.get(0);
        SCMPluginViewModel scmPluginViewModel2 = (SCMPluginViewModel) scmViewModels.get(1);

        assertThat(scmPluginViewModel1.getPluginId(), is("plugin-id1"));
        assertThat(scmPluginViewModel2.getPluginId(), is("plugin-id2"));
        assertThat(scmPluginViewModel1.getVersion(), is("version"));
        assertThat(scmPluginViewModel1.getConfigurations(), is(scmConfigurations.list()));
    }

    @Test
    public void shouldPopulatePluginPackageRepositoryViewModelsOfTypePackageRpository() throws Exception {
        when(defaultPluginManager.isPluginOfType("package-repository", "plugin-id1")).thenReturn(true);
        when(defaultPluginManager.isPluginOfType("package-repository", "plugin-id2")).thenReturn(true);


        List<PluginViewModel> packageRepositoryViewModels = pluginViewModelFactory.getPluginViewModelsOfType("package-repository");
        PackageRepositoryPluginViewModel packageRepositoryPluginViewModel1 = (PackageRepositoryPluginViewModel) packageRepositoryViewModels.get(0);
        PackageRepositoryPluginViewModel packageRepositoryPluginViewModel2 = (PackageRepositoryPluginViewModel) packageRepositoryViewModels.get(1);

        assertThat(packageRepositoryPluginViewModel1.getPluginId(), is("plugin-id1"));
        assertThat(packageRepositoryPluginViewModel2.getPluginId(), is("plugin-id2"));
        assertThat(packageRepositoryPluginViewModel1.getVersion(), is("version"));
        assertThat(packageRepositoryPluginViewModel1.getPackageConfigurations(), is(packageConfigurations.list()));
        assertThat(packageRepositoryPluginViewModel1.getRepositoryConfigurations(), is(repositoryConfigurations.list()));
    }

    @Test
    public void shouldPopulatePluginTaskViewModelsOfTypeTask() throws Exception {
        when(defaultPluginManager.isPluginOfType("task", "plugin-id1")).thenReturn(true);
        PluggableTaskConfigStore.store().setPreferenceFor("plugin-id1", new TaskPreference(task));

        List<PluginViewModel> pluginViewModels = pluginViewModelFactory.getPluginViewModelsOfType("task");
        TaskPluginViewModel taskPluginViewModel = (TaskPluginViewModel) pluginViewModels.get(0);

        assertThat(taskPluginViewModel.getPluginId(), is("plugin-id1"));
        assertThat(taskPluginViewModel.getVersion(), is("version"));
        assertThat(taskPluginViewModel.getConfigurations(), is(taskConfig));
    }

    @Test
    public void shouldpopulateSCMPluginViewModelGivenPluginIdAndTypeOfPlugin() throws Exception {
        when(defaultPluginManager.isPluginOfType("scm", "plugin-id1")).thenReturn(true);

        PluginViewModel pluginViewModel = pluginViewModelFactory.getPluginViewModel("scm", "plugin-id1");
        SCMPluginViewModel scmPluginViewModel = (SCMPluginViewModel) pluginViewModel;

        assertThat(scmPluginViewModel.getPluginId(), is("plugin-id1"));
        assertThat(scmPluginViewModel.getVersion(), is("version"));
        assertThat(scmPluginViewModel.getConfigurations(), is(scmConfigurations.list()));
    }

    @Test
    public void shouldpopulatePackageRepositoryPluginViewModelGivenPluginIdAndTypeOfPlugin() throws Exception {
        when(defaultPluginManager.isPluginOfType("package-repository", "plugin-id1")).thenReturn(true);

        PluginViewModel pluginViewModel = pluginViewModelFactory.getPluginViewModel("package-repository", "plugin-id1");
        PackageRepositoryPluginViewModel packageRepositoryPluginViewModel = (PackageRepositoryPluginViewModel) pluginViewModel;

        assertThat(packageRepositoryPluginViewModel.getPluginId(), is("plugin-id1"));
        assertThat(packageRepositoryPluginViewModel.getVersion(), is("version"));
        assertThat(packageRepositoryPluginViewModel.getPackageConfigurations(), is(packageConfigurations.list()));
        assertThat(packageRepositoryPluginViewModel.getRepositoryConfigurations(), is(repositoryConfigurations.list()));
    }

    @Test
    public void shouldpopulateTaskPluginViewModelGivenPluginIdAndTypeOfPlugin() throws Exception {
        when(defaultPluginManager.isPluginOfType("task", "plugin-id")).thenReturn(true);

        PluginViewModel pluginViewModel = pluginViewModelFactory.getPluginViewModel("task", "plugin-id");
        TaskPluginViewModel taskPluginViewModel = (TaskPluginViewModel) pluginViewModel;

        assertThat(taskPluginViewModel.getPluginId(), is("plugin-id"));
        assertThat(taskPluginViewModel.getVersion(), is("version"));
        assertThat(taskPluginViewModel.getConfigurations(), is(taskConfig));
    }

    @Test
    public void shouldReturnNullPackageRepositoryPluginViewModelForInvalidPluginId() throws Exception {
        PluginViewModel pluginViewModel = pluginViewModelFactory.getPluginViewModel("package_repository", "invalid-plugin-id");
        PackageRepositoryPluginViewModel packageRepositoryPluginViewModel = (PackageRepositoryPluginViewModel) pluginViewModel;

        assertThat(packageRepositoryPluginViewModel, is(nullValue()));
    }

    @Test
    public void shouldReturnNullSCMPluginViewModelForInvalidPluginId() throws Exception {
        when(defaultPluginManager.hasReferenceFor(GoPlugin.class, "invalid-plugin-id")).thenReturn(false);
        PluginViewModel pluginViewModel = pluginViewModelFactory.getPluginViewModel("scm", "invalid-plugin-id");
        SCMPluginViewModel scmPluginViewModel = (SCMPluginViewModel) pluginViewModel;

        assertThat(scmPluginViewModel, is(nullValue()));
    }

    @Test
    public void shouldReturnNullTaskluginViewModelForInvalidPluginId() throws Exception {
        when(defaultPluginManager.hasReferenceFor((Class) anyObject(), anyString())).thenReturn(false);


        PluginViewModel pluginViewModel = pluginViewModelFactory.getPluginViewModel("task", "invalid-plugin-id");
        TaskPluginViewModel taskPluginViewModel = (TaskPluginViewModel) pluginViewModel;

        assertThat(taskPluginViewModel, is(nullValue()));
    }

    @Test
    public void shouldReturnReturnAllTypesOfPlugins() throws Exception {
        when(defaultPluginManager.isPluginOfType("scm", "plugin-id1")).thenReturn(true);
        when(defaultPluginManager.isPluginOfType("task", "plugin-id1")).thenReturn(true);
        when(defaultPluginManager.isPluginOfType("package-repository", "plugin-id1")).thenReturn(true);

        List<PluginViewModel> pluginViewModels = pluginViewModelFactory.getAllPluginViewModels();
        PackageRepositoryPluginViewModel packageRepositoryPluginViewModel = (PackageRepositoryPluginViewModel) pluginViewModels.get(0);
        TaskPluginViewModel taskPluginViewModel = (TaskPluginViewModel) pluginViewModels.get(1);
        SCMPluginViewModel scmPluginViewModel = (SCMPluginViewModel) pluginViewModels.get(2);

        assertThat(packageRepositoryPluginViewModel.getPluginId(), is("plugin-id1"));
        assertThat(taskPluginViewModel.getPluginId(), is("plugin-id1"));
        assertThat(scmPluginViewModel.getPluginId(), is("plugin-id1"));
    }

    @Test
    public void shouldReturnDisablePluginViewModelWithMessageIfPluginIsInvalid() throws Exception {

        PluginStatus pluginStatus = mock(PluginStatus.class);
        when(defaultPluginManager.getPluginDescriptorFor("invalid-plugin")).thenReturn(invalidGoPluginDescriptor);
        when(invalidGoPluginDescriptor.id()).thenReturn("invalid-plugin");
        when(invalidGoPluginDescriptor.isInvalid()).thenReturn(true);
        when(invalidGoPluginDescriptor.getStatus()).thenReturn(pluginStatus);
        List<String> messages = Arrays.asList("Invalid Plugin");
        when(pluginStatus.getMessages()).thenReturn(messages);

        List<PluginViewModel> pluginViewModels = pluginViewModelFactory.getAllPluginViewModels();
        assertThat(pluginViewModels.get(0).getPluginId(), is("invalid-plugin"));
        assertThat(pluginViewModels.get(0).getMessage(), is("[Invalid Plugin]"));
    }
}
