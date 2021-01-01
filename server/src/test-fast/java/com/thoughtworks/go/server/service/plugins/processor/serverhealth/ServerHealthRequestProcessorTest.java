/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service.plugins.processor.serverhealth;

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Map;

import static com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse.INTERNAL_ERROR;
import static com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse.SUCCESS_RESPONSE_CODE;
import static com.thoughtworks.go.server.service.plugins.processor.serverhealth.ServerHealthRequestProcessor.ADD_SERVER_HEALTH_MESSAGES;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

public class ServerHealthRequestProcessorTest {
    private static final String PLUGIN_ID = "plugin-id-1";
    private ServerHealthService serverHealthService;
    private PluginRequestProcessorRegistry requestProcessorRegistry;
    private ServerHealthRequestProcessor processor;
    private GoPluginDescriptor descriptor;

    @Before
    public void setUp() {
        serverHealthService = mock(ServerHealthService.class);
        requestProcessorRegistry = mock(PluginRequestProcessorRegistry.class);

        descriptor = GoPluginDescriptor.builder().id(PLUGIN_ID).build();

        processor = new ServerHealthRequestProcessor(requestProcessorRegistry, serverHealthService);
    }

    @Test
    public void shouldRemoveServerHealthMessagesForThePluginBeforeAddingNewOnes() {
        processor.process(descriptor, createRequest("1.0", "[]"));

        verify(serverHealthService).removeByScope(HealthStateScope.fromPlugin(PLUGIN_ID));
    }

    @Test
    public void shouldAddDeserializedServerHealthMessages() {
        String requestBody = new Gson().toJson(asList(
                new PluginHealthMessage("warning", "message 1"),
                new PluginHealthMessage("error", "message 2")
        ));

        GoApiResponse response = processor.process(descriptor, createRequest("1.0", requestBody));
        assertThat(response.responseCode()).isEqualTo(SUCCESS_RESPONSE_CODE);

        ArgumentCaptor<ServerHealthState> argumentCaptor = ArgumentCaptor.forClass(ServerHealthState.class);
        InOrder inOrder = inOrder(serverHealthService);
        inOrder.verify(serverHealthService, times(1)).removeByScope(HealthStateScope.fromPlugin(PLUGIN_ID));
        inOrder.verify(serverHealthService, times(2)).update(argumentCaptor.capture());

        assertThat(argumentCaptor.getAllValues().get(0).getDescription()).isEqualTo("message 1");
        assertThat(argumentCaptor.getAllValues().get(1).getDescription()).isEqualTo("message 2");
    }

    @Test
    public void shouldRespondWithAFailedResponseCodeAndMessageIfSomethingGoesWrong() {
        GoApiResponse response = processor.process(descriptor, createRequest("1.0", "INVALID_JSON"));

        assertThat(response.responseCode()).isEqualTo(INTERNAL_ERROR);
        assertThat(new Gson().fromJson(response.responseBody(), Map.class)).isEqualTo(m("message", "Failed to deserialize message from plugin: INVALID_JSON"));
    }

    private DefaultGoApiRequest createRequest(String apiVersion, String requestBody) {
        DefaultGoApiRequest request = new DefaultGoApiRequest(ADD_SERVER_HEALTH_MESSAGES, apiVersion, null);
        request.setRequestBody(requestBody);
        return request;
    }
}