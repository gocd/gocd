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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.AgentAssignment;
import com.thoughtworks.go.domain.activity.JobStatusCache;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.BuildCauseProducerService;
import com.thoughtworks.go.server.scheduling.PipelineScheduledTopic;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import static com.thoughtworks.go.helper.ModificationsMother.forceBuild;
import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.util.ArrayUtil.asList;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ScheduleServiceIntegrationTest {
    @Autowired private GoConfigService goConfigService;
    @Autowired private ServerHealthService serverHealthService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private StageDao stageDao;
    @Autowired private JobInstanceDao jobInstanceDao;
    @Autowired private PipelineScheduler buildCauseProducer;
    @Autowired private PipelineService pipelineService;
    @Autowired private ScheduleService scheduleService;
    @Autowired private PipelineScheduledTopic pipelineScheduledTopic;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired private StageService stageService;
    @Autowired private SecurityService securityService;
    @Autowired private StageOrderService stageOrderService;
    @Autowired private SchedulingCheckerService schedulingChecker;
    @Autowired private CachedCurrentActivityService cachedCurrentActivityService;
    @Autowired private JobInstanceService jobInstanceService;
    @Autowired private ScheduleHelper scheduleHelper;
    @Autowired private AgentAssignment agentAssignment;
    @Autowired private EnvironmentConfigService environmentConfigService;
    @Autowired private PipelineLockService pipelineLockService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoCache goCache;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private TransactionSynchronizationManager synchronizationManager;
    @Autowired private JobStatusCache jobStatusCache;
    @Autowired private PipelinePauseService pipelinePauseService;
    @Autowired private BuildCauseProducerService buildCauseProducerService;

    private PipelineConfig mingleConfig;
	@Autowired private DatabaseAccessHelper dbHelper;
    private static final String STAGE_NAME = "dev";
    private GoConfigFileHelper configHelper;
    public Subversion repository;
    public static TestRepo testRepo;
    private PipelineConfig evolveConfig;
    private PipelineWithTwoStages pipelineFixture;
    public static final String JOB_NAME = "unit";

    @BeforeClass
    public static void setupRepos() throws IOException {
        testRepo = new SvnTestRepo("testSvnRepo");
    }

    @AfterClass
    public static void tearDownConfigFileLocation() {
        TestRepo.internalTearDown();
    }

    @Before
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();

        repository = new SvnCommand(null, testRepo.projectRepositoryUrl());
        mingleConfig = configHelper.addPipeline("mingle", STAGE_NAME, repository, JOB_NAME, "functional");
        goConfigService.forceNotifyListeners();
        agentAssignment.clear();
        goCache.clear();
    }

    @After
    public void teardown() throws Exception {
        pipelineFixture.onTearDown();
        dbHelper.onTearDown();
        FileUtil.deleteFolder(goConfigService.artifactsDir());
        pipelineScheduleQueue.clear();
        agentAssignment.clear();
        configHelper.onTearDown();
    }

    @Test
    public void shouldNotSchedulePausedPipeline() throws Exception {
        Pipeline pipeline = PipelineMother.schedule(mingleConfig, modifySomeFiles(mingleConfig));
        pipeline = dbHelper.savePipelineWithStagesAndMaterials(pipeline);
        pipelinePauseService.pause(pipeline.getName(), "", null);
        dbHelper.passStage(pipeline.getStages().first());
        Pipeline newPipeline = manualSchedule(CaseInsensitiveString.str(mingleConfig.name()));
        assertThat(newPipeline.getId(), is(pipeline.getId()));
    }

    @Test
    public void shouldScheduleStageThatWasAddedToConfigFileLater() throws Exception {
        evolveConfig = configHelper.addPipeline("evolve", STAGE_NAME, repository, JOB_NAME);
        autoSchedulePipelines("mingle", "evolve");
        PipelineConfig cruisePlan = configHelper.addPipeline("cruise", "test", repository);
        assertThat(goConfigService.stageConfigNamed("mingle", "dev"), is(notNullValue()));

        String dir = goConfigDao.load().server().artifactsDir();
        new File(dir).mkdirs();
        goConfigService.forceNotifyListeners();

        Stage cruise = stageDao.mostRecentWithBuilds(CaseInsensitiveString.str(cruisePlan.name()), cruisePlan.findBy(new CaseInsensitiveString("test")));
        assertEquals(NullStage.class, cruise.getClass());

        autoSchedulePipelines("cruise");
        cruise = stageDao.mostRecentWithBuilds(CaseInsensitiveString.str(cruisePlan.name()), cruisePlan.findBy(new CaseInsensitiveString("test")));
        for (JobInstance instance : cruise.getJobInstances()) {
            assertThat(instance.getState(), is(JobState.Scheduled));
        }
    }

    @Test
    public void shouldUnlockPipelineWhenLastStageCompletes() throws Exception {
        String pipelineName = "cruise";
        String firstStageName = JOB_NAME;
        String secondStageName = "twist";
        configHelper.addPipeline(pipelineName, firstStageName);
        configHelper.addStageToPipeline(pipelineName, "twist");
        configHelper.lockPipeline(pipelineName);
        final Pipeline pipeline = PipelineMother.completedPipelineWithStagesAndBuilds(pipelineName, asList(firstStageName, secondStageName), asList(JOB_NAME, "twist"));
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.save(pipeline.getBuildCause().getMaterialRevisions());
            }
        });
        pipelineService.save(pipeline);
        assertThat(pipelineLockService.isLocked(pipelineName), is(true));
        scheduleService.unlockIfLastStage(pipeline, pipeline.getStages().first());
        assertThat(pipelineLockService.isLocked(pipelineName), is(true));
        scheduleService.unlockIfLastStage(pipeline, pipeline.getStages().last());
        assertThat(pipelineLockService.isLocked(pipelineName), is(false));
    }

    @Test
    public void shouldUnlockPipelineAsAPartOfTriggeringRelevantStages() throws Exception {
        String pipelineName = "cruise";
        String firstStageName = JOB_NAME;
        String secondStageName = "twist";
        configHelper.addPipeline(pipelineName, firstStageName);
        configHelper.addStageToPipeline(pipelineName, "twist");
        configHelper.lockPipeline(pipelineName);
        final Pipeline pipeline = PipelineMother.completedPipelineWithStagesAndBuilds(pipelineName, asList(firstStageName, secondStageName), asList(JOB_NAME, "twist"));
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.save(pipeline.getBuildCause().getMaterialRevisions());
            }
        });

        pipelineService.save(pipeline);
        assertThat(pipelineLockService.isLocked(pipelineName), is(true));
        scheduleService.automaticallyTriggerRelevantStagesFollowingCompletionOf(pipeline.getStages().last());
        assertThat(pipelineLockService.isLocked(pipelineName), is(false));
    }

    @Test
    public void shouldNotBeAbleToRerunAStageWhichIsNotThereInTheConfigAnymore() throws Exception {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        assertThat(scheduleService.canRun(new PipelineIdentifier(pipeline.getName(), pipeline.getCounter(), pipeline.getLabel()), pipelineFixture.ftStage, "", true), is(true));
        configHelper.removeStage(pipelineFixture.pipelineName, pipelineFixture.ftStage);
        assertThat(scheduleService.canRun(new PipelineIdentifier(pipeline.getName(), pipeline.getCounter(), pipeline.getLabel()), pipelineFixture.ftStage, "", true), is(false));
    }

    @Test
    public void shouldCancelAJob() throws Exception {
        String pipelineName = "cruise";
        String firstStageName = JOB_NAME;

        PipelineConfig pipelineConfig = configHelper.addPipeline(pipelineName, firstStageName);

        Pipeline pipeline = PipelineMother.building(pipelineConfig);

        saveRev(pipeline);

        JobInstance firstJob = pipeline.findStage(firstStageName).getFirstJob();
        scheduleService.cancelJob(firstJob);
        JobInstance instance = jobInstanceService.buildById(firstJob.getId());
        assertThat(instance.getState(), is(JobState.Completed));
        assertThat(instance.getResult(), is(JobResult.Cancelled));
    }

    @Test
    public void shouldFailAJob() throws Exception {
        String pipelineName = "cruise";
        String firstStageName = JOB_NAME;

        PipelineConfig pipelineConfig = configHelper.addPipeline(pipelineName, firstStageName);

        Pipeline pipeline = PipelineMother.building(pipelineConfig);

        saveRev(pipeline);

        JobInstance firstJob = pipeline.findStage(firstStageName).getFirstJob();

        assertThat(stageDao.stageById(firstJob.getStageId()).stageState(), is(StageState.Building));
        assertThat(stageDao.stageById(firstJob.getStageId()).getResult(), is(StageResult.Unknown));

        scheduleService.failJob(firstJob);

        JobInstance instance = jobInstanceService.buildById(firstJob.getId());

        assertThat(instance.getState(), is(JobState.Completed));
        assertThat(instance.getResult(), is(JobResult.Failed));

        assertThat(stageDao.stageById(instance.getStageId()).stageState(), is(StageState.Failing));
        assertThat(stageDao.stageById(instance.getStageId()).getResult(), is(StageResult.Failed));
        assertThat(stageDao.stageById(instance.getStageId()).getCompletedByTransitionId(), is(nullValue()));

        JobInstance secondJob = pipeline.findStage(firstStageName).getJobInstances().get(1);

        scheduleService.failJob(secondJob);

        instance = jobInstanceService.buildByIdWithTransitions(secondJob.getId());

        assertThat(instance.getState(), is(JobState.Completed));
        assertThat(instance.getResult(), is(JobResult.Failed));

        assertThat(stageDao.stageById(instance.getStageId()).stageState(), is(StageState.Failed));
        assertThat(stageDao.stageById(instance.getStageId()).getResult(), is(StageResult.Failed));
        assertThat(stageDao.stageById(instance.getStageId()).getCompletedByTransitionId(), is(instance.getTransitions().latestTransitionId()));
    }

    private void saveRev(final Pipeline pipeline) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.save(pipeline.getBuildCause().getMaterialRevisions());
                pipelineService.save(pipeline);
            }
        });
    }

    @Test
    public void shouldCancelAJobByIdentifier() throws Exception {
        String pipelineName = "cruise";
        String firstStageName = JOB_NAME;

        PipelineConfig pipelineConfig = configHelper.addPipeline(pipelineName, firstStageName);

        Pipeline pipeline = PipelineMother.building(pipelineConfig);
        saveRev(pipeline);

        JobInstance firstJob = pipeline.findStage(firstStageName).getFirstJob();
        scheduleService.cancelJob(firstJob.getIdentifier());
        JobInstance instance = jobInstanceService.buildById(firstJob.getId());
        assertThat(instance.getState(), is(JobState.Completed));
        assertThat(instance.getResult(), is(JobResult.Cancelled));
    }

    @Test
    public void shouldSendMessageWhenScheduled() throws Exception {
        evolveConfig = configHelper.addPipeline("evolve", STAGE_NAME, repository, JOB_NAME);
        autoSchedulePipelines("mingle", "evolve");
        PipelineConfig cruisePlan = configHelper.addPipeline("cruise", "test", repository);
        assertThat(goConfigService.stageConfigNamed("mingle", "dev"), is(notNullValue()));

        Stage cruise = stageDao.mostRecentWithBuilds(CaseInsensitiveString.str(cruisePlan.name()), cruisePlan.findBy(new CaseInsensitiveString("test")));
        assertEquals(NullStage.class, cruise.getClass());

        String dir = goConfigDao.load().server().artifactsDir();
        new File(dir).mkdirs();

        TestGoMessageListener listener = new TestGoMessageListener();
        pipelineScheduledTopic.addListener(listener);

        autoSchedulePipelines("cruise");

        listener.waitForMessage(PipelineScheduledMessageMatchers.messageForPipelineNamed("cruise"));
    }

    @Test //#6826
    public void shouldUseEnvVariableFromNewConfigWhenAPipelineIsRetriggered() throws Exception {
        String pipelineName = "p1";
        String stage = "s1";
        String job = "j1";
        PipelineConfig pipelineConfig = configHelper.addPipeline(pipelineName, stage, job);
        configHelper.addEnvironmentVariableToPipeline(pipelineName, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K1", "V1"))));
        configHelper.addEnvironmentVariableToStage(pipelineName, stage, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K2", "V2"))));
        configHelper.addEnvironmentVariableToJob(pipelineName, stage, job, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K3", "V3"))));
        Pipeline pipeline = runAndPass(pipelineConfig, 1);
        long jobId = pipeline.getFirstStage().getFirstJob().getId();
        JobPlan jobPlan = jobInstanceDao.loadPlan(jobId);

        EnvironmentVariablesConfig variables = jobPlan.getVariables();
        assertThat(variables, hasItems(new EnvironmentVariableConfig("K1", "V1"), new EnvironmentVariableConfig("K2", "V2"), new EnvironmentVariableConfig("K3", "V3")));

        configHelper.addEnvironmentVariableToPipeline(pipelineName, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K1_updated", "V1_updated"))));//add
        configHelper.addEnvironmentVariableToStage(pipelineName, stage, new EnvironmentVariablesConfig()); //delete
        configHelper.addEnvironmentVariableToJob(pipelineName, stage, job, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K3", "V3_updated")))); //edit

        pipeline = runAndPass(pipelineConfig, 2);
        jobId = pipeline.getFirstStage().getFirstJob().getId();
        jobPlan = jobInstanceDao.loadPlan(jobId);

        variables = jobPlan.getVariables();
        assertThat(variables, hasItems(new EnvironmentVariableConfig("K1_updated", "V1_updated"), new EnvironmentVariableConfig("K3", "V3_updated")));
    }

    @Test //#6815
    public void shouldUseEnvVariableFromNewConfigWhenAJobIsRerunAfterChangingTheConfig() throws Exception {
        String pipelineName = "p1";
        String stage = "s1";
        String job = "j1";
        PipelineConfig pipelineConfig = configHelper.addPipeline(pipelineName, stage, job);
        configHelper.addEnvironmentVariableToPipeline(pipelineName, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K1", "V1"))));
        configHelper.addEnvironmentVariableToStage(pipelineName, stage, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K2", "V2"))));
        configHelper.addEnvironmentVariableToJob(pipelineName, stage, job, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K3", "V3"))));
        Pipeline pipeline = runAndPass(pipelineConfig, 1);
        long jobId = pipeline.getFirstStage().getFirstJob().getId();
        JobPlan jobPlan = jobInstanceDao.loadPlan(jobId);

        EnvironmentVariablesConfig variables = jobPlan.getVariables();
        assertThat(variables, hasItems(new EnvironmentVariableConfig("K1", "V1"), new EnvironmentVariableConfig("K2", "V2"), new EnvironmentVariableConfig("K3", "V3")));
        configHelper.addEnvironmentVariableToPipeline(pipelineName, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K1_updated", "V1_updated"))));
        Stage rerunStage = scheduleService.rerunJobs(pipeline.getFirstStage(), Arrays.asList(job), new HttpOperationResult());
        assertThat(rerunStage.getFirstJob().getPlan().getVariables().size(), is(3));
        assertThat(rerunStage.getFirstJob().getPlan().getVariables(),
                hasItems(new EnvironmentVariableConfig("K1_updated", "V1_updated"), new EnvironmentVariableConfig("K2", "V2"), new EnvironmentVariableConfig("K3", "V3")));
    }

    @Test //#6815
    public void shouldUseEnvVariableFromNewConfigWhenAStageIsRerunAfterChangingTheConfig() throws Exception {
        String pipelineName = "p1";
        String stageName = "s1";
        String jobName = "j1";
        PipelineConfig pipelineConfig = configHelper.addPipeline(pipelineName, stageName, jobName);
        configHelper.addEnvironmentVariableToPipeline(pipelineName, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K1", "V1"))));
        configHelper.addEnvironmentVariableToStage(pipelineName, stageName, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K2", "V2"))));
        configHelper.addEnvironmentVariableToJob(pipelineName, stageName, jobName, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K3", "V3"))));
        Pipeline pipeline = runAndPass(pipelineConfig, 1);
        long jobId = pipeline.getFirstStage().getFirstJob().getId();
        JobPlan jobPlan = jobInstanceDao.loadPlan(jobId);

        EnvironmentVariablesConfig variables = jobPlan.getVariables();
        assertThat(variables, hasItems(new EnvironmentVariableConfig("K1", "V1"), new EnvironmentVariableConfig("K2", "V2"), new EnvironmentVariableConfig("K3", "V3")));
        configHelper.addEnvironmentVariableToPipeline(pipelineName, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K1_updated", "V1_updated"))));
        Stage rerunStage = scheduleService.rerunStage(pipelineConfig.name().toString(), "1", stageName);
        assertThat(rerunStage.getFirstJob().getPlan().getVariables().size(), is(3));
        assertThat(rerunStage.getFirstJob().getPlan().getVariables(), hasItems(new EnvironmentVariableConfig("K1_updated", "V1_updated"), new EnvironmentVariableConfig("K2", "V2"), new EnvironmentVariableConfig("K3", "V3")));
    }

    @Test
    public void shouldTriggerNextStageOfPipelineEvenIfOneOfTheListenersFailWithAnError() throws Exception {
        String pipelineName = UUID.randomUUID().toString();
        String firstStage = "firstStage";
        String secondStage = "secondStage";
        PipelineConfig pipelineConfig = configHelper.addPipeline(PipelineConfigMother.createPipelineConfigWithStages(pipelineName, firstStage, secondStage));
        Pipeline pipeline = dbHelper.schedulePipeline(pipelineConfig, forceBuild(pipelineConfig), new TimeProvider());
        stageService.addStageStatusListener(new StageStatusListener() {
            @Override
            public void stageStatusChanged(Stage stage) {
                throw new LinkageError("some nasty linkage error");
            }
        });
        JobInstance job = pipeline.getFirstStage().getFirstJob();
        JobIdentifier jobIdentifier = job.getIdentifier();
        scheduleService.jobCompleting(jobIdentifier, JobResult.Passed, job.getAgentUuid());

        scheduleService.updateJobStatus(jobIdentifier, JobState.Completed);
        assertThat(stageService.findLatestStage(pipelineName, secondStage), is(notNullValue()));
    }

    private Pipeline runAndPass(PipelineConfig pipelineConfig, int counter) {
        BuildCause buildCause = ModificationsMother.modifySomeFiles(pipelineConfig);
        dbHelper.saveMaterials(buildCause.getMaterialRevisions());
        String pipelineName = pipelineConfig.name().toString();
        pipelineScheduleQueue.schedule(pipelineName, buildCause);
        scheduleService.autoSchedulePipelinesFromRequestBuffer();
        Pipeline pipeline = pipelineDao.findPipelineByNameAndCounter(pipelineName, counter);
        pipeline = pipelineDao.loadAssociations(pipeline, pipelineName);
        dbHelper.pass(pipeline);
        return pipeline;
    }

    private void autoSchedulePipelines(String... pipelines) throws Exception {
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(pipelines);
        scheduleService.autoSchedulePipelinesFromRequestBuffer();
    }

    private Pipeline manualSchedule(String pipelineName) {
        final HashMap<String, String> revisions = new HashMap<String, String>();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        final HashMap<String, String> secureEnvironmentVariables = new HashMap<String, String>();
        buildCauseProducer.manualProduceBuildCauseAndSave(pipelineName, new Username(new CaseInsensitiveString("some user name")), new ScheduleOptions(revisions, environmentVariables, secureEnvironmentVariables), new ServerHealthStateOperationResult());
        scheduleService.autoSchedulePipelinesFromRequestBuffer();
        return pipelineService.mostRecentFullPipelineByName(pipelineName);
    }
}
