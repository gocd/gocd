/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.plugin.consolelog;

import com.thoughtworks.go.agent.plugin.consolelog.v1.JsonMessageHandlerV1;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.work.GoPublisher;

import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.util.command.TaggedStreamConsumer.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public class ConsoleLogRequestProcessor implements GoPluginApiRequestProcessor {
    private final HashMap<String, JsonMessageHandler> messageHandlerMap = new HashMap<>();
    private static final List<String> goSupportedVersions = asList("1.0");
    private final GoPublisher goPublisher;

    public ConsoleLogRequestProcessor(GoPublisher goPublisher) {
        this.goPublisher = goPublisher;
        messageHandlerMap.put("1.0", new JsonMessageHandlerV1());
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest request) {
        try {
            validateSupportedVersion(request);
            switch (ConsoleLogRequest.fromString(request.api())) {
                case ARTIFACT_PLUGIN_CONSOLE_LOG:
                    return processArtifactConsoleLogRequest(pluginDescriptor, request);
                case SCM_PLUGIN_CONSOLE_LOG:
                    return processSCMPluginConsoleLogRequest(pluginDescriptor, request);
                case TASK_PLUGIN_CONSOLE_LOG:
                    return processTaskPluginConsoleLogRequest(pluginDescriptor, request);
                default:
                    return DefaultGoApiResponse.error("Illegal api request");
            }
        } catch (Exception e) {
            return DefaultGoApiResponse.error(e.getMessage());
        }
    }

    private GoApiResponse processTaskPluginConsoleLogRequest(GoPluginDescriptor pluginDescriptor, GoApiRequest request) {
        return logToAgentConsole(pluginDescriptor, request, OUT, ERR);
    }

    private GoApiResponse processSCMPluginConsoleLogRequest(GoPluginDescriptor pluginDescriptor, GoApiRequest request) {
        return logToAgentConsole(pluginDescriptor, request, PREP, PREP_ERR);
    }

    private GoApiResponse processArtifactConsoleLogRequest(GoPluginDescriptor pluginDescriptor, GoApiRequest request) {
        return logToAgentConsole(pluginDescriptor, request, PUBLISH, PUBLISH_ERR);
    }

    private GoApiResponse logToAgentConsole(GoPluginDescriptor pluginDescriptor, GoApiRequest request, String infoTag, String errorTag) {
        final ConsoleLogMessage consoleLogMessage = messageHandler(request.apiVersion()).getConsoleLogMessage(request.requestBody());
        final String message = format("[%s] %s", pluginDescriptor.id(), consoleLogMessage.getMessage());
        switch (consoleLogMessage.getLogLevel()) {
            case INFO:
                goPublisher.taggedConsumeLine(infoTag, message);
                break;
            case ERROR:
                goPublisher.taggedConsumeLine(errorTag, message);
                break;
            default:
                return DefaultGoApiResponse.error(format("Unsupported log level `%s`.", consoleLogMessage.getLogLevel()));
        }
        return DefaultGoApiResponse.success(null);
    }

    private void validateSupportedVersion(GoApiRequest request) {
        if (!goSupportedVersions.contains(request.apiVersion())) {
            throw new RuntimeException(format("Unsupported '%s' API version: %s. Supported versions: %s", request.api(), request.apiVersion(), goSupportedVersions));
        }
    }

    private JsonMessageHandler messageHandler(String apiVersion) {
        return messageHandlerMap.get(apiVersion);
    }
}
