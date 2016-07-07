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

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.DefaultSchedulingContext;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.HgTestRepo;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class BuildCauseProducerServiceWithFlipModificationTest {
    private static final String STAGE_NAME = "dev";

    @Autowired private GoConfigService goConfigService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private PipelineScheduler buildCauseProducer;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired private ScheduleHelper scheduleHelper;
    @Autowired PipelineService pipelineService;
	@Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private SubprocessExecutionContext subprocessExecutionContext;

    public Subversion repository;

    public static SvnTestRepo svnRepository;
    private static final String PIPELINE_NAME = "mingle";
    private HgTestRepo hgTestRepo;
    private PipelineConfig mingleConfig;
    private GoConfigFileHelper configHelper;
    private SvnMaterialConfig svnMaterialConfig;


    @Before
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();

        dbHelper.onSetUp();
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();

        svnRepository = new SvnTestRepo("testSvnRepo");
        hgTestRepo = new HgTestRepo("testHgRepo");
        repository = new SvnCommand(null, svnRepository.projectRepositoryUrl());
        svnMaterialConfig = new SvnMaterialConfig(repository.getUrl().forCommandline(), repository.getUserName(), repository.getPassword(), repository.isCheckExternals());
    }

    @After
    public void teardown() throws Exception {
        TestRepo.internalTearDown();
        dbHelper.onTearDown();
        FileUtil.deleteFolder(goConfigService.artifactsDir());
        pipelineScheduleQueue.clear();
        configHelper.onTearDown();
    }

    @Test
    public void shouldHaveModificationChangedAsTrueWhenThereIsNoHistory() throws Exception {
        mingleConfig = configHelper.addPipeline(PIPELINE_NAME, STAGE_NAME, svnMaterialConfig, "unit", "functional");

        scheduleHelper.autoSchedulePipelinesWithRealMaterials(PIPELINE_NAME);
        verifyBuildCauseHasModificationsWith(pipelineScheduleQueue.toBeScheduled(), true);
    }

    @Test
    public void shouldHaveModificationChangedAsFalseWhenForceBuildWithoutModification() throws Exception {
        mingleConfig = configHelper.addPipeline(PIPELINE_NAME, STAGE_NAME, svnMaterialConfig, "unit",
                "functional");
        consume(buildCause());

        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(PIPELINE_NAME, new Username(new CaseInsensitiveString("pavan")), new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());

        verifyBuildCauseHasModificationsWith(pipelineScheduleQueue.toBeScheduled(), false);
    }

    @Test
    public void shouldHaveModificationChangedAsTrueForNewRevisions() throws Exception {
        mingleConfig = configHelper.addPipeline(PIPELINE_NAME, STAGE_NAME, svnMaterialConfig, "unit", "functional");
        consume(buildCause());

        svnRepository.checkInOneFile("abc");
        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(PIPELINE_NAME, new Username(new CaseInsensitiveString("pavan")), new ScheduleOptions(revisions, environmentVariables, new HashMap<String, String>()), new ServerHealthStateOperationResult());
        verifyBuildCauseHasModificationsWith(pipelineScheduleQueue.toBeScheduled(), true);
    }

    @Test
    public void shouldHaveCorrectModificationChangedForMultipleMaterials() throws Exception {
        setUpPipelineWithTwoMaterials();

        scheduleHelper.autoSchedulePipelinesWithRealMaterials(PIPELINE_NAME);
        consume(buildCauseForPipeline());

        svnRepository.checkInOneFile("abc");
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(PIPELINE_NAME);

        assertModificationChangedStateBasedOnMaterial(pipelineScheduleQueue.toBeScheduled());
    }

    @Test
    public void shouldMarkModificationChangedAsTrueIfMaterialsChanged() throws Exception {
        SvnMaterialConfig svnMaterialConfig = setUpPipelineWithTwoMaterials();

        scheduleHelper.autoSchedulePipelinesWithRealMaterials(PIPELINE_NAME);
        consume(buildCauseForPipeline());
        
        configHelper.replaceMaterialForPipeline(PIPELINE_NAME, svnMaterialConfig);
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(PIPELINE_NAME);

        verifyBuildCauseHasModificationsWith(pipelineScheduleQueue.toBeScheduled(), true);
    }

    private SvnMaterialConfig setUpPipelineWithTwoMaterials() throws Exception {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig(repository.getUrl().forCommandline(), repository.getUserName(), repository.getPassword(), repository.isCheckExternals());
        svnMaterialConfig.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FOLDER, "svnDir"));
        MaterialConfigs materialConfigs = new MaterialConfigs(svnMaterialConfig, hgTestRepo.createMaterialConfig("hgDir"));
        mingleConfig = configHelper.addPipeline(PIPELINE_NAME, STAGE_NAME, materialConfigs, "unit", "functional");
        return svnMaterialConfig;
    }

    private void consume(final BuildCause buildCause) throws SQLException {
        dbHelper.saveRevs(buildCause.getMaterialRevisions());
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                Pipeline latestPipeline = pipelineScheduleQueue.createPipeline(buildCause, mingleConfig, new DefaultSchedulingContext(buildCause.getApprover(), new Agents()), "md5",
                        new TimeProvider());
//        Pipeline latestPipeline = PipelineMother.schedule(mingleConfig, buildCause);
                pipelineDao.saveWithStages(latestPipeline);
                dbHelper.passStage(latestPipeline.getStages().first());
            }
        });
    }

    private BuildCause buildCauseForPipeline() {
        BuildCause buildCause = pipelineScheduleQueue.toBeScheduled().get(PIPELINE_NAME);
        assertThat("Should be scheduled", buildCause, is(not(nullValue())));
        return buildCause;
    }

    @Test
    public void shouldProduceBuildCauseWithModificationChangedForTheFirstTime() throws Exception {
        mingleConfig = configHelper.addPipeline(PIPELINE_NAME, STAGE_NAME, svnMaterialConfig, "unit", "functional");
        consume(buildCause());

        preparePipelineWithMaterial();
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(PIPELINE_NAME);
        verifyBuildCauseHasModificationsWith(pipelineScheduleQueue.toBeScheduled(), true);
    }

    private BuildCause buildCause() {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        SvnMaterial svnMaterial = SvnMaterial.createSvnMaterialWithMock(repository);
        materialRevisions.addRevision(svnMaterial, svnMaterial.latestModification(null, subprocessExecutionContext));
        return BuildCause.createWithModifications(materialRevisions, "");
    }

    private void verifyBuildCauseHasModificationsWith(Map<String, BuildCause> load, boolean changed) {
        for (BuildCause buildCause : load.values()) {
            assertBuildCauseWithModificationHasChangedStatus(changed, buildCause);
        }
    }

    private void assertModificationChangedStateBasedOnMaterial(Map<String, BuildCause> load) {
        for (BuildCause buildCause : load.values()) {
            for (MaterialRevision revision : buildCause.getMaterialRevisions()) {
                if (revision.getMaterial() instanceof HgMaterial) {
                    assertThat(revision.isChanged(), is(false));
                } else {
                    assertThat(revision.isChanged(), is(true));
                }
            }
        }
    }

    private void preparePipelineWithMaterial() throws Exception {
        SvnMaterial svnMaterial = SvnMaterial.createSvnMaterialWithMock(repository);
        ReflectionUtil.setField(svnMaterial, ScmMaterialConfig.FOLDER, "asc");
        ReflectionUtil.invoke(svnMaterial, "resetCachedIdentityAttributes");

        MaterialConfigs materialConfigs = new MaterialConfigs();
        materialConfigs.add(svnMaterial.config());

        configHelper.addPipeline("cruise", STAGE_NAME, materialConfigs, "unit", "functional");

        MaterialRevisions materialRevisions = new MaterialRevisions();
        materialRevisions.addRevision(svnMaterial, svnMaterial.latestModification(null, subprocessExecutionContext));
    }

    private void assertBuildCauseWithModificationHasChangedStatus(boolean changed, BuildCause buildCause) {
        for (MaterialRevision revision : buildCause.getMaterialRevisions()) {
            assertThat(revision.isChanged(), is(changed));
        }
    }
}