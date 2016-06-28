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

package com.thoughtworks.go.config.registry;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.Artifact;
import com.thoughtworks.go.domain.BuildOutputMatcher;
import com.thoughtworks.go.domain.OutputMatcher;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.plugins.PluginExtensions;
import com.thoughtworks.go.plugins.presentation.PluggableViewModel;
import com.thoughtworks.go.plugins.presentation.Renderer;
import com.thoughtworks.go.presentation.PluggableTaskViewModel;
import com.thoughtworks.go.presentation.TaskViewModel;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ConfigElementImplementationRegistrarTest {
    @Mock private PluginExtensions pluginExtns;
    @Mock private PluginManager pluginManager;

    private ConfigElementImplementationRegistry registry;
    private ConfigElementImplementationRegistrar registrar;

    @Before
    public void setUp() {
        initMocks(this);

        when(pluginExtns.configTagImplementations()).thenReturn(new ArrayList<ConfigurationExtension>());

        registry = new ConfigElementImplementationRegistry(pluginExtns);
        registrar = new ConfigElementImplementationRegistrar(pluginManager, registry);

        registrar.initialize();
    }

    @Test
    public void testShouldProvideTheDefaultTaskConfigMappingsOnlyForBuiltInTasks() throws Exception {
        List<Class<? extends Task>> tasks = new ArrayList<Class<? extends Task>>();
        tasks.add(AntTask.class);
        tasks.add(NantTask.class);
        tasks.add(ExecTask.class);
        tasks.add(RakeTask.class);
        tasks.add(FetchTask.class);
        tasks.add(PluggableTask.class);

        assertThat(registry.implementersOf(Task.class), is(tasks));
    }

    @Test
    public void testShouldProvideTheDefaultMaterialConfigMappings() throws Exception {
        List<Class<? extends MaterialConfig>> materials = new ArrayList<Class<? extends MaterialConfig>>();
        materials.add(SvnMaterialConfig.class);
        materials.add(HgMaterialConfig.class);
        materials.add(GitMaterialConfig.class);
        materials.add(DependencyMaterialConfig.class);
        materials.add(P4MaterialConfig.class);
        materials.add(TfsMaterialConfig.class);
        materials.add(PackageMaterialConfig.class);
        materials.add(PluggableSCMMaterialConfig.class);

        assertThat(registry.implementersOf(MaterialConfig.class), is(materials));
    }

    @Test
    public void testShouldProvideTheDefaultArtifactsConfigMappings() throws Exception {
        List<Class<? extends Artifact>> artifacts = new ArrayList<Class<? extends Artifact>>();
        artifacts.add(TestArtifactPlan.class);
        artifacts.add(ArtifactPlan.class);

        assertThat(registry.implementersOf(Artifact.class), is(artifacts));
    }

    @Test
    public void testShouldProvideTheDefaultOutputMatcherConfigMappings() throws Exception {
        List<Class<? extends OutputMatcher>> outputMatchers = new ArrayList<Class<? extends OutputMatcher>>();
        outputMatchers.add(BuildOutputMatcher.class);

        assertThat(registry.implementersOf(OutputMatcher.class), is(outputMatchers));
    }

    @Test
    public void testShouldProvideTheDefaultAdminConfigMappings() throws Exception {
        List<Class<? extends Admin>> admin = new ArrayList<Class<? extends Admin>>();
        admin.add(AdminUser.class);
        admin.add(AdminRole.class);

        assertThat(registry.implementersOf(Admin.class), is(admin));
    }

    @Test
    public void shouldRegisterViewEnginesForAllTasks() throws Exception {
        assertReturnsAppropriateViewModelForInbuiltTasks(registry, new AntTask(), "ant");
        assertReturnsAppropriateViewModelForInbuiltTasks(registry, new ExecTask(), "exec");
        assertReturnsAppropriateViewModelForInbuiltTasks(registry, new FetchTask(), "fetch");
        assertReturnsAppropriateViewModelForInbuiltTasks(registry, new RakeTask(), "rake");
        assertReturnsAppropriateViewModelForInbuiltTasks(registry, new NantTask(), "nant");
    }

    @Test
    public void shouldRegisterViewEngineForPluggableTask() throws Exception {
        TaskPreference taskPreference = mock(TaskPreference.class);
        TaskView view = mock(TaskView.class);
        when(taskPreference.getView()).thenReturn(view);
        when(view.template()).thenReturn("plugin-template-value");
        when(view.displayValue()).thenReturn("Plugin display value");
        PluggableTaskConfigStore.store().setPreferenceFor("plugin1", taskPreference);

        PluggableTask pluggableTask = new PluggableTask(new PluginConfiguration("plugin1", "2"), new Configuration());
        PluggableViewModel<PluggableTask> pluggableTaskViewModel = registry.getViewModelFor(pluggableTask, "new");

        assertEquals(PluggableTaskViewModel.class, pluggableTaskViewModel.getClass());
        assertThat(pluggableTaskViewModel.getModel(), is(pluggableTask));
    }

    private void assertReturnsAppropriateViewModelForInbuiltTasks(ConfigElementImplementationRegistry registry, Task task, final String taskType) {
        for (String actionName : new String[]{"new", "edit"}) {
            PluggableViewModel viewModelFor = registry.getViewModelFor(task, actionName);
            assertThat(viewModelFor, is((PluggableViewModel) new TaskViewModel(task, String.format("admin/tasks/%s/%s", taskType, actionName), Renderer.ERB)));
        }
    }

}
