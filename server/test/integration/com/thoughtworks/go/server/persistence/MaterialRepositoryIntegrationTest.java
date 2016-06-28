/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.persistence;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialInstance;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialInstance;
import com.thoughtworks.go.domain.materials.svn.SvnMaterialInstance;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.database.DatabaseStrategy;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.service.ScheduleTestUtil;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.util.json.JsonHelper;
import com.thoughtworks.go.utils.SerializationTester;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class MaterialRepositoryIntegrationTest {

    @Autowired MaterialRepository repo;
    @Autowired
    GoCache goCache;
    @Autowired PipelineSqlMapDao pipelineSqlMapDao;
    @Autowired DatabaseAccessHelper dbHelper;
    @Autowired SessionFactory sessionFactory;
    @Autowired TransactionSynchronizationManager transactionSynchronizationManager;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired private InstanceFactory instanceFactory;
    @Autowired private MaterialConfigConverter materialConfigConverter;
    @Autowired private MaterialExpansionService materialExpansionService;
    @Autowired private DatabaseStrategy databaseStrategy;

    private HibernateTemplate originalTemplate;
    private String md5 = "md5-test";
    private ScheduleTestUtil u;

    @Before
    public void setUp() throws Exception {
        originalTemplate = repo.getHibernateTemplate();
        dbHelper.onSetUp();
        goCache.clear();
        u = new ScheduleTestUtil(transactionTemplate, repo, dbHelper, new GoConfigFileHelper());
    }

    @After
    public void tearDown() throws Exception {
        goCache.clear();
        repo.setHibernateTemplate(originalTemplate);
        dbHelper.onTearDown();
    }

    @Test
    public void shouldBeAbleToPersistAMaterial() throws Exception {
        MaterialInstance original = new SvnMaterialInstance("url", "username", UUID.randomUUID().toString(), true);
        repo.saveOrUpdate(original);

        MaterialInstance loaded = repo.find(original.getId());

        assertThat(loaded, is(original));
    }

    @Test
    public void shouldBeAbleToPersistADependencyMaterial() {
        MaterialInstance materialInstance = new DependencyMaterial(new CaseInsensitiveString("name"), new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"),
                "serverAlias").createMaterialInstance();
        repo.saveOrUpdate(materialInstance);

        MaterialInstance loaded = repo.find(materialInstance.getId());
        assertThat(loaded, is(materialInstance));
    }

    @Test
    public void shouldCacheMaterialInstanceOnSaveAndUpdate() {
        SvnMaterial originalMaterial = MaterialsMother.svnMaterial();
        MaterialInstance materialInstance = originalMaterial.createMaterialInstance();
        repo.saveOrUpdate(materialInstance);

        assertThat(repo.find(materialInstance.getId()), is(materialInstance));

        SvnMaterial changedMaterial = MaterialsMother.svnMaterial();
        changedMaterial.setPassword("SomethingElse");
        MaterialInstance changedInstance = changedMaterial.createMaterialInstance();
        changedInstance.setId(materialInstance.getId());
        repo.saveOrUpdate(changedInstance);

        assertThat(repo.find(materialInstance.getId()), is(changedInstance));
    }

    @Test
    public void findModificationsFor_shouldCacheModifications() {
        HibernateTemplate mockTemplate = mock(HibernateTemplate.class);
        repo.setHibernateTemplate(mockTemplate);

        ArrayList modifications = new ArrayList();
        when(mockTemplate.find("FROM Modification WHERE materialId = ? AND id BETWEEN ? AND ? ORDER BY id DESC", new Object[]{10L, -1L, -1L})).thenReturn(modifications);
        MaterialInstance materialInstance = material().createMaterialInstance();
        materialInstance.setId(10);
        when(mockTemplate.findByCriteria(any(DetachedCriteria.class))).thenReturn(asList(materialInstance));

        PipelineMaterialRevision pmr = pipelineMaterialRevision();
        List<Modification> actual;
        actual = repo.findModificationsFor(pmr);
        actual = repo.findModificationsFor(pmr);

        assertSame(modifications, actual);

        verify(mockTemplate, times(1)).find("FROM Modification WHERE materialId = ? AND id BETWEEN ? AND ? ORDER BY id DESC", new Object[]{10L, -1L, -1L});
    }

    @Test
    public void findPipelineMaterialRevisions_shouldCacheResults() {
        HibernateTemplate mockTemplate = mock(HibernateTemplate.class);
        repo.setHibernateTemplate(mockTemplate);

        repo.findPipelineMaterialRevisions(2);
        repo.findPipelineMaterialRevisions(2);

        verify(mockTemplate, times(1)).find("FROM PipelineMaterialRevision WHERE pipelineId = ? ORDER BY id", 2L);
    }

    @Test
    public void findModificationsSince_shouldNotCacheIfTheResultsetLarge() {
        SvnMaterial material = MaterialsMother.svnMaterial();
        MaterialRevision first = saveOneScmModification(material, "user1", "file1");
        MaterialRevision second = saveOneScmModification(material, "user2", "file2");

        goCache.clear();
        repo = new MaterialRepository(sessionFactory, goCache, 1, transactionSynchronizationManager, materialConfigConverter, materialExpansionService, databaseStrategy);

        repo.findModificationsSince(material, first);
        assertThat(repo.cachedModifications(repo.findMaterialInstance(material)), is(nullValue()));

        repo.findModificationsSince(material, second);
        assertThat(repo.cachedModifications(repo.findMaterialInstance(material)), is(notNullValue()));
        repo.findModificationsSince(material, first);
        assertThat(repo.cachedModifications(repo.findMaterialInstance(material)), is(nullValue()));
    }

    @Test
    public void findModificationsSince_shouldHandleConcurrentModificationToCache() throws InterruptedException {
        final SvnMaterial svn = MaterialsMother.svnMaterial();
        final MaterialRevision first = saveOneScmModification(svn, "user1", "file1");
        final MaterialRevision second = saveOneScmModification(svn, "user2", "file2");
        final MaterialRevision third = saveOneScmModification(svn, "user2", "file3");

        repo = new MaterialRepository(sessionFactory, goCache = new GoCache(goCache) {
            @Override
            public Object get(String key) {
                Object value = super.get(key);
                TestUtils.sleepQuietly(200); // sleep so we can have multiple threads enter the critical section
                return value;
            }
        }, 200, transactionSynchronizationManager, materialConfigConverter, materialExpansionService, databaseStrategy);

        Thread thread1 = new Thread(new Runnable() {
            public void run() {
                repo.findModificationsSince(svn, first);
            }
        });
        thread1.start();
        TestUtils.sleepQuietly(50);

        Thread thread2 = new Thread(new Runnable() {
            public void run() {
                repo.findModificationsSince(svn, second);
            }
        });
        thread2.start();

        thread1.join();
        thread2.join();

        assertThat(repo.cachedModifications(repo.findMaterialInstance(svn)).size(), is(3));
    }

    @Test
    public void findModificationsSince_shouldCacheResults() {
        SvnMaterial material = MaterialsMother.svnMaterial();
        MaterialRevision zero = saveOneScmModification(material, "user1", "file1");
        MaterialRevision first = saveOneScmModification(material, "user1", "file1");
        MaterialRevision second = saveOneScmModification(material, "user2", "file2");
        MaterialRevision third = saveOneScmModification(material, "user2", "file2");

        repo.findModificationsSince(material, first);

        HibernateTemplate mockTemplate = mock(HibernateTemplate.class);
        repo.setHibernateTemplate(mockTemplate);
        List<Modification> modifications = repo.findModificationsSince(material, first);
        assertThat(modifications.size(), is(2));
        assertEquals(third.getLatestModification(), modifications.get(0));
        assertEquals(second.getLatestModification(), modifications.get(1));
        verifyNoMoreInteractions(mockTemplate);
    }

    @Test
    public void findLatestModifications_shouldCacheResults() {
        SvnMaterial material = MaterialsMother.svnMaterial();
        MaterialInstance materialInstance = material.createMaterialInstance();
        repo.saveOrUpdate(materialInstance);

        Modification mod = ModificationsMother.oneModifiedFile("file3");
        mod.setId(8);

        HibernateTemplate mockTemplate = mock(HibernateTemplate.class);
        repo.setHibernateTemplate(mockTemplate);
        when(mockTemplate.execute((HibernateCallback) any())).thenReturn(mod);

        repo.findLatestModification(materialInstance);

        Modification modification = repo.findLatestModification(materialInstance);
        assertSame(mod, modification);

        verify(mockTemplate, times(1)).execute((HibernateCallback) any());

    }

    @Test
    public void findLatestModifications_shouldQueryIfNotEnoughElementsInCache() {
        SvnMaterial material = MaterialsMother.svnMaterial();
        MaterialRevision mod = saveOneScmModification(material, "user2", "file3");
        goCache.remove(repo.latestMaterialModificationsKey(repo.findMaterialInstance(material)));
        HibernateTemplate mockTemplate = mock(HibernateTemplate.class);
        repo.setHibernateTemplate(mockTemplate);
        when(mockTemplate.execute(any(HibernateCallback.class))).thenReturn(mod.getModification(0));
        Modification modification = repo.findLatestModification(repo.findMaterialInstance(material));

        assertThat(modification, is(mod.getLatestModification()));
        verify(mockTemplate).execute(any(HibernateCallback.class));
    }

    @Test
    public void findLatestModifications_shouldQueryIfNotEnoughElementsInCache_Integration() {
        SvnMaterial material = MaterialsMother.svnMaterial();
        MaterialRevision mod = saveOneScmModification(material, "user2", "file3");
        goCache.clear();
        Modification modification = repo.findLatestModification(repo.findMaterialInstance(material));
        assertEquals(mod.getLatestModification(), modification);
    }

    @Test
    public void shouldFindMaterialInstanceIfExists() throws Exception {
        Material svn = MaterialsMother.svnMaterial();
        MaterialInstance material1 = repo.findOrCreateFrom(svn);
        MaterialInstance material2 = repo.findOrCreateFrom(svn);

        assertThat(material1.getId(), is(material2.getId()));
    }

    @Test
    public void materialShouldNotBeSameIfOneFieldIsNull() throws Exception {
        Material svn1 = MaterialsMother.svnMaterial("url", null, "username", "password", false, null);
        MaterialInstance material1 = repo.findOrCreateFrom(svn1);

        Material svn2 = MaterialsMother.svnMaterial("url", null, null, null, false, null);
        MaterialInstance material2 = repo.findOrCreateFrom(svn2);

        assertThat(material1.getId(), not(is(material2.getId())));
    }

    @Test
    public void findOrCreateFrom_shouldEnsureOnlyOneThreadCanCreateAtATime() throws Exception {
        final Material svn = MaterialsMother.svnMaterial("url", null, "username", "password", false, null);

        HibernateTemplate mockTemplate = mock(HibernateTemplate.class);
        repo = new MaterialRepository(repo.getSessionFactory(), goCache, 200, transactionSynchronizationManager, materialConfigConverter, materialExpansionService, databaseStrategy) {
            @Override
            public MaterialInstance findMaterialInstance(Material material) {
                MaterialInstance result = super.findMaterialInstance(material);
                TestUtils.sleepQuietly(20); // force multiple threads to try to create the material
                return result;
            }

            @Override
            public void saveOrUpdate(MaterialInstance material) {
                material.setId(10);
                super.saveOrUpdate(material);
            }
        };

        repo.setHibernateTemplate(mockTemplate);
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    repo.findOrCreateFrom(svn);
                }
            }, "thread-" + i);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        verify(mockTemplate, times(1)).saveOrUpdate(Mockito.<MaterialInstance>any());
    }

    @Test
    public void findOrCreateFrom_shouldCacheMaterialInstanceOnCreate() throws Exception {
        Material svn = MaterialsMother.svnMaterial("url", null, "username", "password", false, null);

        MaterialInstance instance = repo.findOrCreateFrom(svn);
        assertThat(instance, is(notNullValue()));

        HibernateTemplate mockTemplate = mock(HibernateTemplate.class);
        repo.setHibernateTemplate(mockTemplate);

        MaterialInstance cachedInstance = repo.findMaterialInstance(svn);
        assertSame(instance, cachedInstance);

        verifyNoMoreInteractions(mockTemplate);
    }

    @Test
    public void findMaterialInstance_shouldCacheMaterialInstance() throws Exception {
        Material svn1 = MaterialsMother.svnMaterial("url", null, "username", "password", false, null);
        repo.saveOrUpdate(svn1.createMaterialInstance());

        MaterialInstance instance = repo.findMaterialInstance(svn1);

        HibernateTemplate mockTemplate = mock(HibernateTemplate.class);
        repo.setHibernateTemplate(mockTemplate);

        MaterialInstance cachedInstance = repo.findMaterialInstance(svn1);
        assertSame(instance, cachedInstance);

        verifyNoMoreInteractions(mockTemplate);
    }

    @Test
    public void shouldMaterialCacheKeyShouldReturnTheSameInstance() {
        Material svn = MaterialsMother.svnMaterial("url", null, "username", "password", false, null);
        assertSame(repo.materialKey(svn), repo.materialKey(svn));
    }

    @Test
    public void shouldBeAbleToPersistAMaterialWithNullBooleans() throws Exception {
        P4Material p4Material = new P4Material("serverAndPort", "view");

        MaterialInstance original = p4Material.createMaterialInstance();
        repo.saveOrUpdate(original);
        MaterialInstance loaded = repo.find(original.getId());

        Material restored = loaded.toOldMaterial(null, null, null);

        assertThat((P4Material) restored, is(p4Material));
    }

    @Test
    public void shouldPersistModificationsWithMaterials() throws Exception {
        MaterialInstance original = new SvnMaterialInstance("url", "username", UUID.randomUUID().toString(), false);
        repo.saveOrUpdate(original);

        MaterialInstance loaded = repo.find(original.getId());
        assertThat(loaded, is(original));
    }

    @Test
    public void shouldPersistModifiedFiles() throws Exception {
        MaterialInstance original = new SvnMaterialInstance("url", "username", UUID.randomUUID().toString(), true);
        Modification modification = new Modification("user", "comment", "email", new Date(), ModificationsMother.nextRevision());
        modification.createModifiedFile("file1", "folder1", ModifiedAction.added);
        modification.createModifiedFile("file2", "folder2", ModifiedAction.deleted);
        repo.saveOrUpdate(original);

        MaterialInstance loaded = repo.find(original.getId());
        assertThat(loaded, is(original));
    }

    @Test
    public void shouldBeAbleToFindModificationsSinceAPreviousChange() throws Exception {
        SvnMaterial original = MaterialsMother.svnMaterial();

        MaterialRevision originalRevision = saveOneScmModification(original, "user1", "file1");

        MaterialRevision later = saveOneScmModification(original, "user2", "file2");

        List<Modification> modifications = repo.findModificationsSince(original, originalRevision);
        assertEquals(later.getLatestModification(), modifications.get(0));
    }

    @Test
    public void shouldFindNoModificationsSinceLatestChange() throws Exception {
        SvnMaterial original = MaterialsMother.svnMaterial();

        MaterialRevision originalRevision = saveOneScmModification(original, "user", "file1");

        List<Modification> modifications = repo.findModificationsSince(original, originalRevision);
        assertThat(modifications.size(), is(0));
    }


    @Test
    public void materialsShouldBeSerializable() throws Exception {
        SvnMaterial svnMaterial = MaterialsMother.svnMaterial();
        Modification modification = new Modification("user", "comment", "email", new Date(), "revision");
        modification.createModifiedFile("file1", "folder1", ModifiedAction.added);
        modification.createModifiedFile("file2", "folder2", ModifiedAction.deleted);

        final MaterialRevision materialRevision = new MaterialRevision(svnMaterial, modification);
        MaterialInstance materialInstance = (MaterialInstance) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                return repo.saveMaterialRevision(materialRevision);
            }
        });

        List<Modification> mods = repo.findMaterialRevisionsForMaterial(materialInstance.getId());

        List<Modification> deserialized = (List<Modification>) SerializationTester.serializeAndDeserialize(mods);
        assertThat(deserialized, is(mods));
    }

    @Test
    public void hasPipelineEverRunWith() {
        HgMaterial hgMaterial = MaterialsMother.hgMaterial("hgUrl", "dest");
        MaterialRevision materialRevision = saveOneScmModification(hgMaterial, "user", "file");
        PipelineConfig pipelineConfig = PipelineMother.createPipelineConfig("mingle", new MaterialConfigs(hgMaterial.config()), "dev");
        MaterialRevisions materialRevisions = new MaterialRevisions(materialRevision);
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(materialRevisions, Username.ANONYMOUS),
                new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5,
                new TimeProvider());

        pipelineSqlMapDao.save(pipeline);

        MaterialRevisions revisions = new MaterialRevisions(new MaterialRevision(hgMaterial, materialRevision.getLatestModification()));
        assertThat(repo.hasPipelineEverRunWith("mingle", revisions), is(true));
    }

    @Test
    public void hasPipelineEverRunWithIsFalseWhenThereAreNewerModificationsThatHaveNotBeenBuilt() {
        HgMaterial hgMaterial = MaterialsMother.hgMaterial("hgUrl", "dest");
        MaterialRevision materialRevision = saveOneScmModification(hgMaterial, "user", "file");
        PipelineConfig pipelineConfig = PipelineMother.createPipelineConfig("mingle", new MaterialConfigs(hgMaterial.config()), "dev");
        MaterialRevisions materialRevisions = new MaterialRevisions(materialRevision);
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(materialRevisions, Username.ANONYMOUS),
                new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5,
                new TimeProvider());

        pipelineSqlMapDao.save(pipeline);

        MaterialRevision notBuiltRevision = saveOneScmModification(hgMaterial, "user", "file2");

        MaterialRevisions revisions = new MaterialRevisions(new MaterialRevision(hgMaterial, notBuiltRevision.getLatestModification()));
        assertThat(repo.hasPipelineEverRunWith("mingle", revisions), is(false));
    }

    @Test
    public void hasPipelineEverRunWithMultipleMaterials() {
        HgMaterial hgMaterial = MaterialsMother.hgMaterial("hgUrl", "dest");
        MaterialRevision hgMaterialRevision = saveOneScmModification(hgMaterial, "user", "file");
        DependencyMaterial depMaterial = new DependencyMaterial(new CaseInsensitiveString("blahPipeline"), new CaseInsensitiveString("blahStage"));
        MaterialRevision depMaterialRevision = saveOneDependencyModification(depMaterial, "blahPipeline/1/blahStage/1");
        PipelineConfig pipelineConfig = PipelineMother.createPipelineConfig("mingle", new MaterialConfigs(hgMaterial.config(), depMaterial.config()), "dev");
        MaterialRevisions materialRevisions = new MaterialRevisions(hgMaterialRevision, depMaterialRevision);
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(materialRevisions, Username.ANONYMOUS), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5,
                new TimeProvider());

        pipelineSqlMapDao.save(pipeline);

        MaterialRevisions revisions = new MaterialRevisions(new MaterialRevision(depMaterial, depMaterialRevision.getLatestModification()),
                new MaterialRevision(hgMaterial, hgMaterialRevision.getLatestModification()));
        assertThat(repo.hasPipelineEverRunWith("mingle", revisions), is(true));
    }

    @Test
    public void hasPipelineEverRunWithMultipleMaterialsAndMultipleRuns() {
        HgMaterial hgMaterial1 = MaterialsMother.hgMaterial("hgUrl", "dest");
        MaterialRevision hgMaterialRevision1 = saveOneScmModification(hgMaterial1, "user", "file");
        DependencyMaterial depMaterial1 = new DependencyMaterial(new CaseInsensitiveString("blahPipeline"), new CaseInsensitiveString("blahStage"));
        MaterialRevision depMaterialRevision1 = saveOneDependencyModification(depMaterial1, "blahPipeline/1/blahStage/1");
        PipelineConfig pipelineConfig = PipelineMother.createPipelineConfig("mingle", new MaterialConfigs(hgMaterial1.config(), depMaterial1.config()), "dev");
        MaterialRevisions materialRevisions1 = new MaterialRevisions(hgMaterialRevision1, depMaterialRevision1);
        pipelineSqlMapDao.save(instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(materialRevisions1, Username.ANONYMOUS), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5,
                new TimeProvider()));

        HgMaterial hgMaterial2 = MaterialsMother.hgMaterial("hgUrl", "dest");
        MaterialRevision hgMaterialRevision2 = saveOneScmModification(hgMaterial2, "user", "file");
        DependencyMaterial depMaterial2 = new DependencyMaterial(new CaseInsensitiveString("blahPipeline"), new CaseInsensitiveString("blahStage"));
        MaterialRevision depMaterialRevision2 = saveOneDependencyModification(depMaterial2, "blahPipeline/2/blahStage/1");
        PipelineConfig pipelineConfig2 = PipelineMother.createPipelineConfig("mingle", new MaterialConfigs(hgMaterial2.config(), depMaterial2.config()), "dev");
        MaterialRevisions materialRevisions2 = new MaterialRevisions(hgMaterialRevision2, depMaterialRevision2);

        savePipeline(instanceFactory.createPipelineInstance(pipelineConfig2, BuildCause.createManualForced(materialRevisions2, Username.ANONYMOUS), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5,
                new TimeProvider()));

        MaterialRevisions revisions = new MaterialRevisions(new MaterialRevision(depMaterial1, depMaterialRevision1.getLatestModification()),
                new MaterialRevision(hgMaterial2, hgMaterialRevision2.getLatestModification()));
        assertThat(repo.hasPipelineEverRunWith("mingle", revisions), is(true));
    }

    private Pipeline savePipeline(Pipeline pipeline) {
        Integer lastCount = pipelineSqlMapDao.getCounterForPipeline(pipeline.getName());
        pipeline.updateCounter(lastCount);
        pipelineSqlMapDao.insertOrUpdatePipelineCounter(pipeline, lastCount, pipeline.getCounter());
        return pipelineSqlMapDao.save(pipeline);
    }

    @Test
    public void hasPipelineEverRunWithMultipleMaterialsAndNewChanges() {
        HgMaterial material = MaterialsMother.hgMaterial("hgUrl", "dest");
        MaterialRevision hgMaterialRevision = saveOneScmModification(material, "user", "file");

        DependencyMaterial depMaterial = new DependencyMaterial(new CaseInsensitiveString("blahPipeline"), new CaseInsensitiveString("blahStage"));
        MaterialRevision depMaterialRevision = saveOneDependencyModification(depMaterial, "blahPipeline/1/blahStage/1");

        PipelineConfig pipelineConfig = PipelineMother.createPipelineConfig("mingle", new MaterialConfigs(material.config(), depMaterial.config()), "dev");
        MaterialRevisions revisions = new MaterialRevisions(hgMaterialRevision, depMaterialRevision);
        pipelineSqlMapDao.save(instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(revisions, Username.ANONYMOUS), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5,
                new TimeProvider()));

        MaterialRevision laterRevision = saveOneScmModification(material, "user", "file");

        MaterialRevisions newRevisions = new MaterialRevisions(depMaterialRevision, laterRevision);
        assertThat(repo.hasPipelineEverRunWith("mingle", newRevisions), is(false));
    }

    @Test
    public void hasPipelineEverRunWithMultipleMaterialsInPeggedRevisionsCase() {
        HgMaterial firstMaterial = MaterialsMother.hgMaterial("first", "dest");
        MaterialRevision first1 = saveOneScmModification("first1", firstMaterial, "user", "file", "comment");
        MaterialRevision first2 = saveOneScmModification("first2", firstMaterial, "user", "file", "comment");

        HgMaterial secondMaterial = MaterialsMother.hgMaterial("second", "dest");
        MaterialRevision second1 = saveOneScmModification("second1", secondMaterial, "user", "file", "comment");
        MaterialRevision second2 = saveOneScmModification("second2", secondMaterial, "user", "file", "comment");

        MaterialRevisions firstRun = new MaterialRevisions(first1, second2);
        MaterialRevisions secondRun = new MaterialRevisions(first2, second1);

        PipelineConfig config = PipelineMother.createPipelineConfig("mingle", new MaterialConfigs(firstMaterial.config(), secondMaterial.config()), "dev");
        savePipeline(instanceFactory.createPipelineInstance(config, BuildCause.createWithModifications(firstRun, "Pavan"), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider()));
        savePipeline(instanceFactory.createPipelineInstance(config, BuildCause.createWithModifications(secondRun, "Shilpa-who-gets-along-well-with-her"), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5,
                new TimeProvider()));

        assertThat(repo.hasPipelineEverRunWith("mingle", new MaterialRevisions(first2, second2)), is(true));
    }

    @Test
    public void hasPipelineEverRunWith_shouldCacheResultsForPipelineNameMaterialIdAndModificationId() {
        HgMaterial hgMaterial = MaterialsMother.hgMaterial("hgUrl", "dest");
        MaterialRevision materialRevision = saveOneScmModification(hgMaterial, "user", "file");
        PipelineConfig pipelineConfig = PipelineMother.createPipelineConfig("mingle", new MaterialConfigs(hgMaterial.config()), "dev");
        MaterialRevisions materialRevisions = new MaterialRevisions(materialRevision);
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(materialRevisions, Username.ANONYMOUS), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5,
                new TimeProvider());

        GoCache spyGoCache = spy(goCache);
        when(spyGoCache.get(any(String.class))).thenCallRealMethod();
        Mockito.doCallRealMethod().when(spyGoCache).put(any(String.class), any(Object.class));
        repo = new MaterialRepository(sessionFactory, spyGoCache, 2, transactionSynchronizationManager, materialConfigConverter, materialExpansionService, databaseStrategy);

        pipelineSqlMapDao.save(pipeline);

        MaterialRevisions revisions = new MaterialRevisions(new MaterialRevision(hgMaterial, materialRevision.getLatestModification()));

        assertThat("should hit the db and cache it",
                repo.hasPipelineEverRunWith("mingle", revisions), is(true));
        assertThat("should have cached the result on the previous call",
                repo.hasPipelineEverRunWith("mingle", revisions), is(true));

        verify(spyGoCache, times(1)).put(any(String.class), eq(Boolean.TRUE));
    }

    @Test
    public void shouldSavePipelineMaterialRevisions() throws Exception {
        SvnMaterialConfig svnMaterialConfig = MaterialConfigsMother.svnMaterialConfig("gitUrl", "folder", "user", "pass", true, "*.doc");
        assertCanLoadAndSaveMaterialRevisionsFor(svnMaterialConfig);
    }

    @Test
    public void shouldSaveGitPipelineMaterialRevisions() throws Exception {
        GitMaterialConfig gitMaterialConfig = MaterialConfigsMother.gitMaterialConfig("gitUrl", "submoduleFolder", "branch", false);
        assertCanLoadAndSaveMaterialRevisionsFor(gitMaterialConfig);
    }

    @Test
    public void shouldSaveHgPipelineMaterialRevisions() throws Exception {
        HgMaterialConfig hgMaterialConfig = MaterialConfigsMother.hgMaterialConfig("hgUrl", "dest");
        assertCanLoadAndSaveMaterialRevisionsFor(hgMaterialConfig);
    }

    @Test
    public void shouldSaveP4PipelineMaterialRevisions() throws Exception {
        P4MaterialConfig p4MaterialConfig = MaterialConfigsMother.p4MaterialConfig("serverAndPort", "user", "pwd", "view", true);
        assertCanLoadAndSaveMaterialRevisionsFor(p4MaterialConfig);
    }

    @Test
    public void shouldSaveDependencyPipelineMaterialRevisions() throws Exception {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"));
        assertCanLoadAndSaveMaterialRevisionsFor(dependencyMaterialConfig);
    }

    @Test
    public void shouldReturnModificationForASpecificRevision() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("blahPipeline"), new CaseInsensitiveString("blahStage"));
        MaterialRevision originalRevision = saveOneDependencyModification(dependencyMaterial, "blahPipeline/3/blahStage/1");

        Modification modification = repo.findModificationWithRevision(dependencyMaterial, "blahPipeline/3/blahStage/1");

        assertThat(modification.getRevision(), is("blahPipeline/3/blahStage/1"));
        assertEquals(originalRevision.getModification(0).getModifiedTime(), modification.getModifiedTime());
    }

    @Test
    public void shouldPickupTheRightFromAndToForMaterialRevisions() throws Exception {
        HgMaterial material = new HgMaterial("sdg", null);
        MaterialRevision firstRevision = new MaterialRevision(material, new Modifications(modification("6")));
        saveMaterialRev(firstRevision);
        final MaterialRevision secondRevision = new MaterialRevision(material, new Modifications(modification("10"), modification("12"), modification("13")));
        saveMaterialRev(secondRevision);
        final Pipeline pipeline = createPipeline();
        savePMR(secondRevision, pipeline);

        List<Modification> modificationsSince = repo.findModificationsSince(material, firstRevision);

        assertThat(modificationsSince.get(0).getRevision(), is("10"));
        assertThat(modificationsSince.get(modificationsSince.size() - 1).getRevision(), is("13"));
    }

    @Test
    public void shouldUseToAndFromAsRangeForSCMMaterialRevisionWhileSavingAndUpdating() throws Exception {
        HgMaterial material = new HgMaterial("sdg", null);
        final MaterialRevision firstRevision = new MaterialRevision(material, new Modifications(modification("10"), modification("9"), modification("8")));
        saveMaterialRev(firstRevision);
        final Pipeline firstPipeline = createPipeline();
        savePMR(firstRevision, firstPipeline);

        MaterialRevisions revisionsFor11 = repo.findMaterialRevisionsForPipeline(firstPipeline.getId());
        assertThat(revisionsFor11.getModifications(material).size(), is(3));
        assertThat(revisionsFor11.getModifications(material).get(0).getRevision(), is("10"));
        assertThat(revisionsFor11.getModifications(material).get(1).getRevision(), is("9"));
        assertThat(revisionsFor11.getModifications(material).get(2).getRevision(), is("8"));

        MaterialRevision secondRevision = new MaterialRevision(material, new Modifications(modification("11"), modification("10.5")));
        saveMaterialRev(secondRevision);
        Pipeline secondPipeline = createPipeline();
        savePMR(secondRevision, secondPipeline);

        MaterialRevisions revisionsFor12 = repo.findMaterialRevisionsForPipeline(secondPipeline.getId());
        assertThat(revisionsFor12.getModifications(material).size(), is(2));
        assertThat(revisionsFor12.getModifications(material).get(0).getRevision(), is("11"));
        assertThat(revisionsFor12.getModifications(material).get(1).getRevision(), is("10.5"));

        MaterialRevision thirdRevision = new MaterialRevision(material, new Modifications(modification("12")));
        saveMaterialRev(thirdRevision);
        Pipeline thirdPipeline = createPipeline();
        savePMR(thirdRevision, thirdPipeline);

        MaterialRevisions revisionsFor13 = repo.findMaterialRevisionsForPipeline(thirdPipeline.getId());
        assertThat(revisionsFor13.getModifications(material).size(), is(1));
        assertThat(revisionsFor13.getModifications(material).get(0).getRevision(), is("12"));
    }

    @Test
    public void shouldFixToAsFromForDependencyMaterialRevisionWhileSavingAndUpdating() throws Exception {
        Material material = new DependencyMaterial(new CaseInsensitiveString("pipeline_name"), new CaseInsensitiveString("stage_name"));
        MaterialRevision firstRevision = new MaterialRevision(material, new Modifications(modification("pipeline_name/10/stage_name/1"), modification("pipeline_name/9/stage_name/2"), modification("pipeline_name/8/stage_name/2")));
        saveMaterialRev(firstRevision);
        Pipeline firstPipeline = createPipeline();
        savePMR(firstRevision, firstPipeline);
        MaterialRevisions revisionsFor11 = repo.findMaterialRevisionsForPipeline(firstPipeline.getId());
        assertThat(revisionsFor11.getModifications(material).size(), is(1));
        assertThat(revisionsFor11.getModifications(material).get(0).getRevision(), is("pipeline_name/10/stage_name/1"));

        MaterialRevision secondRevision = new MaterialRevision(material, new Modifications(modification("pipeline_name/11/stage_name/2"), modification("pipeline_name/11/stage_name/1")));
        saveMaterialRev(secondRevision);
        Pipeline secondPipeline = createPipeline();
        savePMR(secondRevision, secondPipeline);

        MaterialRevisions revisionsFor12 = repo.findMaterialRevisionsForPipeline(secondPipeline.getId());
        assertThat(revisionsFor12.getModifications(material).size(), is(1));
        assertThat(revisionsFor12.getModifications(material).get(0).getRevision(), is("pipeline_name/11/stage_name/2"));

        MaterialRevision thirdRevision = new MaterialRevision(material, new Modifications(modification("pipeline_name/12/stage_name/1")));
        saveMaterialRev(thirdRevision);
        Pipeline thirdPipeline = createPipeline();
        savePMR(thirdRevision, thirdPipeline);

        savePMR(thirdRevision, thirdPipeline);

        MaterialRevisions revisionsFor13 = repo.findMaterialRevisionsForPipeline(thirdPipeline.getId());
        assertThat(revisionsFor13.getModifications(material).size(), is(1));
        assertThat(revisionsFor13.getModifications(material).get(0).getRevision(), is("pipeline_name/12/stage_name/1"));
    }

    @Test
    public void shouldPersistActualFromRevisionSameAsFromForSCMMaterial() throws Exception {
        HgMaterial material = new HgMaterial("sdg", null);
        MaterialRevision firstRevision = new MaterialRevision(material, new Modifications(modification("10"), modification("9"), modification("8")));
        saveMaterialRev(firstRevision);
        Pipeline firstPipeline = createPipeline();
        savePMR(firstRevision, firstPipeline);
        List<PipelineMaterialRevision> pmrs = repo.findPipelineMaterialRevisions(firstPipeline.getId());
        assertThat(pmrs.get(0).getActualFromRevisionId(), is(pmrs.get(0).getFromModification().getId()));
    }

    @Test
    public void shouldPersistActualFromRevisionUsingTheRealFromForDependencyMaterial() throws Exception {
        Material material = new DependencyMaterial(new CaseInsensitiveString("pipeline_name"), new CaseInsensitiveString("stage_name"));
        Modification actualFrom = modification("pipeline_name/8/stage_name/2");
        Modification from = modification("pipeline_name/10/stage_name/1");
        MaterialRevision firstRevision = new MaterialRevision(material, new Modifications(from, modification("pipeline_name/9/stage_name/2"), actualFrom));
        saveMaterialRev(firstRevision);
        Pipeline firstPipeline = createPipeline();
        savePMR(firstRevision, firstPipeline);

        List<PipelineMaterialRevision> pmrs = repo.findPipelineMaterialRevisions(firstPipeline.getId());
        assertThat(pmrs.get(0).getActualFromRevisionId(), is(actualFrom.getId()));
        assertEquals(from, pmrs.get(0).getFromModification());
    }

    @Test
    public void shouldUseTheFromIdAsActualFromIdWhenThePipelineIsBeingBuiltForTheFirstTime() throws Exception {
        Material material = new DependencyMaterial(new CaseInsensitiveString("pipeline_name"), new CaseInsensitiveString("stage_name"));
        Modification actualFrom = modification("pipeline_name/8/stage_name/2");
        MaterialRevision firstRevision = new MaterialRevision(material, new Modifications(modification("pipeline_name/9/stage_name/2"), actualFrom));
        saveMaterialRev(firstRevision);

        HgMaterial hgMaterial = new HgMaterial("sdg", null);
        MaterialRevision hgRevision = new MaterialRevision(hgMaterial, new Modifications(modification("10"), modification("9")));
        saveMaterialRev(hgRevision);

        Modification from = modification("pipeline_name/10/stage_name/1");
        firstRevision = new MaterialRevision(material, new Modifications(from));
        saveMaterialRev(firstRevision);

        Pipeline firstPipeline = createPipeline();
        savePMR(firstRevision, firstPipeline);

        List<PipelineMaterialRevision> pmrs = repo.findPipelineMaterialRevisions(firstPipeline.getId());

        assertThat(pmrs.get(0).getActualFromRevisionId(), is(from.getId()));
        assertEquals(from, pmrs.get(0).getFromModification());
    }

    @Test
    public void shouldPersistActualFromRevisionForSameRevisionOfDependencyMaterialModifications() throws Exception {
        Material material = new DependencyMaterial(new CaseInsensitiveString("pipeline_name"), new CaseInsensitiveString("stage_name"));
        Modification actualFrom = modification("pipeline_name/8/stage_name/2");
        MaterialRevision firstRevision = new MaterialRevision(material, new Modifications(actualFrom));
        saveMaterialRev(firstRevision);
        Pipeline firstPipeline = createPipeline();
        savePMR(firstRevision, firstPipeline);

        firstPipeline = createPipeline();
        savePMR(firstRevision, firstPipeline);

        List<PipelineMaterialRevision> pmrs = repo.findPipelineMaterialRevisions(firstPipeline.getId());

        assertThat(pmrs.get(0).getActualFromRevisionId(), is(actualFrom.getId()));
        assertEquals(actualFrom, pmrs.get(0).getFromModification());
    }

    @Test
    public void shouldUpdatePipelineMaterialRevisions() throws Exception {
        HgMaterial material = new HgMaterial("sdg", null);
        Modification first = modification("6");
        Modification second = modification("7");
        MaterialRevision firstRevision = new MaterialRevision(material, new Modifications(second, first));
        saveMaterialRev(firstRevision);

        material.setId(repo.findMaterialInstance(material).getId());
        MaterialRevision secondRevision = new MaterialRevision(material, new Modifications(first));
        Pipeline secondPipeline = createPipeline();
        savePMR(secondRevision, secondPipeline);

        MaterialRevisions materialRevisions = repo.findMaterialRevisionsForPipeline(secondPipeline.getId());
        assertEquals(secondRevision, materialRevisions.getMaterialRevision(0));

        List<PipelineMaterialRevision> pipelineMaterialRevisions = repo.findPipelineMaterialRevisions(secondPipeline.getId());
        assertThat(pipelineMaterialRevisions.get(0).getMaterialId(), is(material.getId()));
    }

    @Test
    public void shouldReturnMaterialRevisionsWithEmptyModificationsWhenNoModifications() throws Exception {
        Material material = material();
        repo.saveOrUpdate(material.createMaterialInstance());
        MaterialRevisions materialRevisions = repo.findLatestRevisions(new MaterialConfigs(material.config()));
        assertThat(materialRevisions.numberOfRevisions(), is(1));
        MaterialRevision materialRevision = materialRevisions.getMaterialRevision(0);
        assertThat(materialRevision.getMaterial(), is(material));
        assertThat(materialRevision.getModifications().size(), is(0));
    }

    @Test
    public void shouldReturnMatchedRevisionsForAGivenSearchString() throws Exception {
        ScmMaterial material = material();
        repo.saveOrUpdate(material.createMaterialInstance());
        MaterialRevision materialRevision = saveOneScmModification("40c95a3c41f54b5fb3107982cf2acd08783f102a", material, "pavan", "meet_you_in_hell.txt", "comment");
        saveOneScmModification(material, "turn_her", "of_course_he_will_be_there_first.txt");

        List<MatchedRevision> revisions = repo.findRevisionsMatching(material.config(), "pavan");
        assertThat(revisions.size(), is(1));
        assertMatchedRevision(revisions.get(0), materialRevision.getLatestShortRevision(), materialRevision.getLatestRevisionString(), "pavan", materialRevision.getDateOfLatestModification(), "comment");
    }

    @Test
    public void shouldMatchPipelineLabelForDependencyModifications() throws Exception {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
        repo.saveOrUpdate(material.createMaterialInstance());
        MaterialRevision first = saveOneDependencyModification(material, "pipeline-name/1/stage-name/3", "my-random-label-123");
        MaterialRevision second = saveOneDependencyModification(material, "pipeline-name/3/stage-name/1", "other-label-456");

        List<MatchedRevision> revisions = repo.findRevisionsMatching(material.config(), "my-random");
        assertThat(revisions.size(), is(1));
        assertMatchedRevision(revisions.get(0), first.getLatestShortRevision(), first.getLatestRevisionString(), null, first.getDateOfLatestModification(), "my-random-label-123");

        revisions = repo.findRevisionsMatching(material.config(), "other-label");
        assertThat(revisions.size(), is(1));
        assertMatchedRevision(revisions.get(0), second.getLatestShortRevision(), second.getLatestRevisionString(), null, second.getDateOfLatestModification(), "other-label-456");

        revisions = repo.findRevisionsMatching(material.config(), "something-else");
        assertThat(revisions.size(), is(0));
    }

    @Test
    public void shouldMatchSearchStringAcrossColumn() throws Exception {
        ScmMaterial material = material();
        repo.saveOrUpdate(material.createMaterialInstance());
        MaterialRevision first = saveOneScmModification("40c95a3c41f54b5fb3107982cf2acd08783f102a", material, "pavan", "meet_you_in_hell.txt", "comment");
        MaterialRevision second = saveOneScmModification("c30c471137f31a4bf735f653f888e799f6deec04", material, "turn_her", "of_course_he_will_be_there_first.txt", "comment");

        List<MatchedRevision> revisions = repo.findRevisionsMatching(material.config(), "pavan co");
        assertThat(revisions.size(), is(1));
        assertMatchedRevision(revisions.get(0), first.getLatestShortRevision(), first.getLatestRevisionString(), "pavan", first.getDateOfLatestModification(), "comment");

        revisions = repo.findRevisionsMatching(material.config(), "her co");
        assertThat(revisions.size(), is(1));
        assertMatchedRevision(revisions.get(0), second.getLatestShortRevision(), second.getLatestRevisionString(), "turn_her", second.getDateOfLatestModification(), "comment");

        revisions = repo.findRevisionsMatching(material.config(), "of_curs");
        assertThat(revisions.size(), is(0));
    }

    @Test
    public void shouldMatchSearchStringInDecreasingOrder() throws Exception {
        ScmMaterial material = material();
        repo.saveOrUpdate(material.createMaterialInstance());
        MaterialRevision first = saveOneScmModification("40c95a3c41f54b5fb3107982cf2acd08783f102a", material, "pavan", "meet_you_in_hell.txt", "comment");
        MaterialRevision second = saveOneScmModification("c30c471137f31a4bf735f653f888e799f6deec04", material, "turn_her", "of_course_he_will_be_there_first.txt", "comment");

        List<MatchedRevision> revisions = repo.findRevisionsMatching(material.config(), "");
        assertThat(revisions.size(), is(2));
        assertMatchedRevision(revisions.get(0), second.getLatestShortRevision(), second.getLatestRevisionString(), "turn_her", second.getDateOfLatestModification(), "comment");
        assertMatchedRevision(revisions.get(1), first.getLatestShortRevision(), first.getLatestRevisionString(), "pavan", first.getDateOfLatestModification(), "comment");
    }

    @Test
    public void shouldConsiderFieldToBeEmptyWhenRevisionOrUsernameOrCommentIsNull() throws Exception {
        ScmMaterial material = material();
        repo.saveOrUpdate(material.createMaterialInstance());
        MaterialRevision userIsNullRevision = saveOneScmModification("40c95a3c41f54b5fb3107982cf2acd08783f102a", material, null, "meet_you_in_hell.txt", "bring it on!");
        MaterialRevision commentIsNullRevision = saveOneScmModification("c30c471137f31a4bf735f653f888e799f6deec04", material, "turn_her", "lets_party_in_hell.txt", null);

        List<MatchedRevision> revisions = repo.findRevisionsMatching(material.config(), "bring");
        assertThat(revisions.size(), is(1));
        assertMatchedRevision(revisions.get(0), userIsNullRevision.getLatestShortRevision(), userIsNullRevision.getLatestRevisionString(), null, userIsNullRevision.getDateOfLatestModification(), "bring it on!");

        revisions = repo.findRevisionsMatching(material.config(), "c04 turn");
        assertThat(revisions.size(), is(1));
        assertMatchedRevision(revisions.get(0), commentIsNullRevision.getLatestShortRevision(), commentIsNullRevision.getLatestRevisionString(), "turn_her",
                commentIsNullRevision.getDateOfLatestModification(), null);

        revisions = repo.findRevisionsMatching(material.config(), "null");
        assertThat(revisions.size(), is(0));
    }

    @Test
    public void shouldFindLatestRevision() throws Exception {
        ScmMaterial material = material();
        repo.saveOrUpdate(material.createMaterialInstance());
        MaterialRevision first = saveOneScmModification("40c95a3c41f54b5fb3107982cf2acd08783f102a", material, "pavan", "meet_you_in_hell.txt", "comment");
        MaterialRevision second = saveOneScmModification("c30c471137f31a4bf735f653f888e799f6deec04", material, "turn_her", "of_course_he_will_be_there_first.txt", "comment");

        MaterialRevisions materialRevisions = repo.findLatestModification(material);
        List<MaterialRevision> revisions = materialRevisions.getRevisions();
        assertThat(revisions.size(), is(1));
        MaterialRevision materialRevision = revisions.get(0);
        assertThat(materialRevision.getLatestRevisionString(), is(second.getLatestRevisionString()));
    }

    @Test
    public void shouldCacheModificationCountsForMaterialCorrectly() throws Exception {
        ScmMaterial material = material();
        MaterialInstance materialInstance = material.createMaterialInstance();
        repo.saveOrUpdate(materialInstance);
        saveOneScmModification("1", material, "user1", "1.txt", "comment1");
        saveOneScmModification("2", material, "user2", "2.txt", "comment2");
        saveOneScmModification("3", material, "user3", "3.txt", "comment3");
        saveOneScmModification("4", material, "user4", "4.txt", "comment4");
        saveOneScmModification("5", material, "user5", "5.txt", "comment5");

        Long totalCount = repo.getTotalModificationsFor(materialInstance);

        assertThat(totalCount, is(5L));
    }

    @Test
    public void shouldCacheModificationsForMaterialCorrectly() throws Exception {
        final ScmMaterial material = material();
        MaterialInstance materialInstance = material.createMaterialInstance();
        repo.saveOrUpdate(materialInstance);
        saveOneScmModification("1", material, "user1", "1.txt", "comment1");
        saveOneScmModification("2", material, "user2", "2.txt", "comment2");
        saveOneScmModification("3", material, "user3", "3.txt", "comment3");
        saveOneScmModification("4", material, "user4", "4.txt", "comment4");
        saveOneScmModification("5", material, "user5", "5.txt", "comment5");

        Long totalCount = repo.getTotalModificationsFor(materialInstance);

        totalCount = (Long) goCache.get(repo.materialModificationCountKey(materialInstance));

        final Modification modOne = new Modification("user", "comment", "email@gmail.com", new Date(), "123");
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                MaterialInstance foo = repo.findOrCreateFrom(material);

                repo.saveModifications(foo, asList(modOne));
            }
        });

        totalCount = (Long) goCache.get(repo.materialModificationCountKey(materialInstance));

        assertThat(totalCount, is(nullValue()));
    }

    @Test
    public void shouldGetPaginatedModificationsForMaterialCorrectly() throws Exception {
        ScmMaterial material = material();
        MaterialInstance materialInstance = material.createMaterialInstance();
        repo.saveOrUpdate(materialInstance);
        MaterialRevision first = saveOneScmModification("1", material, "user1", "1.txt", "comment1");
        MaterialRevision second = saveOneScmModification("2", material, "user2", "2.txt", "comment2");
        MaterialRevision third = saveOneScmModification("3", material, "user3", "3.txt", "comment3");
        MaterialRevision fourth = saveOneScmModification("4", material, "user4", "4.txt", "comment4");
        MaterialRevision fifth = saveOneScmModification("5", material, "user5", "5.txt", "comment5");

        Modifications modifications = repo.getModificationsFor(materialInstance, Pagination.pageStartingAt(0, 5, 3));

        assertThat(modifications.size(), is(3));
        assertThat(modifications.get(0).getRevision(), is(fifth.getLatestRevisionString()));
        assertThat(modifications.get(1).getRevision(), is(fourth.getLatestRevisionString()));
        assertThat(modifications.get(2).getRevision(), is(third.getLatestRevisionString()));

        modifications = repo.getModificationsFor(materialInstance, Pagination.pageStartingAt(3, 5, 3));

        assertThat(modifications.size(), is(2));
        assertThat(modifications.get(0).getRevision(), is(second.getLatestRevisionString()));
        assertThat(modifications.get(1).getRevision(), is(first.getLatestRevisionString()));
    }

    @Test
    public void shouldCachePaginatedModificationsForMaterialCorrectly() throws Exception {
        final ScmMaterial material = material();
        MaterialInstance materialInstance = material.createMaterialInstance();
        repo.saveOrUpdate(materialInstance);
        MaterialRevision first = saveOneScmModification("1", material, "user1", "1.txt", "comment1");
        MaterialRevision second = saveOneScmModification("2", material, "user2", "2.txt", "comment2");
        MaterialRevision third = saveOneScmModification("3", material, "user3", "3.txt", "comment3");
        MaterialRevision fourth = saveOneScmModification("4", material, "user4", "4.txt", "comment4");
        MaterialRevision fifth = saveOneScmModification("5", material, "user5", "5.txt", "comment5");

        Pagination page = Pagination.pageStartingAt(0, 5, 3);
        repo.getModificationsFor(materialInstance, page);
        Modifications modificationsFromCache = (Modifications) goCache.get(repo.materialModificationsWithPaginationKey(materialInstance), repo.materialModificationsWithPaginationSubKey(page));

        assertThat(modificationsFromCache.size(), is(3));
        assertThat(modificationsFromCache.get(0).getRevision(), is(fifth.getLatestRevisionString()));
        assertThat(modificationsFromCache.get(1).getRevision(), is(fourth.getLatestRevisionString()));
        assertThat(modificationsFromCache.get(2).getRevision(), is(third.getLatestRevisionString()));


        page = Pagination.pageStartingAt(1, 5, 3);
        repo.getModificationsFor(materialInstance, page);
        modificationsFromCache = (Modifications) goCache.get(repo.materialModificationsWithPaginationKey(materialInstance), repo.materialModificationsWithPaginationSubKey(page));

        assertThat(modificationsFromCache.size(), is(3));
        assertThat(modificationsFromCache.get(0).getRevision(), is(fourth.getLatestRevisionString()));
        assertThat(modificationsFromCache.get(1).getRevision(), is(third.getLatestRevisionString()));
        assertThat(modificationsFromCache.get(2).getRevision(), is(second.getLatestRevisionString()));


        page = Pagination.pageStartingAt(3, 5, 3);
        repo.getModificationsFor(materialInstance, page);
        modificationsFromCache = (Modifications) goCache.get(repo.materialModificationsWithPaginationKey(materialInstance), repo.materialModificationsWithPaginationSubKey(page));

        assertThat(modificationsFromCache.size(), is(2));
        assertThat(modificationsFromCache.get(0).getRevision(), is(second.getLatestRevisionString()));
        assertThat(modificationsFromCache.get(1).getRevision(), is(first.getLatestRevisionString()));

        final Modification modOne = new Modification("user", "comment", "email@gmail.com", new Date(), "123");
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                MaterialInstance foo = repo.findOrCreateFrom(material);

                repo.saveModifications(foo, asList(modOne));
            }
        });

        modificationsFromCache = (Modifications) goCache.get(repo.materialModificationsWithPaginationKey(materialInstance), repo.materialModificationsWithPaginationSubKey(Pagination.pageStartingAt(0, 5, 3)));

        assertThat(modificationsFromCache, is(nullValue()));

        modificationsFromCache = (Modifications) goCache.get(repo.materialModificationsWithPaginationKey(materialInstance), repo.materialModificationsWithPaginationSubKey(Pagination.pageStartingAt(3, 5, 3)));

        assertThat(modificationsFromCache, is(nullValue()));
    }

    @Test
    public void shouldFindlatestModificationRunByPipeline() {
        ScmMaterial material = material();
        repo.saveOrUpdate(material.createMaterialInstance());
        MaterialRevision first = saveOneScmModification("40c95a3c41f54b5fb3107982cf2acd08783f102a", material, "pavan", "meet_you_in_hell.txt", "comment");
        MaterialRevision second = saveOneScmModification("c30c471137f31a4bf735f653f888e799f6deec04", material, "turn_her", "of_course_he_will_be_there_first.txt", "comment");
        Pipeline pipeline = createPipeline();
        savePMR(first, pipeline);
        savePMR(second, pipeline);
        Long latestModId = repo.latestModificationRunByPipeline(new CaseInsensitiveString(pipeline.getName()), material);

        assertThat(latestModId, is(second.getLatestModification().getId()));
    }

    @Test
    public void shouldFindModificationsForAStageIdentifier() {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString("P1"), new CaseInsensitiveString("S1"));
        repo.saveOrUpdate(material.createMaterialInstance());
        saveOneDependencyModification(material, "P1/1/S1/1");
        saveOneDependencyModification(material, "P1/2/S1/1");
        saveOneDependencyModification(material, "P1/1/S2/1");
        saveOneDependencyModification(material, "P2/1/S1/1");
        StageIdentifier stageIdentifier = new StageIdentifier("P1", 2, "2", "S1", "1");
        List<Modification> modifications = repo.modificationFor(stageIdentifier);
        assertThat(modifications.size(), is(1));
        assertThat(modifications.get(0).getRevision(), is("P1/2/S1/1"));
        assertThat((List<Modification>) goCache.get(repo.cacheKeyForModificationsForStageLocator(stageIdentifier)), is(modifications));

        StageIdentifier p2_s1_stageId = new StageIdentifier("P2", 1, "S1", "1");
        List<Modification> mod_p2_s1 = repo.modificationFor(p2_s1_stageId);
        assertThat((List<Modification>) goCache.get(repo.cacheKeyForModificationsForStageLocator(p2_s1_stageId)), is(mod_p2_s1));
        StageIdentifier p2_s1_3 = new StageIdentifier("P2", 1, "S1", "3");
        assertThat(repo.modificationFor(p2_s1_3).isEmpty(), is(true));
        assertThat(goCache.get(repo.cacheKeyForModificationsForStageLocator(p2_s1_3)), is(nullValue()));
    }

    @Test
    public void shouldSavePackageMaterialInstance() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        PackageMaterialInstance savedMaterialInstance = (PackageMaterialInstance) repo.findOrCreateFrom(material);
        assertThat(savedMaterialInstance.getId() > 0, is(true));
        assertThat(savedMaterialInstance.getFingerprint(), is(material.getFingerprint()));
        assertThat(JsonHelper.fromJson(savedMaterialInstance.getConfiguration(), PackageMaterial.class).getPackageDefinition().getConfiguration(), is(material.getPackageDefinition().getConfiguration()));
        assertThat(JsonHelper.fromJson(savedMaterialInstance.getConfiguration(), PackageMaterial.class).getPackageDefinition().getRepository().getPluginConfiguration().getId(), is(material.getPackageDefinition().getRepository().getPluginConfiguration().getId()));
        assertThat(JsonHelper.fromJson(savedMaterialInstance.getConfiguration(), PackageMaterial.class).getPackageDefinition().getRepository().getConfiguration(), is(material.getPackageDefinition().getRepository().getConfiguration()));
    }

    @Test
    public void shouldSavePluggableSCMMaterialInstance() {
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", true, "v2");
        material.setSCMConfig(SCMMother.create("scm-id", "scm-name", "plugin-id", "1.0", new Configuration(k1, k2)));

        PluggableSCMMaterialInstance savedMaterialInstance = (PluggableSCMMaterialInstance) repo.findOrCreateFrom(material);

        assertThat(savedMaterialInstance.getId() > 0, is(true));
        assertThat(savedMaterialInstance.getFingerprint(), is(material.getFingerprint()));
        assertThat(JsonHelper.fromJson(savedMaterialInstance.getConfiguration(), PluggableSCMMaterial.class).getScmConfig().getConfiguration(), is(material.getScmConfig().getConfiguration()));
        assertThat(JsonHelper.fromJson(savedMaterialInstance.getConfiguration(), PluggableSCMMaterial.class).getScmConfig().getPluginConfiguration().getId(), is(material.getScmConfig().getPluginConfiguration().getId()));
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldRemoveDuplicatesBeforeInsertingModifications() {
        final MaterialInstance materialInstance = repo.findOrCreateFrom(new GitMaterial(UUID.randomUUID().toString(), "branch"));
        final ArrayList<Modification> firstSetOfModifications = getModifications(3);
        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                repo.saveModifications(materialInstance, firstSetOfModifications);
                return null;
            }
        });

        Modifications firstSetOfModificationsFromDb = repo.getModificationsFor(materialInstance, Pagination.pageByNumber(1, 10, 10));
        assertThat(firstSetOfModificationsFromDb.size(), is(3));
        for (Modification modification : firstSetOfModifications) {
            assertThat(firstSetOfModificationsFromDb.containsRevisionFor(modification), is(true));
        }

        final ArrayList<Modification> secondSetOfModificationsContainingDuplicateRevisions = getModifications(4);

        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                repo.saveModifications(materialInstance, secondSetOfModificationsContainingDuplicateRevisions);
                return null;
            }
        });
        Modifications secondSetOfModificationsFromDb = repo.getModificationsFor(materialInstance, Pagination.pageByNumber(1, 10, 10));
        assertThat(secondSetOfModificationsFromDb.size(), is(4));
        for (final Modification fromPreviousCycle : firstSetOfModificationsFromDb) {
            Modification modification = ListUtil.find(secondSetOfModificationsFromDb, new ListUtil.Condition() {
                @Override
                public <T> boolean isMet(T item) {
                    Modification modification = (Modification) item;
                    return modification.getId() == fromPreviousCycle.getId();
                }
            });
            assertThat(modification, is(notNullValue()));
        }
        for (Modification modification : secondSetOfModificationsContainingDuplicateRevisions) {
            assertThat(secondSetOfModificationsFromDb.containsRevisionFor(modification), is(true));
        }
    }

    @Test
    public void shouldReportErrorIfAnAttemptIsMadeToInsertOnlyDuplicateModificationsForAGivenMaterial() {
        final MaterialInstance materialInstance = repo.findOrCreateFrom(new GitMaterial(UUID.randomUUID().toString(), "branch"));
        final ArrayList<Modification> firstSetOfModifications = getModifications(3);
        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                repo.saveModifications(materialInstance, firstSetOfModifications);
                return null;
            }
        });
        Modifications firstSetOfModificationsFromDb = repo.getModificationsFor(materialInstance, Pagination.pageByNumber(1, 10, 10));
        assertThat(firstSetOfModificationsFromDb.size(), is(3));
        for (Modification modification : firstSetOfModifications) {
            assertThat(firstSetOfModificationsFromDb.containsRevisionFor(modification), is(true));
        }

        final ArrayList<Modification> secondSetOfModificationsContainingAllDuplicateRevisions = getModifications(3);
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("All modifications already exist in db: [r0, r1, r2]");
        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                repo.saveModifications(materialInstance, secondSetOfModificationsContainingAllDuplicateRevisions);
                return null;
            }
        });
    }

    @Test
    public void shouldAllowSavingModificationsIfRevisionsAcrossDifferentMaterialsHappenToBeSame() {
        final MaterialInstance materialInstance1 = repo.findOrCreateFrom(new GitMaterial(UUID.randomUUID().toString(), "branch"));
        final MaterialInstance materialInstance2 = repo.findOrCreateFrom(new GitMaterial(UUID.randomUUID().toString(), "branch"));
        final ArrayList<Modification> modificationsForFirstMaterial = getModifications(3);
        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                repo.saveModifications(materialInstance1, modificationsForFirstMaterial);
                return null;
            }
        });

        assertThat(repo.getModificationsFor(materialInstance1, Pagination.pageByNumber(1, 10, 10)).size(), is(3));

        final ArrayList<Modification> modificationsForSecondMaterial = getModifications(3);

        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                repo.saveModifications(materialInstance2, modificationsForSecondMaterial);
                return null;
            }
        });
        Modifications modificationsFromDb = repo.getModificationsFor(materialInstance2, Pagination.pageByNumber(1, 10, 10));
        assertThat(modificationsFromDb.size(), is(3));
        for (Modification modification : modificationsForSecondMaterial) {
            assertThat(modificationsFromDb.containsRevisionFor(modification), is(true));
        }
    }

    @Test
    public void shouldNotSaveAndClearCacheWhenThereAreNoNewModifications() {
        final MaterialInstance materialInstance = repo.findOrCreateFrom(new GitMaterial(UUID.randomUUID().toString(), "branch"));
        String key = repo.materialModificationsWithPaginationKey(materialInstance);
        String subKey = repo.materialModificationsWithPaginationSubKey(Pagination.ONE_ITEM);
        goCache.put(key, subKey, new Modifications(new Modification()));
        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                repo.saveModifications(materialInstance, new ArrayList<Modification>());
                return null;
            }
        });
        assertThat(goCache.get(key, subKey), is(notNullValue()));
    }

    //Slow test - takes ~1 min to run. Will remove if it causes issues. - Jyoti
    @Test
    public void shouldBeAbleToHandleLargeNumberOfModifications() {
        final MaterialInstance materialInstance = repo.findOrCreateFrom(new GitMaterial(UUID.randomUUID().toString(), "branch"));
        int count = 10000;
        final ArrayList<Modification> firstSetOfModifications = getModifications(count);
        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                repo.saveModifications(materialInstance, firstSetOfModifications);
                return null;
            }
        });

        assertThat(repo.getTotalModificationsFor(materialInstance), is(new Long(count)));

        final ArrayList<Modification> secondSetOfModifications = getModifications(count + 1);
        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                repo.saveModifications(materialInstance, secondSetOfModifications);
                return null;
            }
        });

        assertThat(repo.getTotalModificationsFor(materialInstance), is(new Long(count+1)));
    }

    private ArrayList<Modification> getModifications(int count) {
        final ArrayList<Modification> modifications = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            modifications.add(new Modification("user", "comment", "email", new Date(), "r" + i));
        }
        return modifications;
    }

    private MaterialRevision saveOneDependencyModification(DependencyMaterial dependencyMaterial, String revision) {
        return saveOneDependencyModification(dependencyMaterial, revision, "MOCK_LABEL-12");
    }

    private MaterialRevision saveOneDependencyModification(final DependencyMaterial dependencyMaterial, final String revision, final String label) {
        return (MaterialRevision) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                Modification modification = new Modification(new Date(), revision, label, null);
                MaterialRevision originalRevision = new MaterialRevision(dependencyMaterial, modification);
                repo.save(new MaterialRevisions(originalRevision));
                return originalRevision;
            }
        });
    }

    private MaterialInstance saveMaterialRev(final MaterialRevision rev) {
        return (MaterialInstance) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                return repo.saveMaterialRevision(rev);
            }
        });
    }

    private Pipeline createPipeline() {
        Pipeline pipeline = PipelineMother.pipeline("pipeline", StageMother.custom("stage"));
        pipeline.getBuildCause().setMaterialRevisions(new MaterialRevisions());
        return savePipeline(pipeline);
    }

    private void savePMR(final MaterialRevision revision, final Pipeline pipeline) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                repo.savePipelineMaterialRevision(pipeline, pipeline.getId(), revision);
            }
        });
    }

    private void assertMatchedRevision(MatchedRevision matchedRevision, String shortRevision, String longRevision, String user, Date checkinTime, String comment) {
        assertThat("shortRevision", matchedRevision.getShortRevision(), is(shortRevision));
        assertThat("longRevision", matchedRevision.getLongRevision(), is(longRevision));
        assertThat("user", matchedRevision.getUser(), is(user));
        assertEquals("checkinTime", checkinTime, matchedRevision.getCheckinTime());
        assertThat("comment", matchedRevision.getComment(), is(comment));
    }

    private Modification modification(String revision) {
        return new Modification("user1", "comment", "foo@bar", new Date(), revision);
    }

    private Material saveMaterialRevisions(final MaterialRevisions revs) {
        Material hg = revs.getMaterialRevision(0).getMaterial();

        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                for (MaterialRevision revision : revs) {
                    repo.saveMaterialRevision(revision);
                }
                return null;
            }
        });
        return hg;
    }

    private DependencyMaterial savePipelineAndCreate_DMRs(Date d, MaterialRevisions hgMaterialRevisions, PipelineConfig config, int... dmrStageCounters) {
        Pipeline pipeline = savePipeline(
                instanceFactory.createPipelineInstance(config,
                        BuildCause.createWithModifications(hgMaterialRevisions, "Loser"),
                        new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider()));

        CaseInsensitiveString stageName = config.get(0).name();
        DependencyMaterial material = new DependencyMaterial(config.name(), stageName);

        String label = pipeline.getLabel();

        ArrayList<Modification> mods = new ArrayList<Modification>();
        for (int i = 0; i < dmrStageCounters.length; i++) {
            int dmrStageCounter = dmrStageCounters[i];
            StageIdentifier stageIdentifier = new StageIdentifier(pipeline.getIdentifier(), CaseInsensitiveString.str(stageName), String.valueOf(dmrStageCounter));
            mods.add(new Modification(d, stageIdentifier.getStageLocator(), label, pipeline.getId()));
        }
        final MaterialRevision materialRevision = new MaterialRevision(material, mods);

        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                repo.saveMaterialRevision(materialRevision);
                return null;
            }
        });
        return material;
    }

    private void assertCanLoadAndSaveMaterialRevisionsFor(MaterialConfig materialConfig) {
        final PipelineConfig pipelineConfig = PipelineMother.createPipelineConfig("mingle", new MaterialConfigs(materialConfig), "dev");

        final MaterialRevisions materialRevisions = ModificationsMother.modifyOneFile(pipelineConfig);
        MaterialRevision materialRevision = materialRevisions.getMaterialRevision(0);

        final Pipeline[] pipeline = new Pipeline[1];

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                //assume that we have saved the materials
                repo.save(materialRevisions);

                PipelineConfig config = PipelineMother.withTwoStagesOneBuildEach("pipeline-name", "stage-1", "stage-2");
                config.setMaterialConfigs(materialRevisions.getMaterials().convertToConfigs());
                pipeline[0] = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createManualForced(materialRevisions, Username.ANONYMOUS), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5,
                        new TimeProvider());

                //this should persist the materials
                pipelineSqlMapDao.save(pipeline[0]);
            }
        });

        assertMaterialRevisions(materialRevision, pipeline[0]);
    }

    private void assertMaterialRevisions(MaterialRevision materialRevision, Pipeline pipeline) {
        assertThat(pipeline.getId(), greaterThan(0L));

        MaterialRevisions actual = repo.findMaterialRevisionsForPipeline(pipeline.getId());

        assertEquals(materialRevision.getMaterial(), actual.getMaterialRevision(0).getMaterial());
        assertEquals(materialRevision.getLatestModification(), actual.getMaterialRevision(0).getLatestModification());
    }

    private MaterialRevision saveOneScmModification(ScmMaterial original, String user, String filename) {
        return saveOneScmModification(ModificationsMother.nextRevision(), original, user, filename, "comment");
    }

    private MaterialRevision saveOneScmModification(final String revision, final Material original, final String user, final String filename, final String comment) {
        return (MaterialRevision) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                Modification modification = new Modification(user, comment, "email", new Date(), revision);
                modification.createModifiedFile(filename, "folder1", ModifiedAction.added);

                MaterialRevision originalRevision = new MaterialRevision(original, modification);

                repo.save(new MaterialRevisions(originalRevision));
                return originalRevision;
            }
        });
    }

    private PipelineMaterialRevision pipelineMaterialRevision() {
        PipelineMaterialRevision pmr = mock(PipelineMaterialRevision.class);
        when(pmr.getMaterial()).thenReturn(material());
        when(pmr.getToModification()).thenReturn(new Modification(new Date(), "123", "MOCK_LABEL-12", null));
        when(pmr.getFromModification()).thenReturn(new Modification(new Date(), "125", "MOCK_LABEL-12", null));
        return pmr;
    }

    private HgMaterial material() {
        return new HgMaterial("url", null);
    }
}
