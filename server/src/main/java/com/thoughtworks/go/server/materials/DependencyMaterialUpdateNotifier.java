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
package com.thoughtworks.go.server.materials;


import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.thoughtworks.go.serverhealth.HealthStateType.general;
import static com.thoughtworks.go.serverhealth.ServerHealthState.error;
import static java.lang.String.format;

/**
 * Listens to Stage/Config changes and notifies MaterialUpdateService to update DependencyMaterial
 */

@Component
public class DependencyMaterialUpdateNotifier implements StageStatusListener, ConfigChangedListener, Initializer, MaterialUpdateCompleteListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyMaterialUpdateNotifier.class);
    private final GoConfigService goConfigService;
    private final MaterialConfigConverter materialConfigConverter;
    private final MaterialUpdateService materialUpdateService;
    private ServerHealthService serverHealthService;
    private boolean skipUpdate = false;

    private volatile Map<String, Material> dependencyMaterials;
    private Set<Material> retryQueue = Collections.synchronizedSet(new HashSet<>());

    @Autowired
    public DependencyMaterialUpdateNotifier(GoConfigService goConfigService, MaterialConfigConverter materialConfigConverter,
                                            MaterialUpdateService materialUpdateService, ServerHealthService serverHealthService) {
        this.goConfigService = goConfigService;
        this.materialConfigConverter = materialConfigConverter;
        this.materialUpdateService = materialUpdateService;
        this.serverHealthService = serverHealthService;
    }

    @Override
    public void initialize() {
        this.dependencyMaterials = dependencyMaterials();

        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
        materialUpdateService.registerMaterialUpdateCompleteListener(this);

        updateMaterialsOnIntialization();
    }

    @Override
    public void startDaemon() {

    }

    @Override
    public void onMaterialUpdate(Material material) {
        if (material instanceof DependencyMaterial) {
            if (retryQueue.remove(material)) {
                LOGGER.debug("[Material Update] Retrying update of dependency material {} ", material);
                updateMaterial(material);
            }
        }
    }

    @Override
    public void stageStatusChanged(Stage stage) {
        if (StageResult.Passed == stage.getResult()) {
            Material material = dependencyMaterials.get(stageIdentifier(stage.getIdentifier().getPipelineName(), stage.getName()));

            if (material != null) {
                updateMaterial(material);
            }
        }
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        scheduleRecentlyAddedMaterialsForUpdate();
    }

    protected EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener() {
        final DependencyMaterialUpdateNotifier self = this;

        return new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onEntityConfigChange(PipelineConfig pipelineConfig) {
                self.scheduleRecentlyAddedMaterialsForUpdate();
            }
        };
    }

    //    for integration tests
    public void disableUpdates() {
        this.skipUpdate = true;
    }

    //    for integration tests
    public void enableUpdates() {
        this.skipUpdate = false;
    }

    private void updateMaterial(Material material) {
        if (skipUpdate) return;

        try {
            if (!materialUpdateService.updateMaterial(material)) {
                retryQueue.add(material);
            }
        } catch (Exception e) {
            HealthStateScope scope = HealthStateScope.forMaterialUpdate(material);
            serverHealthService.update(error(format("Error updating Dependency Material %s", material.getUriForDisplay()), e.getMessage(), general(scope)));
            retryQueue.add(material);
        }
    }

    private void updateMaterialsOnIntialization() {
        for (Material material : this.dependencyMaterials.values()) {
            updateMaterial(material);
        }
    }

    private void scheduleRecentlyAddedMaterialsForUpdate() {
        Collection<Material> materialsBeforeConfigChange = dependencyMaterials.values();

        this.dependencyMaterials = dependencyMaterials();

        Collection<Material> materialsAfterConfigChange = dependencyMaterials.values();

        Collection newMaterials = CollectionUtils.subtract(materialsAfterConfigChange, materialsBeforeConfigChange);

        for (Object material : newMaterials) {
            updateMaterial((Material) material);
        }
    }

    private HashMap<String, Material> dependencyMaterials() {
        HashMap<String, Material> map = new HashMap<>();
        for (DependencyMaterialConfig materialConfig : goConfigService.getSchedulableDependencyMaterials()) {
            String stageIdentifier = stageIdentifier(materialConfig.getPipelineName().toString(), materialConfig.getStageName().toString());
            map.put(stageIdentifier, materialConfigConverter.toMaterial(materialConfig));
        }
        return map;
    }

    private String stageIdentifier(String pipelineName, String stageName) {
        return String.format("%s[%s]", pipelineName.toLowerCase(), stageName.toLowerCase());
    }
}
