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

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.MaterialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
class ScmMaterialUpdater implements MaterialUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScmMaterialUpdater.class);

    private MaterialRepository materialRepository;
    private LegacyMaterialChecker materialChecker;
    private final SubprocessExecutionContext subprocessExecutionContext;
    private final MaterialService materialService;

    @Autowired
    ScmMaterialUpdater(MaterialRepository materialRepository, LegacyMaterialChecker materialChecker,
                       SubprocessExecutionContext subprocessExecutionContext, MaterialService materialService) {
        this.materialRepository = materialRepository;
        this.materialChecker = materialChecker;
        this.subprocessExecutionContext = subprocessExecutionContext;
        this.materialService = materialService;
    }

    @Override
    public void insertLatestOrNewModifications(Material material, MaterialInstance materialInstance, File folder, Modifications list) {
        List<Modification> newChanges = list.isEmpty() ?
                materialChecker.findLatestModification(folder, material, subprocessExecutionContext) :
                materialService.modificationsSince(material, folder, list.latestRevision(material), subprocessExecutionContext);
        if (newChanges.isEmpty()) {
            LOGGER.debug("[Material Update] Did not find any new modifications for material '{}' with flyweight '{}' using working directory '{}'", material, material.getFingerprint(), folder.getAbsolutePath());
        } else {
            LOGGER.info("[Material Update] Found '{}' modifications for material '{}' with flyweight '{}' using working directory '{}'", newChanges.size(), material, material.getFingerprint(), folder.getAbsolutePath());

            materialRepository.saveModifications(materialInstance, newChanges);
        }
    }

    @Override
    public void addNewMaterialWithModifications(Material material, File folder) {
        MaterialRevisions materialRevisions = materialRepository.findLatestModification(material);
        Modifications list = materialRevisions.getModifications(material);
        insertLatestOrNewModifications(material, materialRepository.findOrCreateFrom(material), folder, list);
    }
}
