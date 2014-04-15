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

import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class PackageMaterialUpdaterIntegrationTest {
    @Autowired private MaterialRepository materialRepository;
    @Autowired private ScmMaterialUpdater scmMaterialUpdater;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private TransactionTemplate transactionTemplate;
    private PackageMaterialUpdater packageMaterialUpdater;

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        packageMaterialUpdater = new PackageMaterialUpdater(materialRepository, scmMaterialUpdater, transactionTemplate);
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldUpdateMaterialInstanceWhenPluginIsUpgraded() throws Exception {
        PackageMaterial material = MaterialsMother.packageMaterial();
        MaterialInstance materialInstance = material.createMaterialInstance();
        materialRepository.saveOrUpdate(materialInstance);

        addMetadata(material, "fieldX", false);
        material.getPackageDefinition().getConfiguration().addNewConfiguration("fieldX", true);
        List<Modification> modifications = ModificationsMother.multipleModificationList();

        packageMaterialUpdater.insertLatestOrNewModifications(material, materialInstance,new File(""), new Modifications(modifications));

        MaterialInstance actualInstance = materialRepository.findMaterialInstance(material);
        Assert.assertThat(actualInstance.getConfiguration(), is(material.createMaterialInstance().getConfiguration()));
    }

    private void addMetadata(PackageMaterial material, String field, boolean partOfIdentity) {
        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.addConfiguration(new PackageConfiguration(field).with(PackageConfiguration.PART_OF_IDENTITY, partOfIdentity));
        PackageMetadataStore.getInstance().addMetadataFor(material.getPluginId(), packageConfigurations);
    }
}
