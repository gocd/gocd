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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.NotImplementedException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.PipelineHistoryMother;
import com.thoughtworks.go.helper.PipelineMaterialModificationMother;
import com.thoughtworks.go.presentation.PipelineStatusModel;
import com.thoughtworks.go.presentation.pipelinehistory.*;
import com.thoughtworks.go.server.dao.FeedModifier;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.domain.user.PipelineSelectionsHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.server.util.Pagination;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels.createPipelineInstanceModels;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class PipelineHistoryServiceTest {
    private static final CruiseConfig CRUISE_CONFIG = ConfigMigrator.loadWithMigration("<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "     xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"17\" >"
            + "<server artifactsdir=\"target/testfiles/tmpCCRoot/data/logs\"></server>"
            + "  <pipelines>"
            + "    <pipeline name='pipeline'>"
            + "        <materials>"
            + "            <svn url='ape'/>"
            + "        </materials>"
            + "        <stage name='auto'>"
            + "            <jobs>"
            + "                <job name='unit'/>"
            + "            </jobs>"
            + "        </stage>"
            + "        <stage name='manual'>"
            + "            <jobs>"
            + "                <job name='unit'/>"
            + "            </jobs>"
            + "        </stage>"
            + "    </pipeline>"
            + "  </pipelines>"
            + "</cruise>").config;

    @Mock
    private PipelineDao pipelineDao;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private SecurityService securityService;
    @Mock
    private ScheduleService scheduleService;
    @Mock
    private PipelineTimeline pipelineTimeline;
    @Mock
    private PipelineUnlockApiService pipelineUnlockService;
    @Mock
    private SchedulingCheckerService schedulingCheckerService;
    @Mock
    private PipelineLockService pipelineLockService;
    @Mock
    public PipelinePauseService pipelinePauseService;
    @Mock
    public FeatureToggleService featureToggleService;
    private PipelineHistoryService pipelineHistoryService;
    private PipelineConfig config;

    @BeforeEach
    void setUp() {
        initMocks(this);
        when(goConfigService.isPipelineEditable(any(String.class))).thenReturn(true);
        Toggles.initializeWith(featureToggleService);
        pipelineHistoryService = new PipelineHistoryService(pipelineDao, goConfigService, securityService, scheduleService,
                mock(MaterialRepository.class),
                mock(TriggerMonitor.class),
                pipelineTimeline,
                pipelineUnlockService, schedulingCheckerService, pipelineLockService, pipelinePauseService);
        config = CRUISE_CONFIG.pipelineConfigByName(new CaseInsensitiveString("pipeline"));
    }

    @Test
    void shouldUnderstandLatestPipelineModel() {
        Username username = new Username(new CaseInsensitiveString("loser"));
        String pipelineName = "junk";
        String groupName = "some-pipeline-group";

        PipelineInstanceModel pipeline = PipelineInstanceModel.createPipeline(pipelineName, -1, "1.0", BuildCause.createManualForced(), new StageInstanceModels());
        when(pipelineDao.loadHistory(pipelineName, 1, 0)).thenReturn(createPipelineInstanceModels(pipeline));
        when(schedulingCheckerService.canManuallyTrigger(pipelineName, username)).thenReturn(false);
        when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);
        when(securityService.hasOperatePermissionForPipeline(username.getUsername(), pipelineName)).thenReturn(true);
        when(goConfigService.isLockable(pipelineName)).thenReturn(true);
        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName))).thenReturn(groupName);
        when(goConfigService.isUserAdminOfGroup(username.getUsername(), groupName)).thenReturn(true);
        when(pipelineLockService.isLocked(pipelineName)).thenReturn(true);
        stubConfigServiceToReturnPipeline(pipelineName, PipelineConfigMother.createPipelineConfig(pipelineName, "dev", "foo", "bar"));

        PipelineModel loadedPipeline = pipelineHistoryService.latestPipelineModel(username, pipelineName);

        assertThat(loadedPipeline.canForce()).isFalse();
        assertThat(loadedPipeline.canOperate()).isTrue();
        assertThat(loadedPipeline.getLatestPipelineInstance().isLockable()).isTrue();
        assertThat(loadedPipeline.getLatestPipelineInstance().isCurrentlyLocked()).isTrue();
        assertThat(loadedPipeline.getLatestPipelineInstance()).isEqualTo(pipeline);
        assertThat(loadedPipeline.canAdminister()).isTrue();
    }

    @Test
    void shouldLoadCanOperateAndLockableFlagForAllPipelines() {
        Username jez = new Username(new CaseInsensitiveString("jez"));
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG).config;
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        pipelineConfig.setTrackingTool(new TrackingTool("http://link", "#"));

        pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline2"));

        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        PipelineInstanceModels activePipelineInstances = createPipelineInstanceModels();

        StageInstanceModel stage1 = new StageInstanceModel("stage1", "2", JobHistory.withJob("plan1", JobState.Building, JobResult.Unknown, new Date()));
        activePipelineInstances.add(activePipeline("pipeline1", 10, 1.0, stage1, new NullStageHistoryItem("stage2", true)));

        activePipelineInstances.add(activePipeline("pipeline2", 5, 2.5, new StageInstanceModel("plan1", "2", JobHistory.withJob("plan1", JobState.Building, JobResult.Unknown, new Date()))));

        activePipelineInstances.add(
                activePipeline("non-operatable-pipeline", 5, 2.0, new StageInstanceModel("one", "2", JobHistory.withJob("plan1", JobState.Completed, JobResult.Failed, new Date()))));
        when(goConfigService.isLockable("pipeline1")).thenReturn(true);

        when(pipelineDao.loadActivePipelines()).thenReturn(activePipelineInstances);
        stubPermisssionsForActivePipeline(jez, cruiseConfig, "pipeline1", true, true);
        stubPermisssionsForActivePipeline(jez, cruiseConfig, "pipeline2", true, false);
        stubPermisssionsForActivePipeline(jez, cruiseConfig, "non-operatable-pipeline", false, false);
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);
        when(securityService.hasOperatePermissionForStage("pipeline1", "stage1", CaseInsensitiveString.str(jez.getUsername()))).thenReturn(true);

        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(jez, PipelineSelections.ALL);

        PipelineModel secondPipelineModel = groups.get(0).getPipelineModels().get(1);
        assertPipelineIs(secondPipelineModel, "pipeline2", true, false);
        PipelineInstanceModel secondPim = secondPipelineModel.getLatestPipelineInstance();
        assertThat(secondPim.getTrackingTool()).isNull();
        assertThat(secondPim.isLockable()).isFalse();

        PipelineModel firstPipelineModel = groups.get(0).getPipelineModels().get(0);
        assertPipelineIs(firstPipelineModel, "pipeline1", true, true);
        PipelineInstanceModel pim = firstPipelineModel.getLatestPipelineInstance();
        assertThat(pim.isLockable()).isTrue();
        assertThat(pim.getTrackingTool()).isEqualTo(new TrackingTool("http://link", "#"));

        PipelineModel nonOperatablePipelineModel = groups.get(1).getPipelineModels().get(0);
        assertPipelineIs(nonOperatablePipelineModel, "non-operatable-pipeline", false, false);
    }

    @Test
    void shouldLoadCurrentlyLockedStatusFlagForAllPipelines() {
        Username foo = new Username(new CaseInsensitiveString("foo"));
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG).config;
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        PipelineInstanceModels activePipelineInstances = createPipelineInstanceModels();

        PipelineInstanceModel pipeline1 = activePipeline("pipeline1", 9, 2.0,
                new StageInstanceModel("stage1", "3", StageResult.Failed, new StageIdentifier()),
                new StageInstanceModel("stage2", "4", JobHistory.withJob("plan1", JobState.Building, JobResult.Unknown, new Date())));

        PipelineInstanceModel pipeline2 = activePipeline("pipeline2", 5, 2.5,
                new StageInstanceModel("plan1", "2", JobHistory.withJob("plan1", JobState.Building, JobResult.Unknown, new Date())));

        PipelineInstanceModel pipeline3 = activePipeline("pipeline3", 5, 2.5,
                new StageInstanceModel("plan1", "2", JobHistory.withJob("plan1", JobState.Building, JobResult.Unknown, new Date())));

        activePipelineInstances.add(pipeline1);
        activePipelineInstances.add(pipeline2);
        activePipelineInstances.add(pipeline3);

        when(pipelineDao.loadActivePipelines()).thenReturn(activePipelineInstances);

        stubPermisssionsForActivePipeline(foo, cruiseConfig, "pipeline1", true, true);
        stubPermisssionsForActivePipeline(foo, cruiseConfig, "pipeline2", true, true);
        stubPermisssionsForActivePipeline(foo, cruiseConfig, "pipeline3", true, true);
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);
        when(goConfigService.isLockable("pipeline1")).thenReturn(true);
        when(goConfigService.isLockable("pipeline2")).thenReturn(true);
        when(pipelineLockService.isLocked("pipeline1")).thenReturn(true);

        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(foo, PipelineSelections.ALL);
        PipelineGroupModel group = groups.get(0);

        PipelineModel pipelineModel1 = group.getPipelineModels().get(0);
        PipelineInstanceModel firstPipelinePIM = pipelineModel1.getActivePipelineInstances().get(0);
        assertThat(firstPipelinePIM.isLockable()).isTrue();
        assertThat(firstPipelinePIM.isCurrentlyLocked()).isTrue();

        PipelineModel pipelineModel2 = group.getPipelineModels().get(1);
        PipelineInstanceModel secondPipeline = pipelineModel2.getActivePipelineInstances().get(0);
        assertThat(secondPipeline.isLockable()).isTrue();
        assertThat(secondPipeline.isCurrentlyLocked()).isFalse();

        PipelineModel pipelineModel3 = group.getPipelineModels().get(2);
        PipelineInstanceModel thirdPipeline = pipelineModel3.getActivePipelineInstances().get(0);
        assertThat(thirdPipeline.isLockable()).isFalse();
        assertThat(thirdPipeline.isCurrentlyLocked()).isFalse();
    }

    @Test
    void shouldLoadCanOperateFlagForAllNonActivePipelines() {
        Username jez = new Username(new CaseInsensitiveString("jez"));
        setupExpectationsForAllActivePipelinesWithTwoGroups(jez);

        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(jez, PipelineSelections.ALL);

        PipelineModel secondPipelineModel = groups.get(0).getPipelineModels().get(1);
        assertPipelineIs(secondPipelineModel, "pipeline2", true, false);

        PipelineModel firstPipelineModel = groups.get(0).getPipelineModels().get(0);
        assertPipelineIs(firstPipelineModel, "pipeline1", true, true);

        PipelineModel nonOperatablePipelineModel = groups.get(1).getPipelineModels().get(0);
        assertPipelineIs(nonOperatablePipelineModel, "non-operatable-pipeline", false, false);
    }

    @Test
    void shouldSetAdminPermissionOnEveryGroupModel() {
        Username jez = new Username(new CaseInsensitiveString("jez"));
        setupExpectationsForAllActivePipelinesWithTwoGroups(jez);
        when(goConfigService.isUserAdminOfGroup(jez.getUsername(), "defaultGroup")).thenReturn(true);
        when(goConfigService.isUserAdminOfGroup(jez.getUsername(), "foo")).thenReturn(false);

        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(jez, PipelineSelections.ALL);

        assertThat(groups.get(0).getPipelineModels().get(0).canAdminister()).isTrue();
        assertThat(groups.get(0).getPipelineModels().get(1).canAdminister()).isTrue();
        assertThat(groups.get(1).getPipelineModels().get(0).canAdminister()).isFalse();

        verify(goConfigService, times(1)).isUserAdminOfGroup(jez.getUsername(), "defaultGroup");
        verify(goConfigService, times(1)).isUserAdminOfGroup(jez.getUsername(), "foo");
    }

    @Test
    void shouldRestrictAdminPermissionOnRemotePipelines() {
        when(goConfigService.isPipelineEditable(any(String.class))).thenReturn(false);
        Username jez = new Username(new CaseInsensitiveString("jez"));
        setupExpectationsForAllActivePipelinesWithTwoGroups(jez);
        when(goConfigService.isUserAdminOfGroup(jez.getUsername(), "defaultGroup")).thenReturn(true);
        when(goConfigService.isUserAdminOfGroup(jez.getUsername(), "foo")).thenReturn(false);

        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(jez, PipelineSelections.ALL);

        assertThat(groups.get(0).getPipelineModels().get(0).canAdminister()).isFalse();
        assertThat(groups.get(0).getPipelineModels().get(1).canAdminister()).isFalse();
        assertThat(groups.get(1).getPipelineModels().get(0).canAdminister()).isFalse();

        verify(goConfigService, times(1)).isUserAdminOfGroup(jez.getUsername(), "defaultGroup");
        verify(goConfigService, times(1)).isUserAdminOfGroup(jez.getUsername(), "foo");
    }

    private void setupExpectationsForAllActivePipelinesWithTwoGroups(Username jez) {
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG).config;
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        PipelineInstanceModels activePipelineInstances = createPipelineInstanceModels();

        when(pipelineDao.loadActivePipelines()).thenReturn(activePipelineInstances);
        stubForNonActivePipeline(jez, cruiseConfig, "pipeline1", true, true,
                activePipeline("pipeline1", 10, 2.0, new StageInstanceModel("stage1", "10", JobHistory.withJob("plan1", JobState.Completed, JobResult.Failed, new Date()))));
        stubForNonActivePipeline(jez, cruiseConfig, "pipeline2", true, false,
                activePipeline("pipeline2", 10, 2.0, new StageInstanceModel("stage1", "8", JobHistory.withJob("plan1", JobState.Completed, JobResult.Passed, new Date()))));
        stubForNonActivePipeline(jez, cruiseConfig, "non-operatable-pipeline", false, false,
                activePipeline("non-operatable-pipeline", 10, 2.0, new StageInstanceModel("one", "10", JobHistory.withJob("defaultJob", JobState.Completed, JobResult.Failed, new Date()))));
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);
    }

    private void stubForNonActivePipeline(Username username, CruiseConfig cruiseConfig, String pipelineName, boolean operatePermission, boolean canTrigger, PipelineInstanceModel pipeline) {
        stubPermisssionsForActivePipeline(username, cruiseConfig, pipelineName, operatePermission, canTrigger);
        when(pipelineDao.loadHistory(pipelineName, 1, 0)).thenReturn(createPipelineInstanceModels(pipeline));
    }

    private void stubPermisssionsForActivePipeline(Username username, CruiseConfig cruiseConfig, String pipelineName, boolean operatePermission, boolean canTrigger) {
        when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);
        when(securityService.hasOperatePermissionForPipeline(username.getUsername(), pipelineName)).thenReturn(operatePermission);
        stubConfigServiceToReturnPipeline(pipelineName, cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(pipelineName)));
        when(schedulingCheckerService.canManuallyTrigger(pipelineName, username)).thenReturn(canTrigger);
    }

    private void assertPipelineIs(PipelineModel secondPipelineModel, String pipelineName, boolean canOperate, boolean canForce) {
        assertThat(secondPipelineModel.canOperate()).isEqualTo(canOperate);
        assertThat(secondPipelineModel.canForce()).isEqualTo(canForce);
        PipelineInstanceModel secondActivePipeline = secondPipelineModel.getActivePipelineInstances().get(0);
        assertThat(secondActivePipeline.getName()).isEqualTo(pipelineName);
    }

    private PipelineInstanceModel activePipeline(String pipelineName, int pipelineCounter, double naturalOrder, StageInstanceModel... moreStages) {
        StageInstanceModels stagesForNonOperatablePipeline = new StageInstanceModels();
        stagesForNonOperatablePipeline.addAll(Arrays.asList(moreStages));
        PipelineInstanceModel nonOperatablePipeline = PipelineInstanceModel.createPipeline(pipelineName, -1, "1.0", BuildCause.createWithEmptyModifications(), stagesForNonOperatablePipeline);
        nonOperatablePipeline.setNaturalOrder(naturalOrder);
        nonOperatablePipeline.setCounter(pipelineCounter);
        return nonOperatablePipeline;
    }

    @Test
    void shouldLoadAllActivePipelinesWithPreviousStageState() {
        Username foo = new Username(new CaseInsensitiveString("foo"));
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG).config;
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        PipelineInstanceModels activePipelineInstances = createPipelineInstanceModels();

        PipelineInstanceModel earlier = activePipeline("pipeline1", 9, 2.0,
                new StageInstanceModel("stage1", "3", StageResult.Failed, new StageIdentifier()),
                new StageInstanceModel("stage2", "4", JobHistory.withJob("plan1", JobState.Building, JobResult.Unknown, new Date())));
        earlier.setId(1);
        PipelineInstanceModel latest = activePipeline("pipeline1", 10, 1.0,
                new StageInstanceModel("stage1", "4", JobHistory.withJob("plan1", JobState.Building, JobResult.Unknown, new Date())),
                new NullStageHistoryItem("stage2", true));
        latest.setId(2);

        activePipelineInstances.add(latest);
        activePipelineInstances.add(earlier);

        when(pipelineDao.loadActivePipelines()).thenReturn(activePipelineInstances);

        when(pipelineDao.loadHistory(1)).thenReturn(earlier);
        when(pipelineDao.loadHistory(2)).thenReturn(latest);
        when(pipelineDao.loadHistory(3)).thenReturn(activePipeline("pipeline1", 11, 3.0,
                new StageInstanceModel("stage1", "3", StageResult.Failed, new StageIdentifier()),
                new StageInstanceModel("stage2", "2", StageResult.Passed, new StageIdentifier())));

        when(pipelineTimeline.runBefore(2, new CaseInsensitiveString("pipeline1"))).thenReturn(new PipelineTimelineEntry("pipeline1", 1, 9, new HashMap<>()));
        when(pipelineTimeline.runBefore(1, new CaseInsensitiveString("pipeline1"))).thenReturn(new PipelineTimelineEntry("pipeline1", 3, 11, new HashMap<>()));

        stubPermisssionsForActivePipeline(foo, cruiseConfig, "pipeline1", true, true);
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);

        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(foo, PipelineSelections.ALL);
        PipelineGroupModel group = groups.get(0);
        PipelineModel pipelineModel = group.getPipelineModels().get(0);
        PipelineInstanceModels activePipelines = pipelineModel.getActivePipelineInstances();

        assertPipelineIs(activePipelines.get(0), "pipeline1", "stage1", StageResult.Failed);
        assertPipelineIs(activePipelines.get(1), "pipeline1", "stage2", StageResult.Passed);
    }

    @Test
    void allActivePipelines_shouldOnlyKeepSelectedPipelines() {
        Username foo = new Username(new CaseInsensitiveString("foo"));
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG).config;
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        PipelineInstanceModels activePipelineInstances = createPipelineInstanceModels(
                activePipeline("pipeline1", 1, 1.0),
                activePipeline("pipeline2", 1, 1.0),
                activePipeline("pipeline4", 1, 1.0),
                activePipeline("non-operatable-pipeline", 1, 1.0)
        );
        for (String pipeline : new String[]{"pipeline1", "pipeline2", "pipeline3", "pipeline4", "non-operatable-pipeline"}) {
            stubPermisssionsForActivePipeline(foo, cruiseConfig, pipeline, true, true);
            when(pipelineDao.loadHistory(pipeline, 1, 0)).thenReturn(createPipelineInstanceModels());
        }
        when(pipelineDao.loadActivePipelines()).thenReturn(activePipelineInstances);


        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);
        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(foo, PipelineSelectionsHelper.with(Arrays.asList("pipeline1", "pipeline2")));
        assertThat(groups.size()).isEqualTo(2);
        assertThat(groups.get(0).getName()).isEqualTo("defaultGroup");
        assertThat(groups.get(0).containsPipeline("pipeline1")).isFalse();
        assertThat(groups.get(0).containsPipeline("pipeline2")).isFalse();
        assertThat(groups.get(0).containsPipeline("pipeline3")).isTrue();
        assertThat(groups.get(0).containsPipeline("pipeline4")).isTrue();
        assertThat(groups.get(1).getName()).isEqualTo("foo");
        assertThat(groups.get(1).containsPipeline("non-operatable-pipeline")).isTrue();
    }

    @Test
    void allActivePipelines_shouldRemove_EmptyGroups() {
        Username bar = new Username(new CaseInsensitiveString("non-existant"));
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG).config;
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(pipelineDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels());
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);

        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(bar, new PipelineSelections());

        assertThat(groups.isEmpty()).isTrue();
    }

    @Test
    void shouldGetActivePipeline() {
        Username foo = new Username(new CaseInsensitiveString("foo"));
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG).config;
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        PipelineInstanceModels activePipelineInstances = createPipelineInstanceModels(
                activePipeline("pipeline1", 1, 1.0),
                activePipeline("pipeline2", 1, 1.0),
                activePipeline("pipeline4", 1, 1.0),
                activePipeline("non-operatable-pipeline", 1, 1.0)
        );
        for (String pipeline : new String[]{"pipeline1", "pipeline2", "pipeline3", "pipeline4", "non-operatable-pipeline"}) {
            stubPermisssionsForActivePipeline(foo, cruiseConfig, pipeline, true, true);
            when(pipelineDao.loadHistory(pipeline, 1, 0)).thenReturn(createPipelineInstanceModels());
        }
        when(pipelineDao.loadActivePipelines()).thenReturn(activePipelineInstances);

        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);
        List<PipelineGroupModel> groups = pipelineHistoryService.getActivePipelineInstance(foo, "pipeline1");
        assertThat(groups.size()).isEqualTo(1);
        assertThat(groups.get(0).getName()).isEqualTo("defaultGroup");
        assertThat(groups.get(0).containsPipeline("pipeline1")).isTrue();
    }

    @Test
    void getActivePipelineInstance_shouldRemoveEmptyGroups() {
        Username foo = new Username(new CaseInsensitiveString("foo"));
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG).config;
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(pipelineDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels());
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);
        List<PipelineGroupModel> groups = pipelineHistoryService.getActivePipelineInstance(foo, "pipeline1");
        assertThat(groups.isEmpty()).isTrue();
    }

    @Test
    void allActivePipelines_shouldOnlyKeepSelectedPipelines_andRemove_EmptyGroups() {
        Username foo = new Username(new CaseInsensitiveString("foo"));
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG).config;
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        PipelineInstanceModels activePipelineInstances = createPipelineInstanceModels(
                activePipeline("pipeline1", 1, 1.0),
                activePipeline("pipeline2", 1, 1.0),
                activePipeline("pipeline4", 1, 1.0),
                activePipeline("non-operatable-pipeline", 1, 1.0)
        );
        for (String pipeline : new String[]{"pipeline1", "pipeline2", "pipeline3", "pipeline4", "non-operatable-pipeline"}) {
            stubPermisssionsForActivePipeline(foo, cruiseConfig, pipeline, true, true);
            when(pipelineDao.loadHistory(pipeline, 1, 0)).thenReturn(createPipelineInstanceModels());
        }
        when(pipelineDao.loadActivePipelines()).thenReturn(activePipelineInstances);


        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);
        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(foo, PipelineSelectionsHelper.with(Arrays.asList("non-operatable-pipeline")));
        assertThat(groups.size()).isEqualTo(1);
        assertThat(groups.get(0).getName()).isEqualTo("defaultGroup");
        assertThat(groups.get(0).containsPipeline("pipeline1")).isTrue();
        assertThat(groups.get(0).containsPipeline("pipeline2")).isTrue();
        assertThat(groups.get(0).containsPipeline("pipeline3")).isTrue();
        assertThat(groups.get(0).containsPipeline("pipeline4")).isTrue();
    }

    @Test
    void allActivePipelines_shouldSkipNullPipelinesWhenLoadingPreviousStageStae() {
        Username foo = new Username(new CaseInsensitiveString("foo"));
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG).config;
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        PipelineInstanceModels activePipelineInstances = createPipelineInstanceModels();

        PipelineInstanceModel latest = activePipeline("pipeline1", 10, 1.0,
                new StageInstanceModel("stage1", "4", JobHistory.withJob("plan1", JobState.Building, JobResult.Unknown, new Date())),
                new NullStageHistoryItem("stage2", true));
        latest.setId(2);

        activePipelineInstances.add(latest);

        when(pipelineDao.loadActivePipelines()).thenReturn(activePipelineInstances);

        when(pipelineDao.loadHistory(1)).thenReturn(null);
        when(pipelineDao.loadHistory(2)).thenReturn(latest);
        when(pipelineDao.loadHistory(3)).thenReturn(activePipeline("pipeline1", 11, 3.0,
                new StageInstanceModel("stage1", "3", StageResult.Failed, new StageIdentifier())));

        //2>1>3

        when(pipelineTimeline.runBefore(2, new CaseInsensitiveString("pipeline1"))).thenReturn(new PipelineTimelineEntry("pipeline1", 1, 9, new HashMap<>()));
        when(pipelineTimeline.runBefore(1, new CaseInsensitiveString("pipeline1"))).thenReturn(new PipelineTimelineEntry("pipeline1", 3, 11, new HashMap<>()));

        stubPermisssionsForActivePipeline(foo, cruiseConfig, "pipeline1", true, true);
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);

        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(foo, PipelineSelections.ALL);
        PipelineGroupModel group = groups.get(0);
        PipelineModel pipelineModel = group.getPipelineModels().get(0);
        PipelineInstanceModels activePipelines = pipelineModel.getActivePipelineInstances();

        assertPipelineIs(activePipelines.get(0), "pipeline1", "stage1", StageResult.Failed);
    }

    private void assertPipelineIs(PipelineInstanceModel pipeline, String pipelineName, String activeStageName, StageResult previousStageResult) {
        assertThat(pipeline.getName()).isEqualTo(pipelineName);
        StageInstanceModel activeStage = pipeline.activeStage();
        assertThat(activeStage.getName()).isEqualTo(activeStageName);
        assertThat(activeStage.getPreviousStage().getResult()).isEqualTo(previousStageResult);
    }

    @Test
    void findPipelineInstanceUsingIdShouldPopulateAppendEmptyStagesFromConfig() {
        ensureConfigHasPipeline("pipeline");
        ensureHasPermission(Username.ANONYMOUS, "pipeline");
        PipelineInstanceModel instanceModel = PipelineInstanceModel.createEmptyPipelineInstanceModel("pipeline", BuildCause.createNeverRun(), new StageInstanceModels());
        when(pipelineDao.loadHistoryByIdWithBuildCause(1L)).thenReturn(instanceModel);
        stubConfigServiceToReturnPipeline("pipeline", config);

        PipelineInstanceModel pipelineInstance = pipelineHistoryService.findPipelineInstance("pipeline", 1, 1L, Username.ANONYMOUS, new HttpOperationResult());
        StageInstanceModels models = pipelineInstance.getStageHistory();
        assertThat(models.size()).isEqualTo(2);
    }

    @Test
    void findPipelineInstanceShouldPopulateAppendEmptyStagesFromConfig() {
        ensureConfigHasPipeline("pipeline");
        ensureHasPermission(Username.ANONYMOUS, "pipeline");
        PipelineInstanceModel instanceModel = PipelineInstanceModel.createEmptyPipelineInstanceModel("pipeline", BuildCause.createNeverRun(), new StageInstanceModels());
        when(pipelineDao.findPipelineHistoryByNameAndCounter("pipeline", 1)).thenReturn(instanceModel);
        stubConfigServiceToReturnPipeline("pipeline", config);

        PipelineInstanceModel pipelineInstance = pipelineHistoryService.findPipelineInstance("pipeline", 1, Username.ANONYMOUS, new HttpOperationResult());
        StageInstanceModels models = pipelineInstance.getStageHistory();
        assertThat(models.size()).isEqualTo(2);
    }

    @Test
    void findPipelineInstanceShouldChangeResultTo404WhenPipelineNotFound() {
        ensureConfigHasPipeline("pipeline");
        ensureHasPermission(Username.ANONYMOUS, "pipeline");
        when(pipelineDao.findPipelineHistoryByNameAndCounter("pipeline", 1)).thenReturn(null);
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline"))).thenReturn(config);

        HttpOperationResult operationResult = new HttpOperationResult();
        PipelineInstanceModel pipelineInstance = pipelineHistoryService.findPipelineInstance("pipeline", 1, Username.ANONYMOUS, operationResult);
        assertThat(pipelineInstance).isNull();
        assertThat(operationResult.httpCode()).isEqualTo(404);
    }

    @Test
    void shouldPopulateIfPipelineCanBeUnlockedAndIsLockable() {
        ensureConfigHasPipeline("pipeline");

        stubConfigServiceToReturnPipeline("pipeline", config);
        when(goConfigService.isLockable("pipeline")).thenReturn(true);

        ensureHasPermission(Username.ANONYMOUS, "pipeline");

        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createNeverRun(), new StageInstanceModels());
        when(pipelineDao.findPipelineHistoryByNameAndCounter("pipeline", 1)).thenReturn(instanceModel);

        when(pipelineUnlockService.canUnlock(eq("pipeline"), eq(Username.ANONYMOUS), any(HttpOperationResult.class))).thenReturn(true);

        PipelineInstanceModel pipelineInstance = pipelineHistoryService.findPipelineInstance("pipeline", 1, Username.ANONYMOUS, new HttpOperationResult());
        assertThat(pipelineInstance.canUnlock()).isTrue();
        assertThat(pipelineInstance.isLockable()).isTrue();
    }

    @Test
    void shouldPopulateWetherStageAndPipelineCanBeRunAccordingToOperatePermissions() {
        ensureConfigHasPipeline("pipeline");
        ensureHasPermission(Username.ANONYMOUS, "pipeline");
        StageInstanceModels stages = new StageInstanceModels();
        stages.add(new StageInstanceModel("dev", "1", new JobHistory()));
        stages.add(new StageInstanceModel("qa", "1", new JobHistory()));
        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createNeverRun(), stages);
        when(pipelineDao.findPipelineHistoryByNameAndCounter("pipeline", 1)).thenReturn(instanceModel);
        stubConfigServiceToReturnPipeline("pipeline", config);

        StageInstanceModel firstStage = instanceModel.getStageHistory().get(0);
        when(scheduleService.canRun(eq(instanceModel.getPipelineIdentifier()), eq(firstStage.getName()), eq(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername())), eq(instanceModel.hasPreviousStageBeenScheduled(firstStage.getName())), any(ServerHealthStateOperationResult.class))).thenReturn(true);
        StageInstanceModel secondStage = instanceModel.getStageHistory().get(1);
        when(scheduleService.canRun(instanceModel.getPipelineIdentifier(), secondStage.getName(), CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()),
                instanceModel.hasPreviousStageBeenScheduled(secondStage.getName()))).thenReturn(false);

        when(securityService.hasOperatePermissionForStage("pipeline", "dev", CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()))).thenReturn(true);
        when(securityService.hasOperatePermissionForStage("pipeline", "qa", CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()))).thenReturn(false);

        PipelineInstanceModel pipelineInstance = pipelineHistoryService.findPipelineInstance("pipeline", 1, Username.ANONYMOUS, new HttpOperationResult());
        StageInstanceModels models = pipelineInstance.getStageHistory();
        assertThat(models.get(0).getCanRun()).isTrue();
        assertThat(models.get(1).getCanRun()).isFalse();
        assertThat(models.get(0).hasOperatePermission()).isTrue();
        assertThat(models.get(1).hasOperatePermission()).isFalse();
    }

    @Test
    void shouldPopulatePipelineInstanceModelWithTheBeforeAndAfterForTheGivenPipeline() {
        DateTime now = new DateTime();
        PipelineTimelineEntry first = PipelineMaterialModificationMother.modification(Arrays.asList("first"), 1, "123", now);
        PipelineTimelineEntry second = PipelineMaterialModificationMother.modification(Arrays.asList("first"), 1, "123", now);

        when(pipelineTimeline.runBefore(1, new CaseInsensitiveString("pipeline"))).thenReturn(first);
        when(pipelineTimeline.runAfter(1, new CaseInsensitiveString("pipeline"))).thenReturn(second);


        PipelineInstanceModel expected = PipelineHistoryMother.pipelineHistoryItemWithOneStage("pipeline", "auto", now.toDate());
        expected.setId(1);
        when(pipelineDao.loadHistory(1)).thenReturn(expected);
        when(goConfigService.currentCruiseConfig()).thenReturn(CRUISE_CONFIG);
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline"))).thenReturn(config);
        when(securityService.hasOperatePermissionForStage("pipeline", "auto", CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()))).thenReturn(true);
        ensureHasPermission(Username.ANONYMOUS, "pipeline");

        PipelineInstanceModel model = pipelineHistoryService.load(1, Username.ANONYMOUS, new HttpOperationResult());
        assertThat(model.getPipelineBefore()).isEqualTo(first);
        assertThat(model.getPipelineAfter()).isEqualTo(second);
        assertThat(model.stage("auto").hasOperatePermission()).isTrue();
    }

    @Test
    void shouldReturnPageNumberForAPipelineCounter() {
        when(pipelineDao.getPageNumberForCounter("some-pipeline", 100, 10)).thenReturn(1);
        assertThat(pipelineHistoryService.getPageNumberForCounter("some-pipeline", 100, 10)).isEqualTo(1);
    }

    @Test
    void shouldPopulateDataCorrectly_getPipelineStatus() {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        PipelineConfig pipelineConfig = new PipelineConfig();
        PipelinePauseInfo pipelinePauseInfo = new PipelinePauseInfo(true, "pausing pipeline for some-reason", "some-one");
        when(cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipeline-name"))).thenReturn(pipelineConfig);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(securityService.hasViewPermissionForPipeline(Username.valueOf("user-name"), "pipeline-name")).thenReturn(true);
        when(pipelinePauseService.pipelinePauseInfo("pipeline-name")).thenReturn(pipelinePauseInfo);
        when(pipelineLockService.isLocked("pipeline-name")).thenReturn(true);
        when(schedulingCheckerService.canManuallyTrigger(eq(pipelineConfig), eq("user-name"), any(ServerHealthStateOperationResult.class))).thenReturn(true);

        PipelineStatusModel pipelineStatus = pipelineHistoryService.getPipelineStatus("pipeline-name", "user-name", new HttpOperationResult());

        assertThat(pipelineStatus.isPaused()).isTrue();
        assertThat(pipelineStatus.pausedCause()).isEqualTo("pausing pipeline for some-reason");
        assertThat(pipelineStatus.pausedBy()).isEqualTo("some-one");
        assertThat(pipelineStatus.isLocked()).isTrue();
        assertThat(pipelineStatus.isSchedulable()).isTrue();
    }

    @Test
    void shouldPopulateResultAsNotFound_getPipelineStatus() {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        when(cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipeline-name"))).thenReturn(null);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        HttpOperationResult result = new HttpOperationResult();
        PipelineStatusModel pipelineStatus = pipelineHistoryService.getPipelineStatus("pipeline-name", "user-name", result);

        assertThat(pipelineStatus).isNull();
        assertThat(result.httpCode()).isEqualTo(404);
    }

    @Test
    void shouldPopulateResultAsUnauthorized_getPipelineStatus() {
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        PipelineConfig pipelineConfig = new PipelineConfig();
        when(cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipeline-name"))).thenReturn(pipelineConfig);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(securityService.hasViewPermissionForPipeline(Username.valueOf("user-name"), "pipeline-name")).thenReturn(false);

        HttpOperationResult result = new HttpOperationResult();
        PipelineStatusModel pipelineStatus = pipelineHistoryService.getPipelineStatus("pipeline-name", "user-name", result);

        assertThat(pipelineStatus).isNull();
        assertThat(result.httpCode()).isEqualTo(403);
    }

    @Test
    void shouldPopulateResultAsNotFoundWhenPipelineNotFound_loadMinimalData() {
        String pipelineName = "unknown-pipeline";
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(false);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        HttpOperationResult result = new HttpOperationResult();
        PipelineInstanceModels pipelineInstanceModels = pipelineHistoryService.loadMinimalData(pipelineName,
                Pagination.pageFor(0, 0, 10), new Username(new CaseInsensitiveString("looser")), result);

        assertThat(pipelineInstanceModels).isNull();
        assertThat(result.httpCode()).isEqualTo(404);
        assertThat(result.detailedMessage()).isEqualTo("Not Found { Pipeline " + pipelineName + " not found }\n");
    }

    @Test
    void shouldPopulateResultAsUnauthorizedWhenUserNotAllowedToViewPipeline_loadMinimalData() {
        Username noAccessUserName = new Username(new CaseInsensitiveString("foo"));
        Username withAccessUserName = new Username(new CaseInsensitiveString("admin"));
        String pipelineName = "no-access-pipeline";
        CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
        when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

        when(securityService.hasViewPermissionForPipeline(noAccessUserName, pipelineName)).thenReturn(false);
        when(securityService.hasViewPermissionForPipeline(withAccessUserName, pipelineName)).thenReturn(true);

        when(pipelineDao.loadHistory(pipelineName, 10, 0)).thenReturn(PipelineInstanceModels.createPipelineInstanceModels());

        HttpOperationResult result = new HttpOperationResult();
        PipelineInstanceModels pipelineInstanceModels = pipelineHistoryService.loadMinimalData(pipelineName, Pagination.pageFor(0, 1, 10), noAccessUserName, result);

        assertThat(pipelineInstanceModels).isNull();
        assertThat(result.httpCode()).isEqualTo(403);

        result = new HttpOperationResult();
        pipelineInstanceModels = pipelineHistoryService.loadMinimalData(pipelineName, Pagination.pageFor(0, 1, 10), withAccessUserName, result);

        assertThat(pipelineInstanceModels).isNotNull();
        assertThat(result.canContinue()).isTrue();
    }

    @Nested
    class UpdateComment {
        @Test
        void shouldUpdateCommentUsingPipelineDao() {
            String pipelineName = "pipeline_name";
            CaseInsensitiveString authorizedUser = new CaseInsensitiveString("can-access");
            when(pipelineDao.findPipelineByNameAndCounter(pipelineName, 1)).thenReturn(mock(Pipeline.class));
            when(securityService.hasOperatePermissionForPipeline(authorizedUser, pipelineName)).thenReturn(true);

            pipelineHistoryService.updateComment(pipelineName, 1, "test comment", new Username(authorizedUser));

            verify(pipelineDao, times(1)).updateComment(pipelineName, 1, "test comment");
        }

        @Test
        void shouldFailWhenUserIsUnauthorized() {
            String pipelineName = "pipeline_name";
            CaseInsensitiveString unauthorizedUser = new CaseInsensitiveString("cannot-access");
            when(securityService.hasOperatePermissionForPipeline(unauthorizedUser, pipelineName)).thenReturn(false);

            assertThatCode(() -> pipelineHistoryService.updateComment(pipelineName, 1, "test comment", new Username(unauthorizedUser)))
                    .isInstanceOf(NotAuthorizedException.class)
                    .hasMessage("You do not have operate permissions for pipeline 'pipeline_name'.");

            verifyZeroInteractions(pipelineDao);
        }

        @Test
        void shouldFailWhenPipelineWithCounterDoesNotExist() {
            String pipelineName = "pipeline_name";
            CaseInsensitiveString unauthorizedUser = new CaseInsensitiveString("cannot-access");
            when(pipelineDao.findPipelineByNameAndCounter(pipelineName, 1)).thenReturn(null);
            when(securityService.hasOperatePermissionForPipeline(unauthorizedUser, pipelineName)).thenReturn(false);

            assertThatCode(() -> pipelineHistoryService.updateComment(pipelineName, 1, "test comment", new Username(unauthorizedUser)))
                    .isInstanceOf(NotAuthorizedException.class)
                    .hasMessage("You do not have operate permissions for pipeline 'pipeline_name'.");

            verifyZeroInteractions(pipelineDao);
        }
    }

    @Nested
    class GetLatestAndOldestPipelineRunId {
        @Test
        void shouldReturnTheLatestAndOldestPipelineRunId() {
            String pipelineName = "pipeline";
            Username username = new Username(new CaseInsensitiveString("can-access"));

            CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);
            when(pipelineDao.getOldestAndLatestPipelineId(pipelineName)).thenReturn(new PipelineRunIdInfo(10L, 3L));

            PipelineRunIdInfo oldestAndLatestPipelineId = pipelineHistoryService.getOldestAndLatestPipelineId(pipelineName, username);

            assertThat(oldestAndLatestPipelineId.getLatestRunId()).isEqualTo(10L);
            assertThat(oldestAndLatestPipelineId.getOldestRunId()).isEqualTo(3L);
        }

        @Test
        void shouldThrowIfPipelineDoesNotExist() {
            String pipelineName = "pipeline";
            Username username = new Username(new CaseInsensitiveString("can-access"));
            CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(false);

            assertThatCode(() -> pipelineHistoryService.getOldestAndLatestPipelineId(pipelineName, username))
                    .isInstanceOf(RecordNotFoundException.class)
                    .hasMessage("Pipeline with name 'pipeline' was not found!");
        }

        @Test
        void shouldThrowIfTheUserDoesNotHaveAccess() {
            String pipelineName = "pipeline";
            Username username = new Username(new CaseInsensitiveString("cannot-access"));
            CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(false);

            assertThatCode(() -> pipelineHistoryService.getOldestAndLatestPipelineId(pipelineName, username))
                    .isInstanceOf(NotAuthorizedException.class)
                    .hasMessage("Not authorized to view pipeline");
        }
    }

    @Nested
    class LoadPipelineHistoryData {
        @Test
        void shouldCallDaoToFetchLatestPipelineHistoryData() {
            String pipelineName = "pipeline";
            Username username = new Username(new CaseInsensitiveString("can-access"));
            CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);
            when(pipelineDao.loadHistory(eq(pipelineName), any(), anyLong(), anyInt())).thenReturn(PipelineInstanceModels.createPipelineInstanceModels());

            PipelineInstanceModels pipelineInstanceModels = pipelineHistoryService.loadPipelineHistoryData(username, pipelineName, 0, 0, 10);

            verify(pipelineDao).loadHistory(pipelineName, FeedModifier.Latest, 0, 10);
        }

        @Test
        void shouldCallDaoToFetchPipelineHistoryDataAfterTheGivenCursor() {
            String pipelineName = "pipeline";
            Username username = new Username(new CaseInsensitiveString("can-access"));
            CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);
            when(pipelineDao.loadHistory(eq(pipelineName), any(), anyLong(), anyInt())).thenReturn(PipelineInstanceModels.createPipelineInstanceModels());

            PipelineInstanceModels pipelineInstanceModels = pipelineHistoryService.loadPipelineHistoryData(username, pipelineName, 3, 0, 10);

            verify(pipelineDao).loadHistory(pipelineName, FeedModifier.After, 3L, 10);
        }

        @Test
        void shouldCallDaoToFetchPipelineHistoryDataBeforeTheGivenCursor() {
            String pipelineName = "pipeline";
            Username username = new Username(new CaseInsensitiveString("can-access"));
            CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);
            when(pipelineDao.loadHistory(eq(pipelineName), any(), anyLong(), anyInt())).thenReturn(PipelineInstanceModels.createPipelineInstanceModels());

            PipelineInstanceModels pipelineInstanceModels = pipelineHistoryService.loadPipelineHistoryData(username, pipelineName, 0, 6, 10);

            verify(pipelineDao).loadHistory(pipelineName, FeedModifier.Before, 6L, 10);
        }

        @Test
        void shouldThrowUpIfThePipelineDoesNotExist() {
            String pipelineName = "pipeline";
            Username username = new Username(new CaseInsensitiveString("can-access"));
            CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(false);
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

            assertThatCode(() -> pipelineHistoryService.loadPipelineHistoryData(username, pipelineName, 0, 0, 10))
                    .isInstanceOf(RecordNotFoundException.class)
                    .hasMessage("Pipeline with name 'pipeline' was not found!");

            verifyZeroInteractions(pipelineDao);
        }

        @Test
        void shouldThrowUpIfTheUserDoesNotHaveAccessToThePipeline() {
            String pipelineName = "pipeline";
            Username username = new Username(new CaseInsensitiveString("can-access"));
            CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(false);

            assertThatCode(() -> pipelineHistoryService.loadPipelineHistoryData(username, pipelineName, 0, 0, 10))
                    .isInstanceOf(NotAuthorizedException.class)
                    .hasMessage("Not authorized to view pipeline");

            verifyZeroInteractions(pipelineDao);
        }

        @Test
        void shouldThrowIfTheAfterCursorIsInvalid() {
            String pipelineName = "pipeline";
            Username username = new Username(new CaseInsensitiveString("can-access"));
            CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            assertThatCode(() -> pipelineHistoryService.loadPipelineHistoryData(username, pipelineName, -10L, 0, 10))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The query parameter `after`, if specified, must be positive integer.");

            verifyZeroInteractions(pipelineDao);
        }

        @Test
        void shouldThrowIfTheBeforeCursorIsInvalid() {
            String pipelineName = "pipeline";
            Username username = new Username(new CaseInsensitiveString("can-access"));
            CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);

            when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
            when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
            when(securityService.hasViewPermissionForPipeline(username, pipelineName)).thenReturn(true);

            assertThatCode(() -> pipelineHistoryService.loadPipelineHistoryData(username, pipelineName, 0, -10L, 10))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The query parameter `before`, if specified, must be positive integer.");

            verifyZeroInteractions(pipelineDao);
        }
    }

    private void stubConfigServiceToReturnPipeline(String blahPipelineName, PipelineConfig blahPipelineConfig) {
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString(blahPipelineName))).thenReturn(blahPipelineConfig);
        when(goConfigService.findFirstStageOfPipeline(new CaseInsensitiveString(blahPipelineName))).thenReturn(blahPipelineConfig.get(0));
    }

    private void ensureConfigHasPipeline(String pipelineName) {
        ensureConfigContainsPipelineIs(pipelineName, true);
    }

    private void ensureHasPermission(Username bar, String pipelineName) {
        when(securityService.hasViewPermissionForPipeline(bar, pipelineName)).thenReturn(true);
    }

    private void ensureConfigContainsPipelineIs(String pipelineName, boolean isPresent) {
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(isPresent);
    }
}
