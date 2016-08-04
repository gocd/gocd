/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic;

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.settings.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ElasticAgentExtension extends AbstractExtension {

    private final HashMap<String, ElasticAgentMessageConverter> messageHandlerMap = new HashMap<>();

    @Autowired
    public ElasticAgentExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, Constants.SUPPORTED_VERSIONS, Constants.EXTENSION_NAME), Constants.EXTENSION_NAME);
        addHandler(ElasticAgentExtensionConverterV1.VERSION, new PluginSettingsJsonMessageHandler1_0(), new ElasticAgentExtensionConverterV1());
    }

    private void addHandler(String version, PluginSettingsJsonMessageHandler messageHandler, ElasticAgentMessageConverter extensionHandler) {
        pluginSettingsMessageHandlerMap.put(version, messageHandler);
        messageHandlerMap.put(ElasticAgentExtensionConverterV1.VERSION, extensionHandler);
    }

    public void createAgent(String pluginId, final String autoRegisterKey, final String environment, final Map<String, String> configuration) {
        pluginRequestHelper.submitRequest(pluginId, Constants.REQUEST_CREATE_AGENT, new DefaultPluginInteractionCallback<Void>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).createAgentRequestBody(autoRegisterKey, environment, configuration);
            }
        });
    }

    public void serverPing(final String pluginId) {
        pluginRequestHelper.submitRequest(pluginId, Constants.REQUEST_SERVER_PING, new DefaultPluginInteractionCallback<Void>());
    }

    public boolean shouldAssignWork(String pluginId, final AgentMetadata agent, final String environment, final Map<String, String> configuration) {
        return pluginRequestHelper.submitRequest(pluginId, Constants.REQUEST_SHOULD_ASSIGN_WORK, new DefaultPluginInteractionCallback<Boolean>() {

            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).shouldAssignWorkRequestBody(agent, environment, configuration);
            }

            @Override
            public Boolean onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).shouldAssignWorkResponseFromBody(responseBody);
            }
        });
    }

    public ElasticAgentMessageConverter getElasticAgentMessageConverter(String version) {
        return messageHandlerMap.get(version);
    }
}
