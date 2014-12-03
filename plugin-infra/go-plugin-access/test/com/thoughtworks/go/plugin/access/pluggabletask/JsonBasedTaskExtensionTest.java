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

import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.infra.Action;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class JsonBasedTaskExtensionTest {

    private PluginManager pluginManager;
    private JsonBasedTaskExtension extension;
    private String pluginId;

    @Before
    public void setup(){
        pluginManager = mock(PluginManager.class);
        extension = new JsonBasedTaskExtension(pluginManager);
        pluginId = "plugin-id";
        when(pluginManager.resolveExtensionVersion(eq(pluginId), any(ArrayList.class))).thenReturn("1.0");
    }

    @Test
    public void shouldExecuteTheTask() {
        ActionWithReturn actionWithReturn = mock(ActionWithReturn.class);
        when(actionWithReturn.execute(any(JsonBasedPluggableTask.class), any(GoPluginDescriptor.class))).thenReturn(ExecutionResult.success("yay"));

        ExecutionResult executionResult = extension.execute(pluginId, actionWithReturn);

        verify(actionWithReturn).execute(any(JsonBasedPluggableTask.class), any(GoPluginDescriptor.class));
        assertThat(executionResult.getMessagesForDisplay(), is("yay"));
        assertTrue(executionResult.isSuccessful());
    }

    @Test
    public void shouldPerformTheActionOnTask() {
        Action action = mock(Action.class);
        final GoPluginDescriptor descriptor = mock(GoPluginDescriptor.class);
        when(pluginManager.getPluginDescriptorFor(pluginId)).thenReturn(descriptor);

        extension.doOnTask(pluginId, action);

        verify(action).execute(any(JsonBasedPluggableTask.class), eq(descriptor));
    }

    @Test
    public void shouldValidateTask() {
        GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        JsonBasedTaskExtension jsonBasedTaskExtension = new JsonBasedTaskExtension(pluginManager);
        TaskConfig taskConfig = mock(TaskConfig.class);

        when(response.responseCode()).thenReturn(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE);
        when(response.responseBody()).thenReturn("{\"errors\":{\"key\":\"error\"}}");
        when(pluginManager.submitTo(eq(pluginId), any(GoPluginApiRequest.class))).thenReturn(response);
        ValidationResult validationResult = jsonBasedTaskExtension.validate(pluginId, taskConfig);

        verify(pluginManager).submitTo(eq(pluginId), any(GoPluginApiRequest.class));
        assertFalse(validationResult.isSuccessful());
        assertEquals(validationResult.getErrors().get(0).getKey(), "key");
        assertEquals(validationResult.getErrors().get(0).getMessage(), "error");
    }
}