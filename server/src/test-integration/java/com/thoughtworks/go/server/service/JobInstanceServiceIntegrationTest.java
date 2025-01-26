/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.JobStatusCache;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.fixture.SchedulerFixture;
import com.thoughtworks.go.helper.BuildPlanMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.ui.JobInstancesModel;
import com.thoughtworks.go.server.ui.SortOrder;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeProvider;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.thoughtworks.go.helper.AgentMother.localAgentWithResources;
import static com.thoughtworks.go.helper.BuildPlanMother.withBuildPlans;
import static com.thoughtworks.go.helper.JobInstanceMother.*;
import static com.thoughtworks.go.helper.ModificationsMother.modifyOneFile;
import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class JobInstanceServiceIntegrationTest {
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private JobInstanceDao jobInstanceDao;
    @Autowired
    private JobStatusCache jobStatusCache;
    @Autowired
    private JobInstanceService jobInstanceService;
    @Autowired
    private StageService stageService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private ScheduleHelper scheduleHelper;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private StageDao stageDao;
    @Autowired
    private InstanceFactory instanceFactory;

    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private PipelineWithTwoStages pipelineFixture;
    private SchedulerFixture schedulerFixture;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        dbHelper.onSetUp();
        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        schedulerFixture = new SchedulerFixture(dbHelper, stageDao, scheduleService);
    }

    @AfterEach
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        pipelineFixture.onTearDown();
        jobStatusCache.clear();
    }

    @Test
    public void shouldOnlyLoad25JobsFromLatestStage() {
        StageConfig devStage = pipelineFixture.devStage();

        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        Stage stage = pipeline.getStages().byName(CaseInsensitiveString.str(devStage.name()));

        for (int i = 0; i < 50; i++) {
            Pipeline pipeline1 = pipelineFixture.createdPipelineWithAllStagesPassed();
            schedulerFixture.rerunAndPassStage(pipeline1, devStage);
        }

        JobInstances jobs1 = jobInstanceService.latestCompletedJobs(
                pipelineFixture.pipelineName, pipelineFixture.devStage, stage.getJobInstances().first().getName());
        assertThat(jobs1.size()).isEqualTo(25);
    }

    @Test
    public void shouldClearOutStageByIdCacheOnJobUpdate() {
        StageConfig ftStage = pipelineFixture.ftStage();

        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        Stage stage = pipeline.getStages().byName(CaseInsensitiveString.str(ftStage.name()));

        long stageId = stage.getId();
        Stage stageLoadedBeforeJobUpdate = stageService.stageById(stageId);

        assertThat(stageLoadedBeforeJobUpdate.getJobInstances().get(0).getState()).isEqualTo(JobState.Scheduled);

        JobInstance instance = stage.getJobInstances().get(0);
        instance.changeState(JobState.Building, new Date());
        jobInstanceService.updateStateAndResult(instance);

        Stage stageLoadedAfterJobUpdate = stageService.stageById(stageId);

        assertThat(stageLoadedAfterJobUpdate.getJobInstances().get(0).getState()).isEqualTo(JobState.Building);
    }

    @Test
    public void shouldContainIdentifierAfterSaved() {
        final Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();

        JobConfig jobConfig = pipelineFixture.devStage().allBuildPlans().first();
        RunOnAllAgents.CounterBasedJobNameGenerator jobNameGenerator = new RunOnAllAgents.CounterBasedJobNameGenerator(CaseInsensitiveString.str(jobConfig.name()));
        JobInstances instances = instanceFactory.createJobInstance(new CaseInsensitiveString("someStage"), jobConfig, new DefaultSchedulingContext(), new TimeProvider(), jobNameGenerator);
        final JobInstance newJob = instances.first();

        final StageIdentifier stageIdentifier = new StageIdentifier(pipeline.getName(), pipeline.getCounter(), pipeline.getLabel(),
                pipeline.getFirstStage().getName(), String.valueOf(pipeline.getFirstStage().getCounter()));
        dbHelper.txTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobInstanceService.save(stageIdentifier, pipeline.getFirstStage().getId(), newJob);
            }
        });

        assertThat(newJob.getIdentifier()).isEqualTo(new JobIdentifier(pipeline, pipeline.getFirstStage(), newJob));
    }

    @Test
    public void shouldFindCurrentJobsOrderedByName() {
        StageConfig stageConfig = StageConfigMother.custom("dev", "build", "alpha");
        Stage stage = instanceFactory.createStageInstance(stageConfig, new DefaultSchedulingContext("anyone"), "md5-test", new TimeProvider());

        for (JobInstance instance : stage.getJobInstances()) {
            instance.setIdentifier(new JobIdentifier("cruise", 1, "1", "dev", "1", instance.getName()));
        }

        jobStatusCache.jobStatusChanged(stage.getJobInstances().first());
        jobStatusCache.jobStatusChanged(stage.getJobInstances().last());

        JobInstances jobs = jobInstanceService.currentJobsOfStage("cruise", stageConfig);
        assertThat(jobs.first().getName()).isEqualTo("alpha");
        assertThat(jobs.last().getName()).isEqualTo("build");
    }

    @Test
    public void shouldFindAllCopiesOfJobsRunOnAllAgents() {
        StageConfig stageConfig = StageConfigMother.custom("dev", "build");
        JobConfig jobConfig = stageConfig.jobConfigByInstanceName("build", true);
        jobConfig.setRunOnAllAgents(true);
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("anyone", new Agents(new Agent(uuid1), new Agent(uuid2)));
        Stage stage = instanceFactory.createStageInstance(stageConfig, schedulingContext, "md5-test", new TimeProvider());

        for (JobInstance instance : stage.getJobInstances()) {
            instance.setIdentifier(new JobIdentifier("cruise", 1, "1", "dev", "1", instance.getName()));
        }

        jobStatusCache.jobStatusChanged(stage.getJobInstances().first());
        jobStatusCache.jobStatusChanged(stage.getJobInstances().last());

        JobInstances jobs = jobInstanceService.currentJobsOfStage("cruise", stageConfig);
        assertThat(jobs.first().getName()).isEqualTo(RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker("build", 1));
        assertThat(jobs.last().getName()).isEqualTo(RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker("build", 2));
        assertThat(jobs.size()).isEqualTo(2);
    }

    @Test
    public void shouldFindAllCopiesOfJobsRunMultipleInstance() {
        StageConfig stageConfig = StageConfigMother.custom("dev", "build");
        JobConfig jobConfig = stageConfig.jobConfigByInstanceName("build", true);
        jobConfig.setRunInstanceCount(2);
        DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("anyone", new Agents());
        Stage stage = instanceFactory.createStageInstance(stageConfig, schedulingContext, "md5-test", new TimeProvider());

        for (JobInstance instance : stage.getJobInstances()) {
            instance.setIdentifier(new JobIdentifier("cruise", 1, "1", "dev", "1", instance.getName()));
        }

        jobStatusCache.jobStatusChanged(stage.getJobInstances().first());
        jobStatusCache.jobStatusChanged(stage.getJobInstances().last());

        JobInstances jobs = jobInstanceService.currentJobsOfStage("cruise", stageConfig);
        assertThat(jobs.size()).isEqualTo(2);
        assertThat(jobs.first().getName()).isEqualTo(RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker("build", 1));
        assertThat(jobs.last().getName()).isEqualTo(RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker("build", 2));
    }

    @Test
    public void shouldThrowExceptionIfThereAreNoJobsToBeScheduled() {
        StageConfig stageConfig = StageConfigMother.custom("dev", "build");
        JobConfig jobConfig = stageConfig.jobConfigByInstanceName("build", true);
        jobConfig.setRunOnAllAgents(true);
        jobConfig.addResourceConfig("non-existent");
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("anyone", new Agents(new Agent(uuid1), new Agent(uuid2)));

        try {
            instanceFactory.createStageInstance(stageConfig, schedulingContext, "md5-test", new TimeProvider());
            fail("Expected a CannotScheduleException to be thrown");
        } catch (CannotScheduleException e) {
            //expected
        }
    }

    @Test
    public void orderedScheduledBuilds_shouldLoadJobPlansWithFetchMaterialsFlagFromStage() {
        PipelineConfig pipelineConfig = PipelineMother.withSingleStageWithMaterials("go", "dev", withBuildPlans("unit"));
        pipelineConfig.getFirstStageConfig().setFetchMaterials(false);
        configHelper.addPipeline("go", "dev");
        scheduleHelper.schedule(pipelineConfig, BuildCause.createWithModifications(modifyOneFile(pipelineConfig), ""), DEFAULT_APPROVED_BY);
        List<JobPlan> jobPlans = jobInstanceService.orderedScheduledBuilds();
        assertThat(jobPlans.size()).isEqualTo(1);
        assertThat(jobPlans.get(0).shouldFetchMaterials()).isFalse();
    }

    @Test
    public void orderedScheduledBuilds_shouldLoadJobPlansWithCleanWorkingDirFlagFromStage() {
        PipelineConfig pipelineConfig = PipelineMother.withSingleStageWithMaterials("go", "dev", withBuildPlans("unit"));
        pipelineConfig.getFirstStageConfig().setCleanWorkingDir(true);
        configHelper.addPipeline("go", "dev");
        scheduleHelper.schedule(pipelineConfig, BuildCause.createWithModifications(modifyOneFile(pipelineConfig), ""), DEFAULT_APPROVED_BY);
        List<JobPlan> jobPlans = jobInstanceService.orderedScheduledBuilds();
        assertThat(jobPlans.size()).isEqualTo(1);
        assertThat(jobPlans.get(0).shouldCleanWorkingDir()).isTrue();
    }

    @Test
    public void waitingJobPlans_shouldLoadScheduledJobPlansEnvironmentMapping() {
        PipelineConfig pipelineConfig = PipelineMother.withSingleStageWithMaterials("go", "dev", withBuildPlans("unit"));
        pipelineConfig.getFirstStageConfig().setCleanWorkingDir(true);
        configHelper.addPipeline("go", "dev");
        configHelper.addEnvironments("newEnv");
        configHelper.addPipelineToEnvironment("newEnv", "go");
        scheduleHelper.schedule(pipelineConfig, BuildCause.createWithModifications(modifyOneFile(pipelineConfig), ""), DEFAULT_APPROVED_BY);
        List<JobPlan> jobPlans = jobInstanceService.orderedScheduledBuilds();

        Username viewOnlyUser = new Username(new CaseInsensitiveString("view"));
        configHelper.setViewPermissionForGroup(BasicPipelineConfigs.DEFAULT_GROUP, "view");

        List<WaitingJobPlan> waitingJobPlans = jobInstanceService.waitingJobPlans(viewOnlyUser);
        assertThat(waitingJobPlans.size()).isEqualTo(1);
        assertThat(waitingJobPlans.get(0).jobPlan()).isEqualTo(jobPlans.get(0));
        assertThat(waitingJobPlans.get(0).envName()).isEqualTo("newEnv");
    }

    @Test
    public void waitingJobPlans_shouldOnlyReturnJobPlansForThePipelineViewablebyTheCurrentUser() {
        configHelper.enableSecurity();
        configHelper.addAdmins("root");
        PipelineConfig pipelineConfig = PipelineMother.withSingleStageWithMaterials("go", "dev", withBuildPlans("unit"));
        pipelineConfig.getFirstStageConfig().setCleanWorkingDir(true);
        PipelineConfig pipelineConfig1 = PipelineMother.withSingleStageWithMaterials("build", "build", withBuildPlans("test"));
        configHelper.addPipeline("go", "dev");
        configHelper.addPipelineToGroup(pipelineConfig1, "first");
        scheduleHelper.schedule(pipelineConfig, BuildCause.createWithModifications(modifyOneFile(pipelineConfig), ""), DEFAULT_APPROVED_BY);
        scheduleHelper.schedule(pipelineConfig1, BuildCause.createWithModifications(modifyOneFile(pipelineConfig1), ""), DEFAULT_APPROVED_BY);

        Username viewOnlyUser = new Username(new CaseInsensitiveString("view"));
        configHelper.setViewPermissionForGroup("first", "view");

        List<WaitingJobPlan> waitingJobPlans = jobInstanceService.waitingJobPlans(viewOnlyUser);
        assertThat(waitingJobPlans.size()).isEqualTo(1);
        assertThat(waitingJobPlans.get(0).jobPlan().getPipelineName()).isEqualTo("build");
    }

    @Test
    public void waitingJobPlans_shouldReturnAllJobPlansForTheAdminUser() {
        configHelper.enableSecurity();
        configHelper.addAdmins("root");
        PipelineConfig pipelineConfig = PipelineMother.withSingleStageWithMaterials("go", "dev", withBuildPlans("unit"));
        pipelineConfig.getFirstStageConfig().setCleanWorkingDir(true);
        PipelineConfig pipelineConfig1 = PipelineMother.withSingleStageWithMaterials("build", "build", withBuildPlans("test"));
        configHelper.addPipeline("go", "dev");
        configHelper.addPipelineToGroup(pipelineConfig1, "first");
        scheduleHelper.schedule(pipelineConfig, BuildCause.createWithModifications(modifyOneFile(pipelineConfig), ""), DEFAULT_APPROVED_BY);
        scheduleHelper.schedule(pipelineConfig1, BuildCause.createWithModifications(modifyOneFile(pipelineConfig1), ""), DEFAULT_APPROVED_BY);

        Username viewOnlyUser = new Username(new CaseInsensitiveString("root"));
        configHelper.setViewPermissionForGroup("first", "view");

        List<WaitingJobPlan> waitingJobPlans = jobInstanceService.waitingJobPlans(viewOnlyUser);
        assertThat(waitingJobPlans.size()).isEqualTo(2);
        assertThat(waitingJobPlans.get(0).jobPlan().getPipelineName()).isEqualTo("go");
        assertThat(waitingJobPlans.get(1).jobPlan().getPipelineName()).isEqualTo("build");
    }

    @Test
    public void shouldLoadAllBuildingJobs() {
        PipelineConfig goConfig = PipelineMother.withSingleStageWithMaterials("go", "dev", withBuildPlans("unit"));
        Stage goDev = dbHelper.schedulePipeline(goConfig, new TimeProvider()).getStages().get(0);
        dbHelper.buildingBuildInstance(goDev);

        PipelineConfig mingleConfig = PipelineMother.withSingleStageWithMaterials("mingle", "test", withBuildPlans("integration"));
        Stage mingleTest = dbHelper.schedulePipeline(mingleConfig, new TimeProvider()).getStages().get(0);
        dbHelper.buildingBuildInstance(mingleTest);

        dbHelper.newPipelineWithAllStagesPassed(PipelineMother.withSingleStageWithMaterials("twist", "acceptance", withBuildPlans("firefox"))).getStages().get(0);//a completed pipeline

        assertThat(jobInstanceService.allBuildingJobs().size()).isEqualTo(2);
        assertThat(jobInstanceService.allBuildingJobs()).contains(goDev.getFirstJob().getIdentifier());
        assertThat(jobInstanceService.allBuildingJobs()).contains(mingleTest.getFirstJob().getIdentifier());
    }

    @Test
    public void shouldFailRequestedJobAndNotifyStageChange() {
        PipelineConfig goConfig = PipelineMother.withSingleStageWithMaterials("go", "dev", withBuildPlans("unit"));
        Stage goDev = dbHelper.schedulePipeline(goConfig, new TimeProvider()).getStages().get(0);
        dbHelper.buildingBuildInstance(goDev);

        PipelineConfig mingleConfig = PipelineMother.withSingleStageWithMaterials("mingle", "test", withBuildPlans("integration"));
        Stage mingleTest = dbHelper.schedulePipeline(mingleConfig, new TimeProvider()).getStages().get(0);
        dbHelper.buildingBuildInstance(mingleTest);

        JobInstance jobInstance = dbHelper.newPipelineWithFirstStageScheduled(PipelineMother.withSingleStageWithMaterials("twist", "acceptance", withBuildPlans("firefox"))).getStages().get(0).getJobInstances().get(0);

        final JobInstance[] changedJobPassed = new JobInstance[1];

        jobInstanceService.registerJobStateChangeListener(job -> changedJobPassed[0] = job);

        JobInstance jobInstance1 = jobInstanceService.buildByIdWithTransitions(jobInstance.getId());
        jobInstanceService.failJob(jobInstance1);

        assertThat(changedJobPassed[0].isFailed()).isTrue();
    }

    @Test
    public void shouldSet_BelongsToKnownPipeline_FlagOnEachJob_pipelineForWhichIsStillPresentInConfig() {
        String agentUuid = "special_uuid";

        configHelper.addPipeline("existingPipeline", "existingStage");

        Long existingStage = stageWithId("existingPipeline", "existingStage");

        Long nonExistentStage = stageWithId("existingPipeline", "removedStage");

        JobInstance completedJob = completed("existingJob", JobResult.Passed, new Date(1));
        completedJob.setAgentUuid(agentUuid);
        jobInstanceDao.save(existingStage, completedJob);

        JobInstance rescheduledJob = rescheduled("rescheduled", agentUuid);
        jobInstanceDao.save(nonExistentStage, rescheduledJob);
        jobInstanceDao.ignore(rescheduledJob);

        Long stageFromDeletedPipeline = stageWithId("deletedPipeline", "stage");

        JobInstance cancelledJob = cancelled("job");
        cancelledJob.setAgentUuid(agentUuid);
        jobInstanceDao.save(stageFromDeletedPipeline, cancelledJob);

        //completed
        List<JobInstance> sortedOnCompleted = listOf(jobInstanceService.completedJobsOnAgent(agentUuid, JobInstanceService.JobHistoryColumns.job, SortOrder.ASC, 1, 10));
        assertThat(sortedOnCompleted.size()).isEqualTo(3);
        assertThat(sortedOnCompleted.get(0).getName()).isEqualTo("existingJob");
        assertThat(sortedOnCompleted.get(0).isPipelineStillConfigured()).isTrue();
        assertThat(sortedOnCompleted.get(1).getName()).isEqualTo("job");
        assertThat(sortedOnCompleted.get(1).isPipelineStillConfigured()).isFalse();
        assertThat(sortedOnCompleted.get(2).getName()).isEqualTo("rescheduled");
        assertThat(sortedOnCompleted.get(2).isPipelineStillConfigured()).isTrue();
    }

    @Test
    public void shouldCompletedJobsForGivenAgent() {
        Long stageB_Id = stageWithId("pipeline-aaa", "stage-bbb");

        String agentUuid = "special_uuid";

        JobInstance completedJob = completed("job-bbb", JobResult.Passed, new Date(1));
        completedJob.setAgentUuid(agentUuid);
        jobInstanceDao.save(stageB_Id, completedJob);

        JobInstance rescheduledJob = rescheduled("rescheduled", agentUuid);
        jobInstanceDao.save(stageB_Id, rescheduledJob);
        jobInstanceDao.ignore(rescheduledJob);

        Long stageC_Id = stageWithId("pipeline-bbb", "stage-ccc");

        JobInstance cancelledJob = cancelled("job3");
        cancelledJob.setAgentUuid(agentUuid);
        jobInstanceDao.save(stageC_Id, cancelledJob);

        JobInstance simpleJob = failed("simpleJob");
        simpleJob.getTransition(JobState.Completed).setStateChangeTime(new DateTime().plusYears(2).toDate());
        simpleJob.setAgentUuid(agentUuid);
        jobInstanceDao.save(stageC_Id, simpleJob);

        //completed
        List<JobInstance> sortedOnCompleted = listOf(jobInstanceService.completedJobsOnAgent(agentUuid, JobInstanceService.JobHistoryColumns.completed, SortOrder.DESC, 1, 10));
        assertThat(sortedOnCompleted.size()).isEqualTo(4);
        assertThat(sortedOnCompleted.get(0).getName()).isEqualTo("simpleJob");

        List<JobInstance> sortedOnCompletedAsc = listOf(jobInstanceService.completedJobsOnAgent(agentUuid, JobInstanceService.JobHistoryColumns.completed, SortOrder.ASC, 1, 10));
        assertThat(sortedOnCompletedAsc.size()).isEqualTo(4);
        assertThat(sortedOnCompletedAsc.get(3).getName()).isEqualTo("simpleJob");

        //pipeline
        List<JobInstance> sortedOnPipelineName = listOf(jobInstanceService.completedJobsOnAgent(agentUuid, JobInstanceService.JobHistoryColumns.pipeline, SortOrder.ASC, 1, 10));
        assertThat(sortedOnPipelineName.size()).isEqualTo(4);
        assertThat(sortedOnPipelineName.get(0).getIdentifier().getPipelineName()).isEqualTo("pipeline-aaa");
        assertThat(sortedOnPipelineName.get(1).getIdentifier().getPipelineName()).isEqualTo("pipeline-aaa");
        assertThat(sortedOnPipelineName.get(2).getIdentifier().getPipelineName()).isEqualTo("pipeline-bbb");

        List<JobInstance> sortedOnPipelineNameDesc = listOf(jobInstanceService.completedJobsOnAgent(agentUuid, JobInstanceService.JobHistoryColumns.pipeline, SortOrder.DESC, 1, 10));
        assertThat(sortedOnPipelineNameDesc.size()).isEqualTo(4);
        assertThat(sortedOnPipelineNameDesc.get(0).getIdentifier().getPipelineName()).isEqualTo("pipeline-bbb");
        assertThat(sortedOnPipelineNameDesc.get(2).getIdentifier().getPipelineName()).isEqualTo("pipeline-aaa");

        //stage
        List<JobInstance> sortedOnStageName = listOf(jobInstanceService.completedJobsOnAgent(agentUuid, JobInstanceService.JobHistoryColumns.stage, SortOrder.ASC, 1, 10));
        assertThat(sortedOnStageName.size()).isEqualTo(4);
        assertThat(sortedOnStageName.get(0).getIdentifier().getStageName()).isEqualTo("stage-bbb");
        assertThat(sortedOnStageName.get(2).getIdentifier().getStageName()).isEqualTo("stage-ccc");

        List<JobInstance> sortedOnStageNameDesc = listOf(jobInstanceService.completedJobsOnAgent(agentUuid, JobInstanceService.JobHistoryColumns.stage, SortOrder.DESC, 1, 10));
        assertThat(sortedOnStageNameDesc.size()).isEqualTo(4);
        assertThat(sortedOnStageNameDesc.get(0).getIdentifier().getStageName()).isEqualTo("stage-ccc");
        assertThat(sortedOnStageNameDesc.get(2).getIdentifier().getStageName()).isEqualTo("stage-bbb");

        //result
        List<JobInstance> sortedOnResult = listOf(jobInstanceService.completedJobsOnAgent(agentUuid, JobInstanceService.JobHistoryColumns.result, SortOrder.ASC, 1, 10));
        assertThat(sortedOnResult.size()).isEqualTo(4);
        assertThat(sortedOnResult.get(0).getResult()).isEqualTo(JobResult.Cancelled);
        assertThat(sortedOnResult.get(1).getResult()).isEqualTo(JobResult.Failed);
        assertThat(sortedOnResult.get(2).getResult()).isEqualTo(JobResult.Passed);
        assertThat(sortedOnResult.get(3).getResult()).isEqualTo(JobResult.Unknown);

        List<JobInstance> sortedOnResultDesc = listOf(jobInstanceService.completedJobsOnAgent(agentUuid, JobInstanceService.JobHistoryColumns.result, SortOrder.DESC, 1, 10));
        assertThat(sortedOnResultDesc.size()).isEqualTo(4);
        assertThat(sortedOnResultDesc.get(3).getResult()).isEqualTo(JobResult.Cancelled);
        assertThat(sortedOnResultDesc.get(2).getResult()).isEqualTo(JobResult.Failed);
        assertThat(sortedOnResultDesc.get(1).getResult()).isEqualTo(JobResult.Passed);
        assertThat(sortedOnResultDesc.get(0).getResult()).isEqualTo(JobResult.Unknown);

        // duration
        List<JobInstance> sortedOnDuration = listOf(jobInstanceService.completedJobsOnAgent(agentUuid, JobInstanceService.JobHistoryColumns.duration, SortOrder.ASC, 1, 10));
        assertThat(sortedOnDuration.size()).isEqualTo(4);
        assertThat(sortedOnDuration.get(0).getResult()).isEqualTo(JobResult.Unknown); // duration is null therefore comes first
        assertThat(sortedOnDuration.get(1).getResult()).isEqualTo(JobResult.Passed); // has new Date(1)
        assertThat(sortedOnDuration.get(2).getResult()).isEqualTo(JobResult.Cancelled); // now.minusMinutes(5) always greater than new Date(1)
        assertThat(sortedOnDuration.get(3).getResult()).isEqualTo(JobResult.Failed); // 2 years therefore goes to the end

        List<JobInstance> sortedOnDurationDesc = listOf(jobInstanceService.completedJobsOnAgent(agentUuid, JobInstanceService.JobHistoryColumns.duration, SortOrder.DESC, 1, 10));
        assertThat(sortedOnDurationDesc.size()).isEqualTo(4);
        assertThat(sortedOnDurationDesc.get(0).getResult()).isEqualTo(JobResult.Failed); // 2 years therefore goes to the beginning
        assertThat(sortedOnDurationDesc.get(1).getResult()).isEqualTo(JobResult.Cancelled); // now.minusMinutes(5) always greater than new Date(1)
        assertThat(sortedOnDurationDesc.get(2).getResult()).isEqualTo(JobResult.Passed); // has new Date(1)
        assertThat(sortedOnDurationDesc.get(3).getResult()).isEqualTo(JobResult.Unknown); // duration is null therefore comes last
    }

    private List<JobInstance> listOf(JobInstancesModel instances) {
        return StreamSupport.stream(instances.spliterator(), false).collect(Collectors.toList());
    }

    @Test
    public void shouldNotNotifyListenersWhenTransactionRollsback() {
        final boolean[] isListenerCalled = {false};
        JobStatusListener jobStatusListener = job -> isListenerCalled[0] = true;
        jobInstanceService.registerJobStateChangeListener(jobStatusListener);
        StageConfig ftStage = pipelineFixture.ftStage();
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        Stage stage = pipeline.getStages().byName(CaseInsensitiveString.str(ftStage.name()));
        final JobInstance instance = stage.getJobInstances().get(0);
        instance.changeState(JobState.Building, new Date());
        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    jobInstanceService.updateStateAndResult(instance);
                    throw new RuntimeException("to rollback txn");
                }
            });
            fail("Should have thrown an exception and transaction rolled back. Listeners should not have be called on afterCommit");
        } catch (RuntimeException e) {
        }
        assertThat(isListenerCalled[0]).isFalse();
    }

    @Test
    public void shouldSaveJobDetailsCorrectlyForEveryJobInARunMultipleInstancesJob() {
        PipelineConfig pipelineConfig = PipelineMother.withSingleStageWithMaterials("go", "dev", withBuildPlans("unit"));
        JobConfig jobConfig = pipelineConfig.getFirstStageConfig().getJobs().get(0);
        jobConfig.setRunInstanceCount(2);
        jobConfig.addResourceConfig("blah");
        jobConfig.artifactTypeConfigs().add(new BuildArtifactConfig("src1", "dest1"));
        configHelper.addPipeline("go", "dev");

        scheduleHelper.schedule(pipelineConfig, BuildCause.createWithModifications(modifyOneFile(pipelineConfig), ""), DEFAULT_APPROVED_BY);

        List<JobPlan> jobPlans = jobInstanceService.orderedScheduledBuilds();

        assertThat(jobPlans.size()).isEqualTo(2);

        JobPlan job1 = jobPlans.get(0);
        assertThat(job1.getResources().size()).isEqualTo(1);
        assertThat(job1.getResources().get(0).getName()).isEqualTo("blah");
        assertThat(job1.getArtifactPlans().size()).isEqualTo(1);
        assertThat(job1.getArtifactPlans().get(0).getSrc()).isEqualTo("src1");

        JobPlan job2 = jobPlans.get(1);
        assertThat(job2.getResources().size()).isEqualTo(1);
        assertThat(job2.getResources().get(0).getName()).isEqualTo("blah");
        assertThat(job2.getArtifactPlans().size()).isEqualTo(1);
        assertThat(job2.getArtifactPlans().get(0).getSrc()).isEqualTo("src1");

        assertThat(job1.getResources().get(0).getId()).isNotEqualTo(job2.getResources().get(0).getId());
        assertThat(job1.getArtifactPlans().get(0).getId()).isNotEqualTo(job2.getArtifactPlans().get(0).getId());
    }

    @Test
    public void shouldSaveJobDetailsCorrectlyForEveryJobInARunOnAllAgentsJob() {
        PipelineConfig pipelineConfig = PipelineMother.withSingleStageWithMaterials("go", "dev", withBuildPlans("unit"));
        JobConfig jobConfig = pipelineConfig.getFirstStageConfig().getJobs().get(0);
        jobConfig.setRunOnAllAgents(true);
        jobConfig.addResourceConfig("blah");
        jobConfig.artifactTypeConfigs().add(new BuildArtifactConfig("src1", "dest1"));
        configHelper.addPipeline("go", "dev");

        DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("anyone", new Agents(localAgentWithResources("blah"), localAgentWithResources("blah")));

        scheduleHelper.schedule(pipelineConfig, BuildCause.createWithModifications(modifyOneFile(pipelineConfig), ""), DEFAULT_APPROVED_BY, schedulingContext);

        List<JobPlan> jobPlans = jobInstanceService.orderedScheduledBuilds();

        assertThat(jobPlans.size()).isEqualTo(2);

        JobPlan job1 = jobPlans.get(0);
        assertThat(job1.getResources().size()).isEqualTo(1);
        assertThat(job1.getResources().get(0).getName()).isEqualTo("blah");
        assertThat(job1.getArtifactPlans().size()).isEqualTo(1);
        assertThat(job1.getArtifactPlans().get(0).getSrc()).isEqualTo("src1");

        JobPlan job2 = jobPlans.get(1);
        assertThat(job2.getResources().size()).isEqualTo(1);
        assertThat(job2.getResources().get(0).getName()).isEqualTo("blah");
        assertThat(job2.getArtifactPlans().size()).isEqualTo(1);
        assertThat(job2.getArtifactPlans().get(0).getSrc()).isEqualTo("src1");

        assertThat(job1.getResources().get(0).getId()).isNotEqualTo(job2.getResources().get(0).getId());
        assertThat(job1.getArtifactPlans().get(0).getId()).isNotEqualTo(job2.getArtifactPlans().get(0).getId());
    }

    private Long stageWithId(final String pipelineName, final String stageName) {
        PipelineConfig pipelineConfig = PipelineMother.withSingleStageWithMaterials(pipelineName, stageName, BuildPlanMother.withBuildPlans("job-random"));
        Pipeline savedPipeline = instanceFactory.createPipelineInstance(pipelineConfig, modifySomeFiles(pipelineConfig), new DefaultSchedulingContext("cruise"), "md5-test", new TimeProvider());

        dbHelper.savePipelineWithStagesAndMaterials(savedPipeline);

        Stage savedStage = savedPipeline.getFirstStage();
        return savedStage.getId();
    }

    @Test
    public void shouldGetOldestAndLatestJobRunInstances() {
        List<Pipeline> pipelines = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
            pipelines.add(pipeline);
        }
        String stageName = pipelineFixture.devStage;
        String jobConfigName = PipelineWithTwoStages.JOB_FOR_DEV_STAGE;
        PipelineRunIdInfo runIdInfo = jobInstanceService.getOldestAndLatestJobInstanceId(Username.valueOf("user"), pipelineFixture.pipelineName, stageName, jobConfigName);

        assertThat(runIdInfo.getOldestRunId()).isEqualTo(pipelines.get(0).findStage(stageName).findJob(jobConfigName).getId());
        assertThat(runIdInfo.getLatestRunId()).isEqualTo(pipelines.get(3).findStage(stageName).findJob(jobConfigName).getId());
    }

    @Test
    public void shouldFetchLatestRecordsForJobHistory() {
        String stageName = pipelineFixture.devStage;
        String jobConfigName = PipelineWithTwoStages.JOB_FOR_DEV_STAGE;
        JobInstances jobInstances = new JobInstances();
        for (int i = 0; i < 4; i++) {
            Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
            jobInstances.add(pipeline.findStage(stageName).findJob(jobConfigName));
        }

        JobInstances history = jobInstanceService.getJobHistoryViaCursor(Username.valueOf("user"), pipelineFixture.pipelineName, stageName, jobConfigName, 0, 0, 3);

        assertThat(history.size()).isEqualTo(3);
        assertThat(history.get(0).getId()).isEqualTo(jobInstances.get(3).getId());
        assertThat(history.get(1).getId()).isEqualTo(jobInstances.get(2).getId());
        assertThat(history.get(2).getId()).isEqualTo(jobInstances.get(1).getId());
    }

    @Test
    public void shouldFetchRecordsForJobHistoryAfterTheSpecifiedCursor() { //older records
        String stageName = pipelineFixture.devStage;
        String jobConfigName = PipelineWithTwoStages.JOB_FOR_DEV_STAGE;
        JobInstances jobInstances = new JobInstances();
        for (int i = 0; i < 4; i++) {
            Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
            jobInstances.add(pipeline.findStage(stageName).findJob(jobConfigName));
        }

        JobInstances history = jobInstanceService.getJobHistoryViaCursor(Username.valueOf("user"), pipelineFixture.pipelineName, stageName, jobConfigName, jobInstances.get(2).getId(), 0, 3);

        assertThat(history.size()).isEqualTo(2);
        assertThat(history.get(0).getId()).isEqualTo(jobInstances.get(1).getId());
        assertThat(history.get(1).getId()).isEqualTo(jobInstances.get(0).getId());
    }

    @Test
    public void shouldFetchRecordsForJobHistoryBeforeTheSpecifiedCursor() { //newer records
        String stageName = pipelineFixture.devStage;
        String jobConfigName = PipelineWithTwoStages.JOB_FOR_DEV_STAGE;
        JobInstances jobInstances = new JobInstances();
        for (int i = 0; i < 4; i++) {
            Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
            jobInstances.add(pipeline.findStage(stageName).findJob(jobConfigName));
        }

        JobInstances history = jobInstanceService.getJobHistoryViaCursor(Username.valueOf("user"), pipelineFixture.pipelineName, stageName, jobConfigName, 0, jobInstances.get(2).getId(), 3);

        assertThat(history.size()).isEqualTo(1);
        assertThat(history.get(0).getId()).isEqualTo(jobInstances.get(3).getId());
    }
}
