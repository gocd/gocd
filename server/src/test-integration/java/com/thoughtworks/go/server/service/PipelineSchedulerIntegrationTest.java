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


import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.EnvironmentVariable;
import com.thoughtworks.go.domain.EnvironmentVariables;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.scheduling.ScheduleOptions;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.utils.Assertions;
import com.thoughtworks.go.utils.Timeout;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.BooleanSupplier;

import static com.thoughtworks.go.matchers.RegexMatcher.matches;
import static com.thoughtworks.go.util.GoConfigFileHelper.env;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
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
    @Autowired private GoCache goCache;
    @Autowired private PipelinePauseService pipelinePauseService;

    private static final String PIPELINE_NAME = "pipeline1";
    private static final String DEV_STAGE = "dev";
    private static final String FT_STAGE = "ft";
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    public Subversion repository;
    public static SvnTestRepo testRepo;
    private static final String PIPELINE_MINGLE = "mingle";
    private static final String PIPELINE_EVOLVE = "evolve";
    private Username cruise;

    @BeforeAll
    public static void setupRepos(@TempDir Path tempDir) throws IOException {
        testRepo = new SvnTestRepo(tempDir);
    }

    @BeforeEach
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

    @AfterEach
    public void tearDown() throws Exception {
        serverHealthService.removeAllLogs();
        pipelineScheduleQueue.clear();
        configHelper.onTearDown();
    }

    @Test
    public void shouldPassOverriddenEnvironmentVariablesForScheduling() {
        final ScheduleOptions scheduleOptions = new ScheduleOptions(new HashMap<>(), Collections.singletonMap("KEY", "value"), new HashMap<>());
        HttpOperationResult operationResult = new HttpOperationResult();
        goConfigService.pipelineConfigNamed(new CaseInsensitiveString(PIPELINE_MINGLE)).setVariables(env("KEY", "somejunk"));
        serverHealthService.update(ServerHealthState.failToScheduling(HealthStateType.general(HealthStateScope.forPipeline(PIPELINE_MINGLE)), PIPELINE_MINGLE, "should wait till cleared"));
        pipelineScheduler.manualProduceBuildCauseAndSave(PIPELINE_MINGLE, Username.ANONYMOUS, scheduleOptions, operationResult);
        assertThat(operationResult.message(), operationResult.canContinue(),is(true));
        Assertions.waitUntil(Timeout.ONE_MINUTE, new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return serverHealthService.filterByScope(HealthStateScope.forPipeline(PIPELINE_MINGLE)).size() == 0;
            }
        });
        BuildCause buildCause = pipelineScheduleQueue.toBeScheduled().get(new CaseInsensitiveString(PIPELINE_MINGLE));

        EnvironmentVariables overriddenVariables = buildCause.getVariables();
        assertThat(overriddenVariables, is(new EnvironmentVariables(Arrays.asList(new EnvironmentVariable("KEY", "value")))));
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
        scheduleService.rerunStage(PIPELINE_MINGLE, pipeline.getCounter(), FT_STAGE);
        try {
            scheduleService.rerunStage(PIPELINE_MINGLE, pipeline.getCounter(), FT_STAGE);
            fail("Should throw exception if fails to re-run stage");
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
        String pipelineName = PIPELINE_NAME.toUpperCase();
        configHelper.addPipeline(pipelineName, "stage-name");

        Username userName = new Username(new CaseInsensitiveString("pauseBy"));
        pipelinePauseService.pause(pipelineName, "pauseCause", userName);

        PipelinePauseInfo pauseInfo = pipelinePauseService.pipelinePauseInfo(pipelineName);
        assertThat(pauseInfo.isPaused(), is(true));
        assertThat(pauseInfo.getPauseCause(), is("pauseCause"));
        assertThat(pauseInfo.getPauseBy(), is("pauseBy"));

        pipelinePauseService.unpause(pipelineName);
        pauseInfo = pipelinePauseService.pipelinePauseInfo(pipelineName);
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
}
