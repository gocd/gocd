/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.PluginSqlMapDao;
import com.thoughtworks.go.server.domain.PluginSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@Component
public class PluginSettingsRequestProcessor implements GoPluginApiRequestProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginSettingsRequestProcessor.class);

    public static final String GET_PLUGIN_SETTINGS = "go.processor.plugin-settings.get";

    private PluginSqlMapDao pluginSqlMapDao;
    private final List<GoPluginExtension> extensions;
    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<>();

    @Autowired
    public PluginSettingsRequestProcessor(PluginRequestProcessorRegistry registry, PluginSqlMapDao pluginSqlMapDao, List<GoPluginExtension> extensions) {
        this.pluginSqlMapDao = pluginSqlMapDao;
        this.extensions = extensions;
        registry.registerProcessorFor(GET_PLUGIN_SETTINGS, this);
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        try {
            GoPluginExtension extension = extensionFor(pluginDescriptor.id());

            PluginSettings pluginSettings = pluginSettingsFor(pluginDescriptor.id());

            DefaultGoApiResponse response = new DefaultGoApiResponse(200);
            response.setResponseBody(extension.pluginSettingsJSON(pluginDescriptor.id(), pluginSettings.getSettingsAsKeyValuePair()));

            return response;
        } catch (Exception e) {
            LOGGER.error(format("Error processing PluginSettings request from plugin: %s.", pluginDescriptor.id()), e);

            DefaultGoApiResponse errorResponse = new DefaultGoApiResponse(400);
            errorResponse.setResponseBody(format("Error while processing get PluginSettings request - %s", e.getMessage()));

            return errorResponse;
        }
    }

    private PluginSettings pluginSettingsFor(String pluginId) {
        Plugin plugin = pluginSqlMapDao.findPlugin(pluginId);
        PluginSettings pluginSettings = new PluginSettings(pluginId);
        if (!(plugin instanceof NullPlugin)) {
            pluginSettings.populateSettingsMap(plugin);
        }

        return pluginSettings;
    }

    private GoPluginExtension extensionFor(String pluginId) {
        for(GoPluginExtension extension : extensions) {
            if(extension.canHandlePlugin(pluginId)){
                return extension;
            }
        }

        throw new IllegalArgumentException(format(
                "Plugin '%s' is not supported by any extension point", pluginId));
    }

    Map<String, JsonMessageHandler> getMessageHandlerMap() {
        return messageHandlerMap;
    }
}
