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
package com.thoughtworks.go.server.service.tasks;


import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PluggableTaskServiceTest {
    private PluggableTaskService pluggableTaskService;
    private TaskExtension taskExtension;
    private String pluginId = "abc.def";

    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    @Before
    public void setUp() throws Exception {
        taskExtension = mock(TaskExtension.class);
        pluggableTaskService = new PluggableTaskService(taskExtension);
        final TaskPreference preference = mock(TaskPreference.class);
        final TaskConfig taskConfig = new TaskConfig();
        final TaskConfigProperty key1 = taskConfig.addProperty("KEY1");
        key1.with(Property.REQUIRED, true);
        taskConfig.addProperty("KEY2");
        when(preference.getConfig()).thenReturn(taskConfig);
        PluggableTaskConfigStore.store().setPreferenceFor(pluginId, preference);
    }

    @Test
    public void isValidShouldValidateThePluggableTask() {
        PluggableTask pluggableTask = mock(PluggableTask.class);
        PluginConfiguration pluginConfiguration = new PluginConfiguration("plugin_id", "version");

        when(pluggableTask.isValid()).thenReturn(true);
        when(pluggableTask.toTaskConfig()).thenReturn(new TaskConfig());
        when(pluggableTask.getPluginConfiguration()).thenReturn(pluginConfiguration);
        when(taskExtension.validate(any(String.class), any(TaskConfig.class))).thenReturn(new ValidationResult());

        assertTrue(pluggableTaskService.isValid(pluggableTask));
    }

    @Test
    public void isValidShouldValidateTaskAgainstPlugin() {
        TaskConfig taskConfig = mock(TaskConfig.class);
        ValidationResult validationResult = mock(ValidationResult.class);
        PluggableTask pluggableTask = mock(PluggableTask.class);
        PluginConfiguration pluginConfiguration = new PluginConfiguration("plugin_id", "version");

        when(pluggableTask.isValid()).thenReturn(true);
        when(pluggableTask.getPluginConfiguration()).thenReturn(pluginConfiguration);
        when(pluggableTask.toTaskConfig()).thenReturn(taskConfig);
        when(taskExtension.validate(pluginConfiguration.getId(), taskConfig)).thenReturn(validationResult);
        when(validationResult.isSuccessful()).thenReturn(true);

        assertTrue(pluggableTaskService.isValid(pluggableTask));
    }

    @Test
    public void isValidShouldSkipValidationAgainstPluginIfPluggableTaskIsInvalid() {
        PluggableTask pluggableTask = mock(PluggableTask.class);

        when(pluggableTask.isValid()).thenReturn(false);

        assertFalse(pluggableTaskService.isValid(pluggableTask));

        verifyZeroInteractions(taskExtension);
    }

    @Test
    public void isValidShouldMapPluginValidationErrorsToPluggableTaskConfigrations() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("plugin_id", "version");
        Configuration configuration = new Configuration();
        configuration.add(ConfigurationPropertyMother.create("source", false, "src_dir"));
        configuration.add(ConfigurationPropertyMother.create("destination", false, "dest_dir"));

        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("source", "source directory format is invalid"));
        validationResult.addError(new ValidationError("destination", "destination directory format is invalid"));

        PluggableTask pluggableTask = mock(PluggableTask.class);

        when(pluggableTask.isValid()).thenReturn(true);
        when(pluggableTask.toTaskConfig()).thenReturn(new TaskConfig());
        when(pluggableTask.getPluginConfiguration()).thenReturn(pluginConfiguration);
        when(pluggableTask.getConfiguration()).thenReturn(configuration);
        when(taskExtension.validate(any(String.class), any(TaskConfig.class))).thenReturn(validationResult);

        assertFalse(pluggableTaskService.isValid(pluggableTask));
        assertThat(configuration.getProperty("source").errors().get("source").get(0), is("source directory format is invalid"));
        assertThat(configuration.getProperty("destination").errors().get("destination").get(0), is("destination directory format is invalid"));
    }

    @Test
    public void isValidShouldMapPluginValidationErrorsToPluggableTaskForMissingConfigurations() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("plugin_id", "version");

        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("source", "source is mandatory"));

        PluggableTask pluggableTask = mock(PluggableTask.class);

        when(pluggableTask.isValid()).thenReturn(true);
        when(pluggableTask.toTaskConfig()).thenReturn(new TaskConfig());
        when(pluggableTask.getPluginConfiguration()).thenReturn(pluginConfiguration);
        when(pluggableTask.getConfiguration()).thenReturn(new Configuration());
        when(taskExtension.validate(any(String.class), any(TaskConfig.class))).thenReturn(validationResult);

        assertFalse(pluggableTaskService.isValid(pluggableTask));
        verify(pluggableTask).addError("source", "source is mandatory");
    }
}
