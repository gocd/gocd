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

package com.thoughtworks.go.plugins.presentation;

import com.google.gson.Gson;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.presentation.MissingPluggableTaskViewModel;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.task.TaskView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluggableTaskViewModelFactoryTest {
    private TaskPreference taskPreference;

    @Before
    public void setUp() throws Exception {
        cleanupTaskPreferences();

        taskPreference = mock(TaskPreference.class);
        PluggableTaskConfigStore.store().setPreferenceFor("plugin-1", taskPreference);
        TaskView view = mock(TaskView.class);
        when(taskPreference.getView()).thenReturn(view);
        when(view.template()).thenReturn("<input type='text' ng-model='abc'></input>");
        when(view.displayValue()).thenReturn("First plugin");
    }

    @After
    public void tearDown() throws Exception {
        cleanupTaskPreferences();
    }

    @Test
    public void typeForDisplayAndTemplateOfViewModelShouldBeGotFromThePlugin() throws Exception {
        PluggableTask pluggableTask = new PluggableTask("", new PluginConfiguration("plugin-1", "2"), new Configuration());
        PluggableTaskViewModelFactory factory = new PluggableTaskViewModelFactory();
        PluggableViewModel<PluggableTask> viewModel = factory.viewModelFor(pluggableTask, "new");

        assertThat(viewModel.getTypeForDisplay(), is("First plugin"));
        assertThat((String) viewModel.getParameters().get("template"), is("<input type='text' ng-model='abc'></input>"));
    }

    @Test
    public void templateShouldBeLoadedFromClasspathWithClasspathPrefix() throws Exception {
        PluggableViewModel<PluggableTask> viewModel = getModelWithTaskTemplateAt("/com/thoughtworks/go/plugins/presentation/test-template.html");
        assertThat((String) viewModel.getParameters().get("template"), is("<html>my-template</html>"));
    }

    @Test
    public void shouldReturnErrorMessageIfTemplateIsMissingFromPlugin() {
        PluggableViewModel<PluggableTask> viewModel = getModelWithTaskTemplateAt("/test-template-missing.html");
        assertThat((String) viewModel.getParameters().get("template"), is("Template \"/test-template-missing.html\" is missing."));
    }

    @Test
    public void shouldProvideATemplateWithAnErrorMessageWhenTemplateProvidedIsNull() throws Exception {
        PluggableViewModel<PluggableTask> viewModel = getModelWithTaskTemplateHavingValue(null);
        assertThat((String) viewModel.getParameters().get("template"), is("View template provided by plugin is null."));
    }

    @Test
    public void shouldProvideNoTemplateWhenTemplateProvidedIsEmpty() throws Exception {
        PluggableViewModel<PluggableTask> viewModel = getModelWithTaskTemplateHavingValue("");
        assertThat((String) viewModel.getParameters().get("template"), is(""));
    }

    @Test
    public void dataForViewShouldBeGotFromTheTaskInJSONFormat() throws Exception {
        Configuration configuration = new Configuration(create("key1", false, "value1"), create("KEY2", false, "value2"));
        PluggableTask taskConfig = new PluggableTask("", new PluginConfiguration("plugin-1", "2"), configuration);

        PluggableTaskViewModelFactory factory = new PluggableTaskViewModelFactory();
        PluggableViewModel<PluggableTask> viewModel = factory.viewModelFor(taskConfig, "new");

        String actualData = (String) viewModel.getParameters().get("data");

        Gson gson = new Gson();
        Map actual = gson.fromJson(actualData, Map.class);
        Map expected = gson.fromJson("{\"KEY2\":{\"value\":\"value2\"},\"key1\":{\"value\":\"value1\"}}", Map.class);

        assertEquals(expected, actual);
    }

    @Test
    public void dataForViewShouldIncludeErrorsIfAny() throws Exception {
        ConfigurationProperty property1 = create("key1", false, "value1");
        property1.addError("key1", "error msg");
        ConfigurationProperty property2 = create("KEY2", false, "value2");
        Configuration configuration = new Configuration(property1, property2);
        PluggableTask taskConfig = new PluggableTask("", new PluginConfiguration("plugin-1", "2"), configuration);

        PluggableTaskViewModelFactory factory = new PluggableTaskViewModelFactory();
        PluggableViewModel<PluggableTask> viewModel = factory.viewModelFor(taskConfig, "new");

        String actualData = (String) viewModel.getParameters().get("data");
        Gson gson = new Gson();
        Map actual = gson.fromJson(actualData, Map.class);
        Map expected = gson.fromJson("{\"KEY2\":{\"value\": \"value2\"},\"key1\":{\"value\" : \"value1\", \"errors\" : \"error msg\"}}", Map.class);

        assertEquals(expected, actual);
    }

    @Test
    public void shouldGivePluggableViewModelWithAllTheViewInformationForActionNew() throws Exception {
        assertPluggableViewModel("new", "admin/tasks/pluggable_task/new");
    }

    @Test
    public void shouldGivePluggableViewModelWithAllTheViewInformationForActionEdit() throws Exception {
        assertPluggableViewModel("edit", "admin/tasks/pluggable_task/edit");
    }

    @Test
    public void shouldGivePluggableViewModelWithAllTheViewInformationForActionListEntry() throws Exception {
        assertPluggableViewModel("list-entry", "admin/tasks/pluggable_task/_list_entry.html");
    }

    @Test
    public void shouldReturnMissingPluginTaskViewIfPluginIsMissing() {
        String pluginId = "pluginId";
        PluggableTaskViewModelFactory factory = new PluggableTaskViewModelFactory();
        PluggableViewModel<PluggableTask> viewModel = factory.viewModelFor(new PluggableTask("", new PluginConfiguration(pluginId, "1"), new Configuration()), "edit");
        assertThat((String) viewModel.getParameters().get("template"), is(String.format("Associated plugin '%s' not found. Please contact the Go admin to install the plugin.", pluginId)));
        assertThat(viewModel.getTypeForDisplay(), is(pluginId));
        assertThat(viewModel instanceof MissingPluggableTaskViewModel, is(true));
    }

    private void assertPluggableViewModel(String actionName, String expectedTemplatePath) {
        PluggableTask pluggableTask = new PluggableTask("", new PluginConfiguration("plugin-1", "2"), new Configuration());
        PluggableTaskViewModelFactory factory = new PluggableTaskViewModelFactory();

        PluggableViewModel<PluggableTask> viewModel = factory.viewModelFor(pluggableTask, actionName);
        assertThat(viewModel.getModel(), is(pluggableTask));
        assertThat(viewModel.getRenderingFramework(), is(Renderer.ERB));
        assertThat(viewModel.getTaskType(), is("pluggable_task_plugin_1"));
        assertThat(viewModel.getTemplatePath(), is(expectedTemplatePath));
    }

    private void cleanupTaskPreferences() {
        Set<String> plugins = PluggableTaskConfigStore.store().pluginIds();
        for (String pluginId : plugins) {
            PluggableTaskConfigStore.store().removePreferenceFor(pluginId);
        }
    }

    private PluggableViewModel<PluggableTask> getModelWithTaskTemplateAt(final String filePath) {
        return getModelWithTaskTemplateHavingValue(String.format("classpath:%s", filePath));
    }

    private PluggableViewModel<PluggableTask> getModelWithTaskTemplateHavingValue(final String templateContentValue) {
        when(taskPreference.getView()).thenReturn(new TaskView() {
            @Override
            public String displayValue() {
                return "view";
            }

            @Override
            public String template() {
                return templateContentValue;
            }
        });

        PluggableTask pluggableTask = new PluggableTask("", new PluginConfiguration("plugin-1", "2"), new Configuration());
        PluggableTaskViewModelFactory factory = new PluggableTaskViewModelFactory();
        return factory.viewModelFor(pluggableTask, "new");
    }
}
