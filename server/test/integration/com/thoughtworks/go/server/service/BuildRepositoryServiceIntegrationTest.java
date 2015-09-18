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
import java.util.Date;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.CannotScheduleException;
import com.thoughtworks.go.domain.DefaultSchedulingContext;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.exception.StageAlreadyBuildingException;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.server.dao.DatabaseAccessHelper.AGENT_UUID;
import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class BuildRepositoryServiceIntegrationTest {
    @Autowired private BuildRepositoryService buildRepositoryService;
    @Autowired private StageService stageService;
    @Autowired private PipelineService pipelineService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private ScheduleService scheduleService;
    @Autowired private ServerHealthService serverHealthService;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private JobInstanceDao jobInstanceDao;
    @Autowired private JobInstanceService jobInstanceService;
    @Autowired private GoConfigService goConfigService;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoCache goCache;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private StageDao stageDao;
    @Autowired private InstanceFactory instanceFactory;

    private static GoConfigFileHelper config = new GoConfigFileHelper();
    private PipelineConfig mingle;
    private static final String DEV_STAGE = "dev";
    private static final String FT_STAGE = "ft";
    private Pipeline pipeline;
    private TestRepo svnTestRepo;
    private static final String PIPELINE_NAME = "mingle";
    public Subversion svnRepo;
    private static final String HOSTNAME = "10.18.0.1";
    private final String md5 = "md5-test";

    @AfterClass
    public static void tearDownConfigFileLocation() throws IOException {
        TestRepo.internalTearDown();
    }


    @Before
    public void setUp() throws Exception {

        dbHelper.onSetUp();
        config.onSetUp();
        config.usingCruiseConfigDao(goConfigDao);
        goConfigService.forceNotifyListeners();

        svnTestRepo = new SvnTestRepo("testsvnrepo");

        svnRepo = new SvnCommand(null, svnTestRepo.projectRepositoryUrl());
        config.addPipeline(PIPELINE_NAME, DEV_STAGE, svnRepo, "foo");
        mingle = config.addStageToPipeline(PIPELINE_NAME, FT_STAGE, "bar");
        config.addAgent(HOSTNAME, AGENT_UUID);
        pipeline = dbHelper.newPipelineWithAllStagesPassed(mingle);
        goCache.clear();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        config.onTearDown();
        pipelineScheduleQueue.clear();
    }

    @Test
    public void shouldScheduleNextStageAndPipelineWhenStagePassed() throws Exception {
        createPipelineWithFirstStageCompletedAndNextStageBuilding(StageState.Passed);

        Stage stage1 = stageDao.mostRecentWithBuilds(PIPELINE_NAME, mingle.findBy(new CaseInsensitiveString(DEV_STAGE)));
        assertThat("Should be approved", stage1.isApproved(), is(true));
        assertThat(stage1.getApprovedBy(), is(DEFAULT_APPROVED_BY));

        Stage stage2 = stageDao.mostRecentWithBuilds(PIPELINE_NAME, mingle.findBy(new CaseInsensitiveString(FT_STAGE)));
        assertThat(stage2.stageState(), is(StageState.Building));
        assertThat(stage2.getApprovedBy(), is(DEFAULT_APPROVED_BY));

        assertThat("In same pipeline", stage1.getPipelineId(), is(stage2.getPipelineId()));
    }

    @Test
    @Ignore(value = "#2055 we need to rewrite these as proper integration tests")
    public void shouldTriggerDependentPipelineWhenStagePassed() throws Exception {
        PipelineConfig product = config.addPipeline("product", "dev", svnRepo, "build");
        config.setDependencyOn(product, PIPELINE_NAME, DEV_STAGE);

        int oldSize = pipelineScheduleQueue.toBeScheduled().size();
        createPipelineWithFirstStageCompletedAndNextStageBuilding(StageState.Passed);
        assertThat(pipelineScheduleQueue.toBeScheduled().size(), is(oldSize + 1));
        assertThat(pipelineScheduleQueue.hasBuildCause("product"), is(true));
    }

    @Test
    @Ignore(value = "#2055 we need to rewrite these as proper integration tests")
    public void shouldNotFailCompletedStageIfFailedToTriggerDependentPipeline() throws Exception {
        Subversion notExistsRepo = new SvnCommand(null, "http://notExistSvnURL");
        PipelineConfig dependentPipeline = config.addPipeline("product", "dev", notExistsRepo, "build");
        config.setDependencyOn(dependentPipeline, PIPELINE_NAME, DEV_STAGE);
        int beforeProcessing = serverHealthService.getAllValidLogs(goConfigService.currentCruiseConfig()).size();
        int oldSize = pipelineScheduleQueue.toBeScheduled().size();
        createPipelineWithFirstStageCompleted(mingle);

        assertThat(serverHealthService.getAllValidLogs(goConfigService.currentCruiseConfig()).size(),
                is(beforeProcessing + 1));
        assertThat(pipelineScheduleQueue.toBeScheduled().size(), is(oldSize));
    }

    @Test
    @Ignore(value = "#2055 we need to rewrite these as proper integration tests")
    public void shouldRerunDependentPipelineIfThereAreNoModifications() throws Exception {
        // creating dependentPipeline so that it depends on mingle
        PipelineConfig dependentPipeline = config.addPipeline("product", "dev", svnRepo, "build");
        config.setDependencyOn(dependentPipeline, PIPELINE_NAME, DEV_STAGE);
        // complete dependentPipeline so next time it will not find any modifications
        createPipelineWithFirstStageCompleted(dependentPipeline);
        Pipeline beforeRerun = pipelineService.mostRecentFullPipelineByName(CaseInsensitiveString.str(dependentPipeline.name()));
        Stage firstStage = beforeRerun.getFirstStage();

        // now the first stage of mingle just passed
        createPipelineWithFirstStageCompleted(mingle);

        // verify that dependentPipeline rerun
        Pipeline afterRerun = pipelineService.mostRecentFullPipelineByName(CaseInsensitiveString.str(dependentPipeline.name()));
        assertThat("A new stage instance should have been generated",
                afterRerun.getFirstStage().getId() > firstStage.getId(), is(true));
        assertThat("Should share the same pipeline id", afterRerun.getId(), is(beforeRerun.getId()));
    }

    @Test
    public void shouldNotTriggerPipelineWhenCurrentStageFailed() throws Exception {
        PipelineConfig product = config.addPipeline("product", "dev", svnRepo, "build");
        config.setDependencyOn(product, PIPELINE_NAME, DEV_STAGE);

        int oldSize = pipelineScheduleQueue.toBeScheduled().size();
        createPipelineWithFirstStageCompletedAndNextStageBuilding(StageState.Failed);

        assertThat(pipelineScheduleQueue.toBeScheduled().size(), is(oldSize));
    }

    @Test
    public void shouldNotSkipBuildingStateWhenTransitionFromPreparingToCompleting() throws Exception {
        //Bug Fix for #1630
        PipelineConfig pipelineConfig = config.addPipeline("product", "dev", svnRepo, "build");
        Pipeline pipelineInPreparingState = PipelineMother.preparing(pipelineConfig);
        dbHelper.savePipelineWithStagesAndMaterials(pipelineInPreparingState);
        Pipeline reloadedPipeline = pipelineDao.mostRecentPipeline("product");
        Stage stage = reloadedPipeline.getFirstStage();
        JobInstance job = stage.getJobInstances().first();
        String agentUuid = job.getAgentUuid();
        long buildId = job.getId();

        buildRepositoryService.completing(new JobIdentifier(reloadedPipeline, stage, job), JobResult.Failed,
                agentUuid);

        JobInstance reloadedJob = jobInstanceDao.buildByIdWithTransitions(buildId);
        assertThat(reloadedJob.getTransitions().byState(JobState.Building), is(not(nullValue())));
    }

    @Test
    @Deprecated
    public void shouldNotScheduleDuplicatedStage() throws Exception {
        Pipeline oldPipeline = dbHelper.newPipelineWithFirstStagePassed(mingle);
        Pipeline latestPipeline = dbHelper.newPipelineWithAllStagesPassed(mingle);

        int oldSize = latestPipeline.getStages().size();

        Stage stage = oldPipeline.getStages().first();
        scheduleService.automaticallyTriggerRelevantStagesFollowingCompletionOf(stage);

        oldPipeline = pipelineDao.loadPipeline(oldPipeline.getId());
        Stage ftStage = oldPipeline.getStages().byName(FT_STAGE);
        JobInstance secondJob = ftStage.getJobInstances().first();
        secondJob.setIdentifier(new JobIdentifier(oldPipeline, ftStage, secondJob));
        secondJob.setAgentUuid(AGENT_UUID);

        jobInstanceDao.updateAssignedInfo(secondJob);

        reportJobPassed(secondJob);

        latestPipeline = pipelineDao.loadPipeline(latestPipeline.getId());
        assertThat(latestPipeline.getStages().size(), is(oldSize));
    }

    @Test
    public void shouldNotUpdateIgnoredBuildStatus() throws Exception {
        Stage stage = dbHelper.saveBuildingStage("studios", "dev");
        JobInstance job = stage.getJobInstances().get(0);
        scheduleService.rescheduleJob(job);
        reportJobPassed(job);
        JobInstance reloaded = jobInstanceDao.buildByIdWithTransitions(job.getId());
        assertThat(reloaded.getState(), is(JobState.Rescheduled));
    }

    @Test
    public void shouldNotUpdateIgnoredBuildResult() throws Exception {
        Stage stage = dbHelper.saveBuildingStage("studios", "dev");
        JobInstance job = stage.getJobInstances().get(0);
        scheduleService.rescheduleJob(job);
        buildRepositoryService.completing(job.getIdentifier(), JobResult.Passed, AGENT_UUID);
        JobInstance reloaded = jobInstanceDao.buildByIdWithTransitions(job.getId());
        assertThat(reloaded.getResult(), is(JobResult.Unknown));
    }

    @Test
    public void shouldNotScheduleNextStageWhenApprovalRequiredAndStagePassed() throws Exception {
        config.requireApproval(PIPELINE_NAME, FT_STAGE);
        Pipeline newPipeline = createPipelineWithFirstStageBuilding(mingle);
        completeStageAndTrigger(newPipeline.getFirstStage());

        Stage stage1 = stageDao.mostRecentWithBuilds(PIPELINE_NAME, mingle.findBy(new CaseInsensitiveString(DEV_STAGE)));
        assertThat("Should be approved", stage1.isApproved(), is(true));
        assertThat("1st stage should be approved by Cruise", stage1.getApprovedBy(),
                is(GoConstants.DEFAULT_APPROVED_BY));
        assertThat("Should be passed", stage1.getJobInstances().first().getResult(), is(JobResult.Passed));

        Stage stage2 = stageDao.mostRecentWithBuilds(PIPELINE_NAME, mingle.findBy(new CaseInsensitiveString(FT_STAGE)));
        assertThat(stage2.getPipelineId(), is(pipeline.getId()));
        assertThat(stage2.stageState(), is(StageState.Passed));

        assertThat("In different pipelines", stage1.getPipelineId(), is(not(stage2.getPipelineId())));
    }

    @Test
    public void shouldNotScheduleNextStageIfItContainsNoJobs() throws Exception {
        config.setRunOnAllAgents(PIPELINE_NAME, FT_STAGE, "bar", true);
        config.addResourcesFor(PIPELINE_NAME, FT_STAGE, "bar", "non-existent");
        Pipeline newPipeline = createPipelineWithFirstStageBuilding(mingle);
        try {
            completeStageAndTrigger(newPipeline.getFirstStage());
            fail("should not have scheduled the stage");
        } catch (CannotScheduleException e) {
            // ok
        }

        Stage stage1 = stageDao.mostRecentWithBuilds(PIPELINE_NAME, mingle.findBy(new CaseInsensitiveString(DEV_STAGE)));
        assertThat(stage1.stageState(), is(StageState.Passed));

        Stage stage2 = stageService.findStageWithIdentifier(new StageIdentifier(newPipeline.getIdentifier(), FT_STAGE, "1"));
        assertThat(stage2, is(instanceOf(NullStage.class)));
    }

    @Test
    public void shouldScheduleCurrentStageInNextEligiblePipeline() throws Exception {
        Stage oldFtStage = createPipelineWithFirstStageCompletedAndNextStageBuilding(StageState.Passed);
        createNewPipelineWithFirstStagePassed();
        Stage mostRecentPassedStage = createNewPipelineWithFirstStagePassed();
        createNewPipelineWithFirstStageFailed();

        dbHelper.passStage(oldFtStage);
        reportJobPassed(oldFtStage.getJobInstances().get(0));

        Stage mingleFt = stageDao.mostRecentWithBuilds(PIPELINE_NAME, mingle.findBy(new CaseInsensitiveString(FT_STAGE)));
        assertThat(mingleFt.getPipelineId(), is(mostRecentPassedStage.getPipelineId()));
    }

    @Test
    public void shouldUpdateStageStatusWhenAllJobsPass() throws Exception {
        Stage stage1 = createPipelineWithFirstStageCompletedAndNextStageBuilding(StageState.Passed);
        assertThat(stage1.getResult(), is(StageResult.Unknown));
        JobInstances jobs = stage1.getJobInstances();
        for (JobInstance job : jobs) {
            buildRepositoryService.completing(job.getIdentifier(), JobResult.Passed, AGENT_UUID);
            reportJobPassed(job);
        }

        Stage after = stageService.stageById(stage1.getId());
        JobInstances instances = after.getJobInstances();
        assertThat(instances.size(), is(1));
        assertThat(instances.get(0).getResult(), is(JobResult.Passed));
        assertThat(instances.get(0).getState(), is(JobState.Completed));

        assertThat(after.getResult(), is(StageResult.Passed));
    }


    @Test
    public void shouldNotScheduleCurrentStageIfAlreadyMostRecentPipeline() throws Exception {
        Stage mostRecent = createPipelineWithFirstStageCompletedAndNextStageBuilding(StageState.Passed);

        reportJobPassed(mostRecent.getJobInstances().get(0));

        Stage mingleFt = stageDao.mostRecentWithBuilds(PIPELINE_NAME, mingle.findBy(new CaseInsensitiveString(FT_STAGE)));
        assertThat(mingleFt.getPipelineId(), is(mostRecent.getPipelineId()));
    }

    @Test
    public void shouldNotScheduleNextStageWhenStageAlreadyActive() throws Exception {
        createPipelineWithFirstStageCompletedAndNextStageBuilding(StageState.Passed);
        Stage originalFt = stageDao.mostRecentWithBuilds(PIPELINE_NAME, mingle.findBy(new CaseInsensitiveString(FT_STAGE)));
        createPipelineWithFirstStageCompletedAndNextStageBuilding(StageState.Passed);
        Stage mingleFt = stageDao.mostRecentWithBuilds(PIPELINE_NAME, mingle.findBy(new CaseInsensitiveString(FT_STAGE)));
        assertThat(mingleFt.getId(), is(originalFt.getId()));
    }

    @Test
    public void shouldTriggerCurrentStageInNextEligiblePipelineIfPrevStageIsAutoApproval() throws Exception {
        Stage oldFtStage = createPipelineWithFirstStageCompletedAndNextStageBuilding(StageState.Passed);
        Pipeline newPipeline = createPipelineWithFirstStageCompleted(mingle);
        completeStageAndTrigger(oldFtStage);
        Stage mostRecent = stageDao.mostRecentWithBuilds(PIPELINE_NAME, mingle.findBy(new CaseInsensitiveString(FT_STAGE)));
        assertThat(mostRecent.getPipelineId(), is(oldFtStage.getPipelineId() + 1));
    }

    @Test
    public void shouldNotTriggerCurrentStageInAnyNewerPipelineIfCurrentStageIsManualApproval() throws Exception {
        config.configureStageAsManualApproval(PIPELINE_NAME, FT_STAGE);
        Stage oldFtStage = createPipelineWithFirstStageCompletedAndNextStageBuilding(StageState.Passed);
        Pipeline newPipeline = createPipelineWithFirstStageCompleted(mingle);
        Stage mostRecent = stageDao.mostRecentWithBuilds(PIPELINE_NAME, mingle.findBy(new CaseInsensitiveString(FT_STAGE)));
        assertThat(mostRecent.getId(), is(oldFtStage.getId()));
    }

    private JobInstance completeStageAndTrigger(Stage oldFtStage) throws Exception {
        JobInstance job = oldFtStage.getJobInstances().first();
        buildRepositoryService.completing(job.getIdentifier(), JobResult.Passed, AGENT_UUID);
        reportJobPassed(job);
        return jobInstanceService.buildByIdWithTransitions(job.getId());
    }

    @Test
    public void shouldCancelAllJobs() throws Exception {
        mingle = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl(PIPELINE_NAME, DEV_STAGE,
                svnTestRepo.projectRepositoryUrl());
        StageConfig devStage = mingle.get(0);
        schedulePipeline(mingle);
        final Stage stage = stageDao.mostRecentWithBuilds(CaseInsensitiveString.str(mingle.name()), devStage);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            public void doInTransactionWithoutResult(TransactionStatus status) {
                stageService.cancelStage(stage);
            }
        });

        final Stage cancelled = stageService.stageById(stage.getId());
        assertThat(cancelled.stageState(), is(StageState.Cancelled));
        final JobInstances builds = cancelled.getJobInstances();
        for (JobInstance job : builds) {
            assertThat(job.currentStatus(), is(JobState.Completed));
            assertThat(job.getResult(), is(JobResult.Cancelled));
            assertThat(job.displayStatusWithResult(), is("cancelled"));
        }
    }

    private Stage createPipelineWithFirstStageCompletedAndNextStageBuilding(StageState stageState) throws Exception {
        Pipeline newPipeline = createPipelineWithFirstStageBuilding(mingle);
        Stage mostRecent = newPipeline.getFirstStage();
        if (stageState.equals(StageState.Failed)) {
            dbHelper.failStage(mostRecent);
        } else {
            dbHelper.passStage(mostRecent);
        }
        reportJobPassed(mostRecent.getJobInstances().first());
        Stage nextStage = stageDao.mostRecentWithBuilds(PIPELINE_NAME, mingle.findBy(new CaseInsensitiveString(FT_STAGE)));
        dbHelper.buildingBuildInstance(nextStage);
        return nextStage;
    }

    private void reportJobPassed(JobInstance jobInstance) throws Exception {
        buildRepositoryService.updateStatusFromAgent(jobInstance.getIdentifier(), JobState.Completed, AGENT_UUID);
    }

    private void reportJobCompleting(JobInstance jobInstance) {
        buildRepositoryService.completing(jobInstance.getIdentifier(), JobResult.Passed, AGENT_UUID);
    }

    private Pipeline createPipelineWithFirstStageCompleted(PipelineConfig pipeline) throws Exception {
        Stage firstStage = createPipelineWithFirstStageBuilding(pipeline).getFirstStage();
        reportJobCompleting(firstStage.getJobInstances().first());
        reportJobPassed(firstStage.getJobInstances().first());
        return pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(pipeline.name()));
    }

    private Pipeline createPipelineWithFirstStageBuilding(PipelineConfig pipeline) throws StageAlreadyBuildingException {
        Pipeline scheduledPipeline = schedulePipeline(pipeline);
        dbHelper.saveBuildingStage(scheduledPipeline.getFirstStage());

        return pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(pipeline.name()));
    }

    private Pipeline schedulePipeline(final PipelineConfig pipeline) {
        return (Pipeline) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                MaterialRevisions materialRevisions = new MaterialRevisions();
                for (Material material : MaterialsMother.createMaterialsFromMaterialConfigs(pipeline.materialConfigs())) {
                    materialRevisions.addRevision(material, new Modification("user", "comment", null, new Date(), ModificationsMother.nextRevision()));
                }
                materialRepository.save(materialRevisions);
                Pipeline scheduledPipeline = instanceFactory.createPipelineInstance(pipeline, BuildCause.createManualForced(materialRevisions, Username.ANONYMOUS),
                        new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider());
                pipelineService.save(scheduledPipeline);
                return scheduledPipeline;
            }
        });
    }

    private Stage createNewPipelineWithFirstStageFailed() throws Exception {
        return (Stage) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                Pipeline forcedPipeline = instanceFactory.createPipelineInstance(mingle, modifySomeFiles(mingle), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider());
                materialRepository.save(forcedPipeline.getBuildCause().getMaterialRevisions());
                pipelineService.save(forcedPipeline);
                Stage stage = forcedPipeline.getFirstStage();
                dbHelper.failStage(stage);
                return stage;
            }
        });
    }

    private Stage createNewPipelineWithFirstStagePassed() throws Exception {
        Pipeline forcedPipeline = schedulePipeline(mingle);
        dbHelper.passStage(forcedPipeline.getFirstStage());
        return forcedPipeline.getFirstStage();
    }
}
