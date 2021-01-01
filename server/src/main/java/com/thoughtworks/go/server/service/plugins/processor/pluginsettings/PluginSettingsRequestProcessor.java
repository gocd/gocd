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
import com.thoughtworks.go.server.service.plugins.processor.pluginsettings.v1.MessageHandlerForPluginSettingsRequestProcessor1_0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

@Component
public class PluginSettingsRequestProcessor implements GoPluginApiRequestProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginSettingsRequestProcessor.class);
    public static final String GET_PLUGIN_SETTINGS = "go.processor.plugin-settings.get";

    private final PluginSqlMapDao pluginSqlMapDao;
    private Map<String, MessageHandlerForPluginSettingsRequestProcessor> versionToMessageHandlerMap;

    @Autowired
    public PluginSettingsRequestProcessor(PluginRequestProcessorRegistry registry,
                                          PluginSqlMapDao pluginSqlMapDao) {
        this.pluginSqlMapDao = pluginSqlMapDao;
        this.versionToMessageHandlerMap = new HashMap<>();

        versionToMessageHandlerMap.put("1.0", new MessageHandlerForPluginSettingsRequestProcessor1_0());
        registry.registerProcessorFor(GET_PLUGIN_SETTINGS, this);
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        try {
            MessageHandlerForPluginSettingsRequestProcessor processor = versionToMessageHandlerMap.get(goPluginApiRequest.apiVersion());

            DefaultGoApiResponse response = new DefaultGoApiResponse(200);
            response.setResponseBody(processor.pluginSettingsToJSON(pluginSettingsFor(pluginDescriptor.id())));

            return response;
        } catch (Exception e) {
            LOGGER.error(format("Error processing PluginSettings request from plugin: %s.", pluginDescriptor.id()), e);

            DefaultGoApiResponse errorResponse = new DefaultGoApiResponse(400);
            errorResponse.setResponseBody(format("Error while processing get PluginSettings request - %s", e.getMessage()));

            return errorResponse;
        }
    }

    private Map<String, String> pluginSettingsFor(String pluginId) {
        Plugin plugin = pluginSqlMapDao.findPlugin(pluginId);
        final Map<String, String> pluginSettingsAsMap = new HashMap<>();
        if (plugin instanceof NullPlugin) {
            return pluginSettingsAsMap;
        }

        for (String key : plugin.getAllConfigurationKeys()) {
            pluginSettingsAsMap.put(key, plugin.getConfigurationValue(key));
        }

        return pluginSettingsAsMap;
    }
}
