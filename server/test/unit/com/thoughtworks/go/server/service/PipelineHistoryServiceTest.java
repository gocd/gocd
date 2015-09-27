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
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.presentation.PipelineStatusModel;
import com.thoughtworks.go.presentation.pipelinehistory.*;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.JobDurationStrategy;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.apache.commons.httpclient.HttpStatus;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.*;

import static com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels.createPipelineInstanceModels;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PipelineHistoryServiceTest {
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

    @Mock private StageDao stageDao;
    @Mock private PipelineDao pipelineDao;
    @Mock private GoConfigService goConfigService;
    @Mock private SecurityService securityService;
    @Mock private ScheduleService scheduleService;
    @Mock private PipelineTimeline pipelineTimeline;
    @Mock private PipelineUnlockApiService pipelineUnlockService;
    @Mock private SchedulingCheckerService schedulingCheckerService;
    @Mock private PipelineLockService pipelineLockService;
    @Mock public PipelinePauseService pipelinePauseService;
    @Mock public FeatureToggleService featureToggleService;
    private PipelineHistoryService pipelineHistoryService;
    private static final Username USERNAME = new Username(new CaseInsensitiveString("bar"));
    private PipelineConfig config;

    @Before
    public void setUp() {
        initMocks(this);
        when(featureToggleService.isToggleOn(Toggles.PIPELINE_COMMENT_FEATURE_TOGGLE_KEY)).thenReturn(true);
        Toggles.initializeWith(featureToggleService);
        pipelineHistoryService = new PipelineHistoryService(pipelineDao, stageDao, goConfigService, securityService, scheduleService,
                mock(MaterialRepository.class),
                JobDurationStrategy.ALWAYS_ZERO,
                mock(TriggerMonitor.class),
                pipelineTimeline,
                pipelineUnlockService, schedulingCheckerService, pipelineLockService, pipelinePauseService);
        config = CRUISE_CONFIG.pipelineConfigByName(new CaseInsensitiveString("pipeline"));
    }

    @Test
    public void shouldUnderstandLatestPipelineModel() {
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
        when(pipelineLockService.lockedPipeline(pipelineName)).thenReturn(new StageIdentifier(pipelineName, 9, "stage1", "3"));
        stubConfigServiceToReturnPipeline(pipelineName, PipelineConfigMother.createPipelineConfig(pipelineName, "dev", "foo", "bar"));

        PipelineModel loadedPipeline = pipelineHistoryService.latestPipelineModel(username, pipelineName);

        assertThat(loadedPipeline.canForce(), is(false));
        assertThat(loadedPipeline.canOperate(), is(true));
        assertThat(loadedPipeline.getLatestPipelineInstance().isLockable(), is(true));
        assertThat(loadedPipeline.getLatestPipelineInstance().isCurrentlyLocked(), is(true));
        assertThat(loadedPipeline.getLatestPipelineInstance(), is(pipeline));
        assertThat(loadedPipeline.canAdminister(), is(true));
    }

    @Test
    public void shouldLoadCanOperateAndLockableFlagForAllPipelines() {
        Username jez = new Username(new CaseInsensitiveString("jez"));
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG).config;
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        pipelineConfig.setTrackingTool(new TrackingTool("http://link", "#"));

        pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline2"));
        pipelineConfig.setMingleConfig(new MingleConfig("baseUrl", "id"));

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
        assertThat(secondPim.getTrackingTool(), is(nullValue()));
        assertThat(secondPim.getMingleConfig(), is(new MingleConfig("baseUrl", "id")));
        assertThat(secondPim.isLockable(), is(false));

        PipelineModel firstPipelineModel = groups.get(0).getPipelineModels().get(0);
        assertPipelineIs(firstPipelineModel, "pipeline1", true, true);
        PipelineInstanceModel pim = firstPipelineModel.getLatestPipelineInstance();
        assertThat(pim.isLockable(), is(true));
        assertThat(pim.getTrackingTool(), is(new TrackingTool("http://link", "#")));
        assertThat(pim.getMingleConfig(), is(new MingleConfig()));

        PipelineModel nonOperatablePipelineModel = groups.get(1).getPipelineModels().get(0);
        assertPipelineIs(nonOperatablePipelineModel, "non-operatable-pipeline", false, false);
    }

    @Test
    public void shouldLoadCurrentlyLockedStatusFlagForAllPipelines() {
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
        when(pipelineLockService.lockedPipeline("pipeline1")).thenReturn(new StageIdentifier("pipeline1", 9, "stage1", "3"));

        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(foo, PipelineSelections.ALL);
        PipelineGroupModel group = groups.get(0);

        PipelineModel pipelineModel1 = group.getPipelineModels().get(0);
        PipelineInstanceModel firstPipelinePIM = pipelineModel1.getActivePipelineInstances().get(0);
        assertThat(firstPipelinePIM.isLockable(), is (true));
        assertThat(firstPipelinePIM.isCurrentlyLocked(), is(true));

        PipelineModel pipelineModel2 = group.getPipelineModels().get(1);
        PipelineInstanceModel secondPipeline = pipelineModel2.getActivePipelineInstances().get(0);
        assertThat(secondPipeline.isLockable(), is (true));
        assertThat(secondPipeline.isCurrentlyLocked(), is(false));

        PipelineModel pipelineModel3 = group.getPipelineModels().get(2);
        PipelineInstanceModel thirdPipeline = pipelineModel3.getActivePipelineInstances().get(0);
        assertThat(thirdPipeline.isLockable(), is (false));
        assertThat(thirdPipeline.isCurrentlyLocked(), is(false));
    }

    @Test
    public void shouldLoadCanOperateFlagForAllNonActivePipelines() {
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
    public void shouldSetAdminPermissionOnEveryGroupModel() throws Exception {
        Username jez = new Username(new CaseInsensitiveString("jez"));
        setupExpectationsForAllActivePipelinesWithTwoGroups(jez);
        when(goConfigService.isUserAdminOfGroup(jez.getUsername(), "defaultGroup")).thenReturn(true);
        when(goConfigService.isUserAdminOfGroup(jez.getUsername(), "foo")).thenReturn(false);

        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(jez, PipelineSelections.ALL);

        assertThat(groups.get(0).getPipelineModels().get(0).canAdminister(), is(true));
        assertThat(groups.get(0).getPipelineModels().get(1).canAdminister(), is(true));
        assertThat(groups.get(1).getPipelineModels().get(0).canAdminister(), is(false));

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
        assertThat(secondPipelineModel.canOperate(), is(canOperate));
        assertThat(secondPipelineModel.canForce(), is(canForce));
        PipelineInstanceModel secondActivePipeline = secondPipelineModel.getActivePipelineInstances().get(0);
        assertThat(secondActivePipeline.getName(), is(pipelineName));
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
    public void shouldLoadAllActivePipelinesWithPreviousStageState() {
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

        when(pipelineTimeline.runBefore(2, new CaseInsensitiveString("pipeline1"))).thenReturn(new PipelineTimelineEntry("pipeline1", 1, 9, new HashMap<String, List<PipelineTimelineEntry.Revision>>()));
        when(pipelineTimeline.runBefore(1, new CaseInsensitiveString("pipeline1"))).thenReturn(new PipelineTimelineEntry("pipeline1", 3, 11, new HashMap<String, List<PipelineTimelineEntry.Revision>>()));

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
    public void allActivePipelines_shouldOnlyKeepSelectedPipelines() {
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
            when(pipelineDao.loadHistory(pipeline,1,0)).thenReturn(createPipelineInstanceModels());
        }
        when(pipelineDao.loadActivePipelines()).thenReturn(activePipelineInstances);

        
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);
        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(foo, new PipelineSelections(Arrays.asList("pipeline1","pipeline2")));
        assertThat(groups.size(),is(2));
        assertThat(groups.get(0).getName(),is("defaultGroup"));
        assertThat(groups.get(0).containsPipeline("pipeline1"),is(false));
        assertThat(groups.get(0).containsPipeline("pipeline2"),is(false));
        assertThat(groups.get(0).containsPipeline("pipeline3"),is(true));
        assertThat(groups.get(0).containsPipeline("pipeline4"),is(true));
        assertThat(groups.get(1).getName(),is("foo"));
        assertThat(groups.get(1).containsPipeline("non-operatable-pipeline"),is(true));
    }

    @Test
    public void allActivePipelines_shouldRemove_EmptyGroups() {
        Username bar = new Username(new CaseInsensitiveString("non-existant"));
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG).config;
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(pipelineDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels());
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);

        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(bar, new PipelineSelections());

        assertThat(groups.isEmpty(),is(true));
    }

    @Test
    public void shouldGetActivePipeline() {
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
        assertThat(groups.size(), is(1));
        assertThat(groups.get(0).getName(), is("defaultGroup"));
        assertThat(groups.get(0).containsPipeline("pipeline1"), is(true));
    }

    @Test
    public void getActivePipelineInstance_shouldRemoveEmptyGroups() {
        Username foo = new Username(new CaseInsensitiveString("foo"));
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.CONFIG).config;
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(pipelineDao.loadActivePipelines()).thenReturn(createPipelineInstanceModels());
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);
        List<PipelineGroupModel> groups = pipelineHistoryService.getActivePipelineInstance(foo, "pipeline1");
        assertThat(groups.isEmpty(),is(true));
    }

    @Test
    public void allActivePipelines_shouldOnlyKeepSelectedPipelines_andRemove_EmptyGroups() {
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
            when(pipelineDao.loadHistory(pipeline,1,0)).thenReturn(createPipelineInstanceModels());
        }
        when(pipelineDao.loadActivePipelines()).thenReturn(activePipelineInstances);


        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);
        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(foo, new PipelineSelections(Arrays.asList("non-operatable-pipeline")));
        assertThat(groups.size(), is(1));
        assertThat(groups.get(0).getName(), is("defaultGroup"));
        assertThat(groups.get(0).containsPipeline("pipeline1"), is(true));
        assertThat(groups.get(0).containsPipeline("pipeline2"),is(true));
        assertThat(groups.get(0).containsPipeline("pipeline3"), is(true));
        assertThat(groups.get(0).containsPipeline("pipeline4"), is(true));
    }

    @Test
    public void allActivePipelines_shouldSkipNullPipelinesWhenLoadingPreviousStageStae() {
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
        
        when(pipelineTimeline.runBefore(2, new CaseInsensitiveString("pipeline1"))).thenReturn(new PipelineTimelineEntry("pipeline1", 1, 9, new HashMap<String, List<PipelineTimelineEntry.Revision>>()));
        when(pipelineTimeline.runBefore(1, new CaseInsensitiveString("pipeline1"))).thenReturn(new PipelineTimelineEntry("pipeline1", 3, 11, new HashMap<String, List<PipelineTimelineEntry.Revision>>()));

        stubPermisssionsForActivePipeline(foo, cruiseConfig, "pipeline1", true, true);
        when(goConfigService.hasPipelineNamed(new CaseInsensitiveString(any(String.class)))).thenReturn(true);

        List<PipelineGroupModel> groups = pipelineHistoryService.allActivePipelineInstances(foo, PipelineSelections.ALL);
        PipelineGroupModel group = groups.get(0);
        PipelineModel pipelineModel = group.getPipelineModels().get(0);
        PipelineInstanceModels activePipelines = pipelineModel.getActivePipelineInstances();

        assertPipelineIs(activePipelines.get(0), "pipeline1", "stage1", StageResult.Failed);
    }

    private void assertPipelineIs(PipelineInstanceModel pipeline, String pipelineName, String activeStageName, StageResult previousStageResult) {
        assertThat(pipeline.getName(), is(pipelineName));
        StageInstanceModel activeStage = pipeline.activeStage();
        assertThat(activeStage.getName(), is(activeStageName));
        assertThat(activeStage.getPreviousStage().getResult(), is(previousStageResult));
    }

    @Test
    public void findPipelineInstanceUsingIdShouldPopulateAppendEmptyStagesFromConfig() throws Exception {
        ensureConfigHasPipeline("pipeline");
        ensureHasPermission(Username.ANONYMOUS, "pipeline");
        PipelineInstanceModel instanceModel = PipelineInstanceModel.createEmptyPipelineInstanceModel("pipeline", BuildCause.createNeverRun(), new StageInstanceModels());
        when(pipelineDao.loadHistoryByIdWithBuildCause(1L)).thenReturn(instanceModel);
        stubConfigServiceToReturnPipeline("pipeline", config);

        PipelineInstanceModel pipelineInstance = pipelineHistoryService.findPipelineInstance("pipeline", 1, 1L, Username.ANONYMOUS, new HttpOperationResult());
        StageInstanceModels models = pipelineInstance.getStageHistory();
        assertThat(models.size(), is(2));
    }

    @Test
    public void findPipelineInstanceShouldPopulateAppendEmptyStagesFromConfig() throws Exception {
        ensureConfigHasPipeline("pipeline");
        ensureHasPermission(Username.ANONYMOUS, "pipeline");
        PipelineInstanceModel instanceModel = PipelineInstanceModel.createEmptyPipelineInstanceModel("pipeline", BuildCause.createNeverRun(), new StageInstanceModels());
        when(pipelineDao.findPipelineHistoryByNameAndCounter("pipeline", 1)).thenReturn(instanceModel);
        stubConfigServiceToReturnPipeline("pipeline", config);

        PipelineInstanceModel pipelineInstance = pipelineHistoryService.findPipelineInstance("pipeline", 1, Username.ANONYMOUS, new HttpOperationResult());
        StageInstanceModels models = pipelineInstance.getStageHistory();
        assertThat(models.size(), is(2));
    }

    @Test
    public void findPipelineInstanceShouldChangeResultTo404WhenPipelineNotFound() throws Exception {
        ensureConfigHasPipeline("pipeline");
        ensureHasPermission(Username.ANONYMOUS, "pipeline");
        when(pipelineDao.findPipelineHistoryByNameAndCounter("pipeline", 1)).thenReturn(null);
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline"))).thenReturn(config);

        HttpOperationResult operationResult = new HttpOperationResult();
        PipelineInstanceModel pipelineInstance = pipelineHistoryService.findPipelineInstance("pipeline", 1, Username.ANONYMOUS, operationResult);
        assertThat(pipelineInstance, is(nullValue()));
        assertThat(operationResult.httpCode(), is(404));
    }

    @Test
    public void shouldPopulateIfPipelineCanBeUnlockedAndIsLockable() throws Exception {
        ensureConfigHasPipeline("pipeline");

        stubConfigServiceToReturnPipeline("pipeline", config);
        when(goConfigService.isLockable("pipeline")).thenReturn(true);

        ensureHasPermission(Username.ANONYMOUS, "pipeline");

        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createNeverRun(), new StageInstanceModels());
        when(pipelineDao.findPipelineHistoryByNameAndCounter("pipeline", 1)).thenReturn(instanceModel);

        when(pipelineUnlockService.canUnlock(eq("pipeline"), eq(Username.ANONYMOUS), any(HttpOperationResult.class))).thenReturn(true);

        PipelineInstanceModel pipelineInstance = pipelineHistoryService.findPipelineInstance("pipeline", 1, Username.ANONYMOUS, new HttpOperationResult());
        assertThat(pipelineInstance.canUnlock(), is(true));
        assertThat(pipelineInstance.isLockable(), is(true));
    }

    @Test public void shouldPopulateWetherStageAndPipelineCanBeRunAccordingToOperatePermissions() throws Exception {
        ensureConfigHasPipeline("pipeline");
        ensureHasPermission(Username.ANONYMOUS, "pipeline");
        StageInstanceModels stages = new StageInstanceModels();
        stages.add(new StageInstanceModel("dev", "1", new JobHistory()));
        stages.add(new StageInstanceModel("qa", "1", new JobHistory()));
        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createNeverRun(), stages);
        when(pipelineDao.findPipelineHistoryByNameAndCounter("pipeline", 1)).thenReturn(instanceModel);
        stubConfigServiceToReturnPipeline("pipeline", config);

        StageInstanceModel firstStage = instanceModel.getStageHistory().get(0);
        when(scheduleService.canRun(instanceModel.getPipelineIdentifier(), firstStage.getName(), CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()),
                instanceModel.hasPreviousStageBeenScheduled(firstStage.getName()))).thenReturn(true);
        StageInstanceModel secondStage = instanceModel.getStageHistory().get(1);
        when(scheduleService.canRun(instanceModel.getPipelineIdentifier(), secondStage.getName(), CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()),
                instanceModel.hasPreviousStageBeenScheduled(secondStage.getName()))).thenReturn(false);

        when(securityService.hasOperatePermissionForStage("pipeline", "dev", CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()))).thenReturn(true);
        when(securityService.hasOperatePermissionForStage("pipeline", "qa", CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()))).thenReturn(false);

        PipelineInstanceModel pipelineInstance = pipelineHistoryService.findPipelineInstance("pipeline", 1, Username.ANONYMOUS, new HttpOperationResult());
        StageInstanceModels models = pipelineInstance.getStageHistory();
        assertThat(models.get(0).getCanRun(), is(true));
        assertThat(models.get(1).getCanRun(), is(false));
        assertThat(models.get(0).hasOperatePermission(), is(true));
        assertThat(models.get(1).hasOperatePermission(), is(false));
    }

    @Test public void shouldPopulatePipelineInstanceModelWithTheBeforeAndAfterForTheGivenPipeline() throws Exception {
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
        assertThat(model.getPipelineBefore(), is(first));
        assertThat(model.getPipelineAfter(), is(second));
        assertThat(model.stage("auto").hasOperatePermission(), is(true));
    }

    @Test
    public void shouldReturnDependencyGraphOnlyIfUserHasPermissions() throws Exception {
        PipelineInstanceModel instanceModel = pim("blahPipeline");
        instanceModel.setId(12);
        PipelineDependencyGraphOld expected = new PipelineDependencyGraphOld(instanceModel, createPipelineInstanceModels());
        when(pipelineDao.pipelineGraphByNameAndCounter("blahPipeline", 12)).thenReturn(expected);
        ensureConfigHasPipeline("blahPipeline");
        when(goConfigService.downstreamPipelinesOf("blahPipeline")).thenReturn(new ArrayList<PipelineConfig>());
        stubConfigServiceToReturnPipeline("blahPipeline", PipelineConfigMother.pipelineConfig("blahPipeline"));
        when(pipelineTimeline.pipelineBefore(12)).thenReturn(1L);
        when(pipelineDao.loadHistory(1)).thenReturn(new PipelineInstanceModel("blahPipeline", 21, "prev-label", BuildCause.createWithEmptyModifications(), new StageInstanceModels()));

        Username foo = new Username(new CaseInsensitiveString("foo"));
        ensureHasPermission(USERNAME, "blahPipeline");
        assertThat(pipelineHistoryService.pipelineDependencyGraph("blahPipeline", 12, foo, new HttpOperationResult()), is(nullValue()));
        assertThat(pipelineHistoryService.pipelineDependencyGraph("blahPipeline", 12, USERNAME, new HttpOperationResult()), is(expected));
    }

    @Test
    public void shouldReturnGraphWithADiffOfMaterialRevisionsForTheGivenPipeline() throws Exception {
        PipelineInstanceModel pim = pim("blahPipeline");
        MaterialRevisions actualRevs = ModificationsMother.multipleModifications();
        pim.setId(23);
        pim.setMaterialRevisionsOnBuildCause(actualRevs);

        PipelineDependencyGraphOld expected = new PipelineDependencyGraphOld(pim, createPipelineInstanceModels());

        when(pipelineDao.pipelineGraphByNameAndCounter("blahPipeline", 12)).thenReturn(expected);
        ensureConfigHasPipeline("blahPipeline");
        when(goConfigService.downstreamPipelinesOf("blahPipeline")).thenReturn(new ArrayList<PipelineConfig>());

        stubConfigServiceToReturnPipeline("blahPipeline", PipelineConfigMother.pipelineConfig("blahPipeline"));
        when(pipelineTimeline.pipelineBefore(23)).thenReturn(1L);
        when(pipelineDao.loadHistory(1L)).thenReturn(new PipelineInstanceModel("blahPipeline", 21, "prev-label", BuildCause.createWithEmptyModifications(), new StageInstanceModels()));

        ensureHasPermission(USERNAME, "blahPipeline");

        PipelineDependencyGraphOld actual = pipelineHistoryService.pipelineDependencyGraph("blahPipeline", 12, USERNAME, new HttpOperationResult());

        assertThat(actual.pipeline().getPreviousLabel(), is("prev-label"));
        assertThat(actual.pipeline().getPreviousCounter(), is(21));
        assertThat(actual.pipeline().getCurrentRevisions(), is(actualRevs));
    }

    @Test
    public void shouldSetStageRunAndOperatePermissionsInTheDependencyGraph() throws Exception {
        PipelineInstanceModel pim = pim("blahPipeline");
        pim.setId(23);
        pim.getStageHistory().add(new StageInstanceModel("stage", "1", new JobHistory()));
        PipelineDependencyGraphOld expected = new PipelineDependencyGraphOld(pim, createPipelineInstanceModels());
        when(pipelineDao.pipelineGraphByNameAndCounter("blahPipeline", 12)).thenReturn(expected);
        ensureConfigHasPipeline("blahPipeline");
        when(goConfigService.downstreamPipelinesOf("blahPipeline")).thenReturn(new ArrayList<PipelineConfig>());
        stubConfigServiceToReturnPipeline("blahPipeline", PipelineConfigMother.pipelineConfig("blahPipeline"));
        when(scheduleService.canRun(pim.getPipelineIdentifier(), "stage", CaseInsensitiveString.str(USERNAME.getUsername()), true)).thenReturn(true);
        when(securityService.hasOperatePermissionForStage("blahPipeline", "stage", CaseInsensitiveString.str(USERNAME.getUsername()))).thenReturn(true);
        when(pipelineTimeline.pipelineBefore(23)).thenReturn(-1L);

        ensureHasPermission(USERNAME, "blahPipeline");
        PipelineDependencyGraphOld actual = pipelineHistoryService.pipelineDependencyGraph("blahPipeline", 12, USERNAME, new HttpOperationResult());
        assertThat(actual.pipeline().getStageHistory().size(), is(1));
        assertThat(actual.pipeline().getStageHistory().get(0).getCanRun(), is(true));
        assertThat(actual.pipeline().getStageHistory().get(0).hasOperatePermission(), is(true));
    }

    @Test
    public void shouldSetStageRunAndOperatePermissionsInTheDependencyGraphForTheDependentPipelinesOfAPipelineInstance() throws Exception {
        PipelineInstanceModel down1Pim = pim("down1");
        down1Pim.getStageHistory().add(new StageInstanceModel("stage", "1", new JobHistory()));
        PipelineInstanceModel actualPim = pim("blahPipeline");
        actualPim.setId(23);
        PipelineDependencyGraphOld returned = new PipelineDependencyGraphOld(actualPim, createPipelineInstanceModels(down1Pim));
        when(pipelineDao.pipelineGraphByNameAndCounter("blahPipeline", 12)).thenReturn(returned);

        ensureConfigHasPipeline("blahPipeline");
        ensureHasPermission(USERNAME, "blahPipeline");

        ensureConfigHasPipeline("down1");
        ensureHasPermission(USERNAME, "down1");

        when(goConfigService.downstreamPipelinesOf("blahPipeline")).thenReturn(Arrays.asList(PipelineConfigMother.pipelineConfig("down1")));
        stubConfigServiceToReturnPipeline("blahPipeline", PipelineConfigMother.pipelineConfig("blahPipeline"));
        stubConfigServiceToReturnPipeline("down1", PipelineConfigMother.pipelineConfig("down1"));

        when(scheduleService.canRun(down1Pim.getPipelineIdentifier(), down1Pim.getStageHistory().first().getName(), CaseInsensitiveString.str(USERNAME.getUsername()), true)).thenReturn(true);
        when(securityService.hasOperatePermissionForStage("down1", "stage", CaseInsensitiveString.str(USERNAME.getUsername()))).thenReturn(true);
        when(pipelineTimeline.pipelineBefore(23)).thenReturn(-1L);


        PipelineDependencyGraphOld actual = pipelineHistoryService.pipelineDependencyGraph("blahPipeline", 12, USERNAME, new HttpOperationResult());

        StageInstanceModel firstStageOfDown1 = actual.dependencies().get(0).getStageHistory().get(0);
        assertThat(actual.dependencies().size(), is(1));
        assertThat(firstStageOfDown1.hasOperatePermission(), is(true));
        assertThat(firstStageOfDown1.getCanRun(), is(true));
    }

    @Test
    public void shouldReturn404WhenPipelineInstanceIsNotPresent() throws Exception {
        when(pipelineDao.pipelineGraphByNameAndCounter("blahPipeline", 12)).thenReturn(null);
        ensureConfigHasPipeline("blahPipeline");
        ensureHasPermission(USERNAME, "blahPipeline");
        HttpOperationResult result = new HttpOperationResult();
        pipelineHistoryService.pipelineDependencyGraph("blahPipeline", 12, USERNAME, result);
        assertThat(result.canContinue(), is(false));
        assertThat(result.httpCode(), is(404));
        assertThat(result.message(), is("Pipeline [blahPipeline] with counter [12] is not found"));
    }

    @Test
    public void shouldRemoveDependencyPipelineWhichIsNotPresentInConfig() throws Exception {
        ensureConfigHasPipeline("blahPipeline");
        ensureConfigHasPipeline("down1");
        ensureConfigContainsPipelineIs("down2", false);
        when(goConfigService.downstreamPipelinesOf("blahPipeline")).thenReturn(Arrays.asList(PipelineConfigMother.pipelineConfig("down1")));

        stubConfigServiceToReturnPipeline("blahPipeline", PipelineConfigMother.pipelineConfig("blahPipeline"));
        PipelineConfig downOneConfig = PipelineConfigMother.pipelineConfig("down1");
        stubConfigServiceToReturnPipeline("down1", downOneConfig);
        
        when(goConfigService.isLockable("blahPipeline")).thenReturn(true);
        when(pipelineUnlockService.canUnlock("blahPipeline", USERNAME, new HttpOperationResult())).thenReturn(true);
        when(pipelineTimeline.pipelineBefore(23)).thenReturn(-1L);

        ensureHasPermission(USERNAME, "blahPipeline");
        ensureHasPermission(USERNAME, "down1");

        PipelineInstanceModel actualPim = pim("down1");
        actualPim.setCanRun(false);
        actualPim.setStageHistory(PipelineHistoryMother.stagePerJob("stage", PipelineHistoryMother.job(JobResult.Failed)));
        PipelineInstanceModel returned = pim("blahPipeline");
        returned.setId(23);
        PipelineDependencyGraphOld graph = new PipelineDependencyGraphOld(returned, createPipelineInstanceModels(actualPim, pim("down2")));
        when(pipelineDao.pipelineGraphByNameAndCounter("blahPipeline", 12)).thenReturn(graph);
        when(schedulingCheckerService.canManuallyTrigger(actualPim.getName(), USERNAME)).thenReturn(true);

        PipelineDependencyGraphOld actual = pipelineHistoryService.pipelineDependencyGraph("blahPipeline", 12, USERNAME, new HttpOperationResult());
        assertThat(actual.dependencies().size(), is(1));
        assertThat(actual.dependencies().get(0), is(actualPim));
        assertThat(actual.dependencies().get(0).getCanRun(), is(true));
    }

    @Test
    public void shouldAddOtherDependentPipelinePresentInConfig() throws Exception {
        ensureConfigHasPipeline("blahPipeline");
        ensureConfigHasPipeline("down1");
        ensureConfigHasPipeline("noPermission");

        PipelineConfig other = GoConfigMother.createPipelineConfigWithMaterialConfig("other");
        other.add(new StageConfig(new CaseInsensitiveString("badChildhood"), new JobConfigs()));

        when(pipelineUnlockService.canUnlock(eq("blahPipeline"), eq(USERNAME), any(HttpOperationResult.class))).thenReturn(true);
        when(goConfigService.isLockable("blahPipeline")).thenReturn(true);
        when(goConfigService.downstreamPipelinesOf("blahPipeline")).thenReturn(Arrays.asList(other));
        stubConfigServiceToReturnPipeline("blahPipeline", PipelineConfigMother.pipelineConfig("blahPipeline", new StageConfig(new CaseInsensitiveString("first"), new JobConfigs())));
        ensureHasPermission(USERNAME, "blahPipeline");

        stubConfigServiceToReturnMaterialAndPipeline("down1", new MaterialConfigs(MaterialConfigsMother.svnMaterialConfig()), PipelineConfigMother.pipelineConfig("down1", new StageConfig(
                new CaseInsensitiveString("first"), new JobConfigs()
        )));
        ensureHasPermission(USERNAME, "down1");

        stubConfigServiceToReturnMaterialAndPipeline("other", new MaterialConfigs(), other);
        when(pipelineTimeline.pipelineBefore(23)).thenReturn(-1L);

        PipelineInstanceModel actualPim = pim("down1");
        PipelineInstanceModel noPermission = pim("noPermission");
        PipelineInstanceModel returned = pim("blahPipeline");
        returned.setId(23);
        PipelineDependencyGraphOld graph = new PipelineDependencyGraphOld(returned, createPipelineInstanceModels(actualPim, noPermission));
        when(pipelineDao.pipelineGraphByNameAndCounter("blahPipeline", 12)).thenReturn(graph);

        PipelineDependencyGraphOld actual = pipelineHistoryService.pipelineDependencyGraph("blahPipeline", 12, USERNAME, new HttpOperationResult());
        assertThat(actual.pipeline().isLockable(), is(true));
        assertThat(actual.pipeline().canUnlock(), is(true));
        assertThat(actual.dependencies().size(), is(2));
        assertThat(actual.dependencies().get(0), is(actualPim));
        assertHasMaterial(actualPim, MaterialsMother.svnMaterial().config());
        PipelineInstanceModel otherInstance = actual.dependencies().get(1);
        assertThat(otherInstance.getName(), is("other"));
        assertThat(otherInstance.getMaterials().size(), is(0));
    }

    @Test
    public void shouldReturnPageNumberForAPipelineCounter() {
        when(pipelineDao.getPageNumberForCounter("some-pipeline", 100, 10)).thenReturn(1);
        assertThat(pipelineHistoryService.getPageNumberForCounter("some-pipeline", 100, 10), is(1));
    }

	@Test
	public void shouldPopulateDataCorrectly_getPipelineStatus() {
		CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
		PipelineConfig pipelineConfig = new PipelineConfig();
        PipelinePauseInfo pipelinePauseInfo = new PipelinePauseInfo(true,"pausing pipeline for some-reason", "some-one");
		when(cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipeline-name"))).thenReturn(pipelineConfig);
		when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
		when(securityService.hasViewPermissionForPipeline("user-name", "pipeline-name")).thenReturn(true);
		when(pipelinePauseService.pipelinePauseInfo("pipeline-name")).thenReturn(pipelinePauseInfo);
		when(pipelineLockService.isLocked("pipeline-name")).thenReturn(true);
		when(schedulingCheckerService.canManuallyTrigger(eq(pipelineConfig), eq("user-name"), any(ServerHealthStateOperationResult.class))).thenReturn(true);

		PipelineStatusModel pipelineStatus = pipelineHistoryService.getPipelineStatus("pipeline-name", "user-name", new HttpOperationResult());

		assertThat(pipelineStatus.isPaused(), is(true));
		assertThat(pipelineStatus.pausedCause(), is("pausing pipeline for some-reason"));
		assertThat(pipelineStatus.pausedBy(), is("some-one"));
		assertThat(pipelineStatus.isLocked(), is(true));
		assertThat(pipelineStatus.isSchedulable(), is(true));
	}

	@Test
	public void shouldPopulateResultAsNotFound_getPipelineStatus() {
		CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
		when(cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipeline-name"))).thenReturn(null);
		when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

		HttpOperationResult result = new HttpOperationResult();
		PipelineStatusModel pipelineStatus = pipelineHistoryService.getPipelineStatus("pipeline-name", "user-name", result);

		assertThat(pipelineStatus, is(nullValue()));
		assertThat(result.httpCode(), is(404));
	}

	@Test
	public void shouldPopulateResultAsUnauthorized_getPipelineStatus() {
		CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
		PipelineConfig pipelineConfig = new PipelineConfig();
		when(cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipeline-name"))).thenReturn(pipelineConfig);
		when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
		when(securityService.hasViewPermissionForPipeline("user-name", "pipeline-name")).thenReturn(false);

		HttpOperationResult result = new HttpOperationResult();
		PipelineStatusModel pipelineStatus = pipelineHistoryService.getPipelineStatus("pipeline-name", "user-name", result);

		assertThat(pipelineStatus, is(nullValue()));
		assertThat(result.httpCode(), is(401));
	}

	@Test
	public void shouldPopulateResultAsNotFoundWhenPipelineNotFound_loadMinimalData() {
		String pipelineName = "unknown-pipeline";
		CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
		when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(false);
		when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

		HttpOperationResult result = new HttpOperationResult();
		PipelineInstanceModels pipelineInstanceModels = pipelineHistoryService.loadMinimalData(pipelineName, Pagination.pageFor(0, 0, 10), "looser", result);

		assertThat(pipelineInstanceModels, is(nullValue()));
		assertThat(result.httpCode(), is(404));
		assertThat(result.detailedMessage(), is("Not Found { Pipeline " + pipelineName + " not found }\n"));
	}

	@Test
	public void shouldPopulateResultAsUnauthorizedWhenUserNotAllowedToViewPipeline_loadMinimalData() {
		String noAccessUserName = "foo";
		String withAccessUserName = "admin";
		String pipelineName = "no-access-pipeline";
		CruiseConfig cruiseConfig = mock(BasicCruiseConfig.class);
		when(cruiseConfig.hasPipelineNamed(new CaseInsensitiveString(pipelineName))).thenReturn(true);
		when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);

		when(securityService.hasViewPermissionForPipeline(noAccessUserName, pipelineName)).thenReturn(false);
		when(securityService.hasViewPermissionForPipeline(withAccessUserName, pipelineName)).thenReturn(true);

		when(pipelineDao.loadHistory(pipelineName, 10, 0)).thenReturn(PipelineInstanceModels.createPipelineInstanceModels());

		HttpOperationResult result = new HttpOperationResult();
		PipelineInstanceModels pipelineInstanceModels = pipelineHistoryService.loadMinimalData(pipelineName, Pagination.pageFor(0, 1, 10), noAccessUserName, result);

		assertThat(pipelineInstanceModels, is(nullValue()));
		assertThat(result.httpCode(), is(401));

		result = new HttpOperationResult();
		pipelineInstanceModels = pipelineHistoryService.loadMinimalData(pipelineName, Pagination.pageFor(0, 1, 10), withAccessUserName, result);

		assertThat(pipelineInstanceModels, is(not(nullValue())));
		assertThat(result.canContinue(), is(true));
	}

    @Test
    public void shouldUpdateCommentUsingPipelineDao() {
        CaseInsensitiveString authorizedUser = new CaseInsensitiveString("can-access");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(securityService.hasOperatePermissionForPipeline(authorizedUser, "pipeline_name")).thenReturn(true);

        pipelineHistoryService.updateComment("pipeline_name", 1, "test comment", new Username(authorizedUser), result);

        verify(pipelineDao, times(1)).updateComment("pipeline_name", 1, "test comment");
    }

    @Test
    public void shouldNotUpdateCommentWhenUserIsUnauthorized() {
        CaseInsensitiveString unauthorizedUser = new CaseInsensitiveString("cannot-access");
        String pipelineName = "pipeline_name";
        when(securityService.hasOperatePermissionForPipeline(unauthorizedUser, pipelineName)).thenReturn(false);

        HttpLocalizedOperationResult result = mock(HttpLocalizedOperationResult.class);
        pipelineHistoryService.updateComment(pipelineName, 1, "test comment", new Username(unauthorizedUser), result);

        verify(pipelineDao, never()).updateComment(pipelineName, 1, "test comment");
        verify(result, times(1)).unauthorized(LocalizedMessage.cannotOperatePipeline(pipelineName), HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
    }

    @Test
    public void shouldFailWhenFeatureIsToggledOff_updateComment() {
        when(featureToggleService.isToggleOn(Toggles.PIPELINE_COMMENT_FEATURE_TOGGLE_KEY)).thenReturn(false);
        Toggles.initializeWith(featureToggleService);
        CaseInsensitiveString unauthorizedUser = new CaseInsensitiveString("cannot-access");
        String pipelineName = "pipeline_name";

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        pipelineHistoryService.updateComment(pipelineName, 1, "test comment", new Username(unauthorizedUser), result);

        assertThat(result.httpCode(), is(HttpStatus.SC_NOT_IMPLEMENTED));
        assertThat((Localizable.CurryableLocalizable) result.localizable(), is(LocalizedMessage.string("FEATURE_NOT_AVAILABLE", "Pipeline Comment")));
        verify(pipelineDao, never()).updateComment(pipelineName, 1, "test comment");
    }

    private void stubConfigServiceToReturnMaterialAndPipeline(String downPipelineName, MaterialConfigs downPipelineMaterial, PipelineConfig down1Config) {
        when(goConfigService.materialConfigsFor(new CaseInsensitiveString(downPipelineName))).thenReturn(downPipelineMaterial);
        stubConfigServiceToReturnPipeline(downPipelineName, down1Config);
    }

    private void stubConfigServiceToReturnPipeline(String blahPipelineName, PipelineConfig blahPipelineConfig) {
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString(blahPipelineName))).thenReturn(blahPipelineConfig);
        when(goConfigService.findFirstStageOfPipeline(new CaseInsensitiveString(blahPipelineName))).thenReturn(blahPipelineConfig.get(0));
    }

    private void assertHasMaterial(PipelineInstanceModel otherInstance, MaterialConfig materialConfig) {
        assertThat(otherInstance, is(not(nullValue())));
        assertThat(otherInstance.getMaterials(), is(not(nullValue())));
        assertThat(otherInstance.getMaterials().size(), is(1));
        assertThat(otherInstance.getMaterials(), hasItem(materialConfig));
    }

    private PipelineInstanceModel pim(String pipelineName) {
        return PipelineHistoryMother.singlePipeline(pipelineName, new StageInstanceModels());
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
