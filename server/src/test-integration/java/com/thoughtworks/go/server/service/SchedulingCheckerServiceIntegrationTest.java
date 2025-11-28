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

import static org.assertj.core.api.Assertions.assertThat;
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
    private final GoConfigFileHelper configHelper = new GoConfigFileHelper(ConfigFileFixture.XML_WITH_ENTERPRISE_LICENSE_FOR_TWO_USERS);
    public DiskSpaceSimulator diskSpaceSimulator;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);

        pipelineFixture = new PipelineWithMultipleStages(2, materialRepository, transactionTemplate, tempDir);
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        pipelineFixture.configStageAsManualApprovalWithApprovedUsers(pipelineFixture.ftStage, APPROVED_USER);

        configHelper.addSecurityWithAdminConfig();
        dbHelper.onSetUp();

        diskSpaceSimulator = new DiskSpaceSimulator();
    }

    @AfterEach
    public void teardown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
        pipelineFixture.onTearDown();
        diskSpaceSimulator.onTearDown();
        pipelineScheduleQueue.clear();
        triggerMonitor.clear_for_test();
    }

    @Test
    public void shouldPassCheckingWhenUserHasPermissionForRerun() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        assertTrue(schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, APPROVED_USER, new ServerHealthStateOperationResult()));
    }

    @Test
    public void shouldPassCheckingWhenUserHasPermissionForManualTrigger() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();

        configHelper.addAuthorizedUserForStage(pipeline.getName(), pipelineFixture.devStage, APPROVED_USER);

        assertTrue(schedulingChecker.canManuallyTrigger(pipelineFixture.pipelineConfig(), APPROVED_USER,
                new ServerHealthStateOperationResult()));
    }

    @Test
    public void shouldFailCheckingWhenPipelineNotYetScheduledButInScheduleQueue() {
        String pipelineName = "blahPipeline";
        PipelineConfig pipelineConfig = configHelper.addPipelineWithGroup("group2", pipelineName, "stage", "job");
        pipelineScheduleQueue.schedule(new CaseInsensitiveString(pipelineName), BuildCause.createManualForced());
        HttpOperationResult operationResult = new HttpOperationResult();
        assertThat(schedulingChecker.canManuallyTrigger(pipelineConfig, "blahUser", operationResult)).isFalse();
        assertThat(operationResult.canContinue()).isFalse();
        assertThat(operationResult.message()).isEqualTo("Failed to trigger pipeline: blahPipeline");
    }

    @Test
    public void shouldFailCheckIfTheStageHasAllowOnlyOnSuccessSetAndPreviousStageFailed() {
        configHelper.configureStageAsManualApproval(pipelineFixture.pipelineName, pipelineFixture.ftStage, true);
        configHelper.addAuthorizedUserForStage(pipelineFixture.pipelineName, pipelineFixture.ftStage, APPROVED_USER);
        configHelper.lockPipeline(pipelineFixture.pipelineName);
        Pipeline pipeline = pipelineFixture.schedulePipeline();
        firstStageFailedAndSecondStageNotStarted(pipeline);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, APPROVED_USER, result);
        ServerHealthState serverHealthState = result.getServerHealthState();
        assertThat(serverHealthState.isSuccess()).isFalse();
        assertThat(serverHealthState.getDescription()).isEqualTo("Cannot schedule ft as the previous stage dev has Failed!");
        assertThat(serverHealthState.getMessage()).isEqualTo("Cannot schedule ft as the previous stage dev has Failed!");
        assertThat(serverHealthState.getType()).isEqualTo(HealthStateType.forbidden());
    }

    @Test
    public void shouldReturnSuccessIfTheStageHasAllowOnlyOnSuccessUnSetAndPreviousStageFailed() {
        configHelper.lockPipeline(pipelineFixture.pipelineName);
        Pipeline pipeline = pipelineFixture.schedulePipeline();
        firstStageFailedAndSecondStageNotStarted(pipeline);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, APPROVED_USER, result);
        ServerHealthState serverHealthState = result.getServerHealthState();
        assertThat(serverHealthState.isSuccess()).isTrue();
    }

    @Test
    public void shouldPassCheckingWhenPipelineNotYetScheduledButInScheduleQueueBecauseOfAutoBuild() {
        String pipelineName = "blahPipeline";
        configHelper.addPipelineWithGroup("group2", pipelineName, "stage", "job");
        configHelper.addAuthorizedUserForStage(pipelineName, "stage", APPROVED_USER);

        pipelineScheduleQueue.schedule(new CaseInsensitiveString(pipelineName), BuildCause.createWithEmptyModifications());
        assertThat(schedulingChecker.canManuallyTrigger(pipelineName, new Username(APPROVED_USER))).isTrue();
    }

    @Test
    public void shouldFailCheckingWhenPipelineNotYetScheduledButInTriggerMonitor() {
        String pipelineName = "blahPipeline";
        PipelineConfig pipelineConfig = configHelper.addPipelineWithGroup("group2", pipelineName, "stage", "job");
        triggerMonitor.markPipelineAsAlreadyTriggered(pipelineConfig.name());

        HttpOperationResult operationResult = new HttpOperationResult();
        assertThat(schedulingChecker.canManuallyTrigger(pipelineConfig, "blahUser", operationResult)).isFalse();
        assertThat(operationResult.canContinue()).isFalse();
        assertThat(operationResult.message()).isEqualTo("Failed to trigger pipeline: blahPipeline");
    }

    @Test
    public void shouldNotPassCheckingWhenUserHasNoPermissionForRerun() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, "gli", result)).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains("does not have permission");
    }

    @Test
    public void shouldNotPassCheckingWhenUserHasNoPermissionForManualTrigger() {
        pipelineFixture.createdPipelineWithAllStagesPassed();
        pipelineFixture.configStageAsManualApprovalWithApprovedUsers(pipelineFixture.devStage, APPROVED_USER);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canManuallyTrigger(pipelineFixture.pipelineConfig(), "gli", result)).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains("does not have permission");
    }

    @Test
    public void shouldNotPassCheckingWhenAnyStageIsActiveInPipelineForRerun() {
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStageScheduled();
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, APPROVED_USER, result);
        assertThat(result.getServerHealthState().isSuccess()).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains("still in progress");
        assertThat(result.getServerHealthState().getDescription()).contains(pipeline.getName());
    }

    @Test
    public void shouldNotScheduleLockedPipelineIfAnyStageIsActiveInAnyPipeline() {
        configHelper.lockPipeline(pipelineFixture.pipelineName);
        Pipeline pipeline = pipelineFixture.schedulePipeline();
        firstStagePassedAndSecondStageBuilding(pipeline);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canTriggerManualPipeline(pipeline.getName(), APPROVED_USER, result);
        assertThat(result.getServerHealthState().isSuccess()).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains("is locked");
        assertThat(result.getServerHealthState().getDescription()).contains(pipeline.getName());
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
    public void shouldNotScheduleLockedPipelineFromTimerIfAnyStageIsActiveInAnyPipeline() {
        configHelper.lockPipeline(pipelineFixture.pipelineName);
        Pipeline pipeline = pipelineFixture.schedulePipeline();
        firstStagePassedAndSecondStageBuilding(pipeline);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canTriggerPipelineWithTimer(pipeline.getName(), result);
        assertThat(result.getServerHealthState().isSuccess()).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains("is locked ");
        assertThat(result.getServerHealthState().getDescription()).contains(pipeline.getName());
    }

    @Test
    public void shouldNotScheduleStageInLockedPipelineIfAnyStageIsActiveInAnyPipeline() {
        Pipeline completed = pipelineFixture.createdPipelineWithAllStagesPassed();
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        configHelper.lockPipeline(pipeline.getName());
        pipelineService.save(pipeline);//to ensure locking happens(fixture uses the dao to directly save it to the db, hence lock is not taken)
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();

        configHelper.addAuthorizedUserForStage(pipeline.getName(), pipelineFixture.devStage, APPROVED_USER);
        schedulingChecker.canScheduleStage(completed.getIdentifier(), pipelineFixture.devStage, APPROVED_USER, result);

        assertThat(result.getServerHealthState().isSuccess()).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains("is locked");
        assertThat(result.getServerHealthState().getDescription()).contains(pipeline.getName());
    }

    @Test
    public void shouldScheduleANewstageInALockedPipeline() {
        configHelper.lockPipeline(pipelineFixture.pipelineName);
        Pipeline pipeline = pipelineFixture.schedulePipeline();
        firstStagePassedAndSecondStageNotStarted(pipeline);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, APPROVED_USER, result);
        assertThat(result.getServerHealthState().isSuccess()).isTrue();
    }

    @Test
    public void shouldNotPassCheckingWhenAnyStageIsActiveInPipelineForManualTrigger() {
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStageScheduled();
        PipelineConfig pipelineConfig = pipelineFixture.pipelineConfig();
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canManuallyTrigger(pipelineConfig, APPROVED_USER, result)).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains("still in progress");
        assertThat(result.getServerHealthState().getDescription()).contains(pipeline.getName());
    }

    @Test
    public void shouldNotPassCheckingWhenTargetStageIsActiveInAnyPipelineForRerun() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        pipelineFixture.createPipelineWithFirstStageScheduled();
        configHelper.addAuthorizedUserForStage(pipeline.getName(), pipelineFixture.devStage, APPROVED_USER);

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.devStage, APPROVED_USER, result)).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains("still in progress");
        assertThat(result.getServerHealthState().getDescription()).contains(pipeline.getName());
        assertThat(result.getServerHealthState().getDescription()).contains(pipelineFixture.devStage);
    }

    @Test
    public void shouldNotPassCheckingWhenTargetStageIsActiveInAnyPipelineForManualTrigger() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        pipelineFixture.createPipelineWithFirstStageScheduled();

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canManuallyTrigger(pipelineFixture.pipelineConfig(), APPROVED_USER, result)).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains("still in progress");
        assertThat(result.getServerHealthState().getDescription()).contains(pipeline.getName());
        assertThat(result.getServerHealthState().getDescription()).contains(pipelineFixture.devStage);
    }

    @Test
    public void shouldNotPassCheckingIfPipelineIsPausedForRerun() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        Username userName = new Username(new CaseInsensitiveString("A humble developer"));

        configHelper.setOperatePermissionForGroup(pipelineFixture.groupName, userName.getUsername().toString(), APPROVED_USER);
        pipelinePauseService.pause(pipeline.getName(), "Upgrade scheduled", userName);

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.ftStage, APPROVED_USER, result)).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains(pipeline.getName() + " is paused");
    }

    @Test
    public void shouldNotPassCheckingIfPipelineIsPausedForManualTrigger() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        Username userName = new Username(new CaseInsensitiveString("A humble developer"));

        configHelper.setOperatePermissionForGroup(pipelineFixture.groupName, userName.getUsername().toString(), APPROVED_USER);
        pipelinePauseService.pause(pipeline.getName(), "Upgrade scheduled", userName);

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canManuallyTrigger(pipelineFixture.pipelineConfig(), APPROVED_USER, result)).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains(pipeline.getName() + " is paused");
    }

    @Test
    public void shouldNotPassCheckingIfDiskSpaceIsFullForManualTrigger() {
        String limit = diskSpaceSimulator.simulateDiskFull();

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();

        assertThat(schedulingChecker.canManuallyTrigger(pipelineFixture.pipelineConfig(), APPROVED_USER, result)).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains(String.format("GoCD has less than %sb of disk space", limit));
    }

    @Test
    public void shouldNotPassCheckingIfDiskSpaceIsFullForRerun() {
        String limit = diskSpaceSimulator.simulateDiskFull();

        String pipelineName = pipelineFixture.pipelineName;
        String label = "LATEST";
        String stageName = CaseInsensitiveString.str(pipelineFixture.devStage().name());
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();

        configHelper.addAuthorizedUserForStage(pipelineName, stageName, APPROVED_USER);
        assertThat(schedulingChecker.canScheduleStage(new PipelineIdentifier(pipelineName, 1, label), stageName, APPROVED_USER, result)).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains(String.format("GoCD has less than %sb of disk space", limit));
    }

    @Test
    public void shouldStopStageRerunIfDiskSpaceIsLessThanMinimum() {
        String limit = diskSpaceSimulator.simulateDiskFull();

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canSchedule(result)).isFalse();
        assertThat(result.getServerHealthState().getDescription()).contains(String.format("GoCD has less than %sb of disk space", limit));
    }

    @Test
    public void shouldSkipSecurityCheckingForCruiseUserWhenTimerTriggersPipeline() {
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        schedulingChecker.canTriggerPipelineWithTimer(goConfigService.getAllPipelineConfigs().get(0), result);
        assertThat(result.canContinue()).isTrue();
    }
}
