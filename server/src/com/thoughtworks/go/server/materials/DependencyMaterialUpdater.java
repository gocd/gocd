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

import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DependencyMaterialSourceDao;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.util.Pagination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

@Component
public class DependencyMaterialUpdater implements MaterialUpdater, StageStatusListener {
    public static final String DEPENDENCY_MATERIAL_CACHE_KEY_FORMAT = MaterialDatabaseUpdater.class.getName() + "_dependencyMaterialLock_%s_%s";

    private GoCache goCache;
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private DependencyMaterialSourceDao dependencyMaterialSourceDao;
    private MaterialRepository materialRepository;
    private final MaterialService materialService;

    @Autowired
    public DependencyMaterialUpdater(GoCache goCache, TransactionSynchronizationManager transactionSynchronizationManager, DependencyMaterialSourceDao dependencyMaterialSourceDao,
                                     MaterialRepository materialRepository, MaterialService materialService) {
        this.goCache = goCache;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.dependencyMaterialSourceDao = dependencyMaterialSourceDao;
        this.materialRepository = materialRepository;
        this.materialService = materialService;
    }

    public void insertLatestOrNewModifications(Material material, MaterialInstance materialInstance, File folder, Modifications list) {
        insertDependencyMaterialRevisions((DependencyMaterial) material, list);
    }

    public void addNewMaterialWithModifications(Material material, File folder) {
        insertRevisionsForAllParentStageInstances((DependencyMaterial) material);
    }

    private void insertDependencyMaterialRevisions(final DependencyMaterial dependencyMaterial, Modifications list) {
        final String key = cacheKeyForDependencyMaterial(dependencyMaterial);
        if (goCache.isKeyInCache(key)) {
            return;
        }

        if (list.isEmpty()) {
            insertRevisionsForAllParentStageInstances(dependencyMaterial);
        } else {
            insertRevisionsForParentStagesAfter(dependencyMaterial, list);
        }

        transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                goCache.putInAfterCommit(key, "IS UP TO DATE");
            }
        });
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

    public void stageStatusChanged(Stage stage) {
        if (StageResult.Passed == stage.getResult()) {
            String key = cacheKeyForDependencyMaterial(stage);
            synchronized (key) {
                removeCacheKey(key);
            }
        }
    }

    void removeCacheKey(String key) {
        goCache.remove(key);
    }

    static String cacheKeyForDependencyMaterial(Stage stage) {
        return String.format(DEPENDENCY_MATERIAL_CACHE_KEY_FORMAT, stage.getIdentifier().getPipelineName().toLowerCase(), stage.getIdentifier().getStageName().toLowerCase()).intern();
    }

    static String cacheKeyForDependencyMaterial(Material material) {
        if (material instanceof DependencyMaterial) {
            DependencyMaterial dep = ((DependencyMaterial) material);
            return String.format(DEPENDENCY_MATERIAL_CACHE_KEY_FORMAT, dep.getPipelineName().toLower(), dep.getStageName().toLower()).intern();
        } else {
            return String.format(DEPENDENCY_MATERIAL_CACHE_KEY_FORMAT, material.getFingerprint(), "-this-lock-should-not-be-acquired-by-anyone-else-inadvertently").intern();
        }
    }
}
