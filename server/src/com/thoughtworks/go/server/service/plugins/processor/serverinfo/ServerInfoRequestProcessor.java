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

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class ServerInfoRequestProcessor implements GoPluginApiRequestProcessor {

    public static final String GET_SERVER_ID = "go.processor.server-info.get";

    private final HashMap<String, ServerInfoMessageConverter> messageHandlerMap = new HashMap<>();

    private final GoConfigService configService;

    @Autowired
    public ServerInfoRequestProcessor(PluginRequestProcessorRegistry registry, GoConfigService configService) {
        this.configService = configService;
        registry.registerProcessorFor(GET_SERVER_ID, this);
        messageHandlerMap.put("1.0", new ServerInfoMessageConverterV1());
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        String version = goPluginApiRequest.apiVersion();

        if (!messageHandlerMap.containsKey(version)) {
            throw new RuntimeException(String.format("Unsupported '%s' API version: %s. Supported versions: %s", goPluginApiRequest.api(), version, messageHandlerMap.keySet()));
        }

        return messageHandlerMap.get(version).getServerInfo(configService.serverConfig());
    }
}
