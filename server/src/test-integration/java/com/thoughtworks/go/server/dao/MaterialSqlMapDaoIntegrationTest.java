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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.git.GitMaterialInstance;
import com.thoughtworks.go.domain.materials.mercurial.HgMaterialInstance;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialInstance;
import com.thoughtworks.go.domain.materials.perforce.P4MaterialInstance;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialInstance;
import com.thoughtworks.go.domain.materials.svn.SvnMaterialInstance;
import com.thoughtworks.go.domain.materials.tfs.TfsMaterialInstance;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.transaction.SqlMapClientTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class MaterialSqlMapDaoIntegrationTest {
    @Autowired
    private MaterialSqlMapDao materialSqlMapDao;
    @Autowired
    private GoCache goCache;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private SqlMapClientTemplate actualSqlClientTemplate;

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        goCache.clear();

        actualSqlClientTemplate = materialSqlMapDao.getSqlMapClientTemplate();

        goCache.clear();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
    }

    @After
    public void teardown() throws Exception {
        materialSqlMapDao.setSqlMapClientTemplate(actualSqlClientTemplate);
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldFetchModificationsWithMaterial() {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        SvnMaterial material = MaterialsMother.svnMaterial("http://username:password@localhost");
        List<Modification> modificationList = ModificationsMother.multipleModificationList();
        materialRevisions.addRevision(material, modificationList);
        Modification expectedModification = modificationList.get(0);

        dbHelper.saveRevs(materialRevisions);

        Modifications modifications = materialSqlMapDao.getModificationWithMaterial(1);

        assertThat(modifications.size()).isEqualTo(1);
        Modification modification = modifications.get(0);

        assertModificationAreEqual(modification, expectedModification);

        MaterialInstance instance = modification.getMaterialInstance();

        assertThat(instance).isInstanceOf(SvnMaterialInstance.class);
        assertThat(instance.getFingerprint()).isEqualTo(material.getFingerprint());
        assertThat(instance.getUrl()).isEqualTo(material.getUrl());
        assertThat(instance.getUsername()).isEqualTo(material.getUserName());
        assertThat(instance.getBranch()).isNullOrEmpty();
        assertThat(instance.getCheckExternals()).isEqualTo(material.isCheckExternals());
    }

    @Test
    public void shouldFetchMultipleModifications() {
        MaterialRevisions materialRevisions = new MaterialRevisions();

        GitMaterial material = new GitMaterial("git://username:password@localhost");
        material.setSubmoduleFolder("sub_module");
        List<Modification> modificationList = ModificationsMother.multipleModificationList();
        materialRevisions.addRevision(material, modificationList);
        //        addRevision(materialRevisions, new P4Material("localhost:1666", "view"));

        dbHelper.saveRevs(materialRevisions);

        Modifications modifications = materialSqlMapDao.getModificationWithMaterial(2);

        assertThat(modifications.size()).isEqualTo(2);
        assertModificationAreEqual(modifications.get(0), modificationList.get(0));
        assertModificationAreEqual(modifications.get(1), modificationList.get(1));

        MaterialInstance instance1 = modifications.get(0).getMaterialInstance();
        MaterialInstance instance2 = modifications.get(1).getMaterialInstance();

        assertThat(instance1).isInstanceOf(GitMaterialInstance.class);
        assertThat(instance2).isInstanceOf(GitMaterialInstance.class);
        assertThat(instance1).isEqualTo(instance2);

        assertThat(instance1.getFingerprint()).isEqualTo(material.getFingerprint());
        assertThat(instance1.getUrl()).isEqualTo(material.getUrl());
        assertThat(instance1.getUsername()).isEqualTo(material.getUserName());
        assertThat(instance1.getBranch()).isEqualTo(material.getBranch());
        assertThat(instance1.getSubmoduleFolder()).isEqualTo(material.getSubmoduleFolder());
    }

    @Test
    public void shouldFetchDetailsRelatedToHg() {
        MaterialRevisions materialRevisions = new MaterialRevisions();

        HgMaterial material = MaterialsMother.hgMaterial("http://username:password@localhost");
        material.setBranch("master");
        List<Modification> modificationList = ModificationsMother.multipleModificationList();
        materialRevisions.addRevision(material, modificationList);

        dbHelper.saveRevs(materialRevisions);

        Modifications modifications = materialSqlMapDao.getModificationWithMaterial(1);
        MaterialInstance instance = modifications.get(0).getMaterialInstance();

        assertThat(instance).isInstanceOf(HgMaterialInstance.class);
        assertThat(instance.getFingerprint()).isEqualTo(material.getFingerprint());
        assertThat(instance.getUrl()).isEqualTo(material.getUrl());
        assertThat(instance.getUsername()).isEqualTo(material.getUserName());
        assertThat(instance.getBranch()).isEqualTo(material.getBranch());
    }

    @Test
    public void shouldFetchDetailsRelatedToP4() {
        MaterialRevisions materialRevisions = new MaterialRevisions();

        P4Material material = new P4Material("localhost:1666", "view");
        List<Modification> modificationList = ModificationsMother.multipleModificationList();
        materialRevisions.addRevision(material, modificationList);

        dbHelper.saveRevs(materialRevisions);

        Modifications modifications = materialSqlMapDao.getModificationWithMaterial(1);

        assertThat(modifications.size()).isEqualTo(1);

        MaterialInstance instance = modifications.get(0).getMaterialInstance();

        assertThat(instance).isInstanceOf(P4MaterialInstance.class);
        assertThat(instance.getFingerprint()).isEqualTo(material.getFingerprint());
        assertThat(instance.getUrl()).isEqualTo(material.getUrl());
        assertThat(instance.getUsername()).isEqualTo(material.getUserName());
        assertThat(instance.getView()).isEqualTo(material.getView());
        assertThat(instance.getUseTickets()).isEqualTo(material.getUseTickets());
    }

    @Test
    public void shouldFetchDetailsRelatedToTfs() {
        MaterialRevisions materialRevisions = new MaterialRevisions();

        TfsMaterial material = MaterialsMother.tfsMaterial("http://tfs.com");
        List<Modification> modificationList = ModificationsMother.multipleModificationList();
        materialRevisions.addRevision(material, modificationList);

        dbHelper.saveRevs(materialRevisions);

        Modifications modifications = materialSqlMapDao.getModificationWithMaterial(1);

        assertThat(modifications.size()).isEqualTo(1);

        MaterialInstance instance = modifications.get(0).getMaterialInstance();

        assertThat(instance).isInstanceOf(TfsMaterialInstance.class);
        assertThat(instance.getFingerprint()).isEqualTo(material.getFingerprint());
        assertThat(instance.getUrl()).isEqualTo(material.getUrl());
        assertThat(instance.getUsername()).isEqualTo(material.getUserName());
        assertThat(instance.getProjectPath()).isEqualTo(material.getProjectPath());
        assertThat(instance.getDomain()).isEqualTo(material.getDomain());
    }

    @Test
    public void shouldFetchDetailsRelatedToPackage() {
        MaterialRevisions materialRevisions = new MaterialRevisions();

        PackageMaterial material = MaterialsMother.packageMaterial();
        List<Modification> modificationList = ModificationsMother.multipleModificationList();
        materialRevisions.addRevision(material, modificationList);

        dbHelper.saveRevs(materialRevisions);

        Modifications modifications = materialSqlMapDao.getModificationWithMaterial(1);

        assertThat(modifications.size()).isEqualTo(1);

        MaterialInstance instance = modifications.get(0).getMaterialInstance();

        assertThat(instance).isInstanceOf(PackageMaterialInstance.class);
        assertThat(instance.getFingerprint()).isEqualTo(material.getFingerprint());
        assertThat(instance.getAdditionalData()).isNullOrEmpty();
        PackageMaterial packageMaterial = JsonHelper.fromJson(instance.getConfiguration(), PackageMaterial.class);
        assertThat(packageMaterial).isEqualTo(material);
    }

    @Test
    public void shouldFetchDetailsRelatedToPluginMaterial() {
        MaterialRevisions materialRevisions = new MaterialRevisions();

        PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
        List<Modification> modificationList = ModificationsMother.multipleModificationList();
        materialRevisions.addRevision(material, modificationList);

        dbHelper.saveRevs(materialRevisions);

        Modifications modifications = materialSqlMapDao.getModificationWithMaterial(1);

        assertThat(modifications.size()).isEqualTo(1);

        MaterialInstance instance = modifications.get(0).getMaterialInstance();

        assertThat(instance).isInstanceOf(PluggableSCMMaterialInstance.class);
        assertThat(instance.getFingerprint()).isEqualTo(material.getFingerprint());
        assertThat(instance.getAdditionalData()).isNullOrEmpty();
        PluggableSCMMaterial pluggableSCMMaterial = JsonHelper.fromJson(instance.getConfiguration(), PluggableSCMMaterial.class);
        assertThat(pluggableSCMMaterial).isEqualTo(material);
    }

    private void assertModificationAreEqual(Modification actualModification, Modification expectedModification) {
        assertThat(actualModification.getRevision()).isEqualTo(expectedModification.getRevision());
        assertThat(actualModification.getModifiedTime()).isEqualTo(expectedModification.getModifiedTime());
        assertThat(actualModification.getComment()).isEqualTo(expectedModification.getComment());
        assertThat(actualModification.getUserName()).isEqualTo(expectedModification.getUserName());
        assertThat(actualModification.getEmailAddress()).isEqualTo(expectedModification.getEmailAddress());
    }
}
