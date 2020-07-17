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

import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.transaction.TransactionCallback;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class MaterialDatabaseUpdaterIntegrationTest {
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialDatabaseUpdater updater;
    @Autowired protected MaterialRepository materialRepository;
    @Autowired protected ServerHealthService serverHealthService;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private DependencyMaterialUpdater dependencyMaterialUpdater;
    @Autowired private PackageMaterialUpdater packageMaterialUpdater;
    @Autowired private PluggableSCMMaterialUpdater pluggableSCMMaterialUpdater;
    @Autowired private MaterialExpansionService materialExpansionService;
    @Autowired private SubprocessExecutionContext subprocessExecutionContext;
    @Autowired private GoConfigService goConfigService;
    @Autowired private SecurityService securityService;
    @Autowired private PackageRepositoryExtension packageRepositoryExtension;
    @Autowired private SCMExtension scmExtension;
    @Autowired private SecretParamResolver secretParamResolver;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();


    private GitTestRepo testRepo;
    private MaterialDatabaseUpdaterIntegrationTest.TransactionTemplateWithInvocationCount transactionTemplateWithInvocationCount;

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        testRepo = new GitTestRepo(temporaryFolder);

        MaterialService slowMaterialService = new MaterialServiceWhichSlowsDownFirstTimeModificationCheck(materialRepository, goConfigService, securityService, packageRepositoryExtension, scmExtension);
        LegacyMaterialChecker materialChecker = new LegacyMaterialChecker(slowMaterialService, subprocessExecutionContext);
        ScmMaterialUpdater scmMaterialUpdater = new ScmMaterialUpdater(materialRepository, materialChecker, subprocessExecutionContext, slowMaterialService);
        transactionTemplateWithInvocationCount = new TransactionTemplateWithInvocationCount(transactionTemplate);
        updater = new MaterialDatabaseUpdater(materialRepository, serverHealthService, transactionTemplateWithInvocationCount, dependencyMaterialUpdater,
                scmMaterialUpdater, packageMaterialUpdater, pluggableSCMMaterialUpdater, materialExpansionService, goConfigService);
    }

    @After
    public void tearDown() throws Exception {
        testRepo.tearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldNotThrowUpWhenSameMaterialIsBeingUpdatedByMultipleThreads() throws Exception {
        final ScmMaterial material = new GitMaterial(testRepo.projectRepositoryUrl());
        final List<Exception> threadOneExceptions = new ArrayList();
        final List<Exception> threadTwoExceptions = new ArrayList();

        Thread updateThread1 = new Thread() {
            @Override
            public void run() {
                try {
                    updater.updateMaterial(material);
                } catch (Exception e) {
                    threadOneExceptions.add(e);
                }
            }
        };

        Thread updateThread2 = new Thread() {
            @Override
            public void run() {
                try {
                    updater.updateMaterial(material);
                } catch (Exception e) {
                    threadTwoExceptions.add(e);
                }
            }
        };

        updateThread1.start();
        updateThread2.start();

        updateThread1.join();
        updateThread2.join();


        if (!threadOneExceptions.isEmpty()) {
            throw threadOneExceptions.get(0);
        }
        if (!threadTwoExceptions.isEmpty()) {
            throw threadTwoExceptions.get(0);
        }
        assertThat("transaction template executeWithExceptionHandling should be invoked only once",transactionTemplateWithInvocationCount.invocationCount, is(1));
    }

    private class MaterialServiceWhichSlowsDownFirstTimeModificationCheck extends MaterialService {
        public MaterialServiceWhichSlowsDownFirstTimeModificationCheck(MaterialRepository materialRepository, GoConfigService goConfigService, SecurityService securityService,
                                                                       PackageRepositoryExtension packageRepositoryExtension, SCMExtension scmExtension) {
            super(materialRepository, goConfigService, securityService, packageRepositoryExtension, scmExtension, transactionTemplate, secretParamResolver, null);
        }

        @Override
        public List<Modification> latestModification(Material material, File baseDir, SubprocessExecutionContext execCtx) {
            System.err.println(Thread.currentThread() + "Slowing down latest modification check");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.err.println(Thread.currentThread() + "Done slowing down latest modification check");

            return super.latestModification(material, baseDir, execCtx);
        }
    }

    private class TransactionTemplateWithInvocationCount extends TransactionTemplate {

        public int invocationCount = 0;
        private TransactionTemplate nestedTransactionTemplate;


        public TransactionTemplateWithInvocationCount(TransactionTemplate transactionTemplate) {
            super(null);
            this.nestedTransactionTemplate = transactionTemplate;
        }

        @Override
        public Object executeWithExceptionHandling(TransactionCallback action) throws Exception {
            invocationCount++;
            return nestedTransactionTemplate.executeWithExceptionHandling(action);
        }
    }
}
