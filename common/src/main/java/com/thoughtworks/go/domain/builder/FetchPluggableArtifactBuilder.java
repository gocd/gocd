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
package com.thoughtworks.go.domain.builder;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.artifact.models.FetchArtifactEnvironmentVariable;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.remote.work.artifact.ArtifactRequestProcessor;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.command.*;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.remote.work.artifact.ArtifactRequestProcessor.Request.CONSOLE_LOG;
import static java.lang.String.format;

public class FetchPluggableArtifactBuilder extends Builder {
    private static final Logger LOGGER = LoggerFactory.getLogger(FetchPluggableArtifactBuilder.class);

    private final JobIdentifier jobIdentifier;
    private final String artifactId;
    private final File metadataFileDest;
    private ChecksumFileHandler checksumFileHandler;
    private ArtifactStore artifactStore;
    private Configuration configuration;
    private final String metadataFileLocationOnServer;

    public FetchPluggableArtifactBuilder(RunIfConfigs conditions, Builder cancelBuilder, String description,
                                         JobIdentifier jobIdentifier, ArtifactStore artifactStore, Configuration configuration,
                                         String artifactId, String source, File metadataFileDest, ChecksumFileHandler checksumFileHandler) {
        super(conditions, cancelBuilder, description);
        this.jobIdentifier = jobIdentifier;
        this.artifactStore = artifactStore;
        this.configuration = configuration;
        this.artifactId = artifactId;
        this.metadataFileDest = metadataFileDest;
        this.checksumFileHandler = checksumFileHandler;
        this.metadataFileLocationOnServer = source;
    }

    @Override
    public void build(DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, TaskExtension taskExtension, ArtifactExtension artifactExtension, PluginRequestProcessorRegistry pluginRequestProcessorRegistry, String consoleLogCharset) {
        downloadMetadataFile(publisher);
        try {
            pluginRequestProcessorRegistry.registerProcessorFor(CONSOLE_LOG.requestName(), ArtifactRequestProcessor.forFetchArtifact(publisher, environmentVariableContext));
            final String message = format("[%s] Fetching pluggable artifact using plugin %s.", GoConstants.PRODUCT_NAME, artifactStore.getPluginId());
            LOGGER.info(message);
            publisher.taggedConsumeLine(TaggedStreamConsumer.OUT, message);

            List<FetchArtifactEnvironmentVariable> newEnvironmentVariables = artifactExtension.fetchArtifact(
                    artifactStore.getPluginId(), artifactStore, configuration, getMetadataFromFile(artifactId), agentWorkingDirectory());

            updateEnvironmentVariableContextWith(publisher, environmentVariableContext, newEnvironmentVariables);

        } catch (Exception e) {
            publisher.taggedConsumeLine(TaggedStreamConsumer.ERR, e.getMessage());
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            pluginRequestProcessorRegistry.removeProcessorFor(CONSOLE_LOG.requestName());
        }
    }

    private void updateEnvironmentVariableContextWith(DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext, List<FetchArtifactEnvironmentVariable> newEnvironmentVariables) {
        for (FetchArtifactEnvironmentVariable variable : newEnvironmentVariables) {
            String name = variable.name();

            String message = format(" NOTE: Setting new environment variable: %s = %s", name, variable.displayValue());
            if (environmentVariableContext.hasProperty(name)) {
                message = format("WARNING: Replacing environment variable: %s = %s (previously: %s)", name, variable.displayValue(), environmentVariableContext.getPropertyForDisplay(name));
            }

            publisher.taggedConsumeLine(TaggedStreamConsumer.OUT, message);
            environmentVariableContext.setProperty(name, variable.value(), variable.isSecure());
        }
    }

    private String agentWorkingDirectory() {
        return metadataFileDest.getParentFile().getAbsolutePath();
    }

    private void downloadMetadataFile(DefaultGoPublisher publisher) {
        final FetchArtifactBuilder fetchArtifactBuilder = new FetchArtifactBuilder(conditions, getCancelBuilder(), getDescription(), jobIdentifier, metadataFileLocationOnServer, metadataFileDest.getName(), getHandler(), checksumFileHandler);

        publisher.fetch(fetchArtifactBuilder);
    }

    public FetchHandler getHandler() {
        return new FileHandler(metadataFileDest, metadataFileLocationOnServer);
    }

    public String metadataFileLocator() {
        return jobIdentifier.artifactLocator(metadataFileDest.getName());
    }

    private Map<String, Object> getMetadataFromFile(String artifactId) throws IOException {
        final String fileToString = FileUtils.readFileToString(metadataFileDest, StandardCharsets.UTF_8);
        LOGGER.debug(format("Reading metadata from file %s.", metadataFileDest.getAbsolutePath()));
        final Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        final Map<String, Map> allArtifactsPerPlugin = new GsonBuilder().create().fromJson(fileToString, type);
        return allArtifactsPerPlugin.get(artifactId);
    }

    public JobIdentifier getJobIdentifier() {
        return jobIdentifier;
    }

}
