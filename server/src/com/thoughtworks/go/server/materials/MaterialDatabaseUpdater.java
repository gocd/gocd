/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.transaction.TransactionCallback;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import java.io.File;

/**
 * @understands how to update materials on the database from the real SCMs
 */
@Component
public class MaterialDatabaseUpdater {
    private static final Logger LOGGER = Logger.getLogger(MaterialDatabaseUpdater.class);
    static final int STAGES_PER_PAGE = 100;

    private final MaterialRepository materialRepository;
    private final ServerHealthService healthService;
    private TransactionTemplate transactionTemplate;
    private final GoCache goCache;
    private final DependencyMaterialUpdater dependencyMaterialUpdater;
    private final ScmMaterialUpdater scmMaterialUpdater;
    private MaterialExpansionService materialExpansionService;
    private PackageMaterialUpdater packageMaterialUpdater;
    private PluggableSCMMaterialUpdater pluggableSCMMaterialUpdater;

    @Autowired
    public MaterialDatabaseUpdater(MaterialRepository materialRepository, ServerHealthService healthService, TransactionTemplate transactionTemplate,
                                   GoCache goCache, DependencyMaterialUpdater dependencyMaterialUpdater, ScmMaterialUpdater scmMaterialUpdater, PackageMaterialUpdater packageMaterialUpdater,
                                   PluggableSCMMaterialUpdater pluggableSCMMaterialUpdater, MaterialExpansionService materialExpansionService) {
        this.materialRepository = materialRepository;
        this.healthService = healthService;
        this.transactionTemplate = transactionTemplate;
        this.goCache = goCache;
        this.dependencyMaterialUpdater = dependencyMaterialUpdater;
        this.scmMaterialUpdater = scmMaterialUpdater;
        this.packageMaterialUpdater = packageMaterialUpdater;
        this.pluggableSCMMaterialUpdater = pluggableSCMMaterialUpdater;
        this.materialExpansionService = materialExpansionService;
    }

    public void updateMaterial(final Material material) throws Exception {
        String cacheKeyForMaterial = DependencyMaterialUpdater.cacheKeyForDependencyMaterial(material);
        if (thisDependencyMaterialHasAlreadyBeenProcessed(material, cacheKeyForMaterial)) {
            return;
        }

        HealthStateScope scope = HealthStateScope.forMaterial(material);
        try {
            MaterialInstance materialInstance = materialRepository.findMaterialInstance(material);
            if (materialInstance == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("[Material Update] Material repository not found, creating with latest revision from %s", material));
                }

                synchronized (cacheKeyForMaterial) {
                    if (materialRepository.findMaterialInstance(material) == null) {
                        transactionTemplate.executeWithExceptionHandling(new TransactionCallback() {
                            @Override
                            public Object doInTransaction(TransactionStatus status) throws Exception {
                                initializeMaterialWithLatestRevision(material);
                                return null;
                            }
                        });
                    }
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("[Material Update] Existing material repository, fetching new revisions from %s in flyweight %s", material, materialInstance.getFlyweightName()));
                }

                synchronized (cacheKeyForMaterial) {
                    transactionTemplate.executeWithExceptionHandling(new TransactionCallback() {
                        @Override
                        public Object doInTransaction(TransactionStatus status) throws Exception {
                            updateMaterialWithNewRevisions(material);
                            return null;
                        }
                    });
                }
            }
            healthService.removeByScope(scope);
        } catch (Exception e) {
            String message = "Modification check failed for material: " + material.getLongDescription();
            String errorDescription = e.getMessage() == null ? "Unknown error" : e.getMessage();
            healthService.update(ServerHealthState.error(message, errorDescription, HealthStateType.general(scope)));
            LOGGER.warn(String.format("[Material Update] %s", message), e);
            throw e;
        }
    }

    private boolean thisDependencyMaterialHasAlreadyBeenProcessed(Material material, String cacheKeyForMaterial) {
        return material instanceof DependencyMaterial && goCache.isKeyInCache(cacheKeyForMaterial);
    }

    private void initializeMaterialWithLatestRevision(Material material) {
        Materials materials = new Materials();
        materialExpansionService.expandForHistory(material, materials);
        for (Material expanded : materials) {
            addNewMaterialWithModifications(folderFor(expanded), expanded, updater(expanded));
        }
    }

    void updateMaterialWithNewRevisions(Material material) {
        Materials materials = new Materials();
        materialExpansionService.expandForHistory(material, materials);
        for (Material expanded : materials) {
            MaterialInstance expandedInstance = materialRepository.findMaterialInstance(expanded);
            File expandedFolder = folderFor(expanded);
            if (expandedInstance == null) {
                addNewMaterialWithModifications(expandedFolder, expanded, updater(expanded));
            } else {
                insertLatestOrNewModifications(expanded, expandedInstance, expandedFolder, updater(expanded));
            }
        }
    }

    private void insertLatestOrNewModifications(Material material, MaterialInstance materialInstance, File folder, MaterialUpdater updater) {
        MaterialRevisions materialRevisions = materialRepository.findLatestModification(material);
        Modifications list = materialRevisions.getModifications(material);
        updater.insertLatestOrNewModifications(material, materialInstance, folder, list);
    }

    MaterialUpdater updater(Material material) {
        if (material instanceof DependencyMaterial) {
            return dependencyMaterialUpdater;
        }
        if (material instanceof PackageMaterial) {
            return packageMaterialUpdater;
        }
        if (material instanceof PluggableSCMMaterial) {
            return pluggableSCMMaterialUpdater;
        }
        return scmMaterialUpdater;
    }


    private File folderFor(Material material) {
        return this.materialRepository.folderFor(material);
    }

    private void addNewMaterialWithModifications(File folder, Material expanded, MaterialUpdater updater) {
        updater.addNewMaterialWithModifications(expanded, folder);
    }
}
