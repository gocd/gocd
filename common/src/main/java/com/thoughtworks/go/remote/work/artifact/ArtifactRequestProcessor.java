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

package com.thoughtworks.go.remote.work.artifact;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.work.GoPublisher;

import java.util.List;

import static com.thoughtworks.go.util.command.TaggedStreamConsumer.PUBLISH;
import static com.thoughtworks.go.util.command.TaggedStreamConsumer.PUBLISH_ERR;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public class ArtifactRequestProcessor implements GoPluginApiRequestProcessor {
    private static final List<String> goSupportedVersions = asList("1.0");
    private final GoPublisher goPublisher;

    public ArtifactRequestProcessor(GoPublisher goPublisher) {
        this.goPublisher = goPublisher;
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest request) {
        validatePluginRequest(request);
        switch (Request.fromString(request.api())) {
            case CONSOLE_LOG:
                return processConsoleLogRequest(pluginDescriptor, request);
            default:
                return DefaultGoApiResponse.error("Illegal api request");
        }
    }

    private GoApiResponse processConsoleLogRequest(GoPluginDescriptor pluginDescriptor, GoApiRequest request) {
        final ConsoleLogMessage consoleLogMessage = ConsoleLogMessage.fromJSON(request.requestBody());
        final String message = format("[%s] %s", pluginDescriptor.id(), consoleLogMessage.getMessage());
        switch (consoleLogMessage.getLogLevel()) {
            case INFO:
                goPublisher.taggedConsumeLine(PUBLISH, message);
                break;
            case ERROR:
                goPublisher.taggedConsumeLine(PUBLISH_ERR, message);
                break;
            default:
                return DefaultGoApiResponse.error(format("Unsupported log level `%s`.", consoleLogMessage.getLogLevel()));
        }
        return DefaultGoApiResponse.success(null);
    }

    private void validatePluginRequest(GoApiRequest goPluginApiRequest) {
        if (!goSupportedVersions.contains(goPluginApiRequest.apiVersion())) {
            throw new RuntimeException(format("Unsupported '%s' API version: %s. Supported versions: %s", goPluginApiRequest.api(), goPluginApiRequest.apiVersion(), goSupportedVersions));
        }
    }

    public enum Request {
        CONSOLE_LOG("go.processor.artifact.console-log");
        private final String requestName;

        Request(String requestName) {
            this.requestName = requestName;
        }

        public static Request fromString(String requestName) {
            if (requestName != null) {
                for (Request request : Request.values()) {
                    if (requestName.equalsIgnoreCase(request.requestName)) {
                        return request;
                    }
                }
            }

            return null;
        }

        public String requestName() {
            return requestName;
        }
    }
}
