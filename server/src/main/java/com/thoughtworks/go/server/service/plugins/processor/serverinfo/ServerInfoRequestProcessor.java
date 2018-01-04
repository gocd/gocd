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

package com.thoughtworks.go.server.service.plugins.processor.serverinfo;

import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.GoConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.String.format;

@Component
public class ServerInfoRequestProcessor implements GoPluginApiRequestProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerInfoRequestProcessor.class);
    public static final String GET_SERVER_ID = "go.processor.server-info.get";

    private final GoConfigService configService;
    private final List<GoPluginExtension> extensions;

    @Autowired
    public ServerInfoRequestProcessor(PluginRequestProcessorRegistry registry, GoConfigService configService, List<GoPluginExtension> extensions) {
        this.configService = configService;
        this.extensions = extensions;
        registry.registerProcessorFor(GET_SERVER_ID, this);
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        try {
            GoPluginExtension extension = extensionFor(pluginDescriptor.id());

            ServerConfig serverConfig = configService.serverConfig();
            String serverInfoJSON = extension.serverInfoJSON(pluginDescriptor.id(), serverConfig.getServerId(), serverConfig.getSiteUrl().getUrl(), serverConfig.getSecureSiteUrl().getUrl());

            return DefaultGoApiResponse.success(serverInfoJSON);
        } catch (Exception e) {
            LOGGER.error(format("Error processing ServerInfo request from plugin: %s.", pluginDescriptor.id()), e);
            return DefaultGoApiResponse.badRequest(format("Error while processing get ServerInfo request - %s", e.getMessage()));
        }
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
}
