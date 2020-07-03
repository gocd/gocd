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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.security.GoConfigPipelinePermissionsAuthority;
import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.config.security.permissions.EveryonePermission;
import com.thoughtworks.go.config.security.permissions.NoOnePermission;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.config.security.users.NoOne;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.presentation.pipelinehistory.*;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import com.thoughtworks.go.server.service.PipelineLockService;
import com.thoughtworks.go.server.service.PipelinePauseService;
import com.thoughtworks.go.server.service.PipelineUnlockApiService;
import com.thoughtworks.go.server.service.SchedulingCheckerService;
import com.thoughtworks.go.util.Clock;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.function.Predicate;

import static com.thoughtworks.go.config.CaseInsensitiveString.str;
import static com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels.createPipelineInstanceModels;
import static com.thoughtworks.go.presentation.pipelinehistory.PreparingToScheduleInstance.PreparingToScheduleBuildCause;
import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
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
    @Mock
    private GoConfigPipelinePermissionsAuthority permissionsAuthority;

    private GoConfigMother goConfigMother;
    private CruiseConfig config;
    private static final String COUNTER = "121212";

    private GoDashboardCurrentStateLoader loader;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        loader = new GoDashboardCurrentStateLoader(pipelineSqlMapDao, triggerMonitor, pipelinePauseService,
                pipelineLockService, pipelineUnlockApiService, schedulingCheckerService, permissionsAuthority, new TimeStampBasedCounter(mock(Clock.class)));

        goConfigMother = new GoConfigMother();
        config = goConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldMatchExistingPipelinesInConfigWithAllLoadedActivePipelines() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfig p2Config = goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2", "job2");
        PipelineInstanceModel pimForP1 = pim(p1Config);
        PipelineInstanceModel pimForP2 = pim(p2Config);

        when(pipelineSqlMapDao.loadHistoryForDashboard(Arrays.asList("pipeline1", "pipeline2"))).thenReturn(createPipelineInstanceModels(pimForP1, pimForP2));

        List<GoDashboardPipeline> models = loader.allPipelines(config);

        assertThat(models.size(), is(2));
        assertModel(models.get(1), "group1", pimForP1);  /* Pipeline is actually added in reverse order. */
        assertModel(models.get(0), "group2", pimForP2);
    }

    @Test
    public void shouldIgnoreActivePipelineModelsNotInConfig() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfig pipelineWhichIsNotInConfig = PipelineConfigMother.pipelineConfig("pipelineWhichIsNotInConfig");

        PipelineInstanceModel pimForP1 = pim(p1Config);
        when(pipelineSqlMapDao.loadHistoryForDashboard(CaseInsensitiveString.toStringList(config.getAllPipelineNames()))).thenReturn(createPipelineInstanceModels(pimForP1, pim(pipelineWhichIsNotInConfig)));

        List<GoDashboardPipeline> models = loader.allPipelines(config);

        assertThat(models.size(), is(1));
        assertModel(models.get(0), "group1", pimForP1);
    }

    @Test
    public void shouldHaveASpecialModelForAPipelineWhichIsTriggeredButNotYetActive_DueToMaterialCheckTakingTime() throws Exception {
        goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");

        when(pipelineSqlMapDao.loadHistoryForDashboard(CaseInsensitiveString.toStringList(config.getAllPipelineNames()))).thenReturn(createPipelineInstanceModels());
        when(triggerMonitor.isAlreadyTriggered(new CaseInsensitiveString("pipeline1"))).thenReturn(true);

        List<GoDashboardPipeline> models = loader.allPipelines(config);
        assertThat(models.size(), is(1));
        assertThat(models.get(0).groupName(), is("group1"));
        assertThat(models.get(0).model().getName(), is("pipeline1"));

        PipelineModel model = models.get(0).model();
        assertThat(model.getActivePipelineInstances().size(), is(1));

        PipelineInstanceModel specialPIM = model.getLatestPipelineInstance();
        assertThat(specialPIM.getName(), is("pipeline1"));
        assertThat(specialPIM.getCanRun(), is(false));
        assertThat(specialPIM.isPreparingToSchedule(), is(true));
        assertThat(specialPIM.getCounter(), is(-1));
        assertThat(specialPIM.getBuildCause(), is(new PreparingToScheduleBuildCause()));
        assertStages(specialPIM, "stage1");
    }

    @Test
    public void shouldFallBackToAnEmptyPipelineInstanceModelIfItCannotBeLoadedEvenFromHistory() throws Exception {
        goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");

        when(triggerMonitor.isAlreadyTriggered(new CaseInsensitiveString("pipeline1"))).thenReturn(false);
        when(pipelineSqlMapDao.loadHistoryForDashboard(singletonList("pipeline1"))).thenReturn(createPipelineInstanceModels());

        List<GoDashboardPipeline> models = loader.allPipelines(config);
        assertThat(models.size(), is(1));
        assertThat(models.get(0).groupName(), is("group1"));
        assertThat(models.get(0).model().getName(), is("pipeline1"));

        PipelineModel model = models.get(0).model();
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
        when(pipelineSqlMapDao.loadHistoryForDashboard(CaseInsensitiveString.toStringList(config.getAllPipelineNames()))).thenReturn(createPipelineInstanceModels(firstInstance, secondInstance));

        List<GoDashboardPipeline> models = loader.allPipelines(config);

        PipelineModel model = models.get(0).model();
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

        when(pipelineSqlMapDao.loadHistoryForDashboard(Arrays.asList("pipeline1", "pipeline2"))).thenReturn(createPipelineInstanceModels(pimForP1, pimForP2));


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

        when(pipelineSqlMapDao.loadHistoryForDashboard(Arrays.asList("pipeline1", "pipeline2"))).thenReturn(createPipelineInstanceModels(pim(p1Config), pim(p2Config)));
        when(pipelinePauseService.pipelinePauseInfo("pipeline1")).thenReturn(pipeline1PauseInfo);
        when(pipelinePauseService.pipelinePauseInfo("pipeline2")).thenReturn(pipeline2PauseInfo);

        List<GoDashboardPipeline> models = loader.allPipelines(config);

        assertThat(models.size(), is(2));
        assertThat(models.get(1).model().getPausedInfo(), is(pipeline1PauseInfo));
        assertThat(models.get(0).model().getPausedInfo(), is(pipeline2PauseInfo));
    }

    /* TODO: Even though the test is right, the correct place for lock info is pipeline level, not PIM level */
    @Test
    public void shouldAddPipelineLockInformationAtPipelineInstanceLevel() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        p1Config.lockExplicitly();

        PipelineConfig p2Config = goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2", "job2");

        when(pipelineSqlMapDao.loadHistoryForDashboard(Arrays.asList("pipeline1", "pipeline2"))).thenReturn(createPipelineInstanceModels(pim(p1Config), pim(p2Config)));
        when(pipelineLockService.isLocked("pipeline1")).thenReturn(true);
        when(pipelineUnlockApiService.isUnlockable("pipeline1")).thenReturn(true);

        when(pipelineLockService.isLocked("pipeline2")).thenReturn(false);
        when(pipelineUnlockApiService.isUnlockable("pipeline2")).thenReturn(false);

        List<GoDashboardPipeline> models = loader.allPipelines(config);

        PipelineModel modelForPipeline1 = models.get(1).model();
        assertThat(modelForPipeline1.getLatestPipelineInstance().isLockable(), is(true));
        assertThat(modelForPipeline1.getLatestPipelineInstance().isCurrentlyLocked(), is(true));
        assertThat(modelForPipeline1.getLatestPipelineInstance().canUnlock(), is(true));

        PipelineModel modelForPipeline2 = models.get(0).model();
        assertThat(modelForPipeline2.getLatestPipelineInstance().isLockable(), is(false));
        assertThat(modelForPipeline2.getLatestPipelineInstance().isCurrentlyLocked(), is(false));
        assertThat(modelForPipeline2.getLatestPipelineInstance().canUnlock(), is(false));
    }

    @Test
    public void shouldUpdateAdministrabilityOfAPipelineBasedOnItsOrigin() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        p1Config.setOrigin(new FileConfigOrigin());

        PipelineConfig p2Config = goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2", "job2");
        p2Config.setOrigin(new RepoConfigOrigin());

        when(pipelineSqlMapDao.loadHistoryForDashboard(Arrays.asList("pipeline1", "pipeline2"))).thenReturn(createPipelineInstanceModels(pim(p1Config), pim(p2Config)));

        List<GoDashboardPipeline> models = loader.allPipelines(config);

        assertThat(models.get(1).model().canAdminister(), is(true));
        assertThat(models.get(0).model().canAdminister(), is(false));
    }

    @Test
    public void shouldAddPipelineSchedulabilityInformationAtPipelineLevel() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfig p2Config = goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2", "job2");

        when(pipelineSqlMapDao.loadHistoryForDashboard(Arrays.asList("pipeline1", "pipeline2"))).thenReturn(createPipelineInstanceModels(pim(p1Config), pim(p2Config)));
        when(schedulingCheckerService.pipelineCanBeTriggeredManually(p1Config)).thenReturn(true);
        when(schedulingCheckerService.pipelineCanBeTriggeredManually(p2Config)).thenReturn(false);

        List<GoDashboardPipeline> models = loader.allPipelines(config);

        assertThat(models.get(1).model().canForce(), is(true));
        assertThat(models.get(0).model().canForce(), is(false));
    }

    @Test
    public void shouldAssociateEveryPipelineWithItsPermissions() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfig p2Config = goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2", "job2");
        PipelineInstanceModel pimForP1 = pim(p1Config);
        PipelineInstanceModel pimForP2 = pim(p2Config);

        Permissions permissionsForP1 = new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, EveryonePermission.INSTANCE);
        Permissions permissionsForP2 = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, Everyone.INSTANCE, NoOnePermission.INSTANCE);

        when(pipelineSqlMapDao.loadHistoryForDashboard(Arrays.asList("pipeline1", "pipeline2"))).thenReturn(createPipelineInstanceModels(pimForP1, pimForP2));
        when(permissionsAuthority.pipelinesAndTheirPermissions()).thenReturn(m(p1Config.name(), permissionsForP1, p2Config.name(), permissionsForP2));

        List<GoDashboardPipeline> models = loader.allPipelines(config);

        assertThat(models.get(1).permissions(), is(permissionsForP1));
        assertThat(models.get(0).permissions(), is(permissionsForP2));
    }

    @Test
    public void shouldDefaultToAllowingNoOneToViewAPipelineIfItsPermissionsAreNotFound() throws Exception {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineInstanceModel pimForP1 = pim(p1Config);

        when(pipelineSqlMapDao.loadHistoryForDashboard(CaseInsensitiveString.toStringList(config.getAllPipelineNames()))).thenReturn(createPipelineInstanceModels(pimForP1));
        when(permissionsAuthority.pipelinesAndTheirPermissions()).thenReturn(Collections.emptyMap());

        List<GoDashboardPipeline> models = loader.allPipelines(config);

        Permissions permissions = models.get(0).permissions();
        assertThat(permissions.viewers(), is(NoOne.INSTANCE));
        assertThat(permissions.operators(), is(NoOne.INSTANCE));
        assertThat(permissions.admins(), is(NoOne.INSTANCE));
        assertThat(permissions.pipelineOperators(), is(NoOne.INSTANCE));
    }

    @Test
    public void shouldGetAGoDashboardPipelineGivenASinglePipelineConfigAndItsGroupConfig() throws Exception {
        String pipelineNameStr = "pipeline1";
        CaseInsensitiveString pipelineName = new CaseInsensitiveString(pipelineNameStr);
        Permissions permissions = new Permissions(Everyone.INSTANCE, NoOne.INSTANCE, Everyone.INSTANCE, NoOnePermission.INSTANCE);
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", pipelineNameStr, "stage1", "job1");
        PipelineInstanceModels pipelineInstanceModels = createPipelineInstanceModels(pim(p1Config));

        when(pipelineSqlMapDao.loadHistoryForDashboard(Arrays.asList(pipelineNameStr))).thenReturn(pipelineInstanceModels);
        when(permissionsAuthority.permissionsForPipeline(pipelineName)).thenReturn(permissions);

        GoDashboardPipeline pipeline = loader.pipelineFor(p1Config, config.findGroup("group1"));

        assertThat(pipeline.name(), is(pipelineName));
        assertThat(pipeline.permissions(), is(permissions));
        assertThat(pipeline.model().getActivePipelineInstances(), is(pipelineInstanceModels));

        verify(pipelineSqlMapDao).loadHistoryForDashboard(Arrays.asList(pipelineNameStr));
        verifyNoMoreInteractions(pipelineSqlMapDao);
    }

    @Test
    public void hasEverLoadedCurrentStateIsTrueAfterLoading() {
        assertThat(loader.hasEverLoadedCurrentState(), is(false));
        loader.allPipelines(new BasicCruiseConfig());
        assertThat(loader.hasEverLoadedCurrentState(), is(true));
    }

    @Test
    public void shouldAddTrackingToolInfoWhenLoadingAllPipelines() {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        TrackingTool trackingTool = new TrackingTool("http://example.com/${ID}", "\\d+");
        p1Config.setTrackingTool(trackingTool);
        PipelineInstanceModel pimForP1 = pim(p1Config);
        when(pipelineSqlMapDao.loadHistoryForDashboard(CaseInsensitiveString.toStringList(config.getAllPipelineNames()))).thenReturn(createPipelineInstanceModels(pimForP1));

        List<GoDashboardPipeline> models = loader.allPipelines(config);

        assertThat(models.get(0).getTrackingTool(), is(Optional.of(trackingTool)));
    }

    @Test
    public void shouldAddTrackingToolInfoWhenLoadingAPipeline() {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        TrackingTool trackingTool = new TrackingTool("http://example.com/${ID}", "\\d+");
        p1Config.setTrackingTool(trackingTool);
        PipelineInstanceModel pimForP1 = pim(p1Config);
        when(pipelineSqlMapDao.loadHistoryForDashboard(Arrays.asList(p1Config.getName().toString()))).thenReturn(createPipelineInstanceModels(pimForP1));

        GoDashboardPipeline model = loader.pipelineFor(p1Config, config.findGroup("group1"));

        assertThat(model.getTrackingTool(), is(Optional.of(trackingTool)));
    }

    @Test
    public void shouldNotReloadFromDBIfListOfPipelinesHasNotChanged() {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineInstanceModel pimForP1 = pim(p1Config);

        when(pipelineSqlMapDao.loadHistoryForDashboard(CaseInsensitiveString.toStringList(p1Config.getName()))).thenReturn(createPipelineInstanceModels(pimForP1));

        loader.allPipelines(config);
        goConfigMother.addStageToPipeline(config, p1Config.getName().toString(), "someStage", "someJob");
        loader.allPipelines(config);

        verify(pipelineSqlMapDao, times(1)).loadHistoryForDashboard(CaseInsensitiveString.toStringList(p1Config.getName()));
    }

    @Test
    public void shouldLoadFromDBPipelinesThatHaveBeenAdded() {
        PipelineConfig p1Config = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");

        when(pipelineSqlMapDao.loadHistoryForDashboard(any())).thenAnswer(new Answer<PipelineInstanceModels>() {
            @Override
            public PipelineInstanceModels answer(InvocationOnMock invocation) throws Throwable {
                List<String> pipelineNames = invocation.getArgument(0);
                List<PipelineInstanceModel> models = new ArrayList<>();
                for (String pipelineName : pipelineNames) {
                    PipelineInstanceModel pim = pim(config.getPipelineConfigByName(new CaseInsensitiveString(pipelineName)));
                    models.add(pim);
                }
                return PipelineInstanceModels.createPipelineInstanceModels(models);
            }
        });

        loader.allPipelines(config.cloneForValidation());

        PipelineConfig p2Config = goConfigMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1", "job1");
        config.findGroup("group1").remove(p1Config);

        loader.allPipelines(config.cloneForValidation());
        verify(pipelineSqlMapDao, times(1)).loadHistoryForDashboard(CaseInsensitiveString.toStringList(p1Config.getName()));
        verify(pipelineSqlMapDao, times(1)).loadHistoryForDashboard(CaseInsensitiveString.toStringList(p2Config.getName()));
        verifyNoMoreInteractions(pipelineSqlMapDao);
    }

    @Test
    public void shouldHandlePipelineDeletion() {
        PipelineConfig pipeline1 = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1", "job1");
        PipelineConfig pipeline2 = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline2", "stage1", "job1");
        PipelineConfig pipeline3 = goConfigMother.addPipelineWithGroup(config, "group1", "pipeline3", "stage1", "job1");
        when(pipelineSqlMapDao.loadHistoryForDashboard(ArgumentMatchers.any(List.class))).thenReturn(PipelineInstanceModels.createPipelineInstanceModels());
        List<GoDashboardPipeline> goDashboardPipelines = loader.allPipelines(config);
        assertThat(goDashboardPipelines, hasSize(3));

        for (CaseInsensitiveString pipelineName : Arrays.asList(pipeline1.name(), pipeline2.name(), pipeline3.name())) {
            long matches = goDashboardPipelines.stream().filter(new Predicate<GoDashboardPipeline>() {
                @Override
                public boolean test(GoDashboardPipeline goDashboardPipeline) {
                    return pipelineName.equals(goDashboardPipeline.name());
                }
            }).count();
            assertThat(matches, is(1L));

        }

        config.deletePipeline(pipeline1);
        config.deletePipeline(pipeline2);
        config.getAllPipelineConfigs().remove(pipeline1);
        config.getAllPipelineConfigs().remove(pipeline2);
        goDashboardPipelines = loader.allPipelines(config);
        assertThat(goDashboardPipelines, hasSize(1));
        assertThat(goDashboardPipelines.get(0).name(), is(pipeline3.name()));
    }

    private void assertModel(GoDashboardPipeline pipeline, String group, PipelineInstanceModel... pims) {
        assertThat(pipeline.groupName(), is(group));
        assertThat(pipeline.model().getName(), is(pims[0].getName()));
        assertThat(pipeline.model().getActivePipelineInstances(), is(a(pims)));
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
