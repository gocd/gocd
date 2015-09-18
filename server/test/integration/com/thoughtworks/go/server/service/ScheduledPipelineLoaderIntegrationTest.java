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

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.SvnTestRepoWithExternal;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.materials.StaleMaterialsOnBuildCause;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.ArtifactLogUtil;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.utils.Timeout;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ScheduledPipelineLoaderIntegrationTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private ScheduledPipelineLoader loader;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoCache goCache;
    @Autowired private ServerHealthService serverHealthService;
    @Autowired private JobInstanceService jobInstanceService;
    @Autowired private ArtifactsService artifactsService;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private MaterialExpansionService materialExpansionService;

    GoConfigFileHelper configHelper;
    private SvnTestRepoWithExternal svnRepo;

    @Before
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper(goConfigDao);
        dbHelper.onSetUp();
        goCache.clear();
        configHelper.onSetUp();
        svnRepo = new SvnTestRepoWithExternal();
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
        TestRepo.internalTearDown();
    }

    @Test
    public void shouldLoadPipelineAlongwithBuildCauseHavingMaterialPasswordsPopulated() {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("last", new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job-one"))));
        pipelineConfig.materialConfigs().clear();
        SvnMaterial onDirOne = MaterialsMother.svnMaterial("google.com", "dirOne", "loser", "boozer", false, "**/*.html");
        P4Material onDirTwo = MaterialsMother.p4Material("host:987654321", "zoozer", "secret", "through-the-window", true);
        onDirTwo.setFolder("dirTwo");
        pipelineConfig.addMaterialConfig(onDirOne.config());
        pipelineConfig.addMaterialConfig(onDirTwo.config());

        configHelper.addPipeline(pipelineConfig);

        Pipeline building = PipelineMother.building(pipelineConfig);
        Pipeline pipeline = dbHelper.savePipelineWithMaterials(building);

        final long jobId = pipeline.getStages().get(0).getJobInstances().get(0).getId();
        Pipeline loadedPipeline = (Pipeline) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                return loader.pipelineWithPasswordAwareBuildCauseByBuildId(jobId);
            }
        });

        MaterialRevisions revisions = loadedPipeline.getBuildCause().getMaterialRevisions();
        assertThat(((SvnMaterial) revisions.findRevisionFor(onDirOne).getMaterial()).getPassword(), is("boozer"));
        assertThat(((P4Material) revisions.findRevisionFor(onDirTwo).getMaterial()).getPassword(), is("secret"));
    }

    @Test
    public void shouldSetAServerHealthMessageWhenMaterialForPipelineWithBuildCauseIsNotFound() throws IllegalArtifactLocationException, IOException {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("last", new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job-one"))));
        pipelineConfig.materialConfigs().clear();
        SvnMaterialConfig onDirOne = MaterialConfigsMother.svnMaterialConfig("google.com", "dirOne", "loser", "boozer", false, "**/*.html");
        final P4MaterialConfig onDirTwo = MaterialConfigsMother.p4MaterialConfig("host:987654321", "zoozer", "secret", "through-the-window", true);
        onDirTwo.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FOLDER, "dirTwo"));
        pipelineConfig.addMaterialConfig(onDirOne);
        pipelineConfig.addMaterialConfig(onDirTwo);

        configHelper.addPipeline(pipelineConfig);

        Pipeline building = PipelineMother.building(pipelineConfig);
        final Pipeline pipeline = dbHelper.savePipelineWithMaterials(building);

        CruiseConfig cruiseConfig = configHelper.currentConfig();
        PipelineConfig cfg = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("last"));
        cfg.removeMaterialConfig(cfg.materialConfigs().get(1));
        configHelper.writeConfigFile(cruiseConfig);

        assertThat(serverHealthService.filterByScope(HealthStateScope.forPipeline("last")).size(), is(0));

        final long jobId = pipeline.getStages().get(0).getJobInstances().get(0).getId();

        Date currentTime = new Date(System.currentTimeMillis() - 1);
        Pipeline loadedPipeline = (Pipeline) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                Pipeline loadedPipeline = null;
                try {
                    loadedPipeline = loader.pipelineWithPasswordAwareBuildCauseByBuildId(jobId);
                    fail("should not have loaded pipeline with build-cause as one of the necessary materials was not found");
                } catch (Exception e) {
                    assertThat(e, is(instanceOf(StaleMaterialsOnBuildCause.class)));
                    assertThat(e.getMessage(), is("Cannot load job 'last/" + pipeline.getCounter() + "/stage/1/job-one' because material " + onDirTwo + " was not found in config."));
                }
                return loadedPipeline;
            }
        });
        
        assertThat(loadedPipeline, is(nullValue()));

        JobInstance reloadedJobInstance = jobInstanceService.buildById(jobId);
        assertThat(reloadedJobInstance.getState(), is(JobState.Completed));
        assertThat(reloadedJobInstance.getResult(), is(JobResult.Failed));

        assertThat(serverHealthService.filterByScope(HealthStateScope.forJob("last", "stage", "job-one")).size(), is(1));
        ServerHealthState error = serverHealthService.filterByScope(HealthStateScope.forJob("last", "stage", "job-one")).get(0);
        assertThat(error, is(new ServerHealthState(HealthStateLevel.ERROR,
                HealthStateType.general(HealthStateScope.forJob("last", "stage", "job-one")),
                "Cannot load job 'last/" + pipeline.getCounter() + "/stage/1/job-one' because material " + onDirTwo + " was not found in config.",
                "Job for pipeline 'last/" + pipeline.getCounter() + "/stage/1/job-one' has been failed as one or more material configurations were either changed or removed.",
                Timeout.FIVE_MINUTES)));
        DateTime expiryTime = (DateTime) ReflectionUtil.getField(error, "expiryTime");
        assertThat(expiryTime.toDate().after(currentTime), is(true));
        assertThat(expiryTime.toDate().before(new Date(System.currentTimeMillis() + 5 * 60 * 1000 + 1)), is(true));

        String logText = FileUtil.readToEnd(artifactsService.findArtifact(reloadedJobInstance.getIdentifier(), ArtifactLogUtil.getConsoleLogOutputFolderAndFileName()));
        assertThat(logText, containsString("Cannot load job 'last/" + pipeline.getCounter() + "/stage/1/job-one' because material " + onDirTwo + " was not found in config."));
        assertThat(logText, containsString("Job for pipeline 'last/" + pipeline.getCounter() + "/stage/1/job-one' has been failed as one or more material configurations were either changed or removed."));
    }

    @Test//if other materials have expansion concept at some point, add more tests here
    public void shouldSetPasswordForExpandedSvnMaterial() {
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("last", new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job-one"))));
        pipelineConfig.materialConfigs().clear();
        SvnMaterialConfig materialConfig = svnRepo.materialConfig();
        materialConfig.setConfigAttributes(Collections.singletonMap(SvnMaterialConfig.CHECK_EXTERNALS, String.valueOf(true)));
        materialConfig.setPassword("boozer");
        pipelineConfig.addMaterialConfig(materialConfig);
        configHelper.addPipeline(pipelineConfig);

        MaterialConfigs allConfigsWithExpandedExternals = materialExpansionService.expandMaterialConfigsForScheduling(pipelineConfig.materialConfigs());
        MaterialRevisions materialRevisions = ModificationsMother.modifyOneFile(MaterialsMother.createMaterialsFromMaterialConfigs(allConfigsWithExpandedExternals));
        Pipeline building = PipelineMother.buildingWithRevisions(pipelineConfig, materialRevisions);
        Pipeline pipeline = dbHelper.savePipelineWithMaterials(building);

        final long jobId = pipeline.getStages().get(0).getJobInstances().get(0).getId();
        Pipeline loadedPipeline = (Pipeline) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                return loader.pipelineWithPasswordAwareBuildCauseByBuildId(jobId);
            }
        });

        MaterialRevisions revisions = loadedPipeline.getBuildCause().getMaterialRevisions();
        assertThat(revisions.getRevisions().size(), is(2));
        assertThat(((SvnMaterial) revisions.getRevisions().get(0).getMaterial()).getPassword(), is("boozer"));
        assertThat(((SvnMaterial) revisions.getRevisions().get(1).getMaterial()).getPassword(), is("boozer"));
    }
}
