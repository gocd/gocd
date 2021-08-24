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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.AgentAssignment;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginRegistry;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.JobStatusListener;
import com.thoughtworks.go.server.messaging.JobStatusMessage;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentMessage;
import com.thoughtworks.go.server.messaging.elasticagents.CreateAgentQueueHandler;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static com.thoughtworks.go.helper.ModificationsMother.forceBuild;
import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ScheduleServiceIntegrationTest {
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private PipelineDao pipelineDao;
    @Autowired
    private StageDao stageDao;
    @Autowired
    private JobInstanceDao jobInstanceDao;
    @Autowired
    private PipelineScheduler buildCauseProducer;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired
    private StageService stageService;
    @Autowired
    private JobInstanceService jobInstanceService;
    @Autowired
    private ScheduleHelper scheduleHelper;
    @Autowired
    private AgentAssignment agentAssignment;
    @Autowired
    private PipelineLockService pipelineLockService;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private GoCache goCache;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private PipelinePauseService pipelinePauseService;
    @Autowired
    private JobStatusListener jobStatusListener;
    @Autowired
    private BuildAssignmentService buildAssignmentService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private ElasticAgentPluginService elasticAgentPluginService;
    @Mock
    private EphemeralAutoRegisterKeyService ephemeralAutoRegisterKeyService;

    private PipelineConfig mingleConfig;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    private static final String STAGE_NAME = "dev";
    private GoConfigFileHelper configHelper;
    public Subversion repository;
    public static TestRepo testRepo;
    private PipelineWithTwoStages pipelineFixture;
    public static final String JOB_NAME = "unit";

    @BeforeAll
    public static void setupRepos(@TempDir Path tempDir) throws IOException {
        testRepo = new SvnTestRepo(tempDir);
    }

    @BeforeEach
    public void setup(@TempDir Path tempDir) throws Exception {
        configHelper = new GoConfigFileHelper();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();

        repository = new SvnCommand(null, testRepo.projectRepositoryUrl());
        mingleConfig = configHelper.addPipeline("mingle", STAGE_NAME, repository, JOB_NAME, "functional");
        goConfigService.forceNotifyListeners();
        agentAssignment.clear();
        goCache.clear();
    }

    @AfterEach
    public void teardown() throws Exception {
        pipelineFixture.onTearDown();
        dbHelper.onTearDown();
        FileUtils.deleteQuietly(goConfigService.artifactsDir());
        pipelineScheduleQueue.clear();
        agentAssignment.clear();
        configHelper.onTearDown();
    }

    @Test
    public void shouldNotSchedulePausedPipeline() {
        Pipeline pipeline = PipelineMother.schedule(mingleConfig, modifySomeFiles(mingleConfig));
        pipeline = dbHelper.savePipelineWithStagesAndMaterials(pipeline);
        pipelinePauseService.pause(pipeline.getName(), "", null);
        dbHelper.passStage(pipeline.getStages().first());
        Pipeline newPipeline = manualSchedule(CaseInsensitiveString.str(mingleConfig.name()));
        assertThat(newPipeline.getId(), is(pipeline.getId()));
    }

    @Test
    public void shouldScheduleStageThatWasAddedToConfigFileLater() throws Exception {
        configHelper.addPipeline("evolve", STAGE_NAME, repository, JOB_NAME);
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
        final Pipeline pipeline = PipelineMother.completedPipelineWithStagesAndBuilds(pipelineName, new ArrayList<>(Arrays.asList(firstStageName, secondStageName)), new ArrayList<>(Arrays.asList(JOB_NAME, "twist")));
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.save(pipeline.getBuildCause().getMaterialRevisions());
            }
        });
        pipelineService.save(pipeline);
        assertThat(pipelineLockService.isLocked(pipelineName), is(true));
        scheduleService.unlockIfNecessary(pipeline, pipeline.getStages().first());
        assertThat(pipelineLockService.isLocked(pipelineName), is(true));
        scheduleService.unlockIfNecessary(pipeline, pipeline.getStages().last());
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
        final Pipeline pipeline = PipelineMother.completedPipelineWithStagesAndBuilds(pipelineName, new ArrayList<>(Arrays.asList(firstStageName, secondStageName)), new ArrayList<>(Arrays.asList(JOB_NAME, "twist")));
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
    public void shouldNotBeAbleToRerunAStageWhichIsNotThereInTheConfigAnymore() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        assertThat(scheduleService.canRun(new PipelineIdentifier(pipeline.getName(), pipeline.getCounter(), pipeline.getLabel()), pipelineFixture.ftStage, "", true), is(true));
        configHelper.removeStage(pipelineFixture.pipelineName, pipelineFixture.ftStage);
        assertThat(scheduleService.canRun(new PipelineIdentifier(pipeline.getName(), pipeline.getCounter(), pipeline.getLabel()), pipelineFixture.ftStage, "", true), is(false));
    }

    @Test
    public void shouldCancelAJob() {
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
    public void shouldFailAJob() {
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
    public void shouldCancelAJobByIdentifier() {
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

    @Test //#6826
    public void shouldUseEnvVariableFromNewConfigWhenAPipelineIsRetriggered() {
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

        EnvironmentVariables variables = jobPlan.getVariables();
        assertThat(variables, hasItems(new EnvironmentVariable("K1", "V1"), new EnvironmentVariable("K2", "V2"), new EnvironmentVariable("K3", "V3")));


        Pipeline successfulPipeline = pass(pipeline);
        JobInstance jobInstance = successfulPipeline.getFirstStage().getFirstJob();
        jobStatusListener.onMessage(new JobStatusMessage(jobInstance.getIdentifier(), jobInstance.getState(), jobInstance.getAgentUuid()));

        jobPlan = jobInstanceDao.loadPlan(jobId);
        assertThat(jobPlan.getVariables().size(), is(0));

        configHelper.addEnvironmentVariableToPipeline(pipelineName, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K1_updated", "V1_updated"))));//add
        configHelper.addEnvironmentVariableToStage(pipelineName, stage, new EnvironmentVariablesConfig()); //delete
        configHelper.addEnvironmentVariableToJob(pipelineName, stage, job, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K3", "V3_updated")))); //edit

        pipeline = runAndPass(pipelineConfig, 2);
        jobId = pipeline.getFirstStage().getFirstJob().getId();
        jobPlan = jobInstanceDao.loadPlan(jobId);

        variables = jobPlan.getVariables();
        assertThat(variables, hasItems(new EnvironmentVariable("K1_updated", "V1_updated"), new EnvironmentVariable("K3", "V3_updated")));
    }

    @Test //#6815
    public void shouldUseEnvVariableFromNewConfigWhenAJobIsRerunAfterChangingTheConfig() {
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

        EnvironmentVariables variables = jobPlan.getVariables();
        assertThat(variables, hasItems(new EnvironmentVariable("K1", "V1"), new EnvironmentVariable("K2", "V2"), new EnvironmentVariable("K3", "V3")));

        Pipeline successfulPipeline = pass(pipeline);
        JobInstance jobInstance = successfulPipeline.getFirstStage().getFirstJob();
        jobStatusListener.onMessage(new JobStatusMessage(jobInstance.getIdentifier(), jobInstance.getState(), jobInstance.getAgentUuid()));

        jobPlan = jobInstanceDao.loadPlan(jobId);
        assertThat(jobPlan.getVariables().size(), is(0));

        configHelper.addEnvironmentVariableToPipeline(pipelineName, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K1_updated", "V1_updated"))));
        Stage rerunStage = scheduleService.rerunJobs(pipeline.getFirstStage(), Arrays.asList(job), new HttpOperationResult());
        assertThat(rerunStage.getFirstJob().getPlan().getVariables().size(), is(3));
        assertThat(rerunStage.getFirstJob().getPlan().getVariables(),
                hasItems(new EnvironmentVariable("K1_updated", "V1_updated"), new EnvironmentVariable("K2", "V2"), new EnvironmentVariable("K3", "V3")));
    }

    @Test //#6815
    public void shouldUseEnvVariableFromNewConfigWhenAStageIsRerunAfterChangingTheConfig() {
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

        EnvironmentVariables variables = jobPlan.getVariables();
        assertThat(variables, hasItems(new EnvironmentVariable("K1", "V1"), new EnvironmentVariable("K2", "V2"), new EnvironmentVariable("K3", "V3")));

        Pipeline successfulPipeline = pass(pipeline);
        JobInstance jobInstance = successfulPipeline.getFirstStage().getFirstJob();
        jobStatusListener.onMessage(new JobStatusMessage(jobInstance.getIdentifier(), jobInstance.getState(), jobInstance.getAgentUuid()));

        jobPlan = jobInstanceDao.loadPlan(jobId);
        assertThat(jobPlan.getVariables().size(), is(0));

        configHelper.addEnvironmentVariableToPipeline(pipelineName, new EnvironmentVariablesConfig(Arrays.asList(new EnvironmentVariableConfig("K1_updated", "V1_updated"))));
        Stage rerunStage = scheduleService.rerunStage(pipelineConfig.name().toString(), 1, stageName);
        assertThat(rerunStage.getFirstJob().getPlan().getVariables().size(), is(3));
        assertThat(rerunStage.getFirstJob().getPlan().getVariables(), hasItems(new EnvironmentVariable("K1_updated", "V1_updated"), new EnvironmentVariable("K2", "V2"), new EnvironmentVariable("K3", "V3")));
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

    // This could happen during race condition between rescheduleHungJobs and rescheduleAbandonedBuildIfNecessary.
    // The threads coud have run the queries and gotten the same jobid from the corresponding queries, but waiting to
    // acquire a lock on one of the synchronized objects in rescheduleJob
    @Test
    public void shouldNotRescheduleAJobWhichHasAlreadyBeenRescheduled() {
        PipelineConfig pipelineConfig = configHelper.addPipeline(PipelineConfigMother.createPipelineConfigWithStages(UUID.randomUUID().toString(), "s1"));
        Pipeline pipeline = dbHelper.schedulePipeline(pipelineConfig, forceBuild(pipelineConfig), new TimeProvider());
        scheduleService.rescheduleJob(pipeline.getFirstStage().getFirstJob());
        int scheduledJobsCountOriginal = jobInstanceDao.orderedScheduledBuilds().size();
        scheduleService.rescheduleJob(pipeline.getFirstStage().getFirstJob());
        int scheduledJobsCountAfterSecondReschedule = jobInstanceDao.orderedScheduledBuilds().size();
        assertThat(scheduledJobsCountOriginal, is(scheduledJobsCountAfterSecondReschedule));
    }

    @Test
    //This test is written to verify fix for github issue https://github.com/gocd/gocd/issues/6615
    public void shouldSendCreateAgentRequestToPluginForTheJobRequiringElasticAgentWhichHasBeenRescheduled() {
        String pluginId = "ecs";
        ClusterProfile clusterProfile = new ClusterProfile("cluster_profile", pluginId);
        ElasticProfile elasticAgentProfile = new ElasticProfile("elastic_agent_profile", "cluster_profile");
        String ephemeralAutoRegisterKey = "auto_registry_key";

        //Mock elastic agent extension
        ElasticAgentPluginRegistry elasticAgentPluginRegistry = mock(ElasticAgentPluginRegistry.class);
        when(elasticAgentPluginRegistry.shouldAssignWork(any(), any(), any(), any(), any(), any())).thenReturn(true);
        when(elasticAgentPluginRegistry.has(any())).thenReturn(true);
        when(ephemeralAutoRegisterKeyService.autoRegisterKey()).thenReturn(ephemeralAutoRegisterKey);
        elasticAgentPluginService.setElasticAgentPluginRegistry(elasticAgentPluginRegistry);
        elasticAgentPluginService.setEphemeralAutoRegisterKeyService(ephemeralAutoRegisterKeyService);

        //Mock CreateAgentQueueHandler to verify create agent call was sent to the plugin
        CreateAgentQueueHandler createAgentQueueHandler = mock(CreateAgentQueueHandler.class);
        elasticAgentPluginService.setCreateAgentQueue(createAgentQueueHandler);

        //add a cluster profile and elastic agent profile to the config.
        GoConfigDao goConfigDao = configHelper.getGoConfigDao();
        goConfigDao.loadForEditing().getElasticConfig().getClusterProfiles().add(clusterProfile);
        goConfigDao.loadForEditing().getElasticConfig().getProfiles().add(elasticAgentProfile);

        //create 2 elastic agents for ecs plugin
        Agent agent = AgentMother.elasticAgent();
        agent.setElasticAgentId("elastic-agent-id-1");
        agent.setElasticPluginId(pluginId);
        Agent agentConfig2 = AgentMother.elasticAgent();
        agentConfig2.setElasticAgentId("elastic-agent-id-2");
        agentConfig2.setElasticPluginId(pluginId);
        dbHelper.addAgent(agent);
        dbHelper.addAgent(agentConfig2);

        //define a job in the config requiring elastic agent
        PipelineConfig pipelineToBeAdded = PipelineConfigMother.createPipelineConfigWithStages(UUID.randomUUID().toString(), "s1");
        pipelineToBeAdded.first().getJobs().first().setElasticProfileId("elastic_agent_profile");
        PipelineConfig pipelineConfig = configHelper.addPipeline(pipelineToBeAdded);

        //trigger the pipeline
        Pipeline pipeline = dbHelper.schedulePipeline(pipelineConfig, forceBuild(pipelineConfig), "Bob", new TimeProvider(), Collections.singletonMap("elastic_agent_profile", elasticAgentProfile), Collections.singletonMap("cluster_profile", clusterProfile));

        buildAssignmentService.onTimer();

        //verify no agents are building
        AgentInstance agent1 = agentService.findAgent(agent.getUuid());
        assertFalse(agent1.isBuilding());
        AgentInstance agent2 = agentService.findAgent(agentConfig2.getUuid());
        assertFalse(agent2.isBuilding());

        //assign the current job to elastic agent 1
        Work work = buildAssignmentService.assignWorkToAgent(agent(agent));
        assertThat(work, instanceOf(BuildWork.class));
        assertTrue(agent1.isBuilding());

        //reschedule the job
        scheduleService.rescheduleJob(pipeline.getFirstStage().getFirstJob());

        //when the job is rescheduled, the current job instance is marked as rescheduled and a job with new build id is created for it.

        long existingRescheduledBuildId = pipeline.getFirstStage().getFirstJob().getId();
        //newly scheduled build will have the next counter.
        long newlyScheduledBuildId = pipeline.getFirstStage().getFirstJob().getId() + 1;

        //existing job instance, which is rescheduled
        JobInstance existingRescheduledInstance = jobInstanceService.buildById(existingRescheduledBuildId);

        //newly created job instance
        JobInstance newlyScheduledInstance = jobInstanceService.buildById(newlyScheduledBuildId);

        assertThat(existingRescheduledInstance.getState(), is(JobState.Rescheduled));
        assertThat(newlyScheduledInstance.getState(), is(JobState.Scheduled));

        //verify the newly created instance's job plan include elastic profile and cluster profile
        assertThat(jobInstanceDao.loadPlan(newlyScheduledBuildId).getClusterProfile(), is(clusterProfile));
        assertThat(jobInstanceDao.loadPlan(newlyScheduledBuildId).getElasticProfile(), is(elasticAgentProfile));

        JobPlan jobPlanOfRescheduledInstance = jobInstanceDao.loadPlan(existingRescheduledBuildId);

        //invoke jobStatusListener to simulate first agent reporting job completed scenario
        jobStatusListener.onMessage(new JobStatusMessage(jobPlanOfRescheduledInstance.getIdentifier(), JobState.Completed, jobPlanOfRescheduledInstance.getAgentUuid()));

        //verify the newly created instance's job plan include elastic profile and cluster profile even after the first agent reports job completion
        assertThat(jobInstanceDao.loadPlan(newlyScheduledBuildId).getClusterProfile(), is(clusterProfile));
        assertThat(jobInstanceDao.loadPlan(newlyScheduledBuildId).getElasticProfile(), is(elasticAgentProfile));

        elasticAgentPluginService.createAgentsFor(Collections.emptyList(), Collections.singletonList(jobInstanceDao.loadPlan(newlyScheduledBuildId)));

        //verify create agent request was sent to the plugin
        CreateAgentMessage message = new CreateAgentMessage(ephemeralAutoRegisterKey, null, elasticAgentProfile, clusterProfile, jobPlanOfRescheduledInstance.getIdentifier());
        verify(createAgentQueueHandler, times(1)).post(message, 110000L);
    }

    @Test
    public void shouldThrowErrorIfAllowOnlyOnSuccessIsSetAndPreviousStageFailed() {
        configHelper.configureStageAsManualApproval(pipelineFixture.pipelineName, pipelineFixture.ftStage, true);
        configHelper.lockPipeline(pipelineFixture.pipelineName);
        Pipeline pipeline = pipelineFixture.schedulePipeline();
        firstStageFailedAndSecondStageNotStarted(pipeline);
        HttpOperationResult result = new HttpOperationResult();
        scheduleService.rerunStage(pipeline.getName(), 1, pipelineFixture.ftStage, result);

        assertThat(result.isSuccess(), is(false));
        assertThat(result.message(), is("Cannot schedule ft as the previous stage dev has Failed!"));
        assertThat(result.httpCode(), is(409));

        ServerHealthState serverHealthState = result.getServerHealthState();
        assertThat(serverHealthState.isSuccess(), is(false));
        assertThat(serverHealthState.getDescription(), is("Cannot schedule ft as the previous stage dev has Failed!"));
        assertThat(serverHealthState.getMessage(), is("Cannot schedule ft as the previous stage dev has Failed!"));
    }

    private AgentIdentifier agent(Agent agent) {
        agentService.initialize();
        agentService.approve(agent.getUuid());
        return agentService.findAgent(agent.getUuid()).getAgentIdentifier();
    }

    private Pipeline runAndPass(PipelineConfig pipelineConfig, int counter) {
        BuildCause buildCause = ModificationsMother.modifySomeFiles(pipelineConfig);
        dbHelper.saveMaterials(buildCause.getMaterialRevisions());
        String pipelineName = pipelineConfig.name().toString();
        pipelineScheduleQueue.schedule(pipelineConfig.name(), buildCause);
        scheduleService.autoSchedulePipelinesFromRequestBuffer();
        Pipeline pipeline = pipelineDao.findPipelineByNameAndCounter(pipelineName, counter);
        pipeline = pipelineDao.loadAssociations(pipeline, pipelineName);
        return pipeline;
    }

    private Pipeline pass(Pipeline pipeline) {
        dbHelper.pass(pipeline);
        return pipeline;
    }

    private void autoSchedulePipelines(String... pipelines) throws Exception {
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(pipelines);
        scheduleService.autoSchedulePipelinesFromRequestBuffer();
    }

    private Pipeline manualSchedule(String pipelineName) {
        final HashMap<String, String> revisions = new HashMap<>();
        final HashMap<String, String> environmentVariables = new HashMap<>();
        final HashMap<String, String> secureEnvironmentVariables = new HashMap<>();
        buildCauseProducer.manualProduceBuildCauseAndSave(pipelineName, new Username(new CaseInsensitiveString("some user name")), new ScheduleOptions(revisions, environmentVariables, secureEnvironmentVariables), new ServerHealthStateOperationResult());
        scheduleService.autoSchedulePipelinesFromRequestBuffer();
        return pipelineService.mostRecentFullPipelineByName(pipelineName);
    }

    private void firstStageFailedAndSecondStageNotStarted(Pipeline pipeline) {
        pipelineService.save(pipeline);
        Stage stage = pipeline.getFirstStage();
        stage.building();
        stageService.updateResult(stage);
        dbHelper.completeAllJobs(stage, JobResult.Failed);
        stageService.updateResult(stage);
    }
}
