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

import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialInstance;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionCallback;

import java.io.File;

@Component
public class PluggableSCMMaterialUpdater implements MaterialUpdater {
    private final MaterialRepository materialRepository;
    private final ScmMaterialUpdater scmMaterialUpdater;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public PluggableSCMMaterialUpdater(MaterialRepository materialRepository, ScmMaterialUpdater scmMaterialUpdater, TransactionTemplate transactionTemplate) {
        this.materialRepository = materialRepository;
        this.scmMaterialUpdater = scmMaterialUpdater;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void insertLatestOrNewModifications(final Material material, MaterialInstance materialInstance, File folder, Modifications list) {
        final PluggableSCMMaterialInstance currentMaterialInstance = (PluggableSCMMaterialInstance) materialInstance;

        final PluggableSCMMaterialInstance latestMaterialInstance = (PluggableSCMMaterialInstance) material.createMaterialInstance();
        if (currentMaterialInstance.shouldUpgradeTo(latestMaterialInstance)) {
            transactionTemplate.execute((TransactionCallback) transactionStatus -> {
                PluggableSCMMaterialInstance materialInstance1 = (PluggableSCMMaterialInstance) materialRepository.find(currentMaterialInstance.getId());
                materialInstance1.upgradeTo(latestMaterialInstance);
                materialRepository.saveOrUpdate(materialInstance1);
                return materialInstance1;
            });
        }
        scmMaterialUpdater.insertLatestOrNewModifications(material, currentMaterialInstance, folder, list);
    }

    @Override
    public void addNewMaterialWithModifications(Material material, File folder) {
        scmMaterialUpdater.addNewMaterialWithModifications(material, folder);
    }
}
