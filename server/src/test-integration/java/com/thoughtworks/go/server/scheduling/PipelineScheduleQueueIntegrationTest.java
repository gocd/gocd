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
package com.thoughtworks.go.server.scheduling;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.server.service.PipelineScheduleQueue;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.nio.file.Path;
import java.util.List;

import static com.thoughtworks.go.helper.EnvironmentVariablesConfigMother.env;
import static com.thoughtworks.go.helper.ModificationsMother.*;
import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath:/applicationContext-global.xml",
    "classpath:/applicationContext-dataLocalAccess.xml",
    "classpath:/testPropertyConfigurer.xml",
    "classpath:/spring-all-servlet.xml",
})
public class PipelineScheduleQueueIntegrationTest {
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private PipelineScheduleQueue queue;
    @Autowired
    private JobInstanceService jobService;
    @Autowired
    private PipelineDao pipelineDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private AgentService agentService;

    private GoConfigFileHelper configHelper;

    private PipelineWithTwoStages pipelineFixture;
    private BuildCause newCause;

    @BeforeEach
    public void setup(@TempDir Path tempDir) throws Exception {
        configHelper = new GoConfigFileHelper().usingCruiseConfigDao(goConfigDao);
        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        pipelineFixture.usingDbHelper(dbHelper).usingConfigHelper(configHelper).onSetUp();
        newCause = BuildCause.createWithEmptyModifications();
    }

    @AfterEach
    public void teardown() throws Exception {
        pipelineFixture.onTearDown();
        FileUtils.deleteQuietly(goConfigService.artifactsDir());
        queue.clear();
    }

    @Test
    public void shouldReturnNullBuildCauseIfPipelineHasNoHistory() {
        assertThat(queue.mostRecentScheduled(new CaseInsensitiveString("cruise")).hasNeverRun()).isTrue();
    }

    @Test
    public void shouldReturnMostRecentScheduledBuildCauseIfExists() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();

        BuildCause actual = queue.mostRecentScheduled(new CaseInsensitiveString(pipelineFixture.pipelineName));
        assertThat(actual).isEqualTo(pipeline.getBuildCause());
    }

    @Test
    public void shouldReturnToBeScheduledBuildCauseIfExists() {
        BuildCause beforeSchedule = queue.toBeScheduled().get(new CaseInsensitiveString(pipelineFixture.pipelineName));
        assertThat(beforeSchedule).isNull();

        BuildCause buildCause = BuildCause.createWithEmptyModifications();
        queue.schedule(new CaseInsensitiveString(pipelineFixture.pipelineName), buildCause);

        BuildCause afterSchedule = queue.toBeScheduled().get(new CaseInsensitiveString(pipelineFixture.pipelineName));
        assertThat(afterSchedule).isEqualTo(buildCause);
    }

    @Test
    public void shouldChangeToBeScheduledBuildCauseToAlreadyScheduledAfterBeenFinished() {
        BuildCause buildCause = BuildCause.createWithEmptyModifications();
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, buildCause);

        queue.finishSchedule(cruise, buildCause, newCause);

        assertThat(queue.mostRecentScheduled(cruise)).isEqualTo(buildCause);
        assertThat(queue.toBeScheduled().size()).isEqualTo(0);
    }

    @Test
    public void shouldScheduleBuildCauseConsideringPriority() {
        BuildCause buildCause = BuildCause.createWithModifications(multipleModifications(), "");
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, buildCause);

        queue.schedule(cruise, BuildCause.createManualForced());
        assertThat(queue.toBeScheduled().get(cruise).isForced()).isTrue();
    }

    @Test
    public void shouldClearToBeScheduledIfPipelineIsDeleted() {
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, BuildCause.createWithEmptyModifications());
        queue.clearPipeline(cruise);
        assertThat(queue.toBeScheduled().get(cruise)).isNull();
    }

    @Test
    public void shouldClearMostRecentScheduledIfPipelineIsDeleted() {
        BuildCause buildCause = BuildCause.createWithEmptyModifications();
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, buildCause);
        queue.finishSchedule(cruise, buildCause, newCause);
        queue.clearPipeline(cruise);
        assertThat(queue.mostRecentScheduled(cruise).hasNeverRun()).isTrue();
    }

    @Test
    public void shouldReturnFalseIfThereIsBuildCauseWithoutModifications() {
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, BuildCause.createWithEmptyModifications());
        assertThat(queue.hasBuildCause(cruise)).isFalse();
    }

    @Test
    public void shouldReturnFalseIfThereIsBuildCause() {
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, BuildCause.createWithModifications(multipleModifications(), ""));
        assertThat(queue.hasBuildCause(cruise)).isTrue();
    }

    @Test
    public void shouldReturnTrueIfThereIsForcedBuildCause() {
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, BuildCause.createManualForced());
        assertThat(queue.hasForcedBuildCause(cruise)).isTrue();
    }

    @Test
    public void shouldCreatePipelineIfBuildCauseIsNotTrumped() {
        PipelineConfig pipelineConfig = pipelineFixture.pipelineConfig();
        BuildCause cause = modifySomeFiles(pipelineConfig);
        saveRev(cause);
        queue.schedule(new CaseInsensitiveString(pipelineFixture.pipelineName), cause);

        assertThat(queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider())).isNotNull();
    }

    private void saveRev(final BuildCause cause) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.save(cause.getMaterialRevisions());
            }
        });
    }

    @Test
    public void shouldCreateStageWithApproverFromBuildCauseForCreatePipeline() {
        PipelineConfig pipelineConfig = pipelineFixture.pipelineConfig();
        BuildCause cause = modifySomeFilesAndTriggerAs(pipelineConfig, "cruise-developer");
        saveRev(cause);
        queue.schedule(new CaseInsensitiveString(pipelineFixture.pipelineName), cause);

        Pipeline pipeline = queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());
        Stage stage = pipeline.getStages().first();
        assertThat(stage.getApprovedBy()).isEqualTo("cruise-developer");
    }

    @Test
    public void shouldReturnNullIfBuildCauseIsTrumped() {
        PipelineConfig pipelineConfig = pipelineFixture.pipelineConfig();
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.currentRevision());
        queue.schedule(new CaseInsensitiveString(pipelineFixture.pipelineName), cause);
        queue.finishSchedule(new CaseInsensitiveString(pipelineFixture.pipelineName), cause, cause);

        assertThat(queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider())).isNull();
    }

    @Test
    public void shouldBeCanceledWhenSameBuildCause() {
        PipelineConfig pipelineConfig = pipelineFixture.pipelineConfig();
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.currentRevision());
        queue.finishSchedule(new CaseInsensitiveString(pipelineFixture.pipelineName), cause, cause);
        queue.schedule(new CaseInsensitiveString(pipelineFixture.pipelineName), cause);

        assertThat(queue.toBeScheduled()).containsKey(new CaseInsensitiveString(pipelineFixture.pipelineName));
        assertThat(queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider())).isNull();
        assertThat(queue.toBeScheduled()).doesNotContainKey(new CaseInsensitiveString(pipelineFixture.pipelineName));
    }

    @Test
    public void shouldFinishSchedule() {
        PipelineConfig pipelineConfig = pipelineFixture.pipelineConfig();
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.currentRevision());
        CaseInsensitiveString pipelineName = new CaseInsensitiveString(pipelineFixture.pipelineName);
        queue.schedule(pipelineName, cause);

        BuildCause newCause = modifySomeFiles(pipelineConfig, "somethingElse");
        queue.finishSchedule(pipelineName, cause, newCause);

        assertThat(queue.hasBuildCause(pipelineName)).isFalse();
        assertThat(queue.mostRecentScheduled(pipelineName)).isEqualTo(newCause);
    }

    @Test
    public void shouldNotBeCanceledWhenForcingBuildTwice() {
        PipelineConfig pipelineConfig = pipelineFixture.pipelineConfig();
        BuildCause cause = forceBuild(pipelineConfig);
        saveRev(cause);
        CaseInsensitiveString pipelineName = new CaseInsensitiveString(pipelineFixture.pipelineName);

        queue.finishSchedule(pipelineName, cause, newCause);
        queue.schedule(pipelineName, cause);

        assertThat(pipelineDao.mostRecentPipelineIdentifier(pipelineFixture.pipelineName)).isNull();

        assertThat(queue.toBeScheduled()).containsKey(new CaseInsensitiveString(pipelineFixture.pipelineName));
        assertThat(queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider())).isNotNull();
        assertThat(pipelineDao.mostRecentPipelineIdentifier(pipelineFixture.pipelineName).getLabel()).isEqualTo("label-1");
    }

    @Test
    public void shouldSaveBuildPlansWhenScheduling() {
        JobConfigs jobConfigs = new JobConfigs();
        ResourceConfigs resourceConfigs = new ResourceConfigs(new ResourceConfig("resource1"));
        ArtifactTypeConfigs artifactTypeConfigs = new ArtifactTypeConfigs();
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("test-job"), resourceConfigs, artifactTypeConfigs, null);
        jobConfigs.add(jobConfig);

        StageConfig stage = new StageConfig(new CaseInsensitiveString("test-stage"), jobConfigs);
        MaterialConfigs materialConfigs = new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig());
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("test-pipeline"), materialConfigs, stage);
        configHelper.addPipeline(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(stage.name()));
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.nextRevision());
        saveRev(cause);

        queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());

        List<JobPlan> plans = jobService.orderedScheduledBuilds();
        JobPlan plan = plans.get(0);
        assertThat(plan.getName()).isEqualTo("test-job");
        assertThat(plan.getArtifactPlans()).isEqualTo(artifactTypeConfigs);
        assertThat(plan.getResources().toResourceConfigs()).isEqualTo(resourceConfigs);
    }

    @Test
    public void shouldLogWithInfoIfPipelineISScheduled() {
        try (LogFixture logging = logFixtureFor(PipelineScheduleQueue.class, Level.DEBUG)) {
            JobConfigs jobConfigs = new JobConfigs();
            ResourceConfigs resourceConfigs = new ResourceConfigs(new ResourceConfig("resource1"));
            ArtifactTypeConfigs artifactTypeConfigs = new ArtifactTypeConfigs();
            JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("test-job"), resourceConfigs, artifactTypeConfigs, null);
            jobConfigs.add(jobConfig);

            StageConfig stage = new StageConfig(new CaseInsensitiveString("test-stage"), jobConfigs);
            MaterialConfigs materialConfigs = new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig());
            PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("test-pipeline"), materialConfigs, stage);
            configHelper.addPipeline(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(stage.name()));
            BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.nextRevision());
            saveRev(cause);

            queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());

            assertThat(logging.getLog()).contains("[Pipeline Schedule] Successfully scheduled pipeline test-pipeline, buildCause:[ModificationBuildCause: triggered by " + cause.getMaterialRevisions().latestRevision() + "]");
        }
    }

    @Test
    public void shouldCreateJobsMatchingRealAgentsIfRunOnAllAgentsIsSet() {

        JobConfigs jobConfigs = new JobConfigs();
        ArtifactTypeConfigs artifactTypeConfigs = new ArtifactTypeConfigs();
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("test-job"), new ResourceConfigs(), artifactTypeConfigs, null);
        jobConfig.setRunOnAllAgents(true);
        jobConfigs.add(jobConfig);

        StageConfig stage = new StageConfig(new CaseInsensitiveString("test-stage"), jobConfigs);
        MaterialConfigs materialConfigs = new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig());
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("test-pipeline"), materialConfigs, stage);
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.nextRevision());
        saveRev(cause);

        Agent agentConfigWithUuid1 = new Agent("uuid1", "localhost", "127.0.0.1", "cookie1");
        configHelper.addEnvironments(List.of("env"));
        agentConfigWithUuid1.setEnvironments("env");
        agentService.saveOrUpdate(agentConfigWithUuid1);
        Agent agentConfigWithUuid2 = new Agent("uuid2", "localhost", "127.0.0.1", "cookie2");
        agentService.saveOrUpdate(agentConfigWithUuid2);
        Agent agentConfigWithUuid3 = new Agent("uuid3", "localhost", "127.0.0.1", "cookie3");
        agentService.saveOrUpdate(agentConfigWithUuid3);
        configHelper.addPipeline(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(stage.name()));

        queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents(agentConfigWithUuid1, agentConfigWithUuid2, agentConfigWithUuid3)), "md5-test", new TimeProvider());

        List<JobPlan> plans = jobService.orderedScheduledBuilds();
        assertThat(plans).satisfiesExactlyInAnyOrder(
            plan -> assertThat(plan.getName()).isEqualTo(RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker("test-job", 1)),
            plan -> assertThat(plan.getName()).isEqualTo(RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker("test-job", 2)),
            plan -> assertThat(plan.getName()).isEqualTo(RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker("test-job", 3))
        );
    }

    @Test
    public void shouldCreateMultipleJobsIfRunMultipleInstanceIsSet() {
        JobConfigs jobConfigs = new JobConfigs();
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("test-job"), new ResourceConfigs(), new ArtifactTypeConfigs(), null);
        jobConfig.setRunInstanceCount(3);
        jobConfigs.add(jobConfig);

        StageConfig stage = new StageConfig(new CaseInsensitiveString("test-stage"), jobConfigs);
        MaterialConfigs materialConfigs = new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig());
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("test-pipeline"), materialConfigs, stage);
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.nextRevision());
        saveRev(cause);

        configHelper.addPipeline(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(stage.name()));

        queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), agentService.agents()), "md5-test", new TimeProvider());

        List<JobPlan> plans = jobService.orderedScheduledBuilds();
        assertThat(plans).satisfiesExactlyInAnyOrder(
            plan -> assertThat(plan.getName()).isEqualTo(RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker("test-job", 1)),
            plan -> assertThat(plan.getName()).isEqualTo(RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker("test-job", 2)),
            plan -> assertThat(plan.getName()).isEqualTo(RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker("test-job", 3))
        );
    }

    @Test
    public void shouldPersistTriggerTimeEnvironmentVariable() {
        PipelineConfig pipelineConfig = pipelineFixture.pipelineConfig();
        pipelineConfig.setVariables(env("blahVariable", "blahValue"));
        BuildCause cause = modifySomeFilesAndTriggerAs(pipelineConfig, "cruise-developer");
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        environmentVariables.add(new EnvironmentVariable("blahVariable", "blahOverride"));
        cause.addOverriddenVariables(environmentVariables);
        saveRev(cause);
        queue.schedule(new CaseInsensitiveString(pipelineFixture.pipelineName), cause);
        Pipeline pipeline = queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());
        assertThat(pipeline.scheduleTimeVariables()).isEqualTo(new EnvironmentVariables(List.of(new EnvironmentVariable("blahVariable", "blahOverride"))));
    }

    @Test
    public void shouldSaveCurrentConfigMD5OnStageWhenSchedulingAPipeline() {
        PipelineConfig pipelineConfig = pipelineFixture.pipelineConfig();
        BuildCause cause = modifySomeFilesAndTriggerAs(pipelineConfig, "cruise-developer");
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        environmentVariables.add(new EnvironmentVariable("blahVariable", "blahOverride"));
        cause.addOverriddenVariables(environmentVariables
        );
        saveRev(cause);
        queue.schedule(new CaseInsensitiveString(pipelineFixture.pipelineName), cause);
        Pipeline pipeline = queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());
        assertThat(pipeline.getFirstStage().getConfigVersion()).isEqualTo("md5-test");
    }


    @Test
    public void shouldReturnNullWhenPipelineConfigOriginDoesNotMatchBuildCauseRevision() {
        PipelineConfig pipelineConfig = pipelineFixture.pipelineConfig();
        BuildCause cause = modifySomeFilesAndTriggerAs(pipelineConfig, "cruise-developer");
        MaterialConfig materialConfig = pipelineConfig.materialConfigs().first();
        cause.getMaterialRevisions().findRevisionFor(materialConfig);
        pipelineConfig.setOrigins(new RepoConfigOrigin(
            ConfigRepoConfig.createConfigRepoConfig(materialConfig, "123", "id1"), "plug"));
        saveRev(cause);
        queue.schedule(new CaseInsensitiveString(pipelineFixture.pipelineName), cause);
        Pipeline pipeline = queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());
        assertThat(pipeline).isNull();
    }
}
