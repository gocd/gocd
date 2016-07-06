/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.service.tasks;

import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.FetchTask;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugins.presentation.PluggableViewModel;
import com.thoughtworks.go.plugins.presentation.Renderer;
import com.thoughtworks.go.presentation.TaskViewModel;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.util.DataStructureUtils.s;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class TaskViewServiceTest {
    private ConfigElementImplementationRegistry registry;
    private TaskViewService taskViewService;
    private PluginManager pluginManager;

    @Before
    public void setUp() {
        cleanupTaskPreferences();
        registry = mock(ConfigElementImplementationRegistry.class);
        pluginManager = mock(PluginManager.class);
        taskViewService = new TaskViewService(registry, pluginManager);
    }

    @After
    public void tearDown() throws Exception {
        cleanupTaskPreferences();
    }

    private void cleanupTaskPreferences() {
        Set<String> plugins = PluggableTaskConfigStore.store().pluginIds();
        for (String pluginId : plugins) {
            PluggableTaskConfigStore.store().removePreferenceFor(pluginId);
        }
    }

    @Test
    public void shouldGetViewModelsForBuiltinTasks() {
        List<Class<? extends Task>> taskClasses = taskImplementations();

        when(registry.implementersOf(Task.class)).thenReturn(taskClasses);
        when(registry.getViewModelFor(new AntTask(), "new")).thenReturn(viewModel(new AntTask()));
        when(registry.getViewModelFor(new ExecTask(), "new")).thenReturn(new TaskViewModel(new ExecTask(), "", Renderer.ERB));

        List<PluggableViewModel> taskViewModels = taskViewService.getTaskViewModels();

        assertThat(taskViewModels.size(), is(3));
        assertThat(taskViewModels, hasItem((PluggableViewModel) viewModel(new AntTask())));
        assertThat(taskViewModels, hasItem((PluggableViewModel) new TaskViewModel(new ExecTask(), "", Renderer.ERB)));
    }

    private void storeTaskPreferences(String pluginId, String... properties) {
        FakeTask task = new FakeTask(properties);
        PluggableTaskConfigStore.store().setPreferenceFor(pluginId, new TaskPreference(task));
    }

    private void storeTaskPreferences(String pluginId, Property... properties) {
        FakeTask task = new FakeTask(properties);
        PluggableTaskConfigStore.store().setPreferenceFor(pluginId, new TaskPreference(task));
    }

    @Test
    public void shouldGetViewModelsForPluggedInTasks_ButOnlyForExistingPlugins() throws Exception {
        String plugin1 = "task-plugin-1";
        String plugin2 = "task-plugin-2";
        when(pluginManager.getPluginDescriptorFor(plugin1)).thenReturn(new GoPluginDescriptor(plugin1, "1", null, null, null, false));
        when(pluginManager.getPluginDescriptorFor(plugin2)).thenReturn(new GoPluginDescriptor(plugin2, "1", null, null, null, false));
        storeTaskPreferences(plugin1, "key_1", "key_2");
        storeTaskPreferences(plugin2, "key_3", "key_4");
        when(registry.implementersOf(Task.class)).thenReturn(Arrays.<Class<? extends Task>>asList(ExecTask.class, PluggableTask.class));

        PluggableTask expectedPluggableTaskForPlugin1 = new PluggableTask(new PluginConfiguration(plugin1, "1"), new Configuration(create("key_1"), create("key_2")));
        PluggableTask expectedPluggableTaskForPlugin2 = new PluggableTask(new PluginConfiguration(plugin2, "1"), new Configuration(create("key_3"), create("key_4")));

        when(registry.getViewModelFor(new ExecTask(), "new")).thenReturn(viewModel(new ExecTask()));
        when(registry.getViewModelFor(expectedPluggableTaskForPlugin1, "new")).thenReturn(viewModel(expectedPluggableTaskForPlugin1));
        when(registry.getViewModelFor(expectedPluggableTaskForPlugin2, "new")).thenReturn(viewModel(expectedPluggableTaskForPlugin2));

        List<PluggableViewModel> viewModels = taskViewService.getTaskViewModels();

        assertThat(viewModels.size(), is(3));
        assertThat(viewModels.get(0).getModel(), instanceOf(ExecTask.class));
        assertThat(viewModels.get(1).getModel(), instanceOf(PluggableTask.class));
        assertThat(viewModels.get(2).getModel(), instanceOf(PluggableTask.class));

        assertThat((PluggableTask) viewModels.get(1).getModel(), is(expectedPluggableTaskForPlugin1));
        assertThat((PluggableTask) viewModels.get(2).getModel(), is(expectedPluggableTaskForPlugin2));

        verify(registry).getViewModelFor(expectedPluggableTaskForPlugin1, "new");
        verify(registry).getViewModelFor(expectedPluggableTaskForPlugin2, "new");
    }

    @Test
    public void shouldStoreDefaultValuesGivenForPropertiesInAPluginWhenInitializingANewTaskPlugin() throws Exception {
        String plugin = "task-plugin";
        when(pluginManager.getPluginDescriptorFor(plugin)).thenReturn(new GoPluginDescriptor(plugin, "1", null, null, null, false));
        Property taskConfigProperty1 = new TaskConfigProperty("key1", null);
        Property taskConfigProperty2 = new TaskConfigProperty("key2", null);
        Property taskConfigProperty3 = new TaskConfigProperty("key3", null);
        storeTaskPreferences(plugin, taskConfigProperty1.withDefault("default1"), taskConfigProperty2.withDefault("default2"), taskConfigProperty3);
        when(registry.implementersOf(Task.class)).thenReturn(Arrays.<Class<? extends Task>>asList(PluggableTask.class));

        PluggableTask pluggableTask = (PluggableTask) taskViewService.taskInstanceFor(new PluggableTask(new PluginConfiguration(plugin, "1"), new Configuration()).getTaskType());
        assertThat(pluggableTask.getConfiguration().getProperty("key1").getValue(), is("default1"));
        assertThat(pluggableTask.getConfiguration().getProperty("key2").getValue(), is("default2"));
        assertNull(pluggableTask.getConfiguration().getProperty("key3").getValue());
    }

    @Test
    public void shouldFetchPluggableTasksWithSecureConfigurations() throws Exception {
        String plugin = "task-plugin";
        when(pluginManager.getPluginDescriptorFor(plugin)).thenReturn(new GoPluginDescriptor(plugin, "1", null, null, null, false));
        Property taskConfigProperty = new TaskConfigProperty("key1", null).with(Property.SECURE, true);

        storeTaskPreferences(plugin, taskConfigProperty);
        when(registry.implementersOf(Task.class)).thenReturn(Arrays.<Class<? extends Task>>asList(PluggableTask.class));

        PluggableTask pluggableTask = (PluggableTask) taskViewService.taskInstanceFor(new PluggableTask(new PluginConfiguration(plugin, "1"), null).getTaskType());
        assertTrue(pluggableTask.getConfiguration().first().isSecure());
    }

    @Test
    public void shouldGetViewModelsOnlyForBuiltInTasks_WhenThereAreNoExistingPlugins() throws Exception {
        List<Class<? extends Task>> taskClasses = taskImplementations();
        taskClasses.add(PluggableTask.class);

        when(registry.implementersOf(Task.class)).thenReturn(taskClasses);
        when(registry.getViewModelFor(new AntTask(), "new")).thenReturn(viewModel(new AntTask()));
        when(registry.getViewModelFor(new ExecTask(), "new")).thenReturn(new TaskViewModel(new ExecTask(), "", Renderer.ERB));

        List<PluggableViewModel> taskViewModels = taskViewService.getTaskViewModels();

        assertThat(taskViewModels.size(), is(3));
        assertThat(taskViewModels, hasItem((PluggableViewModel) viewModel(new AntTask())));
        assertThat(taskViewModels, hasItem((PluggableViewModel) new TaskViewModel(new ExecTask(), "", Renderer.ERB)));
    }

    @Test
    public void shouldThrowAnExceptionIfTheTaskOfGivenTypeIsNotFound() {
        ConfigElementImplementationRegistry registry = this.registry;
        List<Class<? extends Task>> taskClasses = new ArrayList<Class<? extends Task>>();
        taskClasses.add(AntTask.class);
        when(registry.implementersOf(Task.class)).thenReturn(taskClasses);

        TaskViewService service = new TaskViewService(registry, mock(PluginManager.class));
        try {
            service.taskInstanceFor("Unknown");
            fail("Should have failed since the given task is not available in the registry");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Could not find any task of type: Unknown"));
        }
    }

    @Test
    public void shouldGetTaskInstanceForAType() throws Exception {
        ConfigElementImplementationRegistry registry = this.registry;
        when(registry.implementersOf(Task.class)).thenReturn(taskImplementations());
        TaskViewService taskViewService = new TaskViewService(registry, mock(PluginManager.class));
        assertThat((AntTask) taskViewService.taskInstanceFor(new AntTask().getTaskType()), is(new AntTask()));
    }

    @Test
    public void shouldGetListOfOnCancelTaskViewModels() {
        ConfigElementImplementationRegistry registry = this.registry;
        when(registry.implementersOf(Task.class)).thenReturn(taskImplementations());

        AntTask ant = new AntTask();
        FetchTask fetch = new FetchTask();
        ExecTask exec = new ExecTask();
        when(registry.getViewModelFor(ant, "new")).thenReturn(viewModel(ant));
        when(registry.getViewModelFor(fetch, "new")).thenReturn(viewModel(fetch));
        when(registry.getViewModelFor(exec, "new")).thenReturn(viewModel(exec));

        TaskViewService taskViewService = new TaskViewService(registry, mock(PluginManager.class));
        List<PluggableViewModel<Task>> onCancelTaskViewModels = taskViewService.getOnCancelTaskViewModels(new AntTask());

        assertThat(onCancelTaskViewModels, is(asList(viewModel(ant), viewModel(exec), viewModel(fetch))));
    }

    @Test
    public void getOnCancelTaskViewModels_shouldUsePassedInTaskOnCancelIfAvailable() {
        when(registry.implementersOf(Task.class)).thenReturn(taskImplementations());

        AntTask ant = new AntTask();
        FetchTask fetch = new FetchTask();
        AntTask given = new AntTask();
        ExecTask cancelExec = new ExecTask("ls", "-la", ".");
        given.setCancelTask(cancelExec);

        when(registry.getViewModelFor(ant, "new")).thenReturn(viewModel(ant));
        when(registry.getViewModelFor(fetch, "new")).thenReturn(viewModel(fetch));
        when(registry.getViewModelFor(cancelExec, "edit")).thenReturn(viewModel(cancelExec));

        List<PluggableViewModel<Task>> onCancelTaskViewModels = new TaskViewService(registry, mock(PluginManager.class)).getOnCancelTaskViewModels(given);

        assertThat(onCancelTaskViewModels, is(asList(viewModel(ant), viewModel(cancelExec), viewModel(fetch))));
    }

    @Test
    public void shouldCreateTaskViewModelsGivenATask() {
        when(registry.implementersOf(Task.class)).thenReturn(taskImplementations());

        ExecTask given = new ExecTask("mkdir", "foo", "work");
        AntTask ant = new AntTask();
        FetchTask fetch = new FetchTask();
        when(registry.getViewModelFor(ant, "new")).thenReturn(viewModel(ant));
        when(registry.getViewModelFor(fetch, "new")).thenReturn(viewModel(fetch));
        when(registry.getViewModelFor(given, "new")).thenReturn(viewModel(given));

        List<PluggableViewModel> viewModels = taskViewService.getTaskViewModelsWith(given);
        assertThat(viewModels.size(), is(3));
        assertThat(viewModels.contains(viewModel(ant)), is(true));
        assertThat(viewModels.contains(viewModel(fetch)), is(true));
        assertThat(viewModels.contains(viewModel(given)), is(true));
    }

    @Test
    public void shouldNotThrowNullPointerExceptionWhenPluginDescriptorOrTaskPreferenceBecomesNullDueToRaceCondition() throws Exception {
        PluggableTaskConfigStore pluggableTaskConfigStore = mock(PluggableTaskConfigStore.class);
        Object original = ReflectionUtil.getStaticField(PluggableTaskConfigStore.class, "PLUGGABLE_TASK_CONFIG_STORE");
        try {
            ReflectionUtil.setStaticField(PluggableTaskConfigStore.class, "PLUGGABLE_TASK_CONFIG_STORE", pluggableTaskConfigStore);

            String pluginId = "some.plugin.id";
            when(pluggableTaskConfigStore.pluginIds()).thenReturn(s(pluginId));
            when(pluginManager.getPluginDescriptorFor(pluginId)).thenReturn(null);

            TaskViewService taskViewService = new TaskViewService(registry, pluginManager);
            taskViewService.getTaskViewModelsWith(mock(Task.class));
        } finally {
            ReflectionUtil.setStaticField(PluggableTaskConfigStore.class, "PLUGGABLE_TASK_CONFIG_STORE", original);
        }
    }

    private List<Class<? extends Task>> taskImplementations() {
        List<Class<? extends Task>> taskClasses = new ArrayList<Class<? extends Task>>();
        taskClasses.add(AntTask.class);
        taskClasses.add(ExecTask.class);
        taskClasses.add(FetchTask.class);
        return taskClasses;
    }

    private PluggableViewModel<Task> viewModel(final Task task) {
        return new TaskViewModel(task, "", Renderer.ERB);
    }

    private class FakeTask implements com.thoughtworks.go.plugin.api.task.Task {
        final Property[] properties;

        private FakeTask(String... properties) {
            this.properties = convertToProperties(properties).toArray(new Property[]{});
        }

        private FakeTask(Property... properties) {
            this.properties = properties;
        }

        private List<TaskConfigProperty> convertToProperties(String[] properties) {
            ArrayList<TaskConfigProperty> taskConfigProperties = new ArrayList<TaskConfigProperty>();
            for (String property : properties) {
                taskConfigProperties.add(new TaskConfigProperty(property, null));
            }
            return taskConfigProperties;
        }

        @Override
        public TaskConfig config() {
            TaskConfig taskConfig = new TaskConfig();
            for (Property property : properties) {
                taskConfig.add(property);
            }
            return taskConfig;
        }

        @Override
        public TaskExecutor executor() {
            return null;
        }

        @Override
        public TaskView view() {
            return null;
        }

        @Override
        public ValidationResult validate(TaskConfig configuration) {
            return null;
        }
    }
}
