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
package com.thoughtworks.go.remote.work.artifact;

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.domain.ArtifactPlan;
import com.thoughtworks.go.domain.ArtifactPlanType;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.artifact.model.PublishArtifactResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.remote.work.artifact.ArtifactRequestProcessor.Request.CONSOLE_LOG;
import static com.thoughtworks.go.util.GoConstants.PRODUCT_NAME;
import static java.lang.String.format;

public class ArtifactsPublisher implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactsPublisher.class);
    public static final String PLUGGABLE_ARTIFACT_METADATA_FOLDER = "pluggable-artifact-metadata";
    private final PluginRequestProcessorRegistry pluginRequestProcessorRegistry;
    private final File workingDirectory;
    private final GoPublisher goPublisher;
    private final ArtifactPlanFilter artifactPlanFilter;
    private ArtifactExtension artifactExtension;
    private ArtifactStores artifactStores;
    private final List<ArtifactPlan> failedArtifact = new ArrayList<>();

    public ArtifactsPublisher(GoPublisher goPublisher, ArtifactExtension artifactExtension, ArtifactStores artifactStores, PluginRequestProcessorRegistry pluginRequestProcessorRegistry, File workingDirectory) {
        this.goPublisher = goPublisher;
        this.artifactStores = artifactStores;
        this.workingDirectory = workingDirectory;
        this.artifactExtension = artifactExtension;
        this.artifactPlanFilter = new ArtifactPlanFilter();
        this.pluginRequestProcessorRegistry = pluginRequestProcessorRegistry;
    }

    public void publishArtifacts(List<ArtifactPlan> artifactPlans, EnvironmentVariableContext environmentVariableContext) {
        final File pluggableArtifactFolder = publishPluggableArtifacts(artifactPlans, environmentVariableContext);
        try {
            final List<ArtifactPlan> mergedPlans = artifactPlanFilter.getBuiltInMergedArtifactPlans(artifactPlans);

            if (isMetadataFolderEmpty(pluggableArtifactFolder)) {
                LOGGER.info("Pluggable metadata folder is empty.");
            } else if (pluggableArtifactFolder != null) {
                mergedPlans.add(0, new ArtifactPlan(ArtifactPlanType.file, format("%s%s*", pluggableArtifactFolder.getName(), File.separator), PLUGGABLE_ARTIFACT_METADATA_FOLDER));
            }

            for (ArtifactPlan artifactPlan : mergedPlans) {
                try {
                    artifactPlan.publishBuiltInArtifacts(goPublisher, workingDirectory);
                } catch (Exception e) {
                    failedArtifact.add(artifactPlan);
                }
            }

            if (!failedArtifact.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (ArtifactPlan artifactPlan : failedArtifact) {
                    artifactPlan.printArtifactInfo(builder);
                }
                throw new RuntimeException(format("[%s] Uploading finished. Failed to upload %s.", PRODUCT_NAME, builder));
            }
        } finally {
            FileUtils.deleteQuietly(pluggableArtifactFolder);
        }
    }

    private boolean isMetadataFolderEmpty(File pluggableArtifactFolder) {
        return pluggableArtifactFolder != null && pluggableArtifactFolder.list().length == 0;
    }

    private File publishPluggableArtifacts(List<ArtifactPlan> artifactPlans, EnvironmentVariableContext environmentVariableContext) {
        try {
            pluginRequestProcessorRegistry.registerProcessorFor(CONSOLE_LOG.requestName(), ArtifactRequestProcessor.forPublishArtifact(goPublisher, environmentVariableContext));
            final List<ArtifactPlan> pluggableArtifactPlans = artifactPlanFilter.getPluggableArtifactPlans(artifactPlans);
            final Map<ArtifactPlan, ArtifactStore> artifactPlanToStores = artifactStoresToPlugin(pluggableArtifactPlans);

            final PluggableArtifactMetadata pluggableArtifactMetadata = new PluggableArtifactMetadata();
            for (Map.Entry<ArtifactPlan, ArtifactStore> artifactPlanAndStore : artifactPlanToStores.entrySet()) {
                publishPluggableArtifact(pluggableArtifactMetadata, artifactPlanAndStore.getKey(), artifactPlanAndStore.getValue(), environmentVariableContext);
            }

            if (!pluggableArtifactPlans.isEmpty() && pluggableArtifactMetadata.isEmpty()) {
                LOGGER.info(format("[%s] No pluggable artifact metadata to upload.", PRODUCT_NAME));
                goPublisher.taggedConsumeLine(GoPublisher.PUBLISH, format("[%s] No pluggable artifact metadata to upload.", PRODUCT_NAME));
                return null;
            }

            return pluggableArtifactMetadata.write(workingDirectory);
        } finally {
            pluginRequestProcessorRegistry.removeProcessorFor(CONSOLE_LOG.requestName());
        }
    }

    private void publishPluggableArtifact(PluggableArtifactMetadata pluggableArtifactMetadata, ArtifactPlan artifactPlan, ArtifactStore artifactStore, EnvironmentVariableContext environmentVariableContext) {
        try {
            final String pluginId = artifactStore.getPluginId();

            final String message = format("[%s] Start to upload pluggable artifact using plugin `%s`.", PRODUCT_NAME, pluginId);
            goPublisher.taggedConsumeLine(GoPublisher.PUBLISH, message);
            LOGGER.info(message);

            final PublishArtifactResponse publishArtifactResponse = artifactExtension.publishArtifact(pluginId, artifactPlan, artifactStore, workingDirectory.getAbsolutePath(), environmentVariableContext);

            if (!publishArtifactResponse.getMetadata().isEmpty()) {
                final String artifactId = (String) artifactPlan.getPluggableArtifactConfiguration().get("id");
                pluggableArtifactMetadata.addMetadata(pluginId, artifactId, publishArtifactResponse.getMetadata());
            }
        } catch (RuntimeException e) {
            failedArtifact.add(artifactPlan);
            goPublisher.taggedConsumeLine(GoPublisher.ERR, format("[%s] %s", PRODUCT_NAME, e.getMessage()));
            LOGGER.error(e.getMessage(), e);
        }
    }

    private Map<ArtifactPlan, ArtifactStore> artifactStoresToPlugin(List<ArtifactPlan> artifactPlans) {
        final HashMap<ArtifactPlan, ArtifactStore> artifactPlanToArtifactStoreMap = new HashMap<>();
        for (ArtifactPlan artifactPlan : artifactPlans) {
            final String storeId = (String) artifactPlan.getPluggableArtifactConfiguration().get("storeId");
            artifactPlanToArtifactStoreMap.put(artifactPlan, artifactStores.find(storeId));
        }
        return artifactPlanToArtifactStoreMap;
    }
}
