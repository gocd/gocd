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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.transaction.TransactionCallback;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import java.io.File;
import java.util.List;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * @understands how to update materials on the database from the real SCMs
 */
@Component
public class MaterialDatabaseUpdater {
    public static final String MATERIALS_MUTEX_FORMAT = MaterialDatabaseUpdater.class.getName() + "_MaterialMutex_%s_%s";
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialDatabaseUpdater.class);
    static final int STAGES_PER_PAGE = 100;

    private final MaterialRepository materialRepository;
    private final ServerHealthService healthService;
    private TransactionTemplate transactionTemplate;
    private final DependencyMaterialUpdater dependencyMaterialUpdater;
    private final ScmMaterialUpdater scmMaterialUpdater;
    private MaterialExpansionService materialExpansionService;
    private GoConfigService goConfigService;
    private PackageMaterialUpdater packageMaterialUpdater;
    private PluggableSCMMaterialUpdater pluggableSCMMaterialUpdater;

    @Autowired
    public MaterialDatabaseUpdater(MaterialRepository materialRepository, ServerHealthService healthService, TransactionTemplate transactionTemplate,
                                   DependencyMaterialUpdater dependencyMaterialUpdater, ScmMaterialUpdater scmMaterialUpdater, PackageMaterialUpdater packageMaterialUpdater,
                                   PluggableSCMMaterialUpdater pluggableSCMMaterialUpdater, MaterialExpansionService materialExpansionService, GoConfigService goConfigService) {
        this.materialRepository = materialRepository;
        this.healthService = healthService;
        this.transactionTemplate = transactionTemplate;
        this.dependencyMaterialUpdater = dependencyMaterialUpdater;
        this.scmMaterialUpdater = scmMaterialUpdater;
        this.packageMaterialUpdater = packageMaterialUpdater;
        this.pluggableSCMMaterialUpdater = pluggableSCMMaterialUpdater;
        this.materialExpansionService = materialExpansionService;
        this.goConfigService = goConfigService;
    }

    public void updateMaterial(final Material material) throws Exception {
        String materialMutex = mutexForMaterial(material);
        HealthStateScope scope = HealthStateScope.forMaterial(material);
        try {
            MaterialInstance materialInstance = materialRepository.findMaterialInstance(material);
            if (materialInstance == null) {
                LOGGER.debug("[Material Update] Material repository not found, creating with latest revision from {}", material);

                synchronized (materialMutex) {
                    if (materialRepository.findMaterialInstance(material) == null) {
                        transactionTemplate.executeWithExceptionHandling(new TransactionCallback() {
                            @Override
                            public Object doInTransaction(TransactionStatus status) {
                                initializeMaterialWithLatestRevision(material);
                                return null;
                            }
                        });
                    }
                }
            } else {
                LOGGER.debug("[Material Update] Existing material repository, fetching new revisions from {} in flyweight {}", material, materialInstance.getFlyweightName());

                synchronized (materialMutex) {
                    transactionTemplate.executeWithExceptionHandling(new TransactionCallback() {
                        @Override
                        public Object doInTransaction(TransactionStatus status) {
                            updateMaterialWithNewRevisions(material);
                            return null;
                        }
                    });
                }
            }
            healthService.removeByScope(scope);
        } catch (Exception e) {
            List<CaseInsensitiveString> pipelineNames = goConfigService.pipelinesWithMaterial(material.config().getFingerprint());
            String message = escapeHtml4("Modification check failed for material: " + material.getLongDescription());
            String affectedPipelinesMessage = "";
            if (pipelineNames.isEmpty()) {
                affectedPipelinesMessage = ("\nNo pipelines are affected by this material, perhaps this material is unused.");
            } else {
                affectedPipelinesMessage = ("\nAffected pipelines are " + StringUtils.join(pipelineNames, ", ") + ".");
            }
            String finalMessage = message + affectedPipelinesMessage;
            String errorDescription = e.getMessage() == null ? "Unknown error" : escapeHtml4(e.getMessage());
            healthService.update(ServerHealthState.errorWithHtml(finalMessage, errorDescription, HealthStateType.general(scope)));
            LOGGER.debug("[Material Update] {}", message, e);
            throw e;
        }
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

    private String mutexForMaterial(Material material) {
        if (material instanceof DependencyMaterial) {
            DependencyMaterial dep = ((DependencyMaterial) material);
            return String.format(MATERIALS_MUTEX_FORMAT, dep.getPipelineName().toLower(), dep.getStageName().toLower()).intern();
        } else {
            return String.format(MATERIALS_MUTEX_FORMAT, material.getFingerprint(), "-this-lock-should-not-be-acquired-by-anyone-else-inadvertently").intern();
        }
    }
}
