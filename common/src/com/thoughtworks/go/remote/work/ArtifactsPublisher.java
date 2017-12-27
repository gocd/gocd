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

package com.thoughtworks.go.remote.work;

import com.google.gson.Gson;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.domain.ArtifactPlan;
import com.thoughtworks.go.domain.ArtifactType;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.plugin.access.artifact.model.PublishArtifactResponse;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.thoughtworks.go.util.GoConstants.PRODUCT_NAME;
import static java.lang.String.format;
import static java.lang.String.join;

public class ArtifactsPublisher implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactsPublisher.class);
    private ArtifactPlanFilter artifactPlanFilter;
    private ArtifactExtension artifactExtension;
    private ArtifactStores artifactStores;

    public ArtifactsPublisher(ArtifactPlanFilter artifactPlanFilter, ArtifactExtension artifactExtension, ArtifactStores artifactStores) {
        this.artifactPlanFilter = artifactPlanFilter;
        this.artifactExtension = artifactExtension;
        this.artifactStores = artifactStores;
    }

    public ArtifactsPublisher(ArtifactExtension artifactExtension, ArtifactStores artifactStores) {
        this(new ArtifactPlanFilter(), artifactExtension, artifactStores);
    }

    public void publishArtifacts(GoPublisher goPublisher, File workingDirectory, List<ArtifactPlan> artifactPlans) {
        final File pluggableArtifactFolder = publishPluggableArtifact(goPublisher, workingDirectory, artifactPlans);
        final List<ArtifactPlan> mergedPlans = artifactPlanFilter.getBuiltInMergedArtifactPlans(artifactPlans);

        if (pluggableArtifactFolder != null) {
            mergedPlans.add(0, new ArtifactPlan(ArtifactType.file, format("%s%s*", pluggableArtifactFolder.getName(), File.separator), "pluggable-artifact-metadata"));
        }

        List<ArtifactPlan> failedArtifact = new ArrayList<>();
        for (ArtifactPlan artifactPlan : mergedPlans) {
            try {
                artifactPlan.publish(goPublisher, workingDirectory);
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
    }

    private File publishPluggableArtifact(GoPublisher goPublisher, File workingDirectory, List<ArtifactPlan> artifactPlans) {
        final List<ArtifactPlan> pluggableArtifactPlans = artifactPlanFilter.getPluggableArtifactPlans(artifactPlans);
        final Map<String, Map<ArtifactStore, List<ArtifactPlan>>> artifactStoresToPlugin = artifactStoresToPlugin(pluggableArtifactPlans);

        final Map<String, PublishArtifactResponse> publishArtifactResponses = new HashMap<>();
        for (Map.Entry<String, Map<ArtifactStore, List<ArtifactPlan>>> artifactStoreAndPlansForAPlugin : artifactStoresToPlugin.entrySet()) {
            try {
                final String pluginId = artifactStoreAndPlansForAPlugin.getKey();
                final String message = format("[%s] Start to upload pluggable artifact using plugin `%s`.", PRODUCT_NAME, pluginId);
                goPublisher.taggedConsumeLine(GoPublisher.PUBLISH, message);
                LOGGER.info(message);

                final PublishArtifactResponse publishArtifactResponse = artifactExtension.publishArtifact(pluginId, artifactStoreAndPlansForAPlugin.getValue());
                publishArtifactResponses.put(pluginId, publishArtifactResponse);

                writeErrorToConsoleLog(goPublisher, publishArtifactResponse.getErrors());
            } catch (RuntimeException e) {
                goPublisher.taggedConsumeLine(GoPublisher.ERR, format("[%s] %s", PRODUCT_NAME, e.getMessage()));
                LOGGER.error(e.getMessage(), e);
            }
        }

        if (publishArtifactResponses.isEmpty()) {
            LOGGER.info(format("[%s] No pluggable artifact metadata to upload.", PRODUCT_NAME));
            goPublisher.taggedConsumeLine(GoPublisher.PUBLISH, format("[%s] No pluggable artifact metadata to upload.", PRODUCT_NAME));
            return null;
        }

        final File pluggableArtifactMetadataFolder = new File(workingDirectory, UUID.randomUUID().toString());

        if (!pluggableArtifactMetadataFolder.mkdirs()) {
            throw new RuntimeException(format("[%s] Could not create pluggable artifact metadata folder `%s`.", PRODUCT_NAME, pluggableArtifactMetadataFolder.getName()));
        }


        for (Map.Entry<String, PublishArtifactResponse> entry : publishArtifactResponses.entrySet()) {
            writeMetadataFile(pluggableArtifactMetadataFolder, entry.getKey(), entry.getValue());
        }

        return pluggableArtifactMetadataFolder;
    }


    private void writeErrorToConsoleLog(GoPublisher goPublisher, List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        goPublisher.taggedConsumeLine(GoPublisher.PUBLISH_ERR, join("\n", errors));
    }

    private void writeMetadataFile(File pluggableArtifactMetadataFolder, String pluginId, PublishArtifactResponse response) {
        if (response.getMetadata() == null || response.getMetadata().isEmpty()) {
            return;
        }

        try {
            LOGGER.info(String.format("Writing metadata file for plugin `%s`.", pluginId));
            FileUtils.writeStringToFile(new File(pluggableArtifactMetadataFolder, format("%s.json", pluginId)), new Gson().toJson(response.getMetadata()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Map<ArtifactStore, List<ArtifactPlan>>> artifactStoresToPlugin(List<ArtifactPlan> artifactPlans) {
        final Map<String, Map<ArtifactStore, List<ArtifactPlan>>> artifactStoresToPlugin = new HashMap<>();
        final Map<String, List<ArtifactPlan>> artifactPlansToArtifactStore = artifactPlansToArtifactStore(artifactPlans);

        for (ArtifactStore artifactStore : artifactStores) {
            final String pluginId = artifactStore.getPluginId();
            artifactStoresToPlugin.putIfAbsent(pluginId, new HashMap<>());
            artifactStoresToPlugin.get(pluginId).put(artifactStore, artifactPlansToArtifactStore.get(artifactStore.getId()));
        }

        return artifactStoresToPlugin;
    }

    public Map<String, List<ArtifactPlan>> artifactPlansToArtifactStore(List<ArtifactPlan> artifactPlans) {
        final Map<String, List<ArtifactPlan>> artifactPlansToArtifactStore = new HashMap<>();
        for (ArtifactPlan artifactPlan : artifactPlans) {
            final String storeId = (String) artifactPlan.getPluggableArtifactConfiguration().get("storeId");
            artifactPlansToArtifactStore.putIfAbsent(storeId, new ArrayList<>());
            artifactPlansToArtifactStore.get(storeId).add(artifactPlan);
        }
        return artifactPlansToArtifactStore;
    }
}
