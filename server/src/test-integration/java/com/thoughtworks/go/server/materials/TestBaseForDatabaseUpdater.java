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

import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialExpansionService;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
@EnableRuleMigrationSupport
public abstract class TestBaseForDatabaseUpdater {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected TestRepo testRepo;
    protected abstract Material material();

    protected abstract TestRepo repo() throws Exception;

    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired protected MaterialRepository materialRepository;
    @Autowired protected ServerHealthService serverHealthService;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private DependencyMaterialUpdater dependencyMaterialUpdater;
    @Autowired private ScmMaterialUpdater scmMaterialUpdater;
    @Autowired private MaterialExpansionService materialExpansionService;
    @Autowired private GoConfigService goConfigService;

    protected MaterialDatabaseUpdater updater;
    protected Material material;

    @BeforeEach
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        updater = new MaterialDatabaseUpdater(materialRepository, serverHealthService, transactionTemplate, dependencyMaterialUpdater, scmMaterialUpdater, null, null, materialExpansionService, goConfigService);
        testRepo = repo();
        material = material();
        testRepo.onSetup();
    }

    @AfterEach
    public void tearDown() throws Exception {
        testRepo.tearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldCreateMaterialWhenFirstChecked() throws Exception {
        updater.updateMaterial(material);

        Modification modification = materialRepository.findLatestModification(material).getMaterialRevision(0).getModification(0);
        assertThat(testRepo.latestModification().get(0), is(modification));

    }

    @Test
    public void shouldUpdateWithChanges() throws Exception {
        List<Modification> modifications = testRepo.checkInOneFile("test.txt", "This is a new checkin");
        updater.updateMaterial(material);

        Modification modification = materialRepository.findLatestModification(material).getMaterialRevision(0).getModification(0);

        assertEquals(modifications.get(0), modification);
    }

    @Test
    public void shouldSaveTheRevisionsInRightOrder() throws Exception {
        updater.updateMaterial(material);
        testRepo.checkInOneFile("test1.txt", "This is a new checkin");

        updater.updateMaterial(material);
        MaterialRevisions original = materialRepository.findLatestModification(material);
        testRepo.checkInOneFile("test2.txt", "Checkin 1");
        testRepo.checkInOneFile("test3.txt", "Checkin 2");
        testRepo.checkInOneFile("test4.txt", "Checkin 3");

        updater.updateMaterial(material);

        List<Modification> changes = materialRepository.findModificationsSince(material,
                original.getMaterialRevision(0));
        assertThat(changes.size(),is(3));
        assertThat(changes.get(0).getComment(), is("Checkin 3"));
        assertThat(changes.get(1).getComment(), is("Checkin 2"));
        assertThat(changes.get(2).getComment(), is("Checkin 1"));
    }
}
