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

import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.server.dao.DependencyMaterialSourceDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.util.Pagination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class DependencyMaterialUpdater implements MaterialUpdater {
    private DependencyMaterialSourceDao dependencyMaterialSourceDao;
    private MaterialRepository materialRepository;

    @Autowired
    public DependencyMaterialUpdater(DependencyMaterialSourceDao dependencyMaterialSourceDao, MaterialRepository materialRepository) {
        this.dependencyMaterialSourceDao = dependencyMaterialSourceDao;
        this.materialRepository = materialRepository;
    }

    @Override
    public void insertLatestOrNewModifications(Material material, MaterialInstance materialInstance, File folder, Modifications list) {
        insertDependencyMaterialRevisions((DependencyMaterial) material, list);
    }

    @Override
    public void addNewMaterialWithModifications(Material material, File folder) {
        insertRevisionsForAllParentStageInstances((DependencyMaterial) material);
    }

    private void insertDependencyMaterialRevisions(final DependencyMaterial dependencyMaterial, Modifications list) {
        if (list.isEmpty()) {
            insertRevisionsForAllParentStageInstances(dependencyMaterial);
        } else {
            insertRevisionsForParentStagesAfter(dependencyMaterial, list);
        }
    }

    private void insertRevisionsForParentStagesAfter(DependencyMaterial dependencyMaterial, Modifications list) {
        Pagination pagination = Pagination.pageStartingAt(0, null, MaterialDatabaseUpdater.STAGES_PER_PAGE);
        List<Modification> modifications = null;
        do {
            modifications = dependencyMaterialSourceDao.getPassedStagesAfter(list.last().getRevision(), dependencyMaterial, pagination);
            for (Modification modification : modifications) {
                MaterialRevision revision = new MaterialRevision(dependencyMaterial, modification);
                materialRepository.saveMaterialRevision(revision);
            }
            pagination = Pagination.pageStartingAt(pagination.getOffset() + pagination.getPageSize(), null, pagination.getPageSize());
        } while (!modifications.isEmpty());
    }

    private void insertRevisionsForAllParentStageInstances(DependencyMaterial dependencyMaterial) {
        Pagination pagination = Pagination.pageStartingAt(0, null, MaterialDatabaseUpdater.STAGES_PER_PAGE);
        List<Modification> modifications;
        do {
            modifications = dependencyMaterialSourceDao.getPassedStagesByName(dependencyMaterial, pagination);
            for (Modification modification : modifications) {
                MaterialRevision revision = new MaterialRevision(dependencyMaterial, modification);
                materialRepository.saveMaterialRevision(revision);
            }
            pagination = Pagination.pageStartingAt(pagination.getOffset() + pagination.getPageSize(), null, pagination.getPageSize());
        } while (!modifications.isEmpty());
    }
}
