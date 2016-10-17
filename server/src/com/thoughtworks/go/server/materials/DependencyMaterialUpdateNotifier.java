/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.initializers.Initializer;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.lang.String.format;

@Component
public class DependencyMaterialUpdateNotifier implements StageStatusListener, ConfigChangedListener, Initializer, MaterialUpdateCompleteListener {
    private static final Logger LOGGER = Logger.getLogger(DependencyMaterialUpdateNotifier.class);
    private final GoConfigService goConfigService;
    private final MaterialConfigConverter materialConfigConverter;
    private final MaterialUpdateService materialUpdateService;

    private Map<String, Material> dependencyMaterials;
    private Set<Material> retryQueue = Collections.synchronizedSet(new HashSet<Material>());

    @Autowired
    public DependencyMaterialUpdateNotifier(GoConfigService goConfigService, MaterialConfigConverter materialConfigConverter,
                                            MaterialUpdateService materialUpdateService) {
        this.goConfigService = goConfigService;
        this.materialConfigConverter = materialConfigConverter;
        this.materialUpdateService = materialUpdateService;
    }

    public void initialize() {
        loadDependencyMaterials();

        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
        materialUpdateService.registerMaterialUpdateCompleteListener(this);

        updateMaterialsOnIntialization();
    }

    @Override
    public void onMaterialUpdate(Material material) {
        if (material instanceof DependencyMaterial) {
            if (retryQueue.remove(material)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(format("[Material Update] Retrying update of dependency material %s ", material));
                }
                updateMaterial(material);
            }
        }
    }

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
        scheduleNewMaterialsForUpdate();
    }

    protected EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener() {
        final DependencyMaterialUpdateNotifier self = this;

        return new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onEntityConfigChange(PipelineConfig pipelineConfig) {
                self.scheduleNewMaterialsForUpdate();
            }
        };
    }

    private void updateMaterial(Material material) {
        try {
            if (!materialUpdateService.updateMaterial(material)) {
                retryQueue.add(material);
            }
        } catch (Exception e) {
            retryQueue.add(material);
        }
    }

    private void updateMaterialsOnIntialization() {
        for (Material material : this.dependencyMaterials.values()) {
            updateMaterial(material);
        }
    }

    private void scheduleNewMaterialsForUpdate() {
        Collection<Material> materialsBeforeConfigChange = dependencyMaterials.values();

        loadDependencyMaterials();

        Collection<Material> materialsAfterConfigChange = dependencyMaterials.values();

        Collection newMaterials = CollectionUtils.subtract(materialsAfterConfigChange, materialsBeforeConfigChange);

        for(Object material : newMaterials) {
            updateMaterial((Material) material);
        }
    }

    private void loadDependencyMaterials() {
        this.dependencyMaterials = new HashMap<>();
        for (MaterialConfig materialConfig : goConfigService.getSchedulableDependencyMaterials()) {
            String stageIdentifier = stageIdentifier(((DependencyMaterialConfig) materialConfig).getPipelineName().toString(), ((DependencyMaterialConfig) materialConfig).getStageName().toString());
            this.dependencyMaterials.put(stageIdentifier, materialConfigConverter.toMaterial(materialConfig));
        }
    }

    private String stageIdentifier(String pipelineName, String stageName) {
        return String.format("%s[%s]", pipelineName.toLowerCase(), stageName.toLowerCase());
    }
}