/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.PLUGGABLE_TASK_EXTENSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

public class JsonBasedPluggableTaskTest {

    private PluginManager pluginManager;
    private JsonBasedPluggableTask task;
    private GoPluginApiResponse goPluginApiResponse;
    private String pluginId;

    @BeforeEach
    public void setup() {
        pluginManager = mock(PluginManager.class);
        pluginId = "plugin-id";
        final List<String> goSupportedVersions = List.of("1.0");
        final Map<String, JsonBasedTaskExtensionHandler> handlerMap = new HashMap<>();
        handlerMap.put("1.0", new JsonBasedTaskExtensionHandler_V1());

        task = new JsonBasedPluggableTask(pluginId, new PluginRequestHelper(pluginManager, goSupportedVersions, PLUGGABLE_TASK_EXTENSION), handlerMap);
        goPluginApiResponse = mock(GoPluginApiResponse.class);
        when(pluginManager.submitTo(eq(pluginId), eq(PLUGGABLE_TASK_EXTENSION), any())).thenReturn(goPluginApiResponse);
        when(pluginManager.resolveExtensionVersion(pluginId, PLUGGABLE_TASK_EXTENSION, goSupportedVersions)).thenReturn("1.0");
        when(goPluginApiResponse.responseCode()).thenReturn(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE);
        when(pluginManager.isPluginOfType(PLUGGABLE_TASK_EXTENSION, pluginId)).thenReturn(true);
    }

    @Test
    public void shouldGetTheTaskConfig() {

        String jsonResponse = "{" +
                "\"URL\":{\"default-value\":\"\",\"secure\":false,\"required\":true}," +
                "\"USER\":{\"default-value\":\"foo\",\"secure\":true,\"required\":true}," +
                "\"PASSWORD\":{}" +
                "}";

        when(goPluginApiResponse.responseBody()).thenReturn(jsonResponse);

        TaskConfig config = task.config();

        Property url = config.get("URL");
        assertThat(url.getOption(Property.REQUIRED)).isTrue();
        assertThat(url.getOption(Property.SECURE)).isFalse();

        Property user = config.get("USER");
        assertThat(user.getOption(Property.REQUIRED)).isTrue();
        assertThat(user.getOption(Property.SECURE)).isTrue();

        Property password = config.get("PASSWORD");
        assertThat(password.getOption(Property.REQUIRED)).isTrue();
        assertThat(password.getOption(Property.SECURE)).isFalse();

        ArgumentCaptor<GoPluginApiRequest> argument = ArgumentCaptor.forClass(GoPluginApiRequest.class);
        verify(pluginManager).submitTo(eq(pluginId), eq(PLUGGABLE_TASK_EXTENSION), argument.capture());
        assertThat(argument.getValue().extension()).isEqualTo((PLUGGABLE_TASK_EXTENSION));
        assertThat(argument.getValue().extensionVersion()).isEqualTo((JsonBasedTaskExtensionHandler_V1.VERSION));
        assertThat(argument.getValue().requestName()).isEqualTo((TaskExtension.CONFIGURATION_REQUEST));
    }

    @Test
    public void shouldGetTaskView() {
        String jsonResponse = "{\"displayValue\":\"MyTaskPlugin\", \"template\":\"<html>junk</html>\"}";
        when(goPluginApiResponse.responseBody()).thenReturn(jsonResponse);

        TaskView view = task.view();
        assertThat(view.displayValue()).isEqualTo("MyTaskPlugin");
        assertThat(view.template()).isEqualTo("<html>junk</html>");

        ArgumentCaptor<GoPluginApiRequest> argument = ArgumentCaptor.forClass(GoPluginApiRequest.class);
        verify(pluginManager).submitTo(eq(pluginId), eq(PLUGGABLE_TASK_EXTENSION), argument.capture());
        assertThat(argument.getValue().extension()).isEqualTo((PLUGGABLE_TASK_EXTENSION));
        assertThat(argument.getValue().extensionVersion()).isEqualTo((JsonBasedTaskExtensionHandler_V1.VERSION));
        assertThat(argument.getValue().requestName()).isEqualTo((TaskExtension.TASK_VIEW_REQUEST));
    }

    @Test
    public void shouldValidateTaskConfig() {
        String jsonResponse = "{\"errors\":{\"key1\":\"err1\",\"key2\":\"err3\"}}";
        String config = "{\"URL\":{\"secure\":false,\"value\":\"http://foo\",\"required\":true}}";

        when(goPluginApiResponse.responseBody()).thenReturn(jsonResponse);

        TaskConfig configuration = new TaskConfig();
        final TaskConfigProperty property = new TaskConfigProperty("URL", "http://foo");
        property.with(Property.SECURE, false);
        property.with(Property.REQUIRED, true);
        configuration.add(property);

        ValidationResult result = task.validate(configuration);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrors().getFirst().getKey()).isEqualTo("key1");
        assertThat(result.getErrors().getFirst().getMessage()).isEqualTo("err1");
        assertThat(result.getErrors().getLast().getKey()).isEqualTo("key2");
        assertThat(result.getErrors().getLast().getMessage()).isEqualTo("err3");

        ArgumentCaptor<GoPluginApiRequest> argument = ArgumentCaptor.forClass(GoPluginApiRequest.class);
        verify(pluginManager).submitTo(eq(pluginId), eq(PLUGGABLE_TASK_EXTENSION), argument.capture());
        assertThat(argument.getValue().requestBody()).isEqualTo(config);

        assertThat(argument.getValue().extension()).isEqualTo((PLUGGABLE_TASK_EXTENSION));
        assertThat(argument.getValue().extensionVersion()).isEqualTo((JsonBasedTaskExtensionHandler_V1.VERSION));
        assertThat(argument.getValue().requestName()).isEqualTo((TaskExtension.VALIDATION_REQUEST));
    }

    @Test
    public void shouldGetTaskExecutor() {
        assertInstanceOf(JsonBasedTaskExecutor.class, task.executor());
    }
}
