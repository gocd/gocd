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

package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ApiBasedTaskExtensionTest {

    private PluginManager pluginManager;
    private ApiBasedTaskExtension extension;
    private String pluginId;

    @Before
    public void setup() {
        pluginManager = mock(PluginManager.class);
        extension = new ApiBasedTaskExtension(pluginManager);
        pluginId = "plugin-id";
    }


    @Test
    public void shouldThrowExceptionForPluginSettingsConfiguration() {
        try {
            extension.getPluginSettingsConfiguration(pluginId);
            fail("should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("not implemented"));
        }
    }

    @Test
    public void shouldThrowExceptionForPluginSettingsView() {
        try {
            extension.getPluginSettingsView(pluginId);
            fail("should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("not implemented"));
        }
    }

    @Test
    public void shouldThrowExceptionForValidatePluginSettings() {
        try {
            extension.validatePluginSettings(pluginId, null);
            fail("should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("not implemented"));
        }
    }

    @Test
    public void shouldExecuteTheTask() {
        final ActionWithReturn actionWithReturn = mock(ActionWithReturn.class);
        when(pluginManager.doOn(Task.class, pluginId, actionWithReturn)).thenReturn(ExecutionResult.failure("failed"));

        ExecutionResult executionResult = extension.execute(pluginId, actionWithReturn);

        verify(pluginManager).doOn(Task.class, pluginId, actionWithReturn);
        assertThat(executionResult.getMessagesForDisplay(), is("failed"));
        assertFalse(executionResult.isSuccessful());
    }

    @Test
    public void shouldGetApiBasedTask() {
        final Action action = mock(Action.class);

        extension.doOnTask(pluginId, action);

        verify(pluginManager).doOn(Task.class, pluginId, action);
    }

    @Test
    public void shouldValidateTask() {
        final Task task = mock(Task.class);
        final TaskConfig taskConfig = mock(TaskConfig.class);
        final ValidationResult validationResult = new ValidationResult();
        when(task.validate(taskConfig)).thenReturn(validationResult);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final ActionWithReturn<Task, Object> actionWithReturn = (ActionWithReturn<Task, Object>) invocationOnMock.getArguments()[2];
                return actionWithReturn.execute(task, null);
            }
        }).when(pluginManager).doOn(eq(Task.class), eq(pluginId), any(ActionWithReturn.class));

        final ValidationResult result = extension.validate(pluginId, taskConfig);
        assertThat(result, is(validationResult));
        verify(task).validate(taskConfig);
        verify(pluginManager).doOn(eq(Task.class), eq(pluginId), any(ActionWithReturn.class));
    }
}