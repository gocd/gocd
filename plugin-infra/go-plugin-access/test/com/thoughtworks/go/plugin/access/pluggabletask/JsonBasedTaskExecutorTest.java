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

import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JsonBasedTaskExecutorTest {
    @Test
    public void shouldExecuteAndReturnSuccessfulExecutionResultTaskThroughPlugin() {
        TaskExecutionContext context = mock(TaskExecutionContext.class);
        PluginManager pluginManager = mock(PluginManager.class);
        String pluginId = "pluginId";
        GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(pluginManager.submitTo(eq(pluginId), any(GoPluginApiRequest.class))).thenReturn(response);
        when(response.responseBody()).thenReturn("{\"success\":true,\"messages\":[\"message1\",\"message2\"]}");
        final JsonBasedTaskExtensionHandler_V1 handler = new JsonBasedTaskExtensionHandler_V1();
        ExecutionResult result = new JsonBasedTaskExecutor(pluginId, pluginManager, handler).execute(config(), context);
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.getMessagesForDisplay(), is("message1\nmessage2"));

        ArgumentCaptor<GoPluginApiRequest> argument = ArgumentCaptor.forClass(GoPluginApiRequest.class);
        verify(pluginManager).submitTo(eq(pluginId), argument.capture());
        assertThat(argument.getValue().extension(), is(JsonBasedTaskExtension.TASK_EXTENSION));
        assertThat(argument.getValue().extensionVersion(), is(handler.version()));
        assertThat(argument.getValue().requestName(), is(JsonBasedTaskExtension.EXECUTION_REQUEST));
    }

    @Test
    public void shouldExecuteAndReturnFailureExecutionResultTaskThroughPlugin() {
        TaskExecutionContext context = mock(TaskExecutionContext.class);
        PluginManager pluginManager = mock(PluginManager.class);
        String pluginId = "pluginId";
        GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(pluginManager.submitTo(eq(pluginId), any(GoPluginApiRequest.class))).thenReturn(response);
        when(response.responseBody()).thenReturn("{\"success\":false,\"messages\":[\"error1\",\"error2\"]}");
        ExecutionResult result = new JsonBasedTaskExecutor(pluginId, pluginManager, new JsonBasedTaskExtensionHandler_V1()).execute(config(), context);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.getMessagesForDisplay(), is("error1\nerror2"));
    }

    private TaskConfig config() {
        TaskConfig taskConfig = new TaskConfig();
        TaskConfigProperty p1 = new TaskConfigProperty("k1", "value1");
        p1.with(Property.DISPLAY_ORDER, 10);
        p1.with(Property.SECURE, true);
        p1.with(Property.DISPLAY_NAME, "display name for k1");
        p1.with(Property.REQUIRED, true);
        TaskConfigProperty p2 = new TaskConfigProperty("k2", "value1");
        p2.with(Property.DISPLAY_ORDER, 1);
        p2.with(Property.SECURE, false);
        p2.with(Property.DISPLAY_NAME, "display name for k2");
        p2.with(Property.REQUIRED, true);
        p2.with(Property.REQUIRED, true);
        taskConfig.add(p1);
        taskConfig.add(p2);
        return taskConfig;
    }
}
