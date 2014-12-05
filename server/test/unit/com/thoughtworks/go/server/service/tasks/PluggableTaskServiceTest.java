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
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PluggableTaskServiceTest {
    private PluginManager pluginManager;
    private PluggableTaskService pluggableTaskService;
    private TaskExtension taskExtension;

    @Before
    public void setUp() throws Exception {
        pluginManager = mock(PluginManager.class);
        taskExtension = mock(TaskExtension.class);
        pluggableTaskService = new PluggableTaskService(pluginManager, taskExtension);
    }

    @Test
    public void shouldValidateTask() {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));
        PluggableTask modifiedTask = new PluggableTask("abc", new PluginConfiguration("abc.def", "1"), configuration);
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("KEY1", "error message"));
        when(taskExtension.validate(eq(modifiedTask.getPluginConfiguration().getId()), any(TaskConfig.class))).thenReturn(validationResult);

        pluggableTaskService.validate(modifiedTask);

        assertThat(modifiedTask.getConfiguration().getProperty("KEY1").errors().isEmpty(), is(false));
        assertThat(modifiedTask.getConfiguration().getProperty("KEY1").errors().firstError(), is("error message"));
        verify(taskExtension).validate(eq(modifiedTask.getPluginConfiguration().getId()), any(TaskConfig.class));
    }
}
