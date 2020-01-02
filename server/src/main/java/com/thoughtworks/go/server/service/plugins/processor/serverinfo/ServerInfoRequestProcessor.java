/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.plugins.processor.serverinfo.v1.MessageHandlerForServerInfoRequestProcessor1_0;
import com.thoughtworks.go.server.service.plugins.processor.serverinfo.v2.MessageHandlerForServerInfoRequestProcessor2_0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

@Component
public class ServerInfoRequestProcessor implements GoPluginApiRequestProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerInfoRequestProcessor.class);
    public static final String GET_SERVER_INFO = "go.processor.server-info.get";

    private final GoConfigService configService;
    private Map<String, MessageHandlerForServerInfoRequestProcessor> versionToMessageHandlerMap;

    @Autowired
    public ServerInfoRequestProcessor(PluginRequestProcessorRegistry registry, GoConfigService configService) {
        this.configService = configService;
        this.versionToMessageHandlerMap = new HashMap<>();

        this.versionToMessageHandlerMap.put("1.0", new MessageHandlerForServerInfoRequestProcessor1_0());
        this.versionToMessageHandlerMap.put("2.0", new MessageHandlerForServerInfoRequestProcessor2_0());
        registry.registerProcessorFor(GET_SERVER_INFO, this);
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        try {
            MessageHandlerForServerInfoRequestProcessor requestProcessor = this.versionToMessageHandlerMap.get(goPluginApiRequest.apiVersion());
            ServerConfig serverConfig = configService.serverConfig();

            String serverInfoJSON = requestProcessor.serverInfoToJSON(serverConfig);

            return DefaultGoApiResponse.success(serverInfoJSON);
        } catch (Exception e) {
            LOGGER.error(format("Error processing ServerInfo request from plugin: %s.", pluginDescriptor.id()), e);
            return DefaultGoApiResponse.badRequest(format("Error while processing get ServerInfo request - %s", e.getMessage()));
        }
    }
}
