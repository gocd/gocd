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

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.domain.ArtifactPlan;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.work.GoPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

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
        publishPluggableArtifact(goPublisher, artifactPlans);

        final List<ArtifactPlan> mergedPlans = artifactPlanFilter.getBuiltInMergedArtifactPlans(artifactPlans);

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
            throw new RuntimeException(format("[%s] Uploading finished. Failed to upload %s", GoConstants.PRODUCT_NAME, builder));
        }
    }

    private void publishPluggableArtifact(GoPublisher goPublisher, List<ArtifactPlan> artifactPlans) {
        final List<ArtifactPlan> pluggableArtifactPlans = artifactPlanFilter.getPluggableArtifactPlans(artifactPlans);
        final Map<String, Map<ArtifactStore, List<ArtifactPlan>>> artifactStoresToPlugin = artifactStoresToPlugin(pluggableArtifactPlans);

        //TODO: Put publish call in try catch
        for (Map.Entry<String, Map<ArtifactStore, List<ArtifactPlan>>> artifactStoreAndPlansForAPlugin : artifactStoresToPlugin.entrySet()) {
            final String message = format("Start to upload pluggable artifact using plugin `%s`", artifactStoreAndPlansForAPlugin.getKey());
            goPublisher.taggedConsumeLine(GoPublisher.PUBLISH, message);
            LOGGER.info(message);
            
            final Map<String, Object> metadata = artifactExtension.publishArtifact(artifactStoreAndPlansForAPlugin.getKey(), artifactStoreAndPlansForAPlugin.getValue());
        }

        //TODO: Upload metadata files to server as build artifact
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
