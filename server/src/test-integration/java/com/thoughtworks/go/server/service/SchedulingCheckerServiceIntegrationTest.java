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
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.fixture.PipelineWithMultipleStages;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class SchedulingCheckerServiceIntegrationTest {
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private SchedulingCheckerService schedulingChecker;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private StageService stageService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired
    private TriggerMonitor triggerMonitor;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private PipelinePauseService pipelinePauseService;

    private PipelineWithMultipleStages pipelineFixture;

    private static final String APPROVED_USER = "jez";
    private static GoConfigFileHelper configFileHelper = new GoConfigFileHelper(ConfigFileFixture.XML_WITH_ENTERPRISE_LICENSE_FOR_TWO_USERS);
    public DiskSpaceSimulator diskSpaceSimulator;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        configFileHelper.onSetUp();
        configFileHelper.usingCruiseConfigDao(goConfigDao);

        pipelineFixture = new PipelineWithMultipleStages(2, materialRepository, transactionTemplate, tempDir);
        pipelineFixture.usingConfigHelper(configFileHelper).usingDbHelper(dbHelper).onSetUp();
        pipelineFixture.configStageAsManualApprovalWithApprovedUsers(pipelineFixture.ftStage, APPROVED_USER);

        configFileHelper.addSecurityWithAdminConfig();
        dbHelper.onSetUp();

        diskSpaceSimulator = new DiskSpaceSimulator();

    }

    @AfterEach
    public void teardown() throws Exception {
        if (configFileHelper != null) {
            configFileHelper.onTearDown();
        }

        dbHelper.onTearDown();
        pipelineFixture.onTearDown();
        diskSpaceSimulator.onTearDown();
        pipelineScheduleQueue.clear();
        triggerMonitor.clear_for_test();
    }

    @Test
    public void shouldPassCheckingWhenUserHasPermissionForRerun() throws Exception {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        assertTrue(schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, APPROVED_USER, new ServerHealthStateOperationResult()));
    }

    @Test
    public void shouldPassCheckingWhenUserHasPermissionForManualTrigger() throws Exception {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();

        configFileHelper.addAuthorizedUserForStage(pipeline.getName(), pipelineFixture.devStage, APPROVED_USER);

        assertTrue(schedulingChecker.canManuallyTrigger(pipelineFixture.pipelineConfig(), APPROVED_USER,
                new ServerHealthStateOperationResult()));
    }

    @Test
    public void shouldFailCheckingWhenPipelineNotYetScheduledButInScheduleQueue() throws Exception {
        String pipelineName = "blahPipeline";
        PipelineConfig pipelineConfig = configFileHelper.addPipelineWithGroup("group2", pipelineName, "stage", "job");
        pipelineScheduleQueue.schedule(new CaseInsensitiveString(pipelineName), BuildCause.createManualForced());
        HttpOperationResult operationResult = new HttpOperationResult();
        assertThat(schedulingChecker.canManuallyTrigger(pipelineConfig, "blahUser", operationResult), is(false));
        assertThat(operationResult.canContinue(), is(false));
        assertThat(operationResult.message(), is("Failed to trigger pipeline: blahPipeline"));
    }

    @Test
    public void shouldFailCheckIfTheStageHasAllowOnlyOnSuccessSetAndPreviousStageFailed() {
        configFileHelper.configureStageAsManualApproval(pipelineFixture.pipelineName, pipelineFixture.ftStage, true);
        configFileHelper.addAuthorizedUserForStage(pipelineFixture.pipelineName, pipelineFixture.ftStage, APPROVED_USER);
        configFileHelper.lockPipeline(pipelineFixture.pipelineName);
        Pipeline pipeline = pipelineFixture.schedulePipeline();
        firstStageFailedAndSecondStageNotStarted(pipeline);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, APPROVED_USER, result);
        ServerHealthState serverHealthState = result.getServerHealthState();
        assertThat(serverHealthState.isSuccess(), is(false));
        assertThat(serverHealthState.getDescription(), is("Cannot schedule ft as the previous stage dev has Failed!"));
        assertThat(serverHealthState.getMessage(), is("Cannot schedule ft as the previous stage dev has Failed!"));
        assertThat(serverHealthState.getType(), is(HealthStateType.forbidden()));
    }

    @Test
    public void shouldReturnSuccessIfTheStageHasAllowOnlyOnSuccessUnSetAndPreviousStageFailed() {
        configFileHelper.lockPipeline(pipelineFixture.pipelineName);
        Pipeline pipeline = pipelineFixture.schedulePipeline();
        firstStageFailedAndSecondStageNotStarted(pipeline);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, APPROVED_USER, result);
        ServerHealthState serverHealthState = result.getServerHealthState();
        assertThat(serverHealthState.isSuccess(), is(true));
    }

    @Test
    public void shouldPassCheckingWhenPipelineNotYetScheduledButInScheduleQueueBecauseOfAutoBuild() throws Exception {
        String pipelineName = "blahPipeline";
        configFileHelper.addPipelineWithGroup("group2", pipelineName, "stage", "job");
        configFileHelper.addAuthorizedUserForStage(pipelineName, "stage", APPROVED_USER);

        pipelineScheduleQueue.schedule(new CaseInsensitiveString(pipelineName), BuildCause.createWithEmptyModifications());
        assertThat(schedulingChecker.canManuallyTrigger(pipelineName, new Username(APPROVED_USER)), is(true));
    }

    @Test
    public void shouldFailCheckingWhenPipelineNotYetScheduledButInTriggerMonitor() throws Exception {
        String pipelineName = "blahPipeline";
        PipelineConfig pipelineConfig = configFileHelper.addPipelineWithGroup("group2", pipelineName, "stage", "job");
        triggerMonitor.markPipelineAsAlreadyTriggered(pipelineConfig.name());

        HttpOperationResult operationResult = new HttpOperationResult();
        assertThat(schedulingChecker.canManuallyTrigger(pipelineConfig, "blahUser", operationResult), is(false));
        assertThat(operationResult.canContinue(), is(false));
        assertThat(operationResult.message(), is("Failed to trigger pipeline: blahPipeline"));
    }

    @Test
    public void shouldNotPassCheckingWhenUserHasNoPermissionForRerun() throws Exception {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, "gli", result), is(false));
        assertThat(result.getServerHealthState().getDescription(), containsString("does not have permission"));
    }

    @Test
    public void shouldNotPassCheckingWhenUserHasNoPermissionForManualTrigger() throws Exception {
        pipelineFixture.createdPipelineWithAllStagesPassed();
        pipelineFixture.configStageAsManualApprovalWithApprovedUsers(pipelineFixture.devStage, APPROVED_USER);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canManuallyTrigger(pipelineFixture.pipelineConfig(), "gli", result), is(false));
        assertThat(result.getServerHealthState().getDescription(), containsString("does not have permission"));
    }

    @Test
    public void shouldNotPassCheckingWhenAnyStageIsActiveInPipelineForRerun() throws Exception {
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStageScheduled();
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, APPROVED_USER, result);
        assertThat(result.getServerHealthState().isSuccess(), is(false));
        assertThat(result.getServerHealthState().getDescription(), containsString("still in progress"));
        assertThat(result.getServerHealthState().getDescription(), containsString(pipeline.getName()));
    }

    @Test
    public void shouldNotScheduleLockedPipelineIfAnyStageIsActiveInAnyPipeline() throws Exception {
        configFileHelper.lockPipeline(pipelineFixture.pipelineName);
        Pipeline pipeline = pipelineFixture.schedulePipeline();
        firstStagePassedAndSecondStageBuilding(pipeline);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canTriggerManualPipeline(pipeline.getName(), APPROVED_USER, result);
        assertThat(result.getServerHealthState().isSuccess(), is(false));
        assertThat(result.getServerHealthState().getDescription(), containsString("is locked"));
        assertThat(result.getServerHealthState().getDescription(), containsString(pipeline.getName()));
    }

    private void firstStagePassedAndSecondStageBuilding(Pipeline pipeline) {
        firstStagePassedAndSecondStageNotStarted(pipeline);
        scheduleService.scheduleStage(pipeline, pipelineFixture.ftStage, APPROVED_USER, new ScheduleService.NewStageInstanceCreator(goConfigService), null);
    }

    private void firstStagePassedAndSecondStageNotStarted(Pipeline pipeline) {
        pipelineService.save(pipeline);
        Stage stage = pipeline.getFirstStage();
        stage.building();
        stageService.updateResult(stage);
        dbHelper.completeAllJobs(stage, JobResult.Passed);
        stageService.updateResult(stage);
    }

    private void firstStageFailedAndSecondStageNotStarted(Pipeline pipeline) {
        pipelineService.save(pipeline);
        Stage stage = pipeline.getFirstStage();
        stage.building();
        stageService.updateResult(stage);
        dbHelper.completeAllJobs(stage, JobResult.Failed);
        stageService.updateResult(stage);
    }

    @Test
    public void shouldNotScheduleLockedPipelineFromTimerIfAnyStageIsActiveInAnyPipeline() throws Exception {
        configFileHelper.lockPipeline(pipelineFixture.pipelineName);
        Pipeline pipeline = pipelineFixture.schedulePipeline();
        firstStagePassedAndSecondStageBuilding(pipeline);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canTriggerPipelineWithTimer(pipeline.getName(), result);
        assertThat(result.getServerHealthState().isSuccess(), is(false));
        assertThat(result.getServerHealthState().getDescription(), containsString("is locked "));
        assertThat(result.getServerHealthState().getDescription(), containsString(pipeline.getName()));
    }

    @Test
    public void shouldNotScheduleStageInLockedPipelineIfAnyStageIsActiveInAnyPipeline() throws Exception {
        Pipeline completed = pipelineFixture.createdPipelineWithAllStagesPassed();
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        configFileHelper.lockPipeline(pipeline.getName());
        pipelineService.save(pipeline);//to ensure locking happens(fixture uses the dao to directly save it to the db, hence lock is not taken)
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();

        configFileHelper.addAuthorizedUserForStage(pipeline.getName(), pipelineFixture.devStage, APPROVED_USER);
        schedulingChecker.canScheduleStage(completed.getIdentifier(), pipelineFixture.devStage, APPROVED_USER, result);

        assertThat(result.getServerHealthState().isSuccess(), is(false));
        assertThat(result.getServerHealthState().getDescription(), containsString("is locked"));
        assertThat(result.getServerHealthState().getDescription(), containsString(pipeline.getName()));
    }

    @Test
    public void shouldScheduleANewstageInALockedPipeline() throws Exception {
        configFileHelper.lockPipeline(pipelineFixture.pipelineName);
        Pipeline pipeline = pipelineFixture.schedulePipeline();
        firstStagePassedAndSecondStageNotStarted(pipeline);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, APPROVED_USER, result);
        assertThat(result.getServerHealthState().isSuccess(), is(true));
    }

    @Test
    public void shouldNotPassCheckingWhenAnyStageIsActiveInPipelineForManualTrigger() throws Exception {
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStageScheduled();
        String username = APPROVED_USER;
        PipelineConfig pipelineConfig = pipelineFixture.pipelineConfig();
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canManuallyTrigger(pipelineConfig, username, result), is(false));
        assertThat(result.getServerHealthState().getDescription(), containsString("still in progress"));
        assertThat(result.getServerHealthState().getDescription(), containsString(pipeline.getName()));
    }

    @Test
    public void shouldNotPassCheckingWhenTargetStageIsActiveInAnyPipelineForRerun() throws Exception {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        pipelineFixture.createPipelineWithFirstStageScheduled();
        configFileHelper.addAuthorizedUserForStage(pipeline.getName(), pipelineFixture.devStage, APPROVED_USER);

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.devStage, APPROVED_USER, result), is(false));
        assertThat(result.getServerHealthState().getDescription(), containsString("still in progress"));
        assertThat(result.getServerHealthState().getDescription(), containsString(pipeline.getName()));
        assertThat(result.getServerHealthState().getDescription(), containsString(pipelineFixture.devStage));
    }

    @Test
    public void shouldNotPassCheckingWhenTargetStageIsActiveInAnyPipelineForManualTrigger() throws Exception {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        pipelineFixture.createPipelineWithFirstStageScheduled();

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canManuallyTrigger(pipelineFixture.pipelineConfig(), APPROVED_USER, result), is(false));
        assertThat(result.getServerHealthState().getDescription(), containsString("still in progress"));
        assertThat(result.getServerHealthState().getDescription(), containsString(pipeline.getName()));
        assertThat(result.getServerHealthState().getDescription(), containsString(pipelineFixture.devStage));
    }

    @Test
    public void shouldNotPassCheckingIfPipelineIsPausedForRerun() throws Exception {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        Username userName = new Username(new CaseInsensitiveString("A humble developer"));

        configFileHelper.setOperatePermissionForGroup(pipelineFixture.groupName, userName.getUsername().toString(), APPROVED_USER);
        pipelinePauseService.pause(pipeline.getName(), "Upgrade scheduled", userName);

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, APPROVED_USER, result), is(false));
        assertThat(result.getServerHealthState().getDescription(), containsString(pipeline.getName() + " is paused"));
    }

    @Test
    public void shouldNotPassCheckingIfPipelineIsPausedForManualTrigger() throws Exception {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        Username userName = new Username(new CaseInsensitiveString("A humble developer"));

        configFileHelper.setOperatePermissionForGroup(pipelineFixture.groupName, userName.getUsername().toString(), APPROVED_USER);
        pipelinePauseService.pause(pipeline.getName(), "Upgrade scheduled", userName);

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canManuallyTrigger(pipelineFixture.pipelineConfig(), APPROVED_USER, result), is(false));
        assertThat(result.getServerHealthState().getDescription(), containsString(pipeline.getName() + " is paused"));
    }

    @Test
    public void shouldNotPassCheckingIfDiskSpaceIsFullForManualTrigger() throws Exception {
        String limit = diskSpaceSimulator.simulateDiskFull();

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();

        assertThat(schedulingChecker.canManuallyTrigger(pipelineFixture.pipelineConfig(), APPROVED_USER, result), is(false));
        assertThat(result.getServerHealthState().getDescription(),
                containsString(String.format("GoCD has less than %sb of disk space", limit)));
    }

    @Test
    public void shouldNotPassCheckingIfDiskSpaceIsFullForRerun() throws Exception {
        String limit = diskSpaceSimulator.simulateDiskFull();

        String pipelineName = pipelineFixture.pipelineName;
        String label = "LATEST";
        String stageName = CaseInsensitiveString.str(pipelineFixture.devStage().name());
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();

        configFileHelper.addAuthorizedUserForStage(pipelineName, stageName, APPROVED_USER);
        assertThat(schedulingChecker.canScheduleStage(new PipelineIdentifier(pipelineName, 1, label), stageName, APPROVED_USER, result), is(false));
        assertThat(result.getServerHealthState().getDescription(),
                containsString(String.format("GoCD has less than %sb of disk space", limit)));
    }

    @Test
    public void shouldStopStageRerunIfDiskSpaceIsLessThanMinimum() throws Exception {
        String limit = diskSpaceSimulator.simulateDiskFull();

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canSchedule(result), is(false));
        assertThat(result.getServerHealthState().getDescription(),
                containsString(String.format("GoCD has less than %sb of disk space", limit)));
    }

    @Test
    public void shouldSkipSecurityCheckingForCruiseUserWhenTimerTriggersPipeline() throws Exception {
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canTriggerPipelineWithTimer(goConfigService.getAllPipelineConfigs().get(0), result);
        assertThat(result.canContinue(), is(true));
    }
}
