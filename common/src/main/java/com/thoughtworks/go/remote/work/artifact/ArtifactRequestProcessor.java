/*
 * Copyright 2022 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.access.artifact.ArtifactExtensionConstants;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.remote.work.artifact.ConsoleLogMessage.LogLevel;
import com.thoughtworks.go.util.command.*;
import com.thoughtworks.go.work.GoPublisher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

public class ArtifactRequestProcessor implements GoPluginApiRequestProcessor {
    private static final List<String> goSupportedVersions = ArtifactExtensionConstants.SUPPORTED_VERSIONS;
    private final SafeOutputStreamConsumer safeOutputStreamConsumer;
    private final ProcessType processType;

    private enum ProcessType {
        FETCH, PUBLISH
    }
    private static final Map<LogLevel, String> FETCH_ARTIFACT_LOG_LEVEL_TAG = new HashMap<LogLevel, String>() {{
        put(LogLevel.INFO, TaggedStreamConsumer.OUT);
        put(LogLevel.ERROR, TaggedStreamConsumer.ERR);
    }};
    private static final Map<LogLevel, String> PUBLISH_ARTIFACT_LOG_LEVEL_TAG = new HashMap<LogLevel, String>() {{
        put(LogLevel.INFO, TaggedStreamConsumer.PUBLISH);
        put(LogLevel.ERROR, TaggedStreamConsumer.PUBLISH_ERR);
    }};

    private ArtifactRequestProcessor(GoPublisher publisher, ProcessType processType, EnvironmentVariableContext environmentVariableContext) {
        CompositeConsumer errorStreamConsumer = new CompositeConsumer(CompositeConsumer.ERR,  publisher);
        CompositeConsumer outputStreamConsumer = new CompositeConsumer(CompositeConsumer.OUT,  publisher);
        this.safeOutputStreamConsumer = new SafeOutputStreamConsumer(new ProcessOutputStreamConsumer(errorStreamConsumer, outputStreamConsumer));
        safeOutputStreamConsumer.addSecrets(environmentVariableContext.secrets());
        this.processType = processType;
    }

    public static ArtifactRequestProcessor forFetchArtifact(GoPublisher goPublisher, EnvironmentVariableContext environmentVariableContext) {
        return new ArtifactRequestProcessor(goPublisher, ProcessType.FETCH, environmentVariableContext);
    }

    public static ArtifactRequestProcessor forPublishArtifact(GoPublisher goPublisher, EnvironmentVariableContext environmentVariableContext) {
        return new ArtifactRequestProcessor(goPublisher, ProcessType.PUBLISH, environmentVariableContext);
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
        Optional<String> parsedTag = parseTag(processType, consoleLogMessage.getLogLevel());
        if (parsedTag.isPresent()) {
            safeOutputStreamConsumer.taggedStdOutput(parsedTag.get(), message);
            return DefaultGoApiResponse.success(null);
        }
        return DefaultGoApiResponse.error(format("Unsupported log level `%s`.", consoleLogMessage.getLogLevel()));
    }

    private Optional<String> parseTag(ProcessType requestType, LogLevel logLevel) {
        switch (requestType) {
            case FETCH:
                return Optional.ofNullable(FETCH_ARTIFACT_LOG_LEVEL_TAG.get(logLevel));
            case PUBLISH:
                return Optional.ofNullable(PUBLISH_ARTIFACT_LOG_LEVEL_TAG.get(logLevel));
        }
        return Optional.empty();
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
