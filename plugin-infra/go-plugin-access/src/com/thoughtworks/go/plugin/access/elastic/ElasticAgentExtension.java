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

    public final static List<String> supportedVersions = Arrays.asList(ElasticAgentExtensionConverterV1.VERSION);

    private final HashMap<String, ElasticAgentMessageConverter> messageHandlerMap = new HashMap<>();

    @Autowired
    public ElasticAgentExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, supportedVersions, Constants.EXTENSION_NAME), Constants.EXTENSION_NAME);
        addHandler(ElasticAgentExtensionConverterV1.VERSION, new PluginSettingsJsonMessageHandler1_0(), new ElasticAgentExtensionConverterV1());
    }

    void addHandler(String version, PluginSettingsJsonMessageHandler messageHandler, ElasticAgentMessageConverter extensionHandler) {
        pluginSettingsMessageHandlerMap.put(version, messageHandler);
        messageHandlerMap.put(ElasticAgentExtensionConverterV1.VERSION, extensionHandler);
    }

    public boolean canPluginHandle(String pluginId, final Collection<String> resources, final String environment) {
        return pluginRequestHelper.submitRequest(pluginId, Constants.REQUEST_CAN_PLUGIN_HANDLE, new DefaultPluginInteractionCallback<Boolean>() {

            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).canHandlePluginRequestBody(resources, environment);
            }

            @Override
            public Boolean onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).canHandlePluginResponseFromBody(responseBody);
            }
        });
    }

    public void createAgent(final String autoRegisterKey, String pluginId, final Collection<String> resources, final String environment, final Map<String, String> configuration) {
        pluginRequestHelper.submitRequest(pluginId, Constants.REQUEST_CREATE_AGENT, new DefaultPluginInteractionCallback<Void>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).createAgentRequestBody(autoRegisterKey, resources, environment, configuration);
            }
        });
    }

    public void serverPing(final String pluginId, final Collection<AgentMetadata> metadata) {
        pluginRequestHelper.submitRequest(pluginId, Constants.REQUEST_SERVER_PING, new DefaultPluginInteractionCallback<Void>(){
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).serverPingRequestBody(metadata);
            }
        });
    }

    public boolean shouldAssignWork(String pluginId, final AgentMetadata agent, final Collection<String> resources, final String environment) {
        return pluginRequestHelper.submitRequest(pluginId, Constants.REQUEST_SHOULD_ASSIGN_WORK, new DefaultPluginInteractionCallback<Boolean>() {

            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).shouldAssignWorkRequestBody(agent, resources, environment);
            }

            @Override
            public Boolean onSuccess(String responseBody, String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).shouldAssignWorkResponseFromBody(responseBody);
            }
        });
    }

    public void notifyAgentBusy(String pluginId, final AgentMetadata agent) {
        pluginRequestHelper.submitRequest(pluginId, Constants.REQUEST_NOTIFY_AGENT_BUSY, new DefaultPluginInteractionCallback<Void>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).notifyAgentBusyRequestBody(agent);
            }
        });
    }

    public void notifyAgentIdle(String pluginId, final AgentMetadata agent) {
        pluginRequestHelper.submitRequest(pluginId, Constants.REQUEST_NOTIFY_AGENT_IDLE, new DefaultPluginInteractionCallback<Void>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return getElasticAgentMessageConverter(resolvedExtensionVersion).notifyAgentIdleRequestBody(agent);
            }
        });
    }

    public ElasticAgentMessageConverter getElasticAgentMessageConverter(String version) {
        return messageHandlerMap.get(version);
    }
}
