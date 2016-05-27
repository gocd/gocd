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
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConstants;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class JsonBasedTaskExtensionTest {
    @Mock
    private PluginManager pluginManager;
    @Mock
    private PluginSettingsJsonMessageHandler1_0 pluginSettingsJSONMessageHandler;
    @Mock
    private JsonBasedTaskExtensionHandler jsonMessageHandler;

    private JsonBasedTaskExtension extension;
    private String pluginId;
    private PluginSettingsConfiguration pluginSettingsConfiguration;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;

    @Before
    public void setup() {
        initMocks(this);
        extension = new JsonBasedTaskExtension(pluginManager);
        pluginId = "plugin-id";
        when(pluginManager.resolveExtensionVersion(eq(pluginId), any(ArrayList.class))).thenReturn("1.0");

        pluginSettingsConfiguration = new PluginSettingsConfiguration();
        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
    }

    @Test
    public void shouldTalkToPluginToGetPluginSettingsConfiguration() throws Exception {
        extension.getPluginSettingsMessageHandlerMap().put("1.0", pluginSettingsJSONMessageHandler);
        extension.getMessageHandlerMap().put("1.0", jsonMessageHandler);

        String responseBody = "expected-response";
        PluginSettingsConfiguration deserializedResponse = new PluginSettingsConfiguration();
        when(pluginSettingsJSONMessageHandler.responseMessageForPluginSettingsConfiguration(responseBody)).thenReturn(deserializedResponse);

        when(pluginManager.isPluginOfType(JsonBasedTaskExtension.TASK_EXTENSION, pluginId)).thenReturn(true);
        when(pluginManager.submitTo(eq(pluginId), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        PluginSettingsConfiguration response = extension.getPluginSettingsConfiguration(pluginId);

        assertRequest(requestArgumentCaptor.getValue(), JsonBasedTaskExtension.TASK_EXTENSION, "1.0", PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_CONFIGURATION, null);
        verify(pluginSettingsJSONMessageHandler).responseMessageForPluginSettingsConfiguration(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetPluginSettingsView() throws Exception {
        extension.getPluginSettingsMessageHandlerMap().put("1.0", pluginSettingsJSONMessageHandler);
        extension.getMessageHandlerMap().put("1.0", jsonMessageHandler);

        String responseBody = "expected-response";
        String deserializedResponse = "";
        when(pluginSettingsJSONMessageHandler.responseMessageForPluginSettingsView(responseBody)).thenReturn(deserializedResponse);

        when(pluginManager.isPluginOfType(JsonBasedTaskExtension.TASK_EXTENSION, pluginId)).thenReturn(true);
        when(pluginManager.submitTo(eq(pluginId), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        String response = extension.getPluginSettingsView(pluginId);

        assertRequest(requestArgumentCaptor.getValue(), JsonBasedTaskExtension.TASK_EXTENSION, "1.0", PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_VIEW, null);
        verify(pluginSettingsJSONMessageHandler).responseMessageForPluginSettingsView(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToValidatePluginSettings() throws Exception {
        extension.getPluginSettingsMessageHandlerMap().put("1.0", pluginSettingsJSONMessageHandler);
        extension.getMessageHandlerMap().put("1.0", jsonMessageHandler);

        String requestBody = "expected-request";
        when(pluginSettingsJSONMessageHandler.requestMessageForPluginSettingsValidation(pluginSettingsConfiguration)).thenReturn(requestBody);

        String responseBody = "expected-response";
        ValidationResult deserializedResponse = new ValidationResult();
        when(pluginSettingsJSONMessageHandler.responseMessageForPluginSettingsValidation(responseBody)).thenReturn(deserializedResponse);

        when(pluginManager.isPluginOfType(JsonBasedTaskExtension.TASK_EXTENSION, pluginId)).thenReturn(true);
        when(pluginManager.submitTo(eq(pluginId), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        ValidationResult response = extension.validatePluginSettings(pluginId, pluginSettingsConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), JsonBasedTaskExtension.TASK_EXTENSION, "1.0", PluginSettingsConstants.REQUEST_VALIDATE_PLUGIN_SETTINGS, requestBody);
        verify(pluginSettingsJSONMessageHandler).responseMessageForPluginSettingsValidation(responseBody);
        assertSame(response, deserializedResponse);
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
        when(pluginManager.isPluginOfType(JsonBasedTaskExtension.TASK_EXTENSION, pluginId)).thenReturn(true);
        when(response.responseBody()).thenReturn("{\"errors\":{\"key\":\"error\"}}");
        when(pluginManager.submitTo(eq(pluginId), any(GoPluginApiRequest.class))).thenReturn(response);
        ValidationResult validationResult = jsonBasedTaskExtension.validate(pluginId, taskConfig);

        verify(pluginManager).submitTo(eq(pluginId), any(GoPluginApiRequest.class));
        assertFalse(validationResult.isSuccessful());
        assertEquals(validationResult.getErrors().get(0).getKey(), "key");
        assertEquals(validationResult.getErrors().get(0).getMessage(), "error");
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) {
        assertThat(goPluginApiRequest.extension(), is(extensionName));
        assertThat(goPluginApiRequest.extensionVersion(), is(version));
        assertThat(goPluginApiRequest.requestName(), is(requestName));
        assertThat(goPluginApiRequest.requestBody(), is(requestBody));
    }
}
