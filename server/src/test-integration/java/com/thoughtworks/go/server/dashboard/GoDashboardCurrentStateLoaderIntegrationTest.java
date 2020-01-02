/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.ScheduleService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.List;

import static com.thoughtworks.go.helper.ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
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

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private SystemEnvironment systemEnvironment = new SystemEnvironment();
    @Autowired
    private ScheduleService scheduleService;

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        File configDir = temporaryFolder.newFolder();
        String absolutePath = new File(configDir, "cruise-config.xml").getAbsolutePath();
        systemEnvironment.setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, absolutePath);
        configHelper = new GoConfigFileHelper(DEFAULT_XML_WITH_2_AGENTS);
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();

        goConfigService.forceNotifyListeners();
        goDashboardCurrentStateLoader.reset();
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldReturnEmptyWhenNoPipelinesArePresentInConfig() throws Exception {
        goConfigService.forceNotifyListeners();
        assertThat(goConfigService.getAllPipelineConfigs(), is(empty()));
        List<GoDashboardPipeline> goDashboardPipelines = goDashboardCurrentStateLoader.allPipelines(goConfigService.currentCruiseConfig());
        assertThat(goDashboardPipelines, is(empty()));
    }

    @Test
    public void shouldReturnSingleDashboardForSingleCompletedGreenPipelineInstance() throws Exception {
        PipelineConfig pipelineConfig = configHelper.addPipeline(PipelineConfigMother.createPipelineConfigWithStages("a-pipeline", "a-stage"));
        goConfigService.forceNotifyListeners();

        Pipeline pipeline = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);

        List<GoDashboardPipeline> goDashboardPipelines = goDashboardCurrentStateLoader.allPipelines(goConfigService.currentCruiseConfig());

        assertThat(goDashboardPipelines, hasSize(1));
        GoDashboardPipeline goDashboardPipeline = goDashboardPipelines.get(0);

        assertThat(goDashboardPipeline.name(), is(pipelineConfig.name()));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().size(), is(1));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0), equalTo(goDashboardPipeline.model().getLatestPipelineInstance()));
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().getId(), is(pipeline.getId()));
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage(pipelineConfig.getStages().first().name().toString()).getId(), is(pipeline.getFirstStage().getId()));
    }

    @Test
    public void shouldShowMultiplePipelineInstancesFromSamePipelineWhenMultipleAreRunning() throws Exception {
        // 2 -> (s1, running)
        // 1 -> (s1, passed), (s2, running)

        PipelineConfig pipelineConfig = configHelper.addPipeline(PipelineConfigMother.createPipelineConfigWithStages("a-pipeline", "a-stage", "b-stage"));
        goConfigService.forceNotifyListeners();

        Pipeline p1 = dbHelper.newPipelineWithFirstStagePassed(pipelineConfig);
        dbHelper.scheduleStage(p1, pipelineConfig.getStage("b-stage"));
        Pipeline p2 = dbHelper.newPipelineWithFirstStageScheduled(pipelineConfig);

        List<GoDashboardPipeline> goDashboardPipelines = goDashboardCurrentStateLoader.allPipelines(goConfigService.currentCruiseConfig());
        assertThat(goDashboardPipelines, hasSize(1));

        GoDashboardPipeline goDashboardPipeline = goDashboardPipelines.get(0);

        assertThat(goDashboardPipeline.name(), is(pipelineConfig.name()));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().size(), is(2));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).getId(), is(p2.getId()));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(1).getId(), is(p1.getId()));

        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().getId(), is(p2.getId()));

        // latest pipeline run
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("a-stage").getResult(), is(StageResult.Unknown));
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("a-stage").getState(), is(StageState.Building));
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("b-stage"), is(instanceOf(NullStageHistoryItem.class)));

        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).stage("a-stage").getResult(), is(StageResult.Unknown));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).stage("a-stage").getState(), is(StageState.Building));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).stage("b-stage"), is(instanceOf(NullStageHistoryItem.class)));

        // one previous pipeline run
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(1).stage("a-stage").getResult(), is(StageResult.Passed));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(1).stage("a-stage").getState(), is(StageState.Passed));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(1).stage("b-stage").getResult(), is(StageResult.Unknown));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(1).stage("b-stage").getState(), is(StageState.Building));
    }

    @Test
    public void shouldReturnNothingIfPipelineHasNeverRun() throws Exception {
        PipelineConfig pipelineConfig = configHelper.addPipeline(PipelineConfigMother.createPipelineConfigWithStages("a-pipeline", "a-stage", "b-stage"));
        goConfigService.forceNotifyListeners();

        List<GoDashboardPipeline> goDashboardPipelines = goDashboardCurrentStateLoader.allPipelines(goConfigService.currentCruiseConfig());
        assertThat(goDashboardPipelines, hasSize(1));

        GoDashboardPipeline goDashboardPipeline = goDashboardPipelines.get(0);

        assertThat(goDashboardPipeline.model().getActivePipelineInstances().size(), is(1));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0), is(instanceOf(EmptyPipelineInstanceModel.class)));
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance(), is(instanceOf(EmptyPipelineInstanceModel.class)));
    }

    @Test
    public void shouldShowDashboardWhenPreviousStageInstanceIsReRun() throws Exception {
        // 2 -> (s1, passed), (s2, passed)
        // 1 -> (s1, running), (s2, passed)

        PipelineConfig pipelineConfig = configHelper.addPipeline(PipelineConfigMother.createPipelineConfigWithStages("a-pipeline", "a-stage", "b-stage"));
        goConfigService.forceNotifyListeners();

        Pipeline p1 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline p2 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);

        scheduleService.rerunStage(pipelineConfig.getName().toString(), p1.getCounter(), pipelineConfig.getFirstStageConfig().name().toString());

        List<GoDashboardPipeline> goDashboardPipelines = goDashboardCurrentStateLoader.allPipelines(goConfigService.currentCruiseConfig());
        assertThat(goDashboardPipelines, hasSize(1));

        GoDashboardPipeline goDashboardPipeline = goDashboardPipelines.get(0);

        // latest pipeline run
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("a-stage").getResult(), is(StageResult.Passed));
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("a-stage").getState(), is(StageState.Passed));
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("b-stage").getResult(), is(StageResult.Passed));
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("b-stage").getState(), is(StageState.Passed));

        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).stage("a-stage").getResult(), is(StageResult.Passed));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).stage("a-stage").getState(), is(StageState.Passed));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).stage("b-stage").getResult(), is(StageResult.Passed));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).stage("b-stage").getState(), is(StageState.Passed));

        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).getId(), is(p2.getId()));

        // one previous pipeline run
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(1).stage("a-stage").getResult(), is(StageResult.Unknown));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(1).stage("a-stage").getState(), is(StageState.Building));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(1).stage("b-stage").getResult(), is(StageResult.Passed));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(1).stage("b-stage").getState(), is(StageState.Passed));

        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(1).getId(), is(p1.getId()));
    }

    @Test
    public void shouldShowLatestPipelineRunWhenNoStagesAreRunning() throws Exception {
        // 2 -> (s1, passed), (s2, passed)
        // 1 -> (s1, passed), (s2, passed)

        PipelineConfig pipelineConfig = configHelper.addPipeline(PipelineConfigMother.createPipelineConfigWithStages("a-pipeline", "a-stage", "b-stage"));
        goConfigService.forceNotifyListeners();

        Pipeline p1 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);
        Pipeline p2 = dbHelper.newPipelineWithAllStagesPassed(pipelineConfig);

        List<GoDashboardPipeline> goDashboardPipelines = goDashboardCurrentStateLoader.allPipelines(goConfigService.currentCruiseConfig());
        assertThat(goDashboardPipelines, hasSize(1));

        GoDashboardPipeline goDashboardPipeline = goDashboardPipelines.get(0);

        // latest pipeline run

        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().getId(), is(p2.getId()));

        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("a-stage").getResult(), is(StageResult.Passed));
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("a-stage").getState(), is(StageState.Passed));
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("b-stage").getResult(), is(StageResult.Passed));
        assertThat(goDashboardPipeline.model().getLatestPipelineInstance().stage("b-stage").getState(), is(StageState.Passed));

        assertThat(goDashboardPipeline.model().getActivePipelineInstances().size(), is(1));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).getId(), is(p2.getId()));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).stage("a-stage").getResult(), is(StageResult.Passed));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).stage("a-stage").getState(), is(StageState.Passed));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).stage("b-stage").getResult(), is(StageResult.Passed));
        assertThat(goDashboardPipeline.model().getActivePipelineInstances().get(0).stage("b-stage").getState(), is(StageState.Passed));
    }
}
