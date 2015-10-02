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
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.Stages;
import com.thoughtworks.go.domain.activity.AgentAssignment;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.DefaultSchedulingContext;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.domain.builder.FetchArtifactBuilder;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.domain.buildcause.BuildCause;
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
import com.thoughtworks.go.remote.work.NoWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.service.builders.BuilderFactory;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.utils.SerializationTester;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.helper.ModificationsMother.modifyNoFiles;
import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;
import static com.thoughtworks.go.util.TestUtils.sleepQuietly;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class BuildAssignmentServiceTest {
    @Autowired private BuildAssignmentService buildAssignmentService;
    @Autowired private GoConfigService goConfigService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private JobInstanceDao jobInstanceDao;
    @Autowired private AgentService agentService;
    @Autowired private AgentAssignment agentAssignment;
    @Autowired private ScheduleService scheduleService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private ScheduleHelper scheduleHelper;
    @Autowired private GoCache goCache;
    @Autowired private StageDao stageDao;
    @Autowired private JobInstanceService jobInstanceService;
    @Autowired private PipelineService pipelineService;
    @Autowired private EnvironmentConfigService environmentConfigService;
    @Autowired private TimeProvider timeProvider;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private BuilderFactory builderFactory;
    @Autowired private InstanceFactory instanceFactory;

    private PipelineConfig evolveConfig;
    private static final String STAGE_NAME = "dev";
    private GoConfigFileHelper configHelper;
    private ScheduleTestUtil u;

    public Subversion repository;
    public static TestRepo testRepo;
    private PipelineWithTwoStages fixture;
    private String md5 = "md5-test";
    private Username loserUser = new Username(new CaseInsensitiveString("loser"));

    @BeforeClass
    public static void setupRepos() throws IOException {
        testRepo = new SvnTestRepo("testSvnRepo");
    }

    @AfterClass
    public static void tearDownConfigFileLocation() throws IOException {
        TestRepo.internalTearDown();
    }

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper().usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        dbHelper.onSetUp();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();

        repository = new SvnCommand(null, testRepo.projectRepositoryUrl());
        evolveConfig = configHelper.addPipeline("evolve", STAGE_NAME, repository, "unit");
        configHelper.addPipeline("anotherPipeline", STAGE_NAME, repository, "anotherTest");
        configHelper.addPipeline("thirdPipeline", STAGE_NAME, repository, "yetAnotherTest");
        goConfigService.forceNotifyListeners();
        goCache.clear();
        u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
    }

    @After
    public void teardown() throws Exception {
        goCache.clear();
        agentService.clearAll();
        fixture.onTearDown();
        dbHelper.onTearDown();
        configHelper.onTearDown();
        FileUtil.deleteFolder(goConfigService.artifactsDir());
        agentAssignment.clear();
    }

    @Test
    public void shouldRescheduleAbandonedBuild() throws SQLException {
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
        assertThat(reloaded.getState(), is(JobState.Rescheduled));
        assertThat(reloaded.isIgnored(), is(true));
    }

    @Test
    public void shouldNotAssignWorkToDeniedAgent() throws Exception {
        AgentConfig deniedAgentConfig = AgentMother.localAgent();
        deniedAgentConfig.disable();

        Work assignedWork = buildAssignmentService.assignWorkToAgent(agent(deniedAgentConfig));
        assertThat(assignedWork, instanceOf(DeniedAgentWork.class));
    }

    @Test
    public void shouldNotAssignWorkWhenPipelineScheduledWithStaleMaterials() {
        AgentIdentifier instance = agent(AgentMother.localAgent());
        Pipeline pipeline = instanceFactory.createPipelineInstance(evolveConfig, modifyNoFiles(evolveConfig), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(pipeline);
        evolveConfig.setMaterialConfigs(new MaterialConfigs(new HgMaterialConfig("foo", null)));
        configHelper.removePipeline(CaseInsensitiveString.str(evolveConfig.name()));
        configHelper.addPipeline(evolveConfig);
        buildAssignmentService.onConfigChange(goConfigService.getCurrentConfig());
        JobInstance job = buildOf(pipeline);
        jobInstanceDao.updateStateAndResult(job);
        assertThat(buildAssignmentService.assignWorkToAgent(instance), is((Work) BuildAssignmentService.NO_WORK));
    }

    @Test
    public void shouldNotAssignCancelledJob() throws Exception {
        AgentIdentifier instance = agent(AgentMother.localAgent());
        Pipeline pipeline = instanceFactory.createPipelineInstance(evolveConfig, modifyNoFiles(evolveConfig), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(pipeline);
        buildAssignmentService.onConfigChange(goConfigService.getCurrentConfig());
        JobInstance job = buildOf(pipeline);
        job.cancel();
        jobInstanceDao.updateStateAndResult(job);

        assertThat(buildAssignmentService.assignWorkToAgent(instance), is((Work) BuildAssignmentService.NO_WORK));
    }

    @Test
    public void shouldUpdateNumberOfActiveRemoteAgentsAfterAssigned() {
        AgentConfig agentConfig = AgentMother.remoteAgent();
        configHelper.addAgent(agentConfig);
        fixture.createPipelineWithFirstStageScheduled();
        buildAssignmentService.onTimer();

        int before = agentService.numberOfActiveRemoteAgents();
        Work work = buildAssignmentService.assignWorkToAgent(agent(agentConfig));
        assertThat(work, instanceOf(BuildWork.class));
        assertThat(agentService.numberOfActiveRemoteAgents(), is(before + 1));
    }

    @Test
    public void shouldCancelOutOfDateBuilds() throws Exception {
        fixture.createPipelineWithFirstStageScheduled();
        buildAssignmentService.onTimer();
        configHelper.removeStage(fixture.pipelineName, fixture.devStage);

        buildAssignmentService.onConfigChange(goConfigService.getCurrentConfig());

        Pipeline pipeline = pipelineDao.mostRecentPipeline(fixture.pipelineName);
        JobInstance job = pipeline.getFirstStage().getJobInstances().first();
        assertThat(job.getState(), is(JobState.Completed));
        assertThat(job.getResult(), is(JobResult.Cancelled));
    }

    @Test
    public void shouldCancelBuildBelongingToNonExistentPipeline() throws Exception {
        fixture.createPipelineWithFirstStageScheduled();
        buildAssignmentService.onTimer();

        configHelper.removePipeline(fixture.pipelineName);

        AgentConfig agentConfig = AgentMother.localAgent();
        agentConfig.addResource(new Resource("some-other-resource"));

        assertThat((NoWork) buildAssignmentService.assignWorkToAgent(agent(agentConfig)), Matchers.is(BuildAssignmentService.NO_WORK));
        Pipeline pipeline = pipelineDao.mostRecentPipeline(fixture.pipelineName);
        JobInstance job = pipeline.getFirstStage().getJobInstances().first();
        assertThat(job.getState(), is(JobState.Completed));
        assertThat(job.getResult(), is(JobResult.Cancelled));
        Stage stage = stageDao.findStageWithIdentifier(job.getIdentifier().getStageIdentifier());
        assertThat(stage.getState(), is(StageState.Cancelled));
        assertThat(stage.getResult(), is(StageResult.Cancelled));
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

        ScheduledPipelineLoader scheduledPipelineLoader = new ScheduledPipelineLoader(null, null, null, null, null, null, null, null) {
            @Override
            public Pipeline pipelineWithPasswordAwareBuildCauseByBuildId(long buildId) {
                sem.release();
                sleepQuietly(1000);
                verify(mockJobInstanceService, times(1)).orderedScheduledBuilds();
                return pipeline1;
            }
        };

        final BuildAssignmentService buildAssignmentServiceUnderTest = new BuildAssignmentService(goConfigService, mockJobInstanceService, scheduleService,
                agentService, environmentConfigService, timeProvider, transactionTemplate, scheduledPipelineLoader, pipelineService, builderFactory);

        final Throwable[] fromThread = new Throwable[1];
        buildAssignmentServiceUnderTest.onTimer();

        Thread assigner = new Thread(new Runnable() {
            public void run() {
                try {
                    final AgentConfig agentConfig = AgentMother.localAgentWithResources("some-other-resource");

                    buildAssignmentServiceUnderTest.assignWorkToAgent(agent(agentConfig));
                } catch (Throwable e) {
                    e.printStackTrace();
                    fromThread[0] = e;
                } finally {

                }
            }
        }, "assignmentThread");
        assigner.start();

        sem.acquire();
        buildAssignmentServiceUnderTest.onTimer();

        assigner.join();
        assertThat(fromThread[0], is(nullValue()));
    }

    @Test
    public void shouldCancelBuildBelongingToNonExistentPipelineWhenCreatingWork() throws Exception {
        fixture.createPipelineWithFirstStageScheduled();
        Pipeline pipeline = pipelineDao.mostRecentPipeline(fixture.pipelineName);

        ScheduledPipelineLoader scheduledPipelineLoader = mock(ScheduledPipelineLoader.class);
        when(scheduledPipelineLoader.pipelineWithPasswordAwareBuildCauseByBuildId(pipeline.getFirstStage().getJobInstances().first().getId())).thenThrow(
                new PipelineNotFoundException("thrown by mockPipelineService"));

        GoConfigService mockGoConfigService = mock(GoConfigService.class);
        CruiseConfig config = configHelper.currentConfig();
        configHelper.removePipeline(fixture.pipelineName, config);
        when(mockGoConfigService.getCurrentConfig()).thenReturn(config);

        buildAssignmentService = new BuildAssignmentService(mockGoConfigService, jobInstanceService, scheduleService, agentService, environmentConfigService, timeProvider,
                transactionTemplate, scheduledPipelineLoader, pipelineService, builderFactory);
        buildAssignmentService.onTimer();

        AgentConfig agentConfig = AgentMother.localAgent();
        agentConfig.addResource(new Resource("some-other-resource"));

        try {
            buildAssignmentService.assignWorkToAgent(agent(agentConfig));
            fail("should have thrown PipelineNotFoundException");
        } catch (PipelineNotFoundException e) {
            // ok
        }

        pipeline = pipelineDao.mostRecentPipeline(fixture.pipelineName);

        JobInstance job = pipeline.getFirstStage().getJobInstances().first();
        assertThat(job.getState(), is(JobState.Completed));
        assertThat(job.getResult(), is(JobResult.Cancelled));
        Stage stage = stageDao.findStageWithIdentifier(job.getIdentifier().getStageIdentifier());
        assertThat(stage.getState(), is(StageState.Cancelled));
        assertThat(stage.getResult(), is(StageResult.Cancelled));
    }

    @Test
    public void shouldBeAbleToSerializeAndDeserializeBuildWork() throws Exception {
        Pipeline pipeline1 = instanceFactory.createPipelineInstance(evolveConfig, modifySomeFiles(evolveConfig), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(pipeline1);

        buildAssignmentService.onTimer();
        BuildWork work = (BuildWork) buildAssignmentService.assignWorkToAgent(agent(AgentMother.localAgent()));

        BuildWork deserialized = (BuildWork) SerializationTester.serializeAndDeserialize(work);

        assertThat(deserialized.getAssignment().materialRevisions(), is(work.getAssignment().materialRevisions()));

        assertThat(deserialized.getAssignment(), is(work.getAssignment()));
        assertThat(deserialized, is(work));
    }

    @Test
    public void shouldCreateWorkWithFetchMaterialsFlagFromStageConfig() throws Exception {
        evolveConfig.getFirstStageConfig().setFetchMaterials(true);
        Pipeline pipeline1 = instanceFactory.createPipelineInstance(evolveConfig, modifySomeFiles(evolveConfig), new DefaultSchedulingContext(DEFAULT_APPROVED_BY), md5, new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(pipeline1);

        buildAssignmentService.onTimer();
        BuildWork work = (BuildWork) buildAssignmentService.assignWorkToAgent(agent(AgentMother.localAgent()));

        assertThat("should have set fetchMaterials on assignment", work.getAssignment().getPlan().shouldFetchMaterials(), is(true));
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
    public void shouldCreateWork_withAncestorFetchArtifactCalls_resolvedToRelevantStage() throws Exception {
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
        configHelper.replaceAllJobsInStage("downest", "downest-stage", new JobConfig(new CaseInsensitiveString("fetcher"), new Resources("fetcher"), new ArtifactPlans(), allFetchTasks));
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
        AgentConfig agentConfig = AgentMother.localAgent();
        agentConfig.addResource(new Resource("fetcher"));
        BuildWork work = (BuildWork) buildAssignmentService.assignWorkToAgent(agent(agentConfig));

        List<Builder> builders = work.getAssignment().getBuilders();
        FetchArtifactBuilder fooZipFetch = (FetchArtifactBuilder) builders.get(0);
        assertThat(fooZipFetch.artifactLocator(), is("uppest/1/uppest-stage/latest/unit/foo.zip"));
        FetchArtifactBuilder barZipFetch = (FetchArtifactBuilder) builders.get(1);
        assertThat(barZipFetch.artifactLocator(), is("uppest/2/uppest-stage/1/unit/bar.zip"));
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


    private AgentIdentifier agent(AgentConfig agentConfig) {
        agentService.sync(new Agents(agentConfig));
        agentService.approve(agentConfig.getUuid());
        return agentService.findAgent(agentConfig.getUuid()).getAgentIdentifier();
    }

    @Test
    public void shouldNotScheduleIfAgentDoesNotHaveResources() throws Exception {
        JobConfig plan = evolveConfig.findBy(new CaseInsensitiveString(STAGE_NAME)).jobConfigByInstanceName("unit", true);
        plan.addResource("some-resource");

        scheduleHelper.schedule(evolveConfig, modifySomeFiles(evolveConfig), DEFAULT_APPROVED_BY);

        Work work = buildAssignmentService.assignWorkToAgent(agent(AgentMother.localAgent()));

        Pipeline pipeline = pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(evolveConfig.name()));
        JobInstance job = pipeline.findStage(STAGE_NAME).findJob("unit");

        assertThat(work, is((Work) BuildAssignmentService.NO_WORK));
        assertThat(job.getState(), is(JobState.Scheduled));
        assertThat(job.getAgentUuid(), is(nullValue()));
    }

    @Test
    public void shouldNotScheduleIfAgentDoesNotHaveMatchingResources() throws Exception {
        JobConfig plan = evolveConfig.findBy(new CaseInsensitiveString(STAGE_NAME)).jobConfigByInstanceName("unit", true);
        plan.addResource("some-resource");

        scheduleHelper.schedule(evolveConfig, modifySomeFiles(evolveConfig), DEFAULT_APPROVED_BY);

        AgentConfig agentConfig = AgentMother.localAgent();
        agentConfig.addResource(new Resource("some-other-resource"));

        Work work = buildAssignmentService.assignWorkToAgent(agent(agentConfig));
        assertThat(work, is((Work) BuildAssignmentService.NO_WORK));

        Pipeline pipeline = pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(evolveConfig.name()));
        JobInstance job = pipeline.findStage(STAGE_NAME).findJob("unit");

        assertThat(job.getState(), is(JobState.Scheduled));
        assertThat(job.getAgentUuid(), is(nullValue()));
    }

    @Test
    public void shouldScheduleIfAgentMatchingResources() throws Exception {
        JobConfig plan = evolveConfig.findBy(new CaseInsensitiveString(STAGE_NAME)).jobConfigByInstanceName("unit", true);
        plan.addResource("some-resource");

        scheduleHelper.schedule(evolveConfig, modifySomeFiles(evolveConfig), DEFAULT_APPROVED_BY);

        AgentConfig agentConfig = AgentMother.localAgent();
        agentConfig.addResource(new Resource("some-resource"));

        buildAssignmentService.onTimer();
        Work work = buildAssignmentService.assignWorkToAgent(agent(agentConfig));
        assertThat(work, is(not((Work) BuildAssignmentService.NO_WORK)));

        Pipeline pipeline = pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(evolveConfig.name()));
        JobInstance job = pipeline.findStage(STAGE_NAME).findJob("unit");

        JobPlan loadedPlan = jobInstanceDao.loadPlan(job.getId());
        assertThat(loadedPlan.getResources(), is((List<Resource>) plan.resources()));


        assertThat(job.getState(), is(JobState.Assigned));
        assertThat(job.getAgentUuid(), is(agentConfig.getUuid()));
    }

    @Test
    public void shouldReScheduleToCorrectAgent() throws Exception {
        JobConfig plan = evolveConfig.findBy(new CaseInsensitiveString(STAGE_NAME)).jobConfigByInstanceName("unit", true);
        plan.addResource("some-resource");

        scheduleHelper.schedule(evolveConfig, modifySomeFiles(evolveConfig), DEFAULT_APPROVED_BY);

        buildAssignmentService.onTimer();

        AgentConfig agentConfig = AgentMother.localAgent();
        agentConfig.addResource(new Resource("some-resource"));
        Work work = buildAssignmentService.assignWorkToAgent(agent(agentConfig));
        assertThat(work, is(not((Work) BuildAssignmentService.NO_WORK)));

        Pipeline pipeline = pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(evolveConfig.name()));
        JobInstance job = pipeline.findStage(STAGE_NAME).findJob("unit");

        JobInstance runningJob = jobInstanceDao.buildByIdWithTransitions(job.getId());

        scheduleService.rescheduleJob(runningJob);

        pipeline = pipelineDao.mostRecentPipeline(CaseInsensitiveString.str(evolveConfig.name()));
        JobInstance rescheduledJob = pipeline.findStage(STAGE_NAME).findJob("unit");

        assertThat(rescheduledJob.getId(), not(runningJob.getId()));

        buildAssignmentService.onTimer();
        Work noResourcesWork = buildAssignmentService.assignWorkToAgent(agent(AgentMother.localAgentWithResources("WITHOUT_RESOURCES")));
        assertThat(noResourcesWork, is((Work) BuildAssignmentService.NO_WORK));

        buildAssignmentService.onTimer();
        Work correctAgentWork = buildAssignmentService.assignWorkToAgent(agent(agentConfig));
        assertThat(correctAgentWork, is(not((Work) BuildAssignmentService.NO_WORK)));

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

        List<JobPlan> plans = (List<JobPlan>) ReflectionUtil.getField(buildAssignmentService, "jobPlans");
        assertThat(plans.isEmpty(), is(false));
        assertThat(plans.size(), is(2));

        configHelper.writeConfigFile(oldConfig);
        plans = (List<JobPlan>) ReflectionUtil.getField(buildAssignmentService, "jobPlans");
        assertThat("Actual size is " + plans.size(), plans.isEmpty(), is(true));
    }

    @Test
    public void shouldCancelAScheduledJobInCaseThePipelineIsRemovedFromTheConfig_SpecificallyAPipelineRenameToADifferentCaseAndStageNameToADifferentName() throws Exception {
        Material hgMaterial = new HgMaterial("url", "folder");
        String[] hgRevs = new String[]{"h1"};
        u.checkinInOrder(hgMaterial, hgRevs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("PIPELINE_WHICH_WILL_EVENTUALLY_CHANGE_CASE", u.m(hgMaterial));

        u.scheduleWith(p1, hgRevs);
        ScheduleTestUtil.AddedPipeline renamedPipeline = u.renamePipelineAndFirstStage(p1, "pipeline_which_will_eventually_change_case", "NEW_RANDOM_STAGE_NAME" + UUID.randomUUID());

        Pipeline p1_2 = u.scheduleWith(renamedPipeline, hgRevs);
        CruiseConfig cruiseConfig = configHelper.load();
        buildAssignmentService.onTimer();   // To Reload Job Plans
        buildAssignmentService.onConfigChange(cruiseConfig);

        Stages allStages = stageDao.findAllStagesFor(p1_2.getName(), p1_2.getCounter());
        assertThat(allStages.byName(CaseInsensitiveString.str(p1.config.first().name())).getState(), is(StageState.Cancelled));
    }

    private JobInstance buildOf(Pipeline pipeline) {
        return pipeline.getStages().first().getJobInstances().first();
    }

}
