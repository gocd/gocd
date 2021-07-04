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

import java.io.File;

import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PackageMaterialUpdaterTest {
    @Mock MaterialRepository materialRepository;
    @Mock ScmMaterialUpdater scmMaterialUpdater;
    @Mock private TransactionTemplate transactionTemplate;
    private PackageMaterialUpdater materialUpdater;

    @BeforeEach
    public void setup() {
        transactionTemplate = new TransactionTemplate(null){
            @Override
            public Object execute(TransactionCallback action) {
                return action.doInTransaction(null);
            }

            @Override
            public Object executeWithExceptionHandling(com.thoughtworks.go.server.transaction.TransactionCallback action) throws Exception {
                return super.executeWithExceptionHandling(action);    //To change body of overridden methods use File | Settings | File Templates.
            }

            @Override
            public <T extends Exception> Object transactionSurrounding(TransactionSurrounding<T> surrounding) throws T {
                return super.transactionSurrounding(surrounding);    //To change body of overridden methods use File | Settings | File Templates.
            }
        };
        materialUpdater = new PackageMaterialUpdater(materialRepository, scmMaterialUpdater, transactionTemplate);
    }

    @Test
    public void shouldUpdateToNewMaterialInstanceWhenConfigHas_Changed() throws Exception {
        PackageMaterial material = MaterialsMother.packageMaterial();
        MaterialInstance materialInstance = material.createMaterialInstance();
        materialInstance.setId(1);

        material.getPackageDefinition().getConfiguration().add(ConfigurationPropertyMother.create("key2", false, "value2"));
        MaterialInstance newMaterialInstance = material.createMaterialInstance();
        newMaterialInstance.setId(1);
        File file = new File("random");

        Modifications modifications = new Modifications();
        when(materialRepository.find(anyLong())).thenReturn(materialInstance);

        materialUpdater.insertLatestOrNewModifications(material, materialInstance, file, modifications);

        verify(materialRepository).saveOrUpdate(newMaterialInstance);
        verify(scmMaterialUpdater).insertLatestOrNewModifications(material, materialInstance, file, modifications);
    }

    @Test
    public void shouldNotUpdateMaterialInstanceWhenConfigHas_NOT_Changed() throws Exception {
        PackageMaterial material = MaterialsMother.packageMaterial();
        MaterialInstance materialInstance = material.createMaterialInstance();

        File file = new File("random");
        Modifications modifications = new Modifications();

        materialUpdater.insertLatestOrNewModifications(material, materialInstance, file, modifications);

        verify(materialRepository, never()).saveOrUpdate(any(MaterialInstance.class));
        verify(scmMaterialUpdater).insertLatestOrNewModifications(material, materialInstance, file, modifications);
    }

    @Test
    public void shouldDelegateToSCMUpdaterToAddNewMaterial() throws Exception {
        PackageMaterial material = MaterialsMother.packageMaterial();
        File file = new File("random");

        materialUpdater.addNewMaterialWithModifications(material, file);

        verify(scmMaterialUpdater).addNewMaterialWithModifications(material, file);
    }
}
