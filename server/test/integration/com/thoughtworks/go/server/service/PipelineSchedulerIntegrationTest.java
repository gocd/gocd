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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.GoConstants;
import junit.framework.Assert;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.DefaultSchedulingContext;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.utils.Assertions;
import com.thoughtworks.go.utils.Timeout;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.matchers.RegexMatcher.matches;
import static com.thoughtworks.go.util.GoConfigFileHelper.env;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class PipelineSchedulerIntegrationTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoConfigService goConfigService;
    @Autowired private PipelineScheduler pipelineScheduler;
    @Autowired private ServerHealthService serverHealthService;
    @Autowired private PipelineService pipelineService;
    @Autowired private ScheduleService scheduleService;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired private ScheduleHelper scheduleHelper;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private GoCache goCache;
    @Autowired private PipelinePauseService pipelinePauseService;
    @Autowired private InstanceFactory instanceFactory;

    private static final String PIPELINE_NAME = "pipeline1";
    private static final String DEV_STAGE = "dev";
    private static final String FT_STAGE = "ft";
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    public Subversion repository;
    public static SvnTestRepo testRepo;
    private static final String PIPELINE_MINGLE = "mingle";
    private static final String PIPELINE_EVOLVE = "evolve";
    private Username cruise;

    @BeforeClass
    public static void setupRepos() throws IOException {
        testRepo = new SvnTestRepo("testSvnRepo");
    }

    @AfterClass
    public static void tearDownConfigFileLocation() throws IOException {
        TestRepo.internalTearDown();
    }

    @Before
    public void setup() throws Exception {
        cruise = new Username(new CaseInsensitiveString("cruise"));

        dbHelper.onSetUp();

        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        repository = new SvnCommand(null, testRepo.projectRepositoryUrl());
        configHelper.addPipeline(PIPELINE_MINGLE, DEV_STAGE, repository, "unit", "functional");
        configHelper.addStageToPipeline(PIPELINE_MINGLE, FT_STAGE);

        Subversion wrongRepository = new SvnCommand(null, "wrongurl");
        configHelper.addPipeline(PIPELINE_EVOLVE, DEV_STAGE, wrongRepository, "unit", "functional");
        goConfigService.forceNotifyListeners();
        serverHealthService.removeAllLogs();
        goCache.clear();
    }

    @After
    public void tearDown() throws Exception {
        serverHealthService.removeAllLogs();
        pipelineScheduleQueue.clear();
        configHelper.onTearDown();
    }

    @Test public void shouldPassOverriddenEnvironmentVariablesForScheduling() {
        final ScheduleOptions scheduleOptions = new ScheduleOptions(new HashMap<String, String>(), Collections.singletonMap("KEY", "value"), new HashMap<String, String>());
        HttpOperationResult operationResult = new HttpOperationResult();
        goConfigService.pipelineConfigNamed(new CaseInsensitiveString(PIPELINE_MINGLE)).setVariables(env("KEY", "somejunk"));
        serverHealthService.update(ServerHealthState.failToScheduling(HealthStateType.general(HealthStateScope.forPipeline(PIPELINE_MINGLE)), PIPELINE_MINGLE, "should wait till cleared"));
        pipelineScheduler.manualProduceBuildCauseAndSave(PIPELINE_MINGLE, Username.ANONYMOUS, scheduleOptions, operationResult);
        assertThat(operationResult.message(), operationResult.canContinue(),is(true));
        Assertions.waitUntil(Timeout.ONE_MINUTE, new Assertions.Predicate() {
            public boolean call() throws Exception {
                return serverHealthService.filterByScope(HealthStateScope.forPipeline(PIPELINE_MINGLE)).size() == 0;
            }
        });
        BuildCause buildCause = pipelineScheduleQueue.toBeScheduled().get(PIPELINE_MINGLE);

        EnvironmentVariablesConfig overriddenVariables = buildCause.getVariables();
        assertThat(overriddenVariables, is(env("KEY", "value")));
    }


    @Test
    public void shouldRemoveErrorLogForPipelineIfSchedulingSucceeded() throws Exception {
        serverHealthService.update(ServerHealthState.error("failed to connect to scm", "failed to connect to scm",
                HealthStateType.general(HealthStateScope.forPipeline(PIPELINE_MINGLE))));
        ServerHealthState serverHealthState = scheduleHelper.manuallySchedulePipelineWithRealMaterials(PIPELINE_MINGLE, cruise);
        assertThat(serverHealthState.isSuccess(), is(true));
        assertCurrentErrorLogNumberIs(PIPELINE_MINGLE, 0);
    }

    private void assertCurrentErrorLogNumberIs(String pipelineName, int number) {
        List<ServerHealthState> entries = serverHealthService.filterByScope(HealthStateScope.forPipeline(pipelineName));
        assertThat(entries.toString(), entries.size(), is(number));
    }

    @Test
    public void shouldRemoveAllErrorLogsForPipelineIfSchedulingSucceeded() throws Exception {
        serverHealthService.update(ServerHealthState.error("failed to connect to scm", "failed to connect to scm",
                HealthStateType.general(HealthStateScope.forPipeline(PIPELINE_MINGLE))));
        serverHealthService.update(ServerHealthState.error("failed to connect to scm", "failed to connect to scm",
                HealthStateType.artifactsDiskFull()));

        ServerHealthState serverHealthState = scheduleHelper.manuallySchedulePipelineWithRealMaterials(PIPELINE_MINGLE, cruise);
        assertThat(serverHealthState.isSuccess(), is(true));
        assertCurrentErrorLogNumberIs(PIPELINE_MINGLE, 0);
    }

    @Test
    public void shouldThrowExceptionIfOtherStageIsRunningInTheSamePipeline() throws Exception {
        Pipeline pipeline = makeCompletedPipeline();
        StageConfig ftStage = goConfigService.stageConfigNamed(PIPELINE_MINGLE, FT_STAGE);
        scheduleService.rerunStage(PIPELINE_MINGLE, pipeline.getCounter().toString(), FT_STAGE);
        try {
            scheduleService.rerunStage(PIPELINE_MINGLE, pipeline.getCounter().toString(), FT_STAGE);
            Assert.fail("Should throw exception if fails to re-run stage");
        } catch (Exception ignored) {
            assertThat(ignored.getMessage(), matches("Cannot schedule: Pipeline.+is still in progress"));
        }
    }

    private Pipeline makeCompletedPipeline() throws Exception {
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(PIPELINE_MINGLE);
        scheduleService.autoSchedulePipelinesFromRequestBuffer();

        Pipeline pipeline = pipelineService.mostRecentFullPipelineByName(PIPELINE_MINGLE);
        dbHelper.pass(pipeline);
        return pipeline;
    }

    @Test
    public void shouldPauseAndUnpausePipeline_identifiedByCaseInsensitiveString() throws Exception {

        configHelper.setOperatePermissionForGroup("defaultGroup", "pausedBy");
        configHelper.addPipeline(PIPELINE_NAME, "stage-name");

        Username userName = new Username(new CaseInsensitiveString("pauseBy"));
        pipelinePauseService.pause(PIPELINE_NAME, "pauseCause", userName);

        PipelinePauseInfo pauseInfo = pipelinePauseService.pipelinePauseInfo(PIPELINE_NAME);
        assertThat(pauseInfo.isPaused(), is(true));
        assertThat(pauseInfo.getPauseCause(), is("pauseCause"));
        assertThat(pauseInfo.getPauseBy(), is("pauseBy"));

        pipelinePauseService.unpause(PIPELINE_NAME);
        pauseInfo = pipelinePauseService.pipelinePauseInfo(PIPELINE_NAME);
        assertThat(pauseInfo.isPaused(), is(false));
    }

    @Test
    public void shouldPauseAndUnpausePipeline() throws Exception {
        configHelper.setOperatePermissionForGroup("defaultGroup", "pausedBy");
        configHelper.addPipeline(PIPELINE_NAME, "stage-name");

        Username userName = new Username(new CaseInsensitiveString("pauseBy"));
        pipelinePauseService.pause(PIPELINE_NAME, "pauseCause", userName);

        PipelinePauseInfo pauseInfo = pipelinePauseService.pipelinePauseInfo(PIPELINE_NAME);
        assertThat(pauseInfo.isPaused(), is(true));
        assertThat(pauseInfo.getPauseCause(), is("pauseCause"));
        assertThat(pauseInfo.getPauseBy(), is("pauseBy"));

        pipelinePauseService.unpause(PIPELINE_NAME);
        pauseInfo = pipelinePauseService.pipelinePauseInfo(PIPELINE_NAME);
        assertThat(pauseInfo.isPaused(), is(false));
    }


    @Test public void returnPipelineForBuildDetailViewShouldContainOnlyMods() throws Exception {
        Pipeline pipeline = createPipelineWithStagesAndMods();
        JobInstance job = pipeline.getFirstStage().getJobInstances().first();

        Pipeline slimPipeline = pipelineService.wrapBuildDetails(job);
        assertThat(slimPipeline.getBuildCause().getMaterialRevisions().totalNumberOfModifications(), is(1));
        assertThat(slimPipeline.getName(), is(pipeline.getName()));
        assertThat(slimPipeline.getFirstStage().getJobInstances().size(), is(1));
    }

    @Test
    public void shouldApplyLabelFromPreviousPipeline() throws Exception {
        String oldLabel = createNewPipeline().getLabel();
        String newLabel = createNewPipeline().getLabel();
        assertThat(newLabel, is(greaterThan(oldLabel)));
    }

    private Pipeline createNewPipeline() {
        if (!goConfigService.hasPipelineNamed(new CaseInsensitiveString("Test"))) {
            configHelper.addPipeline("Test", "dev");
        }
        Pipeline pipeline = new Pipeline("Test", "testing-${COUNT}", BuildCause.createWithEmptyModifications());
        return pipelineService.save(pipeline);
    }

    @Test
    public void shouldIncreaseCounterFromPreviousPipeline() {
        Pipeline pipeline1 = createNewPipeline();
        Pipeline pipeline2 = createNewPipeline();
        assertThat(pipeline2.getCounter(), is(pipeline1.getCounter() + 1));
    }

    @Test
    public void shouldFindPipelineByLabel() {
        Pipeline pipeline = createPipelineWhoseLabelIsNumberAndNotSameWithCounter();
        Pipeline actual = pipelineService.findPipelineByCounterOrLabel("Test", "10");
        assertThat(actual.getId(), is(pipeline.getId()));
        assertThat(actual.getLabel(), is(pipeline.getLabel()));
        assertThat(actual.getCounter(), is(pipeline.getCounter()));
    }

    @Test
    public void shouldFindPipelineByCounter() {
        Pipeline pipeline = createNewPipeline();
        Pipeline actual = pipelineService.findPipelineByCounterOrLabel("Test", pipeline.getCounter().toString());
        assertThat(actual.getId(), is(pipeline.getId()));
        assertThat(actual.getLabel(), is(pipeline.getLabel()));
        assertThat(actual.getCounter(), is(pipeline.getCounter()));
    }

    @Test
    public void shouldReturnFullPipelineByCounter() {
        Pipeline pipeline = createPipelineWithStagesAndMods();
        Pipeline actual = pipelineService.fullPipelineByCounterOrLabel(pipeline.getName(),
                pipeline.getCounter().toString());
        assertThat(actual.getStages().size(), is(not(0)));
        assertThat(actual.getBuildCause().getMaterialRevisions().getRevisions().size(), is(not(0)));
    }

    private Pipeline createPipelineWhoseLabelIsNumberAndNotSameWithCounter() {
        Pipeline pipeline = new Pipeline("Test", "${COUNT}0", BuildCause.createWithEmptyModifications());
        pipeline.updateCounter(9);
        pipelineDao.save(pipeline);
        return pipeline;
    }

    private Pipeline createPipelineWithStagesAndMods() {
        PipelineConfig config = PipelineMother.twoBuildPlansWithResourcesAndMaterials("tester", "dev");
        configHelper.addPipeline(CaseInsensitiveString.str(config.name()), CaseInsensitiveString.str(config.first().name()));
        Pipeline pipeline = instanceFactory.createPipelineInstance(config, modifySomeFiles(config), new DefaultSchedulingContext(GoConstants.DEFAULT_APPROVED_BY), "md5-test", new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(pipeline);
        return pipeline;
    }

}
