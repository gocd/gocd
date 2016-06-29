/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.presentation.pipelinehistory.*;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import org.hamcrest.Matcher;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static com.thoughtworks.go.config.CaseInsensitiveString.str;
import static com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels.createPipelineInstanceModels;
import static com.thoughtworks.go.util.DataStructureUtils.a;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class GoDashboardCurrentStateLoaderTest {
    @Mock
    private PipelineSqlMapDao pipelineSqlMapDao;
    @Mock
    private TriggerMonitor triggerMonitor;
    @Mock
    private PipelinePauseService pipelinePauseService;
    @Mock
    private PipelineLockService pipelineLockService;
    @Mock
    private PipelineUnlockApiService pipelineUnlockApiService;
    @Mock
    private SchedulingCheckerService schedulingCheckerService;

    private GoConfigMother goConfigMother;
    private CruiseConfig config;
    private static final String COUNTER = "121212";

    private GoDashboardCurrentStateLoader loader;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        loader = new GoDashboardCurrentStateLoader(pipelineSqlMapDao, triggerMonitor, pipelinePauseService,
                pipelineLockService, pipelineUnlockApiService, schedulingCheckerService);

        goConfigMother = new GoConfigMother();
        config = goConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldMatchExistingPipelinesInConfigWithAllLoadedActivePipelines() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfig p2Config = goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2", "job2");
        PipelineInstanceModel pimForP1 = pim(p1Config);
        PipelineInstanceModel pimForP2 = pim(p2Config);

        when(pipelineSqlMapDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels(pimForP1, pimForP2));

        List<PipelineGroupModel> models = loader.allPipelines(config);

        assertThat(models.size(), is(2));
        assertModel(models.get(1), "group1", pimForP1);  /* Pipeline is actually added in reverse order. */
        assertModel(models.get(0), "group2", pimForP2);
    }

    @Test
    public void shouldIgnoreActivePipelineModelsNotInConfig() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfig pipelineWhichIsNotInConfig = PipelineConfigMother.pipelineConfig("pipelineWhichIsNotInConfig");

        PipelineInstanceModel pimForP1 = pim(p1Config);
        when(pipelineSqlMapDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels(pimForP1, pim(pipelineWhichIsNotInConfig)));

        List<PipelineGroupModel> models = loader.allPipelines(config);

        assertThat(models.size(), is(1));
        assertModel(models.get(0), "group1", pimForP1);
    }

    @Test
    public void shouldHaveASpecialModelForAPipelineWhichIsTriggeredButNotYetActive_DueToMaterialCheckTakingTime() throws Exception {
        goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");

        when(pipelineSqlMapDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels());
        when(triggerMonitor.isAlreadyTriggered("pipeline1")).thenReturn(true);

        List<PipelineGroupModel> groupModels = loader.allPipelines(config);
        assertThat(groupModels.size(), is(1));
        assertThat(groupModels.get(0).getName(), is("group1"));
        assertThat(groupModels.get(0).getPipelineModels().size(), is(1));

        PipelineModel model = groupModels.get(0).getPipelineModel("pipeline1");
        assertThat(model.getActivePipelineInstances().size(), is(1));

        PipelineInstanceModel specialPIM = model.getLatestPipelineInstance();
        assertThat(specialPIM.getName(), is("pipeline1"));
        assertThat(specialPIM.getCanRun(), is(false));
        assertThat(specialPIM.isPreparingToSchedule(), is(true));
        assertThat(specialPIM.getCounter(), is(-1));
        assertThat(specialPIM.getBuildCause(), Is.<BuildCause>is(new PreparingToScheduleInstance.PreparingToScheduleBuildCause()));
        assertStages(specialPIM, "stage1");
    }

    @Test
    public void shouldTryToLoadFromHistoryIfAPipelineIsNotFoundInLoadedActivePipelines_AndIsNotAlreadyTriggered() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfig p2Config = goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2", "job2");
        PipelineInstanceModel pimForP1 = pim(p1Config);
        PipelineInstanceModel pimForP2 = pim(p2Config);

        when(pipelineSqlMapDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels(pimForP1));
        when(triggerMonitor.isAlreadyTriggered("pipeline2")).thenReturn(false);
        when(pipelineSqlMapDao.loadHistory("pipeline2", 1, 0)).thenReturn(createPipelineInstanceModels(pimForP2));

        List<PipelineGroupModel> models = loader.allPipelines(config);

        assertThat(models.size(), is(2));
        assertModel(models.get(1), "group1", pimForP1);
        assertModel(models.get(0), "group2", pimForP2);
    }

    @Test
    public void shouldFallBackToAnEmptyPipelineInstanceModelIfItCannotBeLoadedEvenFromHistory() throws Exception {
        goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");

        when(pipelineSqlMapDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels());
        when(triggerMonitor.isAlreadyTriggered("pipeline1")).thenReturn(false);
        when(pipelineSqlMapDao.loadHistory("pipeline1", 1, 0)).thenReturn(createPipelineInstanceModels());


        List<PipelineGroupModel> groupModels = loader.allPipelines(config);
        assertThat(groupModels.size(), is(1));
        assertThat(groupModels.get(0).getName(), is("group1"));
        assertThat(groupModels.get(0).getPipelineModels().size(), is(1));

        PipelineModel model = groupModels.get(0).getPipelineModel("pipeline1");
        assertThat(model.getActivePipelineInstances().size(), is(1));

        PipelineInstanceModel emptyPIM = model.getLatestPipelineInstance();
        assertThat(emptyPIM.getName(), is("pipeline1"));
        assertThat(emptyPIM.hasHistoricalData(), is(false));
        assertThat(emptyPIM.isPreparingToSchedule(), is(false));
        assertThat(emptyPIM.getCounter(), is(0));
        assertThat(emptyPIM.getBuildCause(), is(BuildCause.createWithEmptyModifications()));
        assertStages(emptyPIM, "stage1");
    }

    @Test
    public void shouldAllowASinglePipelineInConfigToHaveMultipleInstances() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");

        PipelineInstanceModel firstInstance = pim(p1Config);
        PipelineInstanceModel secondInstance = pim(p1Config);
        when(pipelineSqlMapDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels(firstInstance, secondInstance));

        List<PipelineGroupModel> groupModels = loader.allPipelines(config);

        PipelineModel model = groupModels.get(0).getPipelineModel("pipeline1");

        assertThat(model.getActivePipelineInstances().size(), is(2));
        assertThat(model.getActivePipelineInstances().get(0), is(firstInstance));
        assertThat(model.getActivePipelineInstances().get(1), is(secondInstance));
    }

    @Test
    public void shouldAddStagesWhichHaveNotYetRunIntoEachInstanceOfAPipeline_FromConfig() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stageP1_S1", "jobP1_S1_J1");
        goConfigMother.addStageToPipeline(config, "pipeline1", "stageP1_S2", "jobP1_S2_J1");

        PipelineConfig p2Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline2", "stageP2_S1", "jobP2_S1_J1");
        goConfigMother.addStageToPipeline(config, "pipeline2", "stageP2_S2", "jobP2_S2_J1");
        goConfigMother.addStageToPipeline(config, "pipeline2", "stageP2_S3", "jobP2_S3_J1");

        PipelineInstanceModel pimForP1 = pim(p1Config);
        pimForP1.getStageHistory().add(new StageInstanceModel("stageP1_S2", COUNTER, new JobHistory()));

        PipelineInstanceModel pimForP2 = pim(p2Config);

        when(pipelineSqlMapDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels(pimForP1, pimForP2));


        loader.allPipelines(config);

        assertStages(pimForP1, "stageP1_S1", "stageP1_S2");
        assertStages(pimForP2, "stageP2_S1", "stageP2_S2", "stageP2_S3");

        assertThat(pimForP1.getStageHistory().get(0).getCounter(), is(COUNTER));
        assertThat(pimForP1.getStageHistory().get(1).getCounter(), is(COUNTER));

        Matcher<String> counterForAStageAddedFromConfig = not(COUNTER);
        assertThat(pimForP2.getStageHistory().get(0).getCounter(), is(COUNTER));
        assertThat(pimForP2.getStageHistory().get(1).getCounter(), is(counterForAStageAddedFromConfig));
        assertThat(pimForP2.getStageHistory().get(2).getCounter(), is(counterForAStageAddedFromConfig));
    }

    @Test
    public void shouldAddPipelinePauseInfoAtPipelineLevel() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfig p2Config = goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2", "job2");

        PipelinePauseInfo pipeline1PauseInfo = PipelinePauseInfo.notPaused();
        PipelinePauseInfo pipeline2PauseInfo = PipelinePauseInfo.paused("Reason 1", "user1");

        when(pipelineSqlMapDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels(pim(p1Config), pim(p2Config)));
        when(pipelinePauseService.pipelinePauseInfo("pipeline1")).thenReturn(pipeline1PauseInfo);
        when(pipelinePauseService.pipelinePauseInfo("pipeline2")).thenReturn(pipeline2PauseInfo);

        List<PipelineGroupModel> models = loader.allPipelines(config);

        assertThat(models.size(), is(2));
        assertThat(models.get(1).getPipelineModel("pipeline1").getPausedInfo(), is(pipeline1PauseInfo));
        assertThat(models.get(0).getPipelineModel("pipeline2").getPausedInfo(), is(pipeline2PauseInfo));
    }

    /* TODO: Even though the test is right, the correct place for lock info is pipeline level, not PIM level */
    @Test
    public void shouldAddPipelineLockInformationAtPipelineInstanceLevel() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        p1Config.lockExplicitly();

        PipelineConfig p2Config = goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2", "job2");

        when(pipelineSqlMapDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels(pim(p1Config), pim(p2Config)));
        when(pipelineLockService.isLocked("pipeline1")).thenReturn(true);
        when(pipelineUnlockApiService.isUnlockable("pipeline1")).thenReturn(true);

        when(pipelineLockService.isLocked("pipeline2")).thenReturn(false);
        when(pipelineUnlockApiService.isUnlockable("pipeline2")).thenReturn(false);

        List<PipelineGroupModel> models = loader.allPipelines(config);

        assertThat(models.get(1).getPipelineModel("pipeline1").getLatestPipelineInstance().isLockable(), is(true));
        assertThat(models.get(1).getPipelineModel("pipeline1").getLatestPipelineInstance().isCurrentlyLocked(), is(true));
        assertThat(models.get(1).getPipelineModel("pipeline1").getLatestPipelineInstance().canUnlock(), is(true));

        assertThat(models.get(0).getPipelineModel("pipeline2").getLatestPipelineInstance().isLockable(), is(false));
        assertThat(models.get(0).getPipelineModel("pipeline2").getLatestPipelineInstance().isCurrentlyLocked(), is(false));
        assertThat(models.get(0).getPipelineModel("pipeline2").getLatestPipelineInstance().canUnlock(), is(false));
    }

    @Test
    public void shouldUpdateAdministrabilityOfAPipelineBasedOnItsOrigin() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        p1Config.setOrigin(new FileConfigOrigin());

        PipelineConfig p2Config = goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2", "job2");
        p2Config.setOrigin(new RepoConfigOrigin());

        when(pipelineSqlMapDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels(pim(p1Config), pim(p2Config)));

        List<PipelineGroupModel> models = loader.allPipelines(config);

        assertThat(models.get(1).getPipelineModel("pipeline1").canAdminister(), is(true));
        assertThat(models.get(0).getPipelineModel("pipeline2").canAdminister(), is(false));
    }

    @Test
    public void shouldAddPipelineSchedulabilityInformationAtPipelineLevel() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfig p2Config = goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2", "job2");

        when(pipelineSqlMapDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels(pim(p1Config), pim(p2Config)));
        when(schedulingCheckerService.pipelineCanBeTriggeredManually(p1Config)).thenReturn(true);
        when(schedulingCheckerService.pipelineCanBeTriggeredManually(p2Config)).thenReturn(false);

        List<PipelineGroupModel> models = loader.allPipelines(config);

        assertThat(models.get(1).getPipelineModel("pipeline1").canForce(), is(true));
        assertThat(models.get(0).getPipelineModel("pipeline2").canForce(), is(false));
    }

    private void assertModel(PipelineGroupModel groupModel, String group, PipelineInstanceModel... pims) {
        assertThat(groupModel.getName(), is(group));
        assertThat(groupModel.getPipelineModel(pims[0].getName()).getActivePipelineInstances(), is(a(pims)));
    }

    private void assertStages(PipelineInstanceModel pim, String... stages) {
        assertThat(pim.getStageHistory().size(), is(stages.length));

        for (int i = 0; i < pim.getStageHistory().size(); i++) {
            StageInstanceModel stageInstanceModel = pim.getStageHistory().get(i);
            assertThat(stageInstanceModel.getName(), is(stages[i]));
        }
    }

    private PipelineInstanceModel pim(PipelineConfig pipelineConfig) {
        StageInstanceModels stageHistory = new StageInstanceModels();
        stageHistory.add(new StageInstanceModel(str(pipelineConfig.getFirstStageConfig().name()), COUNTER, new JobHistory()));
        return PipelineInstanceModel.createPipeline(str(pipelineConfig.name()), 123, "LABEL", BuildCause.createManualForced(), stageHistory);
    }
}