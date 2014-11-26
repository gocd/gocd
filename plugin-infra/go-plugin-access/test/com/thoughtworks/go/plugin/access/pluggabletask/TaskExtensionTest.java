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
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static junitx.framework.Assert.fail;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TaskExtensionTest {
    @Test
    public void shouldReturnCorrectTaskExtensionImplForAPIBasedTaskPlugin() {
        PluginManager pluginManager = mock(PluginManager.class);
        String apiBasedPluginId = "APi-task";
        TaskExtension taskExtension = new TaskExtension(pluginManager);

        when(pluginManager.hasReferenceFor(Task.class, apiBasedPluginId)).thenReturn(true);

        assertTrue(taskExtension.getExtension(apiBasedPluginId) instanceof ApiBasedTaskExtension);
    }

    @Test
    public void shouldReturnMessageBasedTaskExtensionForMessageBasedTaskPlugin() {
        PluginManager pluginManager = mock(PluginManager.class);
        String messageBasedPluginId = "messageBased-task";
        TaskExtension taskExtension = new TaskExtension(pluginManager);

        when(pluginManager.hasReferenceFor(Task.class, messageBasedPluginId)).thenReturn(false);
        when(pluginManager.hasReferenceFor(PluggableJsonBasedTask.TASK_EXTENSION, messageBasedPluginId)).thenReturn(true);

        Assert.assertTrue(taskExtension.getExtension(messageBasedPluginId) instanceof JsonBasedTaskExtension);
    }

    @Test
    public void shouldThrowExceptionIfPluginDoesNotImplementEitherMessageOrApiBasedExtension() {
        PluginManager pluginManager = mock(PluginManager.class);
        String pluginId = "messageBased-task";
        when(pluginManager.hasReferenceFor(Task.class, pluginId)).thenReturn(false);
        when(pluginManager.hasReferenceFor(PluggableJsonBasedTask.TASK_EXTENSION, pluginId)).thenReturn(false);

        TaskExtension taskExtension = new TaskExtension(pluginManager);
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

        PluginManager pluginManager = mock(PluginManager.class);
        String messageBasedPluginId = "messageBased-task";
        TaskExtension taskExtension = new TaskExtension(pluginManager);
        when(pluginManager.hasReferenceFor(Task.class, messageBasedPluginId)).thenReturn(false);
        when(pluginManager.hasReferenceFor(PluggableJsonBasedTask.TASK_EXTENSION, messageBasedPluginId)).thenReturn(true);

        taskExtension.execute(messageBasedPluginId, new ActionWithReturn<Task, ExecutionResult>() {
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
        PluginManager pluginManager = mock(PluginManager.class);
        String pluginId = "APIBased-task";
        TaskExtension taskExtension = new TaskExtension(pluginManager);
        when(pluginManager.hasReferenceFor(Task.class, pluginId)).thenReturn(true);
        when(pluginManager.hasReferenceFor(PluggableJsonBasedTask.TASK_EXTENSION, pluginId)).thenReturn(false);
        ActionWithReturn actionWithReturn = mock(ActionWithReturn.class);
        when(pluginManager.doOn(Task.class, pluginId, actionWithReturn)).thenReturn(ExecutionResult.success("success"));

        ExecutionResult result = taskExtension.execute(pluginId, actionWithReturn);

        verify(pluginManager).doOn(Task.class, pluginId, actionWithReturn);
        assertThat(result.getMessagesForDisplay(), is("success"));
    }

    @Test
    public void shouldDoOnTask() {
        final TaskExtension taskExtension = spy(new TaskExtension(mock(PluginManager.class)));
        final TaskExtensionContract actualImpl = mock(TaskExtensionContract.class);

        final String pluginId = "pluginId";
        doReturn(actualImpl).when(taskExtension).getExtension(pluginId);

        final Action action = mock(Action.class);
        taskExtension.doOnTask(pluginId, action);

        verify(actualImpl).doOnTask(pluginId, action);
    }
}