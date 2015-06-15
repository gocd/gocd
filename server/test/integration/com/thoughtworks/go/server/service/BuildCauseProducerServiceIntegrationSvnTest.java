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

package com.thoughtworks.go.server.service;

import java.io.File;
import java.sql.SQLException;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.SvnTestRepoWithExternal;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.materials.MaterialDatabaseUpdater;
import com.thoughtworks.go.server.scheduling.BuildCauseProducerService;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class BuildCauseProducerServiceIntegrationSvnTest {

    private static final String STAGE_NAME = "dev";

    @Autowired private GoConfigService goConfigService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired private BuildCauseProducerService buildCauseProducerService;
    @Autowired private MaterialDatabaseUpdater materialDatabaseUpdater;
    @Autowired private SubprocessExecutionContext subprocessExecutionContext;

    @Autowired PipelineService pipelineService;
	@Autowired private DatabaseAccessHelper dbHelper;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    public Subversion repository;
    public SvnMaterial svnMaterial;
    public static SvnTestRepo svnRepository;
    private Pipeline latestPipeline;
    private File workingFolder;
    PipelineConfig mingleConfig;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        workingFolder = TestFileUtil.createTempFolder("workingFolder");
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
        FileUtil.deleteFolder(goConfigService.artifactsDir());
        FileUtil.deleteFolder(workingFolder);
        TestRepo.internalTearDown();
        pipelineScheduleQueue.clear();
        configHelper.onTearDown();
    }

    @Test
    public void shouldCreateBuildCauseWithModifications() throws Exception {
        repositoryForMaterial(new SvnTestRepo("svnTestRepo"));
        prepareAPipelineWithHistory();

        checkInFiles("foo");

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        materialDatabaseUpdater.updateMaterial(svnMaterial);
        buildCauseProducerService.autoSchedulePipeline(CaseInsensitiveString.str(mingleConfig.name()), result, 123);

        assertThat(result.canContinue(), is(true));

        BuildCause mingleBuildCause = pipelineScheduleQueue.toBeScheduled().get(CaseInsensitiveString.str(mingleConfig.name()));

        MaterialRevisions materialRevisions = mingleBuildCause.getMaterialRevisions();
        assertThat(materialRevisions.getRevisions().size(), is(1));
        Materials materials = materialRevisions.getMaterials();
        assertThat(materials.size(), is(1));
        assertThat(materials.get(0), is((Material) svnMaterial));
    }

    @Test
    public void shouldCreateBuildCauseWithModificationsForSvnRepoWithExternal() throws Exception {
        SvnTestRepoWithExternal repo = new SvnTestRepoWithExternal();
        repositoryForMaterial(repo);
        prepareAPipelineWithHistory();

        checkInFiles("foo");

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        materialDatabaseUpdater.updateMaterial(svnMaterial);
        buildCauseProducerService.autoSchedulePipeline(CaseInsensitiveString.str(mingleConfig.name()), result, 123);

        assertThat(result.canContinue(), is(true));

        BuildCause mingleBuildCause = pipelineScheduleQueue.toBeScheduled().get(CaseInsensitiveString.str(mingleConfig.name()));

        MaterialRevisions materialRevisions = mingleBuildCause.getMaterialRevisions();
        assertThat(materialRevisions.getRevisions().size(), is(2));
        Materials materials = materialRevisions.getMaterials();
        assertThat(materials.size(), is(2));
        assertThat(materials.get(0), is((Material) svnMaterial));
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
            FileUtils.writeStringToFile(file, "bla");
            svnRepository.checkInOneFile(fileName, "random commit " + fileName);
        }
    }
}