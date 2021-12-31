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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.joda.time.DateTimeUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provides a list of unique SCMMaterials to be updated which will be consumed by MaterialUpdateService
 */

@Component
public class SCMMaterialSource extends EntityConfigChangedListener<ConfigRepoConfig> implements ConfigChangedListener, MaterialSource, MaterialUpdateCompleteListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SCMMaterialSource.class);

    private final GoConfigService goConfigService;
    private ConcurrentMap<Material, Long> materialLastUpdateTimeMap = new ConcurrentHashMap<>();
    private final MaterialConfigConverter materialConfigConverter;
    private final MaterialUpdateService materialUpdateService;
    private final long materialUpdateInterval;
    private Set<Material> schedulableMaterials;

    @Autowired
    public SCMMaterialSource(GoConfigService goConfigService, SystemEnvironment systemEnvironment,
                             MaterialConfigConverter materialConfigConverter, MaterialUpdateService materialUpdateService) {
        this.goConfigService = goConfigService;
        this.materialConfigConverter = materialConfigConverter;
        this.materialUpdateService = materialUpdateService;
        this.materialUpdateInterval = systemEnvironment.getMaterialUpdateIdleInterval();
    }

    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(new InternalConfigChangeListener() {
            @Override
            public void onEntityConfigChange(Object entity) {
                updateSchedulableMaterials(true);
            }
        });
        materialUpdateService.registerMaterialSources(this);
        materialUpdateService.registerMaterialUpdateCompleteListener(this);
    }

    @Override
    public Set<Material> materialsForUpdate() {
        updateSchedulableMaterials(false);

        return materialsWithUpdateIntervalElapsed();
    }

    @Override
    public void onMaterialUpdate(Material material) {
        if (!(material instanceof DependencyMaterial)) {
            updateLastUpdateTimeForScmMaterial(material);
        }
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        updateSchedulableMaterials(true);
    }

    @Override
    public void onEntityConfigChange(ConfigRepoConfig entity) {
        updateSchedulableMaterials(true);
    }

    protected EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener() {
        final SCMMaterialSource self = this;
        return new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onEntityConfigChange(PipelineConfig pipelineConfig) {
                self.onConfigChange(null);
            }
        };
    }

    private Set<Material> materialsWithUpdateIntervalElapsed() {
        Set<Material> materialsForUpdate = new HashSet<>();
        for (Material material : schedulableMaterials) {
            if (hasUpdateIntervalElapsedForScmMaterial(material)) {
                materialsForUpdate.add(material);
            }
        }

        return materialsForUpdate;
    }

    boolean hasUpdateIntervalElapsedForScmMaterial(Material material) {
        Long lastMaterialUpdateTime = materialLastUpdateTimeMap.get(material);
        if (lastMaterialUpdateTime != null) {
            boolean shouldUpdateMaterial = (DateTimeUtils.currentTimeMillis() - lastMaterialUpdateTime) >= materialUpdateInterval;
            if (LOGGER.isDebugEnabled() && !shouldUpdateMaterial) {
                LOGGER.debug("[Material Update] Skipping update of material {} which has been last updated at {}", material, new Date(lastMaterialUpdateTime));
            }
            return shouldUpdateMaterial;
        }
        return true;
    }

    private void updateLastUpdateTimeForScmMaterial(Material material) {
        materialLastUpdateTimeMap.put(material, DateTimeUtils.currentTimeMillis());
    }

    private void updateSchedulableMaterials(boolean forceLoad) {
        if (forceLoad || schedulableMaterials == null) {
            schedulableMaterials = materialConfigConverter.toMaterials(goConfigService.getSchedulableSCMMaterials());
        }
    }

    private abstract class InternalConfigChangeListener extends EntityConfigChangedListener<Object> {
        private final List<Class<?>> securityConfigClasses = Arrays.asList(
                PipelineConfig.class,
                PackageDefinition.class,
                PackageRepository.class,
                SCM.class
        );

        @Override
        public boolean shouldCareAbout(Object entity) {
            return securityConfigClasses.stream().anyMatch(aClass -> aClass.isAssignableFrom(entity.getClass()));
        }
    }
}
