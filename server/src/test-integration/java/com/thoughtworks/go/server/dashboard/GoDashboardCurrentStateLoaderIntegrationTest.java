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
package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.presentation.pipelinehistory.EmptyPipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.NullStageHistoryItem;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.ScheduleService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.List;

import static com.thoughtworks.go.helper.ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class GoDashboardCurrentStateLoaderIntegrationTest {

    @Autowired
    private GoDashboardCurrentStateLoader goDashboardCurrentStateLoader;

    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoConfigDao goConfigDao;

    @Autowired
    private GoConfigService goConfigService;

    private GoConfigFileHelper configHelper;

    private final SystemEnvironment systemEnvironment = new SystemEnvironment();
    @Autowired
    private ScheduleService scheduleService;

    @BeforeEach
    public void setUp(@TempDir File configDir) throws Exception {
        dbHelper.onSetUp();
        String absolutePath = new File(configDir, "cruise-config.xml").getAbsolutePath();
        systemEnvironment.setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, absolutePath);
        configHelper = new GoConfigFileHelper(DEFAULT_XML_WITH_2_AGENTS);
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        goConfigService.forceNotifyListeners();
        goDashboardCurrentStateLoader.reset();
    }

    @AfterEach
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldReturnEmptyWhenNoPipelinesArePresentInConfig() {
        goConfigService.forceNotifyListeners();
        assertThat(goConfigService.getAllPipelineConfigs()).isEmpty();
        List<GoDashboardPipeline> goDashboardPipelines = goDashboardCurrentStateLoader.allPipelines(goConfigService.currentCruiseConfig());
        assertThat(goDashboardPipelines).isEmpty();
    }

    @Test
    public void shouldReturnSingleDashboardForSingleCompletedGreenPipelineInstance() {
        PipelineConfig pipelineConfig = configHelper.addPipeline(PipelineConfigMother.createPipelineConfigWithStages("a-pipeline", "a-stage"));
        goConfigService.forceNotifyListeners();

        Pipeline pipeline = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);

        List<GoDashboardPipeline> goDashboardPipelines = goDashboardCurrentStateLoader.allPipelines(goConfigService.currentCruiseConfig());

        assertThat(goDashboardPipelines).hasSize(1);
        GoDashboardPipeline goDashboardPipeline = goDashboardPipelines.getFirst();

        assertThat(goDashboardPipeline.name()).isEqualTo(pipelineConfig.name());
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().size()).isEqualTo(1);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst()).isEqualTo(goDashboardPipeline.model().getLatestPipelineInstance());
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().getId()).isEqualTo(pipeline.getId());
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage(pipelineConfig.getStages().getFirst().name().toString()).getId()).isEqualTo(pipeline.getFirstStage().getId());
    }

    @Test
    public void shouldShowMultiplePipelineInstancesFromSamePipelineWhenMultipleAreRunning() {
        // 2 -> (s1, running)
        // 1 -> (s1, passed), (s2, running)

        PipelineConfig pipelineConfig = configHelper.addPipeline(PipelineConfigMother.createPipelineConfigWithStages("a-pipeline", "a-stage", "b-stage"));
        goConfigService.forceNotifyListeners();

        Pipeline p1 = dbHelper.newPipelineWithFirstStagePassed(pipelineConfig);
        dbHelper.scheduleStage(p1, pipelineConfig.getStage("b-stage"));
        Pipeline p2 = dbHelper.newPipelineWithFirstStageScheduled(pipelineConfig);

        List<GoDashboardPipeline> goDashboardPipelines = goDashboardCurrentStateLoader.allPipelines(goConfigService.currentCruiseConfig());
        assertThat(goDashboardPipelines).hasSize(1);

        GoDashboardPipeline goDashboardPipeline = goDashboardPipelines.getFirst();

        assertThat(goDashboardPipeline.name()).isEqualTo(pipelineConfig.name());
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().size()).isEqualTo(2);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().getId()).isEqualTo(p2.getId());
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getLast().getId()).isEqualTo(p1.getId());

        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().getId()).isEqualTo(p2.getId());

        // latest pipeline run
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("a-stage").getResult()).isEqualTo(StageResult.Unknown);
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("a-stage").getState()).isEqualTo(StageState.Building);
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("b-stage")).isInstanceOf(NullStageHistoryItem.class);

        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().stage("a-stage").getResult()).isEqualTo(StageResult.Unknown);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().stage("a-stage").getState()).isEqualTo(StageState.Building);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().stage("b-stage")).isInstanceOf(NullStageHistoryItem.class);

        // one previous pipeline run
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getLast().stage("a-stage").getResult()).isEqualTo(StageResult.Passed);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getLast().stage("a-stage").getState()).isEqualTo(StageState.Passed);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getLast().stage("b-stage").getResult()).isEqualTo(StageResult.Unknown);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getLast().stage("b-stage").getState()).isEqualTo(StageState.Building);
    }

    @Test
    public void shouldReturnNothingIfPipelineHasNeverRun() {
        PipelineConfig pipelineConfig = configHelper.addPipeline(PipelineConfigMother.createPipelineConfigWithStages("a-pipeline", "a-stage", "b-stage"));
        goConfigService.forceNotifyListeners();

        List<GoDashboardPipeline> goDashboardPipelines = goDashboardCurrentStateLoader.allPipelines(goConfigService.currentCruiseConfig());
        assertThat(goDashboardPipelines).hasSize(1);

        GoDashboardPipeline goDashboardPipeline = goDashboardPipelines.getFirst();

        assertThat(goDashboardPipeline.model().getActivePipelineInstances().size()).isEqualTo(1);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst()).isInstanceOf(EmptyPipelineInstanceModel.class);
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance()).isInstanceOf(EmptyPipelineInstanceModel.class);
    }

    @Test
    public void shouldShowDashboardWhenPreviousStageInstanceIsReRun() {
        // 2 -> (s1, passed), (s2, passed)
        // 1 -> (s1, running), (s2, passed)

        PipelineConfig pipelineConfig = configHelper.addPipeline(PipelineConfigMother.createPipelineConfigWithStages("a-pipeline", "a-stage", "b-stage"));
        goConfigService.forceNotifyListeners();

        Pipeline p1 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline p2 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);

        scheduleService.rerunStage(pipelineConfig.getName().toString(), p1.getCounter(), pipelineConfig.getFirstStageConfig().name().toString());

        List<GoDashboardPipeline> goDashboardPipelines = goDashboardCurrentStateLoader.allPipelines(goConfigService.currentCruiseConfig());
        assertThat(goDashboardPipelines).hasSize(1);

        GoDashboardPipeline goDashboardPipeline = goDashboardPipelines.getFirst();

        // latest pipeline run
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("a-stage").getResult()).isEqualTo(StageResult.Passed);
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("a-stage").getState()).isEqualTo(StageState.Passed);
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("b-stage").getResult()).isEqualTo(StageResult.Passed);
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("b-stage").getState()).isEqualTo(StageState.Passed);

        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().stage("a-stage").getResult()).isEqualTo(StageResult.Passed);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().stage("a-stage").getState()).isEqualTo(StageState.Passed);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().stage("b-stage").getResult()).isEqualTo(StageResult.Passed);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().stage("b-stage").getState()).isEqualTo(StageState.Passed);

        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().getId()).isEqualTo(p2.getId());

        // one previous pipeline run
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getLast().stage("a-stage").getResult()).isEqualTo(StageResult.Unknown);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getLast().stage("a-stage").getState()).isEqualTo(StageState.Building);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getLast().stage("b-stage").getResult()).isEqualTo(StageResult.Passed);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getLast().stage("b-stage").getState()).isEqualTo(StageState.Passed);

        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getLast().getId()).isEqualTo(p1.getId());
    }

    @Test
    public void shouldShowLatestPipelineRunWhenNoStagesAreRunning() {
        // 2 -> (s1, passed), (s2, passed)
        // 1 -> (s1, passed), (s2, passed)

        PipelineConfig pipelineConfig = configHelper.addPipeline(PipelineConfigMother.createPipelineConfigWithStages("a-pipeline", "a-stage", "b-stage"));
        goConfigService.forceNotifyListeners();

        Pipeline p1 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline p2 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);

        List<GoDashboardPipeline> goDashboardPipelines = goDashboardCurrentStateLoader.allPipelines(goConfigService.currentCruiseConfig());
        assertThat(goDashboardPipelines).hasSize(1);

        GoDashboardPipeline goDashboardPipeline = goDashboardPipelines.getFirst();

        // latest pipeline run

        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().getId()).isEqualTo(p2.getId());

        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("a-stage").getResult()).isEqualTo(StageResult.Passed);
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("a-stage").getState()).isEqualTo(StageState.Passed);
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("b-stage").getResult()).isEqualTo(StageResult.Passed);
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("b-stage").getState()).isEqualTo(StageState.Passed);

        assertThat(goDashboardPipeline.model().getActivePipelineInstances().size()).isEqualTo(1);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().getId()).isEqualTo(p2.getId());
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().stage("a-stage").getResult()).isEqualTo(StageResult.Passed);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().stage("a-stage").getState()).isEqualTo(StageState.Passed);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().stage("b-stage").getResult()).isEqualTo(StageResult.Passed);
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().getFirst().stage("b-stage").getState()).isEqualTo(StageState.Passed);
    }
}
