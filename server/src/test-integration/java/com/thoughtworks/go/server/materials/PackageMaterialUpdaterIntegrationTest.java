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

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.io.File;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PackageMaterialUpdaterIntegrationTest {
    @Autowired
    private MaterialRepository materialRepository;
    private ScmMaterialUpdater scmMaterialUpdater;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private TransactionTemplate transactionTemplate;
    private PackageMaterialUpdater packageMaterialUpdater;
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        scmMaterialUpdater = Mockito.mock(ScmMaterialUpdater.class);
        packageMaterialUpdater = new PackageMaterialUpdater(materialRepository, scmMaterialUpdater, transactionTemplate);
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldUpdateMaterialInstanceWhenPluginIsUpgraded() throws Exception {
        final PackageMaterial material = MaterialsMother.packageMaterial();
        final MaterialInstance materialInstance = material.createMaterialInstance();
        materialRepository.saveOrUpdate(materialInstance);

        addMetadata(material, "fieldX", false);
        material.getPackageDefinition().getConfiguration().addNewConfiguration("fieldX", true);
        final List<Modification> modifications = ModificationsMother.multipleModificationList();
        doNothing().when(scmMaterialUpdater).insertLatestOrNewModifications(material, materialInstance, new File(""), new Modifications(modifications));
        transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                packageMaterialUpdater.insertLatestOrNewModifications(material, materialInstance, new File(""), new Modifications(modifications));
                return null;
            }
        });

        MaterialInstance actualInstance = materialRepository.findMaterialInstance(material);
        assertThat(actualInstance.getConfiguration(), is(material.createMaterialInstance().getConfiguration()));
    }

    private void addMetadata(PackageMaterial material, String field, boolean partOfIdentity) {
        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.addConfiguration(new PackageConfiguration(field).with(PackageConfiguration.PART_OF_IDENTITY, partOfIdentity));
        PackageMetadataStore.getInstance().addMetadataFor(material.getPluginId(), packageConfigurations);
    }
}
