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

package com.thoughtworks.go.server.service.tasks;


import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.util.ListUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PluggableTaskServiceTest {
    private PluggableTaskService pluggableTaskService;
    private TaskExtension taskExtension;
    private String pluginId = "abc.def";
    private Localizer localizer;

    @Before
    public void setUp() throws Exception {
        taskExtension = mock(TaskExtension.class);
        localizer = mock(Localizer.class);
        pluggableTaskService = new PluggableTaskService(taskExtension, localizer);
        final TaskPreference preference = mock(TaskPreference.class);
        final TaskConfig taskConfig = new TaskConfig();
        final TaskConfigProperty key1 = taskConfig.addProperty("KEY1");
        key1.with(Property.REQUIRED, true);
        taskConfig.addProperty("KEY2");
        when(preference.getConfig()).thenReturn(taskConfig);
        PluggableTaskConfigStore.store().setPreferenceFor(pluginId, preference);
    }

    @After
    public void teardown() {
        PluggableTaskConfigStore.store().removePreferenceFor(pluginId);
    }

    @Test
    public void shouldValidateTask() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        PluggableTask modifiedTask = new PluggableTask("abc", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("KEY1", "error message"));
        when(taskExtension.validate(eq(modifiedTask.getPluginConfiguration().getId()), any(TaskConfig.class))).thenReturn(validationResult);

        pluggableTaskService.validate(modifiedTask);

        assertThat(modifiedTask.getConfiguration().getProperty("KEY1").errors().isEmpty(), is(false));
        assertThat(modifiedTask.getConfiguration().getProperty("KEY1").errors().firstError(), is("error message"));
        verify(taskExtension).validate(eq(modifiedTask.getPluginConfiguration().getId()), any(TaskConfig.class));
    }

    @Test
    public void shouldValidateMandatoryFields() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        PluggableTask modifiedTask = new PluggableTask("abc", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        when(taskExtension.validate(eq(modifiedTask.getPluginConfiguration().getId()), any(TaskConfig.class))).thenReturn(validationResult);
        when(localizer.localize("MANDATORY_CONFIGURATION_FIELD")).thenReturn("MANDATORY_CONFIGURATION_FIELD");

        pluggableTaskService.validate(modifiedTask);

        final List<ValidationError> validationErrors = validationResult.getErrors();
        assertFalse(validationErrors.isEmpty());
        final ValidationError validationError = ListUtil.find(validationErrors, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                return ((ValidationError) item).getKey().equals("KEY1");
            }
        });
        assertNotNull(validationError);
        assertThat(validationError.getMessage(), is("MANDATORY_CONFIGURATION_FIELD"));
    }

    @Test
    public void shouldPassValidationIfAllRequiredFieldsHaveValues() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        configuration.getProperty("KEY1").setConfigurationValue(new ConfigurationValue("junk"));
        PluggableTask modifiedTask = new PluggableTask("abc", new PluginConfiguration(pluginId, "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        when(taskExtension.validate(eq(modifiedTask.getPluginConfiguration().getId()), any(TaskConfig.class))).thenReturn(validationResult);

        pluggableTaskService.validate(modifiedTask);

        final List<ValidationError> validationErrors = validationResult.getErrors();
        assertTrue(validationErrors.isEmpty());
    }
}
