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
package com.thoughtworks.go.server.service.plugins.processor.console;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.ConsoleService;
import com.thoughtworks.go.server.service.plugins.processor.console.v1.MessageHandlerForConsoleLogRequestProcessorImpl1_0;
import com.thoughtworks.go.server.service.plugins.processor.console.v2.MessageHandlerForConsoleLogRequestProcessorImpl2_0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;

@Component
public class ConsoleLogRequestProcessor implements GoPluginApiRequestProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleLogRequestProcessor.class);

    public static final String APPEND_TO_CONSOLE_LOG = "go.processor.console-log.append";

    public static final String VERSION_1 = "1.0";
    public static final String VERSION_2 = "2.0";
    private static final List<String> supportedVersions = asList(VERSION_1, VERSION_2);

    private Map<String, MessageHandlerForConsoleLogRequestProcessor> versionToMessageHandlerMap;
    private ConsoleService consoleService;

    @Autowired
    public ConsoleLogRequestProcessor(PluginRequestProcessorRegistry registry, ConsoleService consoleService) {
        this.consoleService = consoleService;
        versionToMessageHandlerMap = new HashMap<>();
        versionToMessageHandlerMap.put(VERSION_1, new MessageHandlerForConsoleLogRequestProcessorImpl1_0());
        versionToMessageHandlerMap.put(VERSION_2, new MessageHandlerForConsoleLogRequestProcessorImpl2_0());

        registry.registerProcessorFor(APPEND_TO_CONSOLE_LOG, this);
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest request) {
        try {
            validatePluginRequest(request);

            final MessageHandlerForConsoleLogRequestProcessor handler = versionToMessageHandlerMap.get(request.apiVersion());
            final ConsoleLogAppendRequest logUpdateRequest = handler.deserializeConsoleLogAppendRequest(request.requestBody());
            consoleService.appendToConsoleLog(logUpdateRequest.jobIdentifier(), logUpdateRequest.text());
        } catch (Exception e) {
            DefaultGoApiResponse response = new DefaultGoApiResponse(DefaultGoApiResponse.INTERNAL_ERROR);
            response.setResponseBody(format("'{' \"message\": \"Error: {0}\" '}'", e.getMessage()));
            LOGGER.warn("Failed to handle message from plugin {}: {}", pluginDescriptor.id(), request.requestBody(), e);
            return response;
        }

        return new DefaultGoApiResponse(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE);
    }

    private void validatePluginRequest(GoApiRequest goPluginApiRequest) {
        if (!supportedVersions.contains(goPluginApiRequest.apiVersion())) {
            throw new RuntimeException(String.format("Unsupported '%s' API version: %s. Supported versions: %s",
                    goPluginApiRequest.api(), goPluginApiRequest.apiVersion(), supportedVersions));
        }
    }
}
