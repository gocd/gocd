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

import java.io.File;
import java.util.List;

import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.MaterialService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class ScmMaterialUpdater implements MaterialUpdater {
    private static final Logger LOGGER = Logger.getLogger(ScmMaterialUpdater.class);

    private MaterialRepository materialRepository;
    private LegacyMaterialChecker materialChecker;
    private final SubprocessExecutionContext subprocessExecutionContext;
    private final MaterialService materialService;
    private ScmMaterialCheckoutService scmMaterialCheckoutService;

    @Autowired
    ScmMaterialUpdater(MaterialRepository materialRepository, LegacyMaterialChecker materialChecker,
                       SubprocessExecutionContext subprocessExecutionContext, MaterialService materialService,
                       ScmMaterialCheckoutService scmMaterialCheckoutService) {
        this.materialRepository = materialRepository;
        this.materialChecker = materialChecker;
        this.subprocessExecutionContext = subprocessExecutionContext;
        this.materialService = materialService;
        this.scmMaterialCheckoutService = scmMaterialCheckoutService;
    }

    public void insertLatestOrNewModifications(Material material, MaterialInstance materialInstance, File folder, Modifications list) {
        String revision;
        List<Modification> newChanges;
        if(list.isEmpty()) {
            newChanges = materialChecker.findLatestModification(folder, material, subprocessExecutionContext);
            revision = newChanges.get(newChanges.size() -1).getRevision();
        }
        else {
            Revision lastRevision = list.latestRevision(material);
            newChanges = materialService.modificationsSince(material, folder, lastRevision, subprocessExecutionContext);
            revision = newChanges.isEmpty() ? lastRevision.getRevision() : newChanges.get(newChanges.size() - 1).getRevision();
        }
        if (newChanges.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        String.format("[Material Update] Did not find any new modifications for material '%s' with flyweight '%s' using working directory '%s'", material, material.getFingerprint(),
                                folder.getAbsolutePath()));
            }
        } else {
            LOGGER.info(String.format("[Material Update] Found '%s' modifications for material '%s' with flyweight '%s' using working directory '%s'", newChanges.size(), material,
                    material.getFingerprint(), folder.getAbsolutePath()));
        }
        materialRepository.saveModifications(materialInstance, newChanges);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    String.format("[Material Update] Passing checkout directory '%s' for further use '%s' with flyweight '%s' ",
                            folder.getAbsolutePath(),material, material.getFingerprint()));
        }
        this.scmMaterialCheckoutService.onCheckoutComplete(material,newChanges,folder,revision);
    }

    public void addNewMaterialWithModifications(Material material, File folder) {
        MaterialRevisions materialRevisions = materialRepository.findLatestModification(material);
        Modifications list = materialRevisions.getModifications(material);
        insertLatestOrNewModifications(material, materialRepository.findOrCreateFrom(material), folder, list);
    }
}
