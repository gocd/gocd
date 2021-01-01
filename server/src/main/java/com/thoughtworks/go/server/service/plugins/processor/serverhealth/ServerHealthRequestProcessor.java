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

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.plugins.processor.serverhealth.v1.MessageHandlerForServerHealthRequestProcessorV1;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.text.MessageFormat.format;

@Component
public class ServerHealthRequestProcessor implements GoPluginApiRequestProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerHealthRequestProcessor.class);
    static final String ADD_SERVER_HEALTH_MESSAGES = "go.processor.server-health.add-messages";
    private final ServerHealthService serverHealthService;
    private Map<String, MessageHandlerForServerHealthRequestProcessor> versionToMessageHandlerMap;

    @Autowired
    public ServerHealthRequestProcessor(PluginRequestProcessorRegistry registry, ServerHealthService serverHealthService) {
        this.serverHealthService = serverHealthService;
        this.versionToMessageHandlerMap = new HashMap<>();

        versionToMessageHandlerMap.put("1.0", new MessageHandlerForServerHealthRequestProcessorV1());

        registry.registerProcessorFor(ADD_SERVER_HEALTH_MESSAGES, this);
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        String errorMessageTitle = format("Message from plugin: {0}", pluginDescriptor.id());
        HealthStateScope scope = HealthStateScope.fromPlugin(pluginDescriptor.id());

        try {
            MessageHandlerForServerHealthRequestProcessor messageHandler = versionToMessageHandlerMap.get(goPluginApiRequest.apiVersion());
            List<PluginHealthMessage> pluginHealthMessages = messageHandler.deserializeServerHealthMessages(goPluginApiRequest.requestBody());
            replaceServerHealthMessages(errorMessageTitle, scope, pluginHealthMessages);
        } catch (Exception e) {
            DefaultGoApiResponse response = new DefaultGoApiResponse(DefaultGoApiResponse.INTERNAL_ERROR);
            response.setResponseBody(format("'{' \"message\": \"{0}\" '}'", e.getMessage()));
            LOGGER.warn("Failed to handle message from plugin {}: {}", pluginDescriptor.id(), goPluginApiRequest.requestBody(), e);
            return response;
        }

        return new DefaultGoApiResponse(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE);
    }

    private void replaceServerHealthMessages(String errorMessageTitle, HealthStateScope scope, List<PluginHealthMessage> pluginHealthMessages) {
        serverHealthService.removeByScope(scope);

        for (int index = 0; index < pluginHealthMessages.size(); index++) {
            PluginHealthMessage pluginHealthMessage = pluginHealthMessages.get(index);
            ServerHealthState state;

            if (pluginHealthMessage.isWarning()) {
                state = ServerHealthState.warning(errorMessageTitle, pluginHealthMessage.message(), HealthStateType.withSubkey(scope, "message_" + index));
            } else {
                state = ServerHealthState.error(errorMessageTitle, pluginHealthMessage.message(), HealthStateType.withSubkey(scope, "message_" + index));
            }

            serverHealthService.update(state);
        }
    }
}
