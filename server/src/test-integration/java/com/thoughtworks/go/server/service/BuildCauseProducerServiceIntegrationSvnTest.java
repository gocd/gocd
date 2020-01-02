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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.materials.MaterialDatabaseUpdater;
import com.thoughtworks.go.server.scheduling.BuildCauseProducerService;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.apache.commons.io.FileUtils;
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
import java.sql.SQLException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class BuildCauseProducerServiceIntegrationSvnTest {

    private static final String STAGE_NAME = "dev";

    @Autowired private GoConfigService goConfigService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired private BuildCauseProducerService buildCauseProducerService;
    @Autowired private MaterialDatabaseUpdater materialDatabaseUpdater;
    @Autowired private SubprocessExecutionContext subprocessExecutionContext;

    @Autowired private DatabaseAccessHelper dbHelper;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    public SvnMaterial svnMaterial;
    private static SvnTestRepo svnRepository;
    private Pipeline latestPipeline;
    private File workingFolder;
    PipelineConfig mingleConfig;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        workingFolder = temporaryFolder.newFolder("workingFolder");
    }

    private void repositoryForMaterial(SvnTestRepo svnRepository) {
        this.svnRepository = svnRepository;
        svnMaterial = MaterialsMother.svnMaterial(svnRepository.projectRepositoryUrl(), "foo", "user", "pass", true, "*.doc");
        mingleConfig = configHelper.addPipeline("cruise", STAGE_NAME, svnMaterial.config(), "unit", "functional");
    }

    @After
    public void teardown() throws Exception {
        TestRepo.internalTearDown();
        dbHelper.onTearDown();
        FileUtils.deleteQuietly(goConfigService.artifactsDir());
        FileUtils.deleteQuietly(workingFolder);
        TestRepo.internalTearDown();
        pipelineScheduleQueue.clear();
        configHelper.onTearDown();
    }

    @Test
    public void shouldCreateBuildCauseWithModifications() throws Exception {
        repositoryForMaterial(new SvnTestRepo(temporaryFolder));
        prepareAPipelineWithHistory();

        checkInFiles("foo");

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        materialDatabaseUpdater.updateMaterial(svnMaterial);
        buildCauseProducerService.autoSchedulePipeline(CaseInsensitiveString.str(mingleConfig.name()), result, 123);

        assertThat(result.canContinue(), is(true));

        BuildCause mingleBuildCause = pipelineScheduleQueue.toBeScheduled().get(mingleConfig.name());

        MaterialRevisions materialRevisions = mingleBuildCause.getMaterialRevisions();
        assertThat(materialRevisions.getRevisions().size(), is(1));
        Materials materials = materialRevisions.getMaterials();
        assertThat(materials.size(), is(1));
        assertThat(materials.get(0), is(svnMaterial));
    }

    @Test
    public void shouldCreateBuildCauseWithModificationsForSvnRepoWithExternal() throws Exception {
        SvnTestRepoWithExternal repo = new SvnTestRepoWithExternal(temporaryFolder);
        repositoryForMaterial(repo);
        prepareAPipelineWithHistory();

        checkInFiles("foo");

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        materialDatabaseUpdater.updateMaterial(svnMaterial);
        buildCauseProducerService.autoSchedulePipeline(CaseInsensitiveString.str(mingleConfig.name()), result, 123);

        assertThat(result.canContinue(), is(true));

        BuildCause mingleBuildCause = pipelineScheduleQueue.toBeScheduled().get(mingleConfig.name());

        MaterialRevisions materialRevisions = mingleBuildCause.getMaterialRevisions();
        assertThat(materialRevisions.getRevisions().size(), is(2));
        Materials materials = materialRevisions.getMaterials();
        assertThat(materials.size(), is(2));
        assertThat(materials.get(0), is(svnMaterial));
        SvnMaterial external = (SvnMaterial) materials.get(1);
        assertThat(external.getUrl(), is(repo.externalRepositoryUrl()));
    }

    private void prepareAPipelineWithHistory() throws SQLException {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        materialRevisions.addRevision(svnMaterial, svnMaterial.latestModification(workingFolder, subprocessExecutionContext));
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, "");

        latestPipeline = PipelineMother.schedule(mingleConfig, buildCause);
        latestPipeline = dbHelper.savePipelineWithStagesAndMaterials(latestPipeline);
        dbHelper.passStage(latestPipeline.getStages().first());
    }

    private void checkInFiles(String... files) throws Exception {
        for (String fileName : files) {
            File file = new File(workingFolder, fileName);
            FileUtils.writeStringToFile(file, "bla", UTF_8);
            svnRepository.checkInOneFile(fileName, "random commit " + fileName);
        }
    }
}
