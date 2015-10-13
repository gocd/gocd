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

import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class TaskExtensionTest {
    private PluginManager pluginManager;
    private TaskExtension taskExtension;
    private final String messageBasedPlugin = "messageBased-task";
    private final String apiBasedPlugin = "APi-task";

    @Before
    public void setUp() throws Exception {
        pluginManager = mock(PluginManager.class);
        taskExtension = new TaskExtension(pluginManager);
        when(pluginManager.getPluginDescriptorFor(apiBasedPlugin)).thenReturn(new GoPluginDescriptor(apiBasedPlugin, null, null, null, null, false));
        when(pluginManager.getPluginDescriptorFor(messageBasedPlugin)).thenReturn(new GoPluginDescriptor(messageBasedPlugin, null, null, null, null, false));
    }

    @Test
    public void shouldReportIfThePluginIsMissing() {
        try {
            taskExtension.execute("junk", null);
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Associated plugin 'junk' not found. Please contact the Go admin to install the plugin."));
        }
        try {
            taskExtension.validate("junk", null);
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Associated plugin 'junk' not found. Please contact the Go admin to install the plugin."));
        }
    }

    @Test
    public void shouldReportIfThePluginDoesNotImplementEitherTypeOfPluginExtension() {
        try {
            taskExtension.doOnTask("junk", null);
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Plugin should use either message-based or api-based extension. Plugin-id: junk"));
        }
    }

    @Test
    public void shouldReturnCorrectTaskExtensionImplForAPIBasedTaskPlugin() {
        when(pluginManager.hasReferenceFor(Task.class, apiBasedPlugin)).thenReturn(true);

        assertTrue(taskExtension.getExtension(apiBasedPlugin) instanceof ApiBasedTaskExtension);
    }

    @Test
    public void shouldReturnMessageBasedTaskExtensionForMessageBasedTaskPlugin() {
        when(pluginManager.hasReferenceFor(Task.class, messageBasedPlugin)).thenReturn(false);
        when(pluginManager.isPluginOfType(JsonBasedTaskExtension.TASK_EXTENSION, messageBasedPlugin)).thenReturn(true);

        assertTrue(taskExtension.getExtension(messageBasedPlugin) instanceof JsonBasedTaskExtension);
    }

    @Test
    public void shouldThrowExceptionIfPluginDoesNotImplementEitherMessageOrApiBasedExtension() {
        String pluginId = "messageBased-task";
        when(pluginManager.hasReferenceFor(Task.class, pluginId)).thenReturn(false);
        when(pluginManager.isPluginOfType(JsonBasedTaskExtension.TASK_EXTENSION, pluginId)).thenReturn(false);

        try {
            taskExtension.getExtension(pluginId);
            fail("Should throw exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().equals("Plugin should use either message-based or api-based extension. Plugin-id: " + pluginId));
        }
    }

    @Test
    public void shouldExecuteTheGivenMessageBasedTaskPlugin() {
        final Boolean[] executed = {false};

        when(pluginManager.hasReferenceFor(Task.class, messageBasedPlugin)).thenReturn(false);
        when(pluginManager.isPluginOfType(JsonBasedTaskExtension.TASK_EXTENSION, messageBasedPlugin)).thenReturn(true);

        taskExtension.execute(messageBasedPlugin, new ActionWithReturn<Task, ExecutionResult>() {
            @Override
            public ExecutionResult execute(Task task, GoPluginDescriptor pluginDescriptor) {
                executed[0] = true;
                return null;
            }
        });

        assertTrue(executed[0]);
    }

    @Test
    public void shouldExecuteTheGivenAPIBasedTaskPlugin() {
        String pluginId = "APi-task";
        when(pluginManager.hasReferenceFor(Task.class, pluginId)).thenReturn(true);
        when(pluginManager.isPluginOfType(JsonBasedTaskExtension.TASK_EXTENSION, pluginId)).thenReturn(false);
        ActionWithReturn actionWithReturn = mock(ActionWithReturn.class);
        when(pluginManager.doOn(Task.class, pluginId, actionWithReturn)).thenReturn(ExecutionResult.success("success"));

        ExecutionResult result = taskExtension.execute(pluginId, actionWithReturn);

        verify(pluginManager).doOn(Task.class, pluginId, actionWithReturn);
        assertThat(result.getMessagesForDisplay(), is("success"));
    }

    @Test
    public void shouldGetPluginSettingsConfiguration() {
        TaskExtension taskExtension = spy(this.taskExtension);
        TaskExtensionContract actualImpl = mock(TaskExtensionContract.class);

        String pluginId = "pluginId";
        doReturn(actualImpl).when(taskExtension).getExtension(pluginId);

        taskExtension.getPluginSettingsConfiguration(pluginId);

        verify(actualImpl).getPluginSettingsConfiguration(pluginId);
    }

    @Test
    public void shouldGetPluginSettingsView() {
        TaskExtension taskExtension = spy(this.taskExtension);
        TaskExtensionContract actualImpl = mock(TaskExtensionContract.class);

        String pluginId = "pluginId";
        doReturn(actualImpl).when(taskExtension).getExtension(pluginId);

        taskExtension.getPluginSettingsView(pluginId);

        verify(actualImpl).getPluginSettingsView(pluginId);
    }

    @Test
    public void shouldValidatePluginSettings() {
        TaskExtension taskExtension = spy(this.taskExtension);
        TaskExtensionContract actualImpl = mock(TaskExtensionContract.class);

        String pluginId = "pluginId";
        doReturn(actualImpl).when(taskExtension).getExtension(pluginId);

        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        taskExtension.validatePluginSettings(pluginId, configuration);

        verify(actualImpl).validatePluginSettings(pluginId, configuration);
    }

    @Test
    public void shouldDoOnTask() {
        TaskExtension taskExtension = spy(this.taskExtension);
        TaskExtensionContract actualImpl = mock(TaskExtensionContract.class);

        String pluginId = "pluginId";
        doReturn(actualImpl).when(taskExtension).getTaskExtensionContract(pluginId);

        Action action = mock(Action.class);
        taskExtension.doOnTask(pluginId, action);

        verify(actualImpl).doOnTask(pluginId, action);
    }

    @Test
    public void shouldValidateTask() {
        TaskExtension taskExtension = spy(this.taskExtension);
        TaskExtensionContract actualImpl = mock(TaskExtensionContract.class);
        String pluginId = "pluginId";
        TaskConfig taskConfig = mock(TaskConfig.class);
        doReturn(actualImpl).when(taskExtension).getExtension(pluginId);

        taskExtension.validate(pluginId, taskConfig);

        verify(actualImpl).validate(pluginId, taskConfig);
    }
}
