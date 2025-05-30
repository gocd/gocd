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
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.AgentAssignment;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.FetchArtifactBuilder;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.DeniedAgentWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.ServerMaintenanceMode;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.DependencyMaterialUpdateNotifier;
import com.thoughtworks.go.server.messaging.JobStatusTopic;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.service.builders.BuilderFactory;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.*;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static com.thoughtworks.go.helper.MaterialConfigsMother.hg;
import static com.thoughtworks.go.helper.ModificationsMother.modifyNoFiles;
import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;
import static com.thoughtworks.go.util.TestUtils.sleepQuietly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class BuildAssignmentServiceIntegrationTest {
    @Autowired
    private BuildAssignmentService buildAssignmentService;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private PipelineDao pipelineDao;
    @Autowired
    private JobInstanceDao jobInstanceDao;
    @Autowired
    private AgentService agentService;
    @Autowired
    private AgentAssignment agentAssignment;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private ScheduleHelper scheduleHelper;
    @Autowired
    private GoCache goCache;
    @Autowired
    private StageDao stageDao;
    @Autowired
    private JobInstanceService jobInstanceService;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private EnvironmentConfigService environmentConfigService;
    @Autowired
    private SystemEnvironment systemEnvironment;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private BuilderFactory builderFactory;
    @Autowired
    private InstanceFactory instanceFactory;
    @Autowired
    private PipelineConfigService pipelineConfigService;
    @Autowired
    private ElasticAgentPluginService elasticAgentPluginService;
    @Autowired
    private DependencyMaterialUpdateNotifier notifier;
    @Autowired
    private MaintenanceModeService maintenanceModeService;
    @Autowired
    private SecretParamResolver secretParamResolver;
    @Autowired
    private ConsoleService consoleService;
    @Autowired
    private JobStatusTopic jobStatusTopic;
    @Autowired
    private EntityHashingService entityHashingService;

    private PipelineConfig evolveConfig;
    private static final String STAGE_NAME = "dev";
    private GoConfigFileHelper configHelper;
    private ScheduleTestUtil u;

    public Subversion repository;
    public static TestRepo testRepo;
    private PipelineWithTwoStages fixture;
    private String md5 = "md5-test";
    private Username loserUser = new Username(new CaseInsensitiveString("loser"));
    private ConfigCache configCache;
    private ConfigElementImplementationRegistry registry;

    @BeforeAll
    public static void setupRepos(@TempDir Path tempDir) throws IOException {
        testRepo = new SvnTestRepo(tempDir);
    }

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        maintenanceModeService.update(new ServerMaintenanceMode(false, "admin", new Date()));
        configCache = new ConfigCache();
        registry = ConfigElementImplementationRegistryMother.withNoPlugins();
        configHelper = new GoConfigFileHelper().usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        dbHelper.onSetUp();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();

        repository = new SvnCommand(null, testRepo.projectRepositoryUrl());
        evolveConfig = configHelper.addPipeline("evolve", STAGE_NAME, repository, "unit");
        configHelper.addPipeline("anotherPipeline", STAGE_NAME, repository, "anotherTest");
        configHelper.addPipeline("thirdPipeline", STAGE_NAME, repository, "yetAnotherTest");
        goConfigService.forceNotifyListeners();
        goCache.clear();
        u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);

        notifier.disableUpdates();
    }

    @AfterEach
    public void teardown() throws Exception {
        notifier.enableUpdates();
        goCache.clear();
        agentService.clearAll();
        fixture.onTearDown();
        dbHelper.onTearDown();
        configHelper.onTearDown();
        FileUtils.deleteQuietly(goConfigService.artifactsDir());
        agentAssignment.clear();
    }

    @Test
    public void shouldRescheduleAbandonedBuild() {
        AgentIdentifier instance = agent(AgentMother.localAgent());
        Pipeline pipeline = instanceFactory.createPipelineInstance(evolveConfig, modifyNoFiles(evolveConfig), new DefaultSchedulingContext(
                DEFAULT_APPROVED_BY), md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(pipeline);
        buildAssignmentService.onConfigChange(goConfigService.getCurrentConfig());
        buildAssignmentService.onTimer();
        buildAssignmentService.assignWorkToAgent(instance);
        long firstAssignedBuildId = buildOf(pipeline).getId();

        //somehow agent abandoned its original build...

        buildAssignmentService.assignWorkToAgent(instance);
        JobInstance reloaded = jobInstanceDao.buildByIdWithTransitions(firstAssignedBuildId);
        assertThat(reloaded.getState()).isEqualTo(JobState.Rescheduled);
        assertThat(reloaded.isIgnored()).isTrue();
    }

    @Test
    public void shouldNotAssignWorkToDeniedAgent() {
        Agent deniedAgentConfig = AgentMother.localAgent();
        deniedAgentConfig.disable();

        Work assignedWork = buildAssignmentService.assignWorkToAgent(agent(deniedAgentConfig));
        assertThat(assignedWork).isInstanceOf(DeniedAgentWork.class);
    }

    @Test
    public void shouldNotAssignWorkWhenPipelineScheduledWithStaleMaterials() {
        AgentIdentifier instance = agent(AgentMother.localAgent());
        Pipeline pipeline = instanceFactory.createPipelineInstance(evolveConfig, modifyNoFiles(evolveConfig), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(pipeline);
        evolveConfig.setMaterialConfigs(new MaterialConfigs(hg("foo", null)));
        configHelper.removePipeline(CaseInsensitiveString.str(evolveConfig.name()));
        configHelper.addPipeline(evolveConfig);
        buildAssignmentService.onConfigChange(goConfigService.getCurrentConfig());
        JobInstance job = buildOf(pipeline);
        jobInstanceDao.updateStateAndResult(job);
        assertThat(buildAssignmentService.assignWorkToAgent(instance)).isEqualTo(BuildAssignmentService.NO_WORK);
    }

    @Test
    public void shouldNotAssignCancelledJob() {
        AgentIdentifier instance = agent(AgentMother.localAgent());
        Pipeline pipeline = instanceFactory.createPipelineInstance(evolveConfig, modifyNoFiles(evolveConfig), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(pipeline);
        buildAssignmentService.onConfigChange(goConfigService.getCurrentConfig());
        JobInstance job = buildOf(pipeline);
        job.cancel();
        jobInstanceDao.updateStateAndResult(job);

        assertThat(buildAssignmentService.assignWorkToAgent(instance)).isEqualTo(BuildAssignmentService.NO_WORK);
    }

    @Test
    public void shouldUpdateNumberOfActiveRemoteAgentsAfterAssigned() {
        Agent agent = AgentMother.remoteAgent();
        agentService.saveOrUpdate(agent);

        fixture.createPipelineWithFirstStageScheduled();
        buildAssignmentService.onTimer();

        AgentInstance agentInstance = agentService.findAgent(agent.getUuid());
        assertFalse(agentInstance.isBuilding());

        Work work = buildAssignmentService.assignWorkToAgent(agent(agent));
        assertThat(work).isInstanceOf(BuildWork.class);
        assertTrue(agentInstance.isBuilding());
    }

    @Test
    public void shouldCancelOutOfDateBuilds() {
        fixture.createPipelineWithFirstStageScheduled();
        buildAssignmentService.onTimer();
        configHelper.removeStage(fixture.pipelineName, fixture.devStage);

        buildAssignmentService.onConfigChange(goConfigService.getCurrentConfig());

        Pipeline pipeline = pipelineDao.mostRecentPipeline(fixture.pipelineName);
        JobInstance job = pipeline.getFirstStage().getJobInstances().first();
        assertThat(job.getState()).isEqualTo(JobState.Completed);
        assertThat(job.getResult()).isEqualTo(JobResult.Cancelled);
    }

    @Test
    public void shouldCancelBuildsForDeletedStagesWhenPipelineConfigChanges() {
        buildAssignmentService.initialize();

        fixture.createPipelineWithFirstStageScheduled();
        buildAssignmentService.onTimer();

        PipelineConfig originalPipelineConfig = configHelper.getCachedGoConfig().currentConfig().getPipelineConfigByName(new CaseInsensitiveString(fixture.pipelineName));
        PipelineConfig pipelineConfig = configHelper.deepClone(originalPipelineConfig);
        String md5 = entityHashingService.hashForEntity(originalPipelineConfig, fixture.groupName);
        StageConfig devStage = pipelineConfig.findBy(new CaseInsensitiveString(fixture.devStage));
        pipelineConfig.remove(devStage);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelineConfigService.updatePipelineConfig(loserUser, pipelineConfig, fixture.groupName, md5, result);

        Pipeline pipeline = pipelineDao.mostRecentPipeline(fixture.pipelineName);
        JobInstance job = pipeline.getFirstStage().getJobInstances().first();
        assertThat(job.getState()).isEqualTo(JobState.Completed);
        assertThat(job.getResult()).isEqualTo(JobResult.Cancelled);

        buildAssignmentService.onTimer();
        List<JobPlan> latestJobPlans = buildAssignmentService.jobPlans();
        assertThat(latestJobPlans.size()).isEqualTo(0);
    }

    @Test
    public void shouldCancelBuildsForDeletedJobsWhenPipelineConfigChanges(@TempDir Path tempDir) throws Exception {
        buildAssignmentService.initialize();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir).usingTwoJobs();
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        fixture.createPipelineWithFirstStageScheduled();

        buildAssignmentService.onTimer();

        PipelineConfig originalPipelineConfig = configHelper.getCachedGoConfig().currentConfig().getPipelineConfigByName(new CaseInsensitiveString(fixture.pipelineName));
        PipelineConfig pipelineConfig = configHelper.deepClone(originalPipelineConfig);
        String xml = new MagicalGoConfigXmlWriter(configCache, registry).toXmlPartial(pipelineConfig);
        String md5 = entityHashingService.hashForEntity(originalPipelineConfig, fixture.groupName);
        StageConfig devStage = pipelineConfig.findBy(new CaseInsensitiveString(fixture.devStage));
        devStage.getJobs().remove(devStage.jobConfigByConfigName(new CaseInsensitiveString(PipelineWithTwoStages.JOB_FOR_DEV_STAGE)));
        pipelineConfigService.updatePipelineConfig(loserUser, pipelineConfig, fixture.groupName, md5, new HttpLocalizedOperationResult());

        Pipeline pipeline = pipelineDao.mostRecentPipeline(fixture.pipelineName);
        JobInstance deletedJob = pipeline.getFirstStage().getJobInstances().getByName(PipelineWithTwoStages.JOB_FOR_DEV_STAGE);
        assertThat(deletedJob.getState()).isEqualTo(JobState.Completed);
        assertThat(deletedJob.getResult()).isEqualTo(JobResult.Cancelled);
        JobInstance retainedJob = pipeline.getFirstStage().getJobInstances().getByName(PipelineWithTwoStages.DEV_STAGE_SECOND_JOB);
        assertThat(retainedJob.getState()).isEqualTo(JobState.Scheduled);
        assertThat(retainedJob.getResult()).isEqualTo(JobResult.Unknown);

        buildAssignmentService.onTimer();
        List<JobPlan> latestJobPlans = buildAssignmentService.jobPlans();
        assertThat(latestJobPlans.size()).isEqualTo(1);
        assertThat(latestJobPlans.get(0).getName()).isEqualTo(retainedJob.getName());
    }

    @Test
    public void shouldCancelBuildsForAllJobsWhenPipelineIsDeleted(@TempDir Path tempDir) throws Exception {
        buildAssignmentService.initialize();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir).usingTwoJobs();
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        fixture.createPipelineWithFirstStageScheduled();

        buildAssignmentService.onTimer();

        PipelineConfig pipelineConfig = configHelper.deepClone(configHelper.getCachedGoConfig().currentConfig().getPipelineConfigByName(new CaseInsensitiveString(fixture.pipelineName)));

        pipelineConfigService.deletePipelineConfig(loserUser, pipelineConfig, new HttpLocalizedOperationResult());

        Pipeline pipeline = pipelineDao.mostRecentPipeline(fixture.pipelineName);
        JobInstance job1 = pipeline.getFirstStage().getJobInstances().getByName(PipelineWithTwoStages.JOB_FOR_DEV_STAGE);
        JobInstance job2 = pipeline.getFirstStage().getJobInstances().getByName(PipelineWithTwoStages.DEV_STAGE_SECOND_JOB);

        assertThat(job1.getState()).isEqualTo(JobState.Completed);
        assertThat(job1.getResult()).isEqualTo(JobResult.Cancelled);
        assertThat(job2.getState()).isEqualTo(JobState.Completed);
        assertThat(job2.getResult()).isEqualTo(JobResult.Cancelled);

        buildAssignmentService.onTimer();
        List<JobPlan> latestJobPlans = buildAssignmentService.jobPlans();
        assertThat(latestJobPlans.size()).isEqualTo(0);
    }

    @Test
    public void shouldCancelBuildBelongingToNonExistentPipeline() {
        fixture.createPipelineWithFirstStageScheduled();
        buildAssignmentService.onTimer();

        configHelper.removePipeline(fixture.pipelineName);

        Agent agent = AgentMother.localAgent();
        agent.setResources("some-other-resource");

        assertThat(buildAssignmentService.assignWorkToAgent(agent(agent))).isEqualTo((BuildAssignmentService.NO_WORK));
        Pipeline pipeline = pipelineDao.mostRecentPipeline(fixture.pipelineName);
        JobInstance job = pipeline.getFirstStage().getJobInstances().first();
        assertThat(job.getState()).isEqualTo(JobState.Completed);
        assertThat(job.getResult()).isEqualTo(JobResult.Cancelled);
        Stage stage = stageDao.findStageWithIdentifier(job.getIdentifier().getStageIdentifier());
        assertThat(stage.getState()).isEqualTo(StageState.Cancelled);
        assertThat(stage.getResult()).isEqualTo(StageResult.Cancelled);
    }

    @Test
    public void shouldNotReloadScheduledJobPlansWhenAgentWorkAssignmentIsInProgress() throws Exception {
        fixture.createPipelineWithFirstStageScheduled();
        Pipeline pipeline = pipelineDao.mostRecentPipeline(fixture.pipelineName);
        JobInstance job = pipeline.getFirstStage().getJobInstances().first();

        final JobInstanceService mockJobInstanceService = mock(JobInstanceService.class);

        final Pipeline pipeline1 = pipeline;
        final Semaphore sem = new Semaphore(1);
        sem.acquire();
        when(mockJobInstanceService.orderedScheduledBuilds()).thenReturn(jobInstanceService.orderedScheduledBuilds());
        when(mockJobInstanceService.buildByIdWithTransitions(job.getId())).thenReturn(jobInstanceService.buildByIdWithTransitions(job.getId()));

        ScheduledPipelineLoader scheduledPipelineLoader = new ScheduledPipelineLoader(null, null, null, null, null, null, null) {
            @Override
            public Pipeline pipelineWithPasswordAwareBuildCauseByBuildId(long buildId) {
                sem.release();
                sleepQuietly(1000);
                verify(mockJobInstanceService, times(1)).orderedScheduledBuilds();
                return pipeline1;
            }
        };

        final BuildAssignmentService buildAssignmentServiceUnderTest = new BuildAssignmentService(goConfigService, mockJobInstanceService, scheduleService,
                agentService, environmentConfigService, transactionTemplate, scheduledPipelineLoader, pipelineService, builderFactory,
                maintenanceModeService, elasticAgentPluginService, systemEnvironment, secretParamResolver, jobStatusTopic, consoleService);

        final Throwable[] fromThread = new Throwable[1];
        buildAssignmentServiceUnderTest.onTimer();

        Thread assigner = new Thread(() -> {
            try {
                final Agent agent = AgentMother.localAgentWithResources("some-other-resource");

                buildAssignmentServiceUnderTest.assignWorkToAgent(agent(agent));
            } catch (Throwable e) {
                e.printStackTrace();
                fromThread[0] = e;
            }
        }, "assignmentThread");
        assigner.start();

        sem.acquire();
        buildAssignmentServiceUnderTest.onTimer();

        assigner.join();
        assertThat(fromThread[0]).isNull();
    }

    @Test
    public void shouldCancelBuildBelongingToNonExistentPipelineWhenCreatingWork() {
        fixture.createPipelineWithFirstStageScheduled();
        Pipeline pipeline = pipelineDao.mostRecentPipeline(fixture.pipelineName);

        ScheduledPipelineLoader scheduledPipelineLoader = mock(ScheduledPipelineLoader.class);
        when(scheduledPipelineLoader.pipelineWithPasswordAwareBuildCauseByBuildId(pipeline.getFirstStage().getJobInstances().first().getId())).thenThrow(
                new RecordNotFoundException(EntityType.Pipeline, fixture.pipelineName));

        GoConfigService mockGoConfigService = mock(GoConfigService.class);
        CruiseConfig config = configHelper.currentConfig();
        configHelper.removePipeline(fixture.pipelineName, config);
        when(mockGoConfigService.getCurrentConfig()).thenReturn(config);

        buildAssignmentService = new BuildAssignmentService(mockGoConfigService, jobInstanceService, scheduleService, agentService, environmentConfigService,
                transactionTemplate, scheduledPipelineLoader, pipelineService, builderFactory, maintenanceModeService, elasticAgentPluginService,
                systemEnvironment, secretParamResolver, jobStatusTopic, consoleService);
        buildAssignmentService.onTimer();

        Agent agent = AgentMother.localAgent();
        agent.setResources("some-other-resource");

        try {
            buildAssignmentService.assignWorkToAgent(agent(agent));
            fail("should have thrown RecordNotFoundException");
        } catch (RecordNotFoundException e) {
            // ok
        }

        pipeline = pipelineDao.mostRecentPipeline(fixture.pipelineName);

        JobInstance job = pipeline.getFirstStage().getJobInstances().first();
        assertThat(job.getState()).isEqualTo(JobState.Completed);
        assertThat(job.getResult()).isEqualTo(JobResult.Cancelled);
        Stage stage = stageDao.findStageWithIdentifier(job.getIdentifier().getStageIdentifier());
        assertThat(stage.getState()).isEqualTo(StageState.Cancelled);
        assertThat(stage.getResult()).isEqualTo(StageResult.Cancelled);
    }

    @Test
    public void shouldBeAbleToSerializeAndDeserializeBuildWork() throws Exception {
        Pipeline pipeline1 = instanceFactory.createPipelineInstance(evolveConfig, modifySomeFiles(evolveConfig), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(pipeline1);

        buildAssignmentService.onTimer();
        BuildWork work = (BuildWork) buildAssignmentService.assignWorkToAgent(agent(AgentMother.localAgent()));

        BuildWork deserialized = SerializationTester.objectSerializeAndDeserialize(work);

        assertThat(deserialized.getAssignment().materialRevisions()).isEqualTo(work.getAssignment().materialRevisions());

        assertThat(deserialized.getAssignment()).isEqualTo(work.getAssignment());
        assertThat(deserialized).isEqualTo(work);
    }

    @Test
    public void shouldCreateWorkWithFetchMaterialsFlagFromStageConfig() {
        evolveConfig.getFirstStageConfig().setFetchMaterials(true);
        Pipeline pipeline1 = instanceFactory.createPipelineInstance(evolveConfig, modifySomeFiles(evolveConfig), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(pipeline1);

        buildAssignmentService.onTimer();
        BuildWork work = (BuildWork) buildAssignmentService.assignWorkToAgent(agent(AgentMother.localAgent()));

        assertThat(work.getAssignment().shouldFetchMaterials()).isTrue();
    }

    /**
     * (uppest/2/uppest-stage/1)
     * |------------------> upper-peer -------
     * |  ...................................|...............................................
     * |  .                                  |                                              .
     * [ uppest-stage ............................|......................    {bar.zip uppest/upper-peer/downer}
     * V                     .
     * uppest  uppest-stage-2  ------> upper ------> downer ------> downest {foo.zip uppest/upper/downer}
     * (uppest/1/uppest-stage-2/1)
     * uppest-stage-3 ]
     * <p/>
     * .... :: fetch artifact call
     * ---> :: material dependency
     */
    @Test
    public void shouldCreateWork_withAncestorFetchArtifactCalls_resolvedToRelevantStage() {
        configHelper.addPipeline("uppest", "uppest-stage");
        configHelper.addStageToPipeline("uppest", "uppest-stage-2");
        PipelineConfig uppest = configHelper.addStageToPipeline("uppest", "uppest-stage-3");

        configHelper.addPipeline("upper", "upper-stage");
        DependencyMaterial upper_sMaterial = new DependencyMaterial(new CaseInsensitiveString("uppest"), new CaseInsensitiveString("uppest-stage-2"));
        PipelineConfig upper = configHelper.setMaterialConfigForPipeline("upper", upper_sMaterial.config());

        configHelper.addPipeline("upper-peer", "upper-peer-stage");
        DependencyMaterial upperPeer_sMaterial = new DependencyMaterial(new CaseInsensitiveString("uppest"), new CaseInsensitiveString("uppest-stage"));
        PipelineConfig upperPeer = configHelper.setMaterialConfigForPipeline("upper-peer", upperPeer_sMaterial.config());

        configHelper.addPipeline("downer", "downer-stage");
        DependencyMaterial downer_sUpperMaterial = new DependencyMaterial(new CaseInsensitiveString("upper"), new CaseInsensitiveString("upper-stage"));
        configHelper.setMaterialConfigForPipeline("downer", downer_sUpperMaterial.config());
        DependencyMaterial downer_sUpperPeerMaterial = new DependencyMaterial(new CaseInsensitiveString("upper-peer"), new CaseInsensitiveString("upper-peer-stage"));
        PipelineConfig downer = configHelper.addMaterialToPipeline("downer", downer_sUpperPeerMaterial.config());

        configHelper.addPipeline("downest", "downest-stage");
        DependencyMaterial downest_sMaterial = new DependencyMaterial(new CaseInsensitiveString("downer"), new CaseInsensitiveString("downer-stage"));
        configHelper.setMaterialConfigForPipeline("downest", downest_sMaterial.config());
        Tasks allFetchTasks = new Tasks();
        allFetchTasks.add(new FetchTask(new CaseInsensitiveString("uppest/upper/downer"), new CaseInsensitiveString("uppest-stage"), new CaseInsensitiveString("unit"), "foo.zip", "bar"));
        allFetchTasks.add(new FetchTask(new CaseInsensitiveString("uppest/upper-peer/downer"), new CaseInsensitiveString("uppest-stage"), new CaseInsensitiveString("unit"), "bar.zip", "baz"));
        configHelper.replaceAllJobsInStage("downest", "downest-stage", new JobConfig(new CaseInsensitiveString("fetcher"), new ResourceConfigs("fetcher"), new ArtifactTypeConfigs(), allFetchTasks));
        PipelineConfig downest = goConfigService.getCurrentConfig().pipelineConfigByName(new CaseInsensitiveString("downest"));

        DefaultSchedulingContext defaultSchedulingCtx = new DefaultSchedulingContext(DEFAULT_APPROVED_BY);
        Pipeline uppestInstanceForUpper = instanceFactory.createPipelineInstance(uppest, modifySomeFiles(uppest), defaultSchedulingCtx, md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(uppestInstanceForUpper);
        dbHelper.passStage(uppestInstanceForUpper.findStage("uppest-stage"));
        Stage upper_sMaterialStage = dbHelper.scheduleStage(uppestInstanceForUpper, uppest.getStage(new CaseInsensitiveString("uppest-stage-2")));
        dbHelper.passStage(upper_sMaterialStage);

        Pipeline uppestInstanceForUpperPeer = instanceFactory.createPipelineInstance(uppest, modifySomeFiles(uppest), new DefaultSchedulingContext("super-hero"), md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(uppestInstanceForUpperPeer);
        Stage upperPeer_sMaterialStage = uppestInstanceForUpperPeer.findStage("uppest-stage");
        dbHelper.passStage(upperPeer_sMaterialStage);

        Pipeline upperInstance = instanceFactory.createPipelineInstance(upper, buildCauseForDependency(upper_sMaterial, upper_sMaterialStage), defaultSchedulingCtx, md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(upperInstance);
        Stage downer_sUpperMaterialStage = upperInstance.findStage("upper-stage");
        dbHelper.passStage(downer_sUpperMaterialStage);

        Pipeline upperPeerInstance = instanceFactory.createPipelineInstance(upperPeer, buildCauseForDependency(upperPeer_sMaterial, upperPeer_sMaterialStage), defaultSchedulingCtx, md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(upperPeerInstance);
        Stage downer_sUpperPeerMaterialStage = upperPeerInstance.findStage("upper-peer-stage");
        dbHelper.passStage(downer_sUpperPeerMaterialStage);

        MaterialRevisions downer_sMaterialRevisions = new MaterialRevisions(
                materialRevisionForDownstream(downer_sUpperMaterial, downer_sUpperMaterialStage),
                materialRevisionForDownstream(downer_sUpperPeerMaterial, downer_sUpperPeerMaterialStage));

        Pipeline downerInstance = instanceFactory.createPipelineInstance(downer, BuildCause.createManualForced(downer_sMaterialRevisions, loserUser), defaultSchedulingCtx, md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(downerInstance);
        Stage downest_sMaterialStage = downerInstance.findStage("downer-stage");
        dbHelper.passStage(downest_sMaterialStage);

        Pipeline downestInstance = instanceFactory.createPipelineInstance(downest, buildCauseForDependency(downest_sMaterial, downest_sMaterialStage), defaultSchedulingCtx, md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(downestInstance);

        buildAssignmentService.onTimer();
        Agent agent = AgentMother.localAgent();
        agent.setResources("fetcher");
        BuildWork work = (BuildWork) buildAssignmentService.assignWorkToAgent(agent(agent));

        List<Builder> builders = work.getAssignment().getBuilders();
        FetchArtifactBuilder fooZipFetch = (FetchArtifactBuilder) builders.get(0);
        assertThat(fooZipFetch.artifactLocator()).isEqualTo("uppest/1/uppest-stage/latest/unit/foo.zip");
        FetchArtifactBuilder barZipFetch = (FetchArtifactBuilder) builders.get(1);
        assertThat(barZipFetch.artifactLocator()).isEqualTo("uppest/2/uppest-stage/1/unit/bar.zip");
    }

    private BuildCause buildCauseForDependency(DependencyMaterial material, Stage upstreamStage) {
        return BuildCause.createManualForced(new MaterialRevisions(materialRevisionForDownstream(material, upstreamStage)), loserUser);
    }

    private MaterialRevision materialRevisionForDownstream(DependencyMaterial material, Stage upstreamStage) {
        StageIdentifier identifier = upstreamStage.getIdentifier();
        String rev = identifier.getStageLocator();
        String pipelineLabel = identifier.getPipelineLabel();
        return new MaterialRevision(material, new Modification(new Date(), rev, pipelineLabel, upstreamStage.getPipelineId()));
    }


    private AgentIdentifier agent(Agent agent) {
        agentService.saveOrUpdate(agent);
        return agentService.findAgent(agent.getUuid()).getAgentIdentifier();
    }

    @Test
    public void shouldNotScheduleIfAgentDoesNotHaveResources() {
        JobConfig plan = evolveConfig.findBy(new CaseInsensitiveString(STAGE_NAME)).jobConfigByInstanceName("unit", true);
        plan.addResourceConfig("some-resource");

        scheduleHelper.schedule(evolveConfig, modifySomeFiles(evolveConfig), DEFAULT_APPROVED_BY);

        Work work = buildAssignmentService.assignWorkToAgent(agent(AgentMother.localAgent()));

        Pipeline pipeline = pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(evolveConfig.name()));
        JobInstance job = pipeline.findStage(STAGE_NAME).findJob("unit");

        assertThat(work).isEqualTo(BuildAssignmentService.NO_WORK);
        assertThat(job.getState()).isEqualTo(JobState.Scheduled);
        assertThat(job.getAgentUuid()).isNull();
    }

    @Test
    public void shouldNotScheduleIfAgentDoesNotHaveMatchingResources() {
        JobConfig plan = evolveConfig.findBy(new CaseInsensitiveString(STAGE_NAME)).jobConfigByInstanceName("unit", true);
        plan.addResourceConfig("some-resource");

        scheduleHelper.schedule(evolveConfig, modifySomeFiles(evolveConfig), DEFAULT_APPROVED_BY);

        Agent agent = AgentMother.localAgent();
        agent.setResources("some-other-resource");

        Work work = buildAssignmentService.assignWorkToAgent(agent(agent));
        assertThat(work).isEqualTo(BuildAssignmentService.NO_WORK);

        Pipeline pipeline = pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(evolveConfig.name()));
        JobInstance job = pipeline.findStage(STAGE_NAME).findJob("unit");

        assertThat(job.getState()).isEqualTo(JobState.Scheduled);
        assertThat(job.getAgentUuid()).isNull();
    }

    @Test
    public void shouldScheduleIfAgentMatchingResources() {
        JobConfig jobConfig = evolveConfig.findBy(new CaseInsensitiveString(STAGE_NAME)).jobConfigByInstanceName("unit", true);
        jobConfig.addResourceConfig("some-resource");

        scheduleHelper.schedule(evolveConfig, modifySomeFiles(evolveConfig), DEFAULT_APPROVED_BY);

        Agent agent = AgentMother.localAgent();
        agent.setResources("some-resource");

        buildAssignmentService.onTimer();
        Work work = buildAssignmentService.assignWorkToAgent(agent(agent));
        assertThat(work).isNotEqualTo(BuildAssignmentService.NO_WORK);

        Pipeline pipeline = pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(evolveConfig.name()));
        JobInstance job = pipeline.findStage(STAGE_NAME).findJob("unit");

        JobPlan loadedPlan = jobInstanceDao.loadPlan(job.getId());
        assertThat((List<ResourceConfig>) loadedPlan.getResources().toResourceConfigs()).isEqualTo(jobConfig.resourceConfigs());

        assertThat(job.getState()).isEqualTo(JobState.Assigned);
        assertThat(job.getAgentUuid()).isEqualTo(agent.getUuid());
    }

    @Test
    public void shouldNotScheduleJobsDuringServerMaintenanceMode() {
        maintenanceModeService.update(new ServerMaintenanceMode(true, "admin", new Date()));

        JobConfig jobConfig = evolveConfig.findBy(new CaseInsensitiveString(STAGE_NAME)).jobConfigByInstanceName("unit", true);
        jobConfig.addResourceConfig("some-resource");

        scheduleHelper.schedule(evolveConfig, modifySomeFiles(evolveConfig), DEFAULT_APPROVED_BY);

        Agent agent = AgentMother.localAgent();
        agent.setResources("some-resource");

        buildAssignmentService.onTimer();
        Work work = buildAssignmentService.assignWorkToAgent(agent(agent));
        assertThat(work).isEqualTo(BuildAssignmentService.NO_WORK);

        Pipeline pipeline = pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(evolveConfig.name()));
        JobInstance job = pipeline.findStage(STAGE_NAME).findJob("unit");

        JobPlan loadedPlan = jobInstanceDao.loadPlan(job.getId());
        assertThat((List<ResourceConfig>) loadedPlan.getResources().toResourceConfigs()).isEqualTo(jobConfig.resourceConfigs());

        assertThat(job.getState()).isEqualTo(JobState.Scheduled);
        assertNull(job.getAgentUuid());
    }

    @Test
    public void shouldReScheduleToCorrectAgent() {
        JobConfig plan = evolveConfig.findBy(new CaseInsensitiveString(STAGE_NAME)).jobConfigByInstanceName("unit", true);
        plan.addResourceConfig("some-resource");

        scheduleHelper.schedule(evolveConfig, modifySomeFiles(evolveConfig), DEFAULT_APPROVED_BY);

        buildAssignmentService.onTimer();

        Agent agent = AgentMother.localAgent();
        agent.setResources("some-resource");
        Work work = buildAssignmentService.assignWorkToAgent(agent(agent));
        assertThat(work).isNotEqualTo(BuildAssignmentService.NO_WORK);

        Pipeline pipeline = pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(evolveConfig.name()));
        JobInstance job = pipeline.findStage(STAGE_NAME).findJob("unit");

        JobInstance runningJob = jobInstanceDao.buildByIdWithTransitions(job.getId());

        scheduleService.rescheduleJob(runningJob);

        pipeline = pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(evolveConfig.name()));
        JobInstance rescheduledJob = pipeline.findStage(STAGE_NAME).findJob("unit");

        assertThat(rescheduledJob.getId()).isNotEqualTo(runningJob.getId());

        buildAssignmentService.onTimer();
        Work noResourcesWork = buildAssignmentService.assignWorkToAgent(agent(AgentMother.localAgentWithResources("WITHOUT_RESOURCES")));
        assertThat(noResourcesWork).isEqualTo(BuildAssignmentService.NO_WORK);

        buildAssignmentService.onTimer();
        Work correctAgentWork = buildAssignmentService.assignWorkToAgent(agent(agent));
        assertThat(correctAgentWork).isNotEqualTo(BuildAssignmentService.NO_WORK);

    }

    @Test
    public void shouldRemoveAllJobPlansThatAreNotInConfig() {
        CruiseConfig oldConfig = goConfigService.getCurrentConfig();
        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", "s1", u.m(new HgMaterial("hg", null)));
        Pipeline p1_1 = instanceFactory.createPipelineInstance(p1.config, modifyNoFiles(p1.config), new DefaultSchedulingContext(
                DEFAULT_APPROVED_BY), md5, new TimeProvider());
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", "s1", u.m(new HgMaterial("hg", null)));
        Pipeline p2_1 = instanceFactory.createPipelineInstance(p2.config, modifyNoFiles(p2.config), new DefaultSchedulingContext(
                DEFAULT_APPROVED_BY), md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(p1_1);
        dbHelper.savePipelineWithStagesAndMaterials(p2_1);
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();
        buildAssignmentService.onConfigChange(cruiseConfig);
        buildAssignmentService.onTimer();

        List<JobPlan> plans = ReflectionUtil.getField(buildAssignmentService, "jobPlans");
        assertThat(plans.isEmpty()).isFalse();
        assertThat(plans.size()).isEqualTo(2);

        configHelper.writeConfigFile(oldConfig);
        plans = ReflectionUtil.getField(buildAssignmentService, "jobPlans");
        assertThat(plans).isEmpty();
    }

    @Test
    public void shouldCancelAScheduledJobInCaseStageHasBeenRenamed() {
        Material hgMaterial = new HgMaterial("url", "folder");
        String[] hgRevs = new String[]{"h1"};
        u.checkinInOrder(hgMaterial, hgRevs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("PIPELINE_WHICH_WILL_EVENTUALLY_CHANGE_CASE", u.m(hgMaterial));
        BuildCause buildCause = BuildCause.createWithModifications(u.mrs(u.mr(u.m(hgMaterial).material, true, hgRevs)), "user");
        Pipeline originalPipelineRun = scheduleService.schedulePipeline(p1.config.name(), buildCause);
        ScheduleTestUtil.AddedPipeline renamedPipeline = u.renamePipelineAndFirstStage(p1, "pipeline_which_will_eventually_change_case", "NEW_RANDOM_STAGE_NAME" + UUID.randomUUID());

        CruiseConfig cruiseConfig = configHelper.load();
        buildAssignmentService.onTimer();   // To Reload Job Plans
        buildAssignmentService.onConfigChange(cruiseConfig);
        Stages allStages = stageDao.findAllStagesFor(originalPipelineRun.getName(), originalPipelineRun.getCounter());
        assertThat(allStages.byName(CaseInsensitiveString.str(p1.config.first().name())).getState()).isEqualTo(StageState.Cancelled);

        u.checkinInOrder(hgMaterial, "h2");
        BuildCause buildCauseForRenamedPipeline = BuildCause.createWithModifications(u.mrs(u.mr(u.m(hgMaterial).material, true, "h2")), "user");
        Pipeline p1_2 = scheduleService.schedulePipeline(renamedPipeline.config.name(), buildCauseForRenamedPipeline);
        Stages allStagesForRenamedPipeline = stageDao.findAllStagesFor(p1_2.getName(), p1_2.getCounter());
        assertThat(allStagesForRenamedPipeline.byName(p1_2.getFirstStage().getName()).getState()).isEqualTo(StageState.Building);
    }

    @Test
    public void shouldNotCancelAScheduledJobInCaseThePipelineAndStageHaveBeenRenamedWithADifferentCase() {
        Material hgMaterial = new HgMaterial("url", "folder");
        String[] hgRevs = new String[]{"h1"};
        u.checkinInOrder(hgMaterial, hgRevs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("ANOTHER_PIPELINE_WHICH_WILL_EVENTUALLY_CHANGE_CASE", "STAGE_WHICH_WILL_EVENTUALLY_CHANGE_CASE", u.m(hgMaterial));
        BuildCause buildCause = BuildCause.createWithModifications(u.mrs(u.mr(u.m(hgMaterial).material, true, hgRevs)), "user");
        Pipeline originalPipelineRun = scheduleService.schedulePipeline(p1.config.name(), buildCause);
        ScheduleTestUtil.AddedPipeline renamedPipeline = u.renamePipelineAndFirstStage(p1, p1.config.name().toLower(), p1.config.getStages().first().name().toLower());
        CruiseConfig cruiseConfig = configHelper.load();
        buildAssignmentService.onTimer();   // To Reload Job Plans
        buildAssignmentService.onConfigChange(cruiseConfig);

        Stages allStages = stageDao.findAllStagesFor(originalPipelineRun.getName(), originalPipelineRun.getCounter());
        assertThat(allStages.byName(CaseInsensitiveString.str(p1.config.first().name())).getState()).isEqualTo(StageState.Building);

        u.checkinInOrder(hgMaterial, "h2");
        BuildCause buildCauseForRenamedPipeline = BuildCause.createWithModifications(u.mrs(u.mr(u.m(hgMaterial).material, true, "h2")), "user");
        Pipeline p1_2 = scheduleService.schedulePipeline(renamedPipeline.config.name(), buildCauseForRenamedPipeline);
        assertThat(p1_2).isNull();
    }

    private JobInstance buildOf(Pipeline pipeline) {
        return pipeline.getStages().first().getJobInstances().first();
    }
}
