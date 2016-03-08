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

package com.thoughtworks.go.server.service.plugins.processor.pluginsettings;

import com.thoughtworks.go.domain.NullPlugin;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.PluginSqlMapDao;
import com.thoughtworks.go.server.domain.PluginSettings;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@Component
public class PluginSettingsRequestProcessor implements GoPluginApiRequestProcessor {
    private static final Logger LOGGER = Logger.getLogger(PluginSettingsRequestProcessor.class);

    public static final String GET_PLUGIN_SETTINGS = "go.processor.plugin-settings.get";
    private static final List<String> goSupportedVersions = asList("1.0");

    private PluginSqlMapDao pluginSqlMapDao;
    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<>();

    @Autowired
    public PluginSettingsRequestProcessor(PluginRequestProcessorRegistry registry, PluginSqlMapDao pluginSqlMapDao) {
        this.pluginSqlMapDao = pluginSqlMapDao;
        registry.registerProcessorFor(GET_PLUGIN_SETTINGS, this);
        this.messageHandlerMap.put("1.0", new JsonMessageHandler1_0());
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        try {
            String version = goPluginApiRequest.apiVersion();
            if (!goSupportedVersions.contains(version)) {
                throw new RuntimeException(String.format("Unsupported '%s' API version: %s. Supported versions: %s", goPluginApiRequest.api(), version, goSupportedVersions));
            }

            if (goPluginApiRequest.api().equals(GET_PLUGIN_SETTINGS)) {
                return handlePluginSettingsGetRequest(pluginDescriptor.id(), goPluginApiRequest);
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while authenticating user", e);
        }
        return new DefaultGoApiResponse(400);
    }

    private GoApiResponse handlePluginSettingsGetRequest(String pluginId, GoApiRequest goPluginApiRequest) {
        Plugin plugin = pluginSqlMapDao.findPlugin(pluginId);
        PluginSettings pluginSettings = new PluginSettings(pluginId);
        if (!(plugin instanceof NullPlugin)) {
            pluginSettings.populateSettingsMap(plugin);
        }
        DefaultGoApiResponse response = new DefaultGoApiResponse(200);
        response.setResponseBody(messageHandlerMap.get(goPluginApiRequest.apiVersion()).responseMessagePluginSettingsGet(pluginSettings));
        return response;
    }

    Map<String, JsonMessageHandler> getMessageHandlerMap() {
        return messageHandlerMap;
    }
}
