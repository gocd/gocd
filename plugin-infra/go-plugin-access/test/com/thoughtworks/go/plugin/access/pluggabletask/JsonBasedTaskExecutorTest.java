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

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.task.*;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class JsonBasedTaskExecutorTest {

    private TaskExecutionContext context;
    private PluginManager pluginManager;
    private String pluginId;
    private GoPluginApiResponse response;
    private JsonBasedTaskExtensionHandler handler;

    @Before
    public void setup() {
        context = mock(TaskExecutionContext.class);
        pluginManager = mock(PluginManager.class);
        pluginId = "pluginId";
        response = mock(GoPluginApiResponse.class);
        handler = mock(JsonBasedTaskExtensionHandler.class);
    }

    @Test
    public void shouldExecuteAndReturnSuccessfulExecutionResultTaskThroughPlugin() {
        when(pluginManager.submitTo(eq(pluginId), any(GoPluginApiRequest.class))).thenReturn(response);
        when(handler.toExecutionResult(response)).thenReturn(ExecutionResult.success("message1"));

        ExecutionResult result = new JsonBasedTaskExecutor(pluginId, pluginManager, handler).execute(config(), context);

        assertThat(result.isSuccessful(), is(true));
        assertThat(result.getMessagesForDisplay(), is("message1"));

        ArgumentCaptor<GoPluginApiRequest> argument = ArgumentCaptor.forClass(GoPluginApiRequest.class);
        verify(pluginManager).submitTo(eq(pluginId), argument.capture());
        assertThat(argument.getValue().extension(), is(JsonBasedTaskExtension.TASK_EXTENSION));
        assertThat(argument.getValue().extensionVersion(), is(handler.version()));
        assertThat(argument.getValue().requestName(), is(JsonBasedTaskExtension.EXECUTION_REQUEST));
    }

    @Test
    public void shouldExecuteAndReturnFailureExecutionResultTaskThroughPlugin() {
        when(pluginManager.submitTo(eq(pluginId), any(GoPluginApiRequest.class))).thenReturn(response);
        when(handler.toExecutionResult(response)).thenReturn(ExecutionResult.failure("error1"));

        ExecutionResult result = new JsonBasedTaskExecutor(pluginId, pluginManager, handler).execute(config(), context);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.getMessagesForDisplay(), is("error1"));
    }

    @Test
    public void shouldConstructExecutionRequestWithRequiredDetails() {
        String workingDir = "working-dir";
        com.thoughtworks.go.plugin.api.task.Console console = mock(com.thoughtworks.go.plugin.api.task.Console.class);
        when(context.workingDir()).thenReturn(workingDir);
        EnvironmentVariables environment = getEnvironmentVariables();
        when(context.environment()).thenReturn(environment);
        when(context.console()).thenReturn(console);
        final GoPluginApiRequest[] executionRequest = new GoPluginApiRequest[1];
        when(response.responseBody()).thenReturn("{\"success\":true,\"messages\":[\"message1\",\"message2\"]}");


        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                GoPluginApiRequest request = (GoPluginApiRequest) invocationOnMock.getArguments()[1];
                executionRequest[0] = request;
                return response;
            }
        }).when(pluginManager).submitTo(eq(pluginId), any(GoPluginApiRequest.class));
        handler = new JsonBasedTaskExtensionHandler_V1();

        new JsonBasedTaskExecutor(pluginId, pluginManager, handler).execute(config(), context);

        assertTrue(executionRequest.length == 1);
        Map result = (Map) new GsonBuilder().create().fromJson(executionRequest[0].requestBody(), Object.class);
        Map context = (Map) result.get("context");

        assertThat((String) context.get("workingDirectory"), is(workingDir));
        Map environmentVariables = (Map) context.get("environmentVariables");
        assertThat(environmentVariables.size(), is(2));
        assertThat(environmentVariables.get("ENV1").toString(), is("VAL1"));
        assertThat(environmentVariables.get("ENV2").toString(), is("VAL2"));
        assertTrue(executionRequest[0].requestParameters().get("console") instanceof ConsoleWrapper);
        ConsoleWrapper consoleSentToPlugin = (ConsoleWrapper) executionRequest[0].requestParameters().get("console");
        assertThat(consoleSentToPlugin.getConsole(), is(console));
        assertThat(consoleSentToPlugin.getEnvironment(), is(environment));
    }

    private EnvironmentVariables getEnvironmentVariables() {
        return new EnvironmentVariables() {
            @Override
            public Map<String, String> asMap() {
                final HashMap<String, String> map = new HashMap<String, String>();
                map.put("ENV1", "VAL1");
                map.put("ENV2", "VAL2");
                return map;
            }

            @Override
            public void writeTo(com.thoughtworks.go.plugin.api.task.Console console) {
            }

            @Override
            public com.thoughtworks.go.plugin.api.task.Console.SecureEnvVarSpecifier secureEnvSpecifier() {
                return null;
            }
        };
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
