/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import ch.qos.logback.classic.Level;
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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.helper.ModificationsMother.*;
import static com.thoughtworks.go.util.GoConfigFileHelper.env;
import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
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
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private GoConfigFileHelper configFileEditor;

    private PipelineWithTwoStages fixture;
    private BuildCause newCause;

    @Before
    public void setup() throws Exception {
        configFileEditor = new GoConfigFileHelper();
        configFileEditor.onSetUp();
        dbHelper.onSetUp();
        configFileEditor.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, temporaryFolder);
        fixture.usingDbHelper(dbHelper).usingConfigHelper(configFileEditor).onSetUp();
        newCause = BuildCause.createWithEmptyModifications();
    }

    @After
    public void teardown() throws Exception {
        fixture.onTearDown();
        dbHelper.onTearDown();
        configFileEditor.onTearDown();
        FileUtils.deleteQuietly(goConfigService.artifactsDir());
        queue.clear();
    }

    @Test
    public void shouldReturnNullBuildCauseIfPipelineHasNoHistory() {
        assertThat(queue.mostRecentScheduled(new CaseInsensitiveString("cruise")).hasNeverRun(), is(true));
    }

    @Test
    public void shouldReturnMostRecentScheduledBuildCauseIfExists() {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();

        BuildCause actual = queue.mostRecentScheduled(new CaseInsensitiveString(fixture.pipelineName));
        assertThat(actual, is(pipeline.getBuildCause()));
    }

    @Test
    public void shouldReturnToBeScheduledBuildCauseIfExists() {
        BuildCause beforeSchedule = queue.toBeScheduled().get(new CaseInsensitiveString(fixture.pipelineName));
        assertThat(beforeSchedule, is(nullValue()));

        BuildCause buildCause = BuildCause.createWithEmptyModifications();
        queue.schedule(new CaseInsensitiveString(fixture.pipelineName), buildCause);

        BuildCause afterSchedule = queue.toBeScheduled().get(new CaseInsensitiveString(fixture.pipelineName));
        assertThat(afterSchedule, is(buildCause));
    }

    @Test
    public void shouldChangeToBeScheduledBuildCauseToAlreadyScheduledAfterBeenFinished() throws Exception {
        BuildCause buildCause = BuildCause.createWithEmptyModifications();
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, buildCause);

        queue.finishSchedule(cruise, buildCause, newCause);

        assertThat(queue.mostRecentScheduled(cruise), is(buildCause));
        assertThat(queue.toBeScheduled().size(), is(0));
    }

    @Test
    public void shouldScheduleBuildCauseConsideringPriority() throws Exception {
        BuildCause buildCause = BuildCause.createWithModifications(multipleModifications(), "");
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, buildCause);

        queue.schedule(cruise, BuildCause.createManualForced());
        assertThat(queue.toBeScheduled().get(cruise).isForced(), is(true));
    }

    @Test
    public void shouldClearToBeScheduledIfPipelineIsDeleted() {
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, BuildCause.createWithEmptyModifications());
        queue.clearPipeline(cruise);
        assertThat(queue.toBeScheduled().get(cruise), is(nullValue()));
    }

    @Test
    public void shouldClearMostRecentScheduledIfPipelineIsDeleted() {
        BuildCause buildCause = BuildCause.createWithEmptyModifications();
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, buildCause);
        queue.finishSchedule(cruise, buildCause, newCause);
        queue.clearPipeline(cruise);
        assertThat(queue.mostRecentScheduled(cruise).hasNeverRun(), is(true));
    }

    @Test
    public void shouldReturnFalseIfThereIsBuildCauseWithoutModifications() throws Exception {
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, BuildCause.createWithEmptyModifications());
        assertThat(queue.hasBuildCause(cruise), is(false));
    }

    @Test
    public void shouldReturnFalseIfThereIsBuildCause() throws Exception {
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, BuildCause.createWithModifications(multipleModifications(), ""));
        assertThat(queue.hasBuildCause(cruise), is(true));
    }

    @Test
    public void shouldReturnTrueIfThereIsForcedBuildCause() throws Exception {
        CaseInsensitiveString cruise = new CaseInsensitiveString("cruise");
        queue.schedule(cruise, BuildCause.createManualForced());
        assertThat(queue.hasForcedBuildCause(cruise), is(true));
    }

    @Test
    public void shouldCreatePipelineIfBuildCauseIsNotTrumped() throws Exception {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = modifySomeFiles(pipelineConfig);
        saveRev(cause);
        queue.schedule(new CaseInsensitiveString(fixture.pipelineName), cause);

        assertThat(queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider()), is(not(nullValue())));
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
    public void shouldCreateStageWithApproverFromBuildCauseForCreatePipeline() throws Exception {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = modifySomeFilesAndTriggerAs(pipelineConfig, "cruise-developer");
        saveRev(cause);
        queue.schedule(new CaseInsensitiveString(fixture.pipelineName), cause);

        Pipeline pipeline = queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());
        Stage stage = pipeline.getStages().first();
        assertThat(stage.getApprovedBy(), is("cruise-developer"));
    }

    @Test
    public void shouldReturnNullIfBuildCauseIsTrumped() throws Exception {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.currentRevision());
        queue.schedule(new CaseInsensitiveString(fixture.pipelineName), cause);
        queue.finishSchedule(new CaseInsensitiveString(fixture.pipelineName), cause, cause);

        assertThat(queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider()), is(nullValue()));
    }

    @Test
    public void shouldBeCanceledWhenSameBuildCause() throws Exception {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.currentRevision());
        queue.finishSchedule(new CaseInsensitiveString(fixture.pipelineName), cause, cause);
        queue.schedule(new CaseInsensitiveString(fixture.pipelineName), cause);

        assertThat(new CaseInsensitiveString(fixture.pipelineName), is(scheduledOn(queue)));
        assertThat(queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider()), is(nullValue()));
        assertThat(new CaseInsensitiveString(fixture.pipelineName), is(not(scheduledOn(queue))));
    }

    @Test
    public void shouldFinishSchedule() throws Exception {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.currentRevision());
        CaseInsensitiveString pipelineName = new CaseInsensitiveString(fixture.pipelineName);
        queue.schedule(pipelineName, cause);

        BuildCause newCause = modifySomeFiles(pipelineConfig, "somethingElse");
        queue.finishSchedule(pipelineName, cause, newCause);

        assertThat(queue.hasBuildCause(pipelineName), is(false));
        assertThat(queue.mostRecentScheduled(pipelineName), is(newCause));
    }

    @Test
    public void shouldNotBeCanceledWhenForcingBuildTwice() throws Exception {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = forceBuild(pipelineConfig);
        saveRev(cause);
        CaseInsensitiveString pipelineName = new CaseInsensitiveString(fixture.pipelineName);

        queue.finishSchedule(pipelineName, cause, newCause);
        queue.schedule(pipelineName, cause);

        assertThat(pipelineDao.mostRecentPipelineIdentifier(fixture.pipelineName), is(nullValue()));

        assertThat(new CaseInsensitiveString(fixture.pipelineName), is(scheduledOn(queue)));
        assertThat(queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider()), is(not(nullValue())));
        assertThat(pipelineDao.mostRecentPipelineIdentifier(fixture.pipelineName).getLabel(), is("label-1"));
    }

    private TypeSafeMatcher<CaseInsensitiveString> scheduledOn(final PipelineScheduleQueue queue) {
        return new TypeSafeMatcher<CaseInsensitiveString>() {
            @Override
            public boolean matchesSafely(CaseInsensitiveString item) {
                return queue.toBeScheduled().containsKey(item);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("to be scheduled");
            }
        };
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
        configFileEditor.addPipeline(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(stage.name()));
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.nextRevision());
        saveRev(cause);

        queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());

        JobInstances instances = jobService.currentJobsOfStage("test-pipeline", stage);
        assertThat(instances.size(), is(1));

        List<JobPlan> plans = jobService.orderedScheduledBuilds();
        JobPlan plan = plans.get(0);
        assertThat(plan.getName(), is("test-job"));
        assertThat(plan.getArtifactPlans(), is(artifactTypeConfigs));
        assertThat(plan.getResources().toResourceConfigs(), is(resourceConfigs));
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
            configFileEditor.addPipeline(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(stage.name()));
            BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.nextRevision());
            saveRev(cause);

            queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());

            assertThat(logging.getLog(), containsString("[Pipeline Schedule] Successfully scheduled pipeline test-pipeline, buildCause:[ModificationBuildCause: triggered by " + cause.getMaterialRevisions().latestRevision() + "]"));
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
        configFileEditor.addEnvironments(Arrays.asList("env"));
        agentConfigWithUuid1.setEnvironments("env");
        agentService.saveOrUpdate(agentConfigWithUuid1);
        Agent agentConfigWithUuid2 = new Agent("uuid2", "localhost", "127.0.0.1", "cookie2");
        agentService.saveOrUpdate(agentConfigWithUuid2);
        Agent agentConfigWithUuid3 = new Agent("uuid3", "localhost", "127.0.0.1", "cookie3");
        agentService.saveOrUpdate(agentConfigWithUuid3);
        configFileEditor.addPipeline(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(stage.name()));

        queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents(agentConfigWithUuid1, agentConfigWithUuid2, agentConfigWithUuid3)), "md5-test", new TimeProvider());

        List<JobPlan> plans = jobService.orderedScheduledBuilds();
        assertThat(plans.toArray(), hasItemInArray(hasProperty("name", is(RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker("test-job", 1)))));
        assertThat(plans.toArray(), hasItemInArray(hasProperty("name", is(RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker("test-job", 2)))));
        assertThat(plans.toArray(), hasItemInArray(hasProperty("name", is(RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker("test-job", 3)))));
        assertThat(plans.size(), is(3));
    }

    @Test
    public void shouldCreateMultipleJobsIfRunMultipleInstanceIsSet() throws Exception {
        JobConfigs jobConfigs = new JobConfigs();
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("test-job"), new ResourceConfigs(), new ArtifactTypeConfigs(), null);
        jobConfig.setRunInstanceCount(3);
        jobConfigs.add(jobConfig);

        StageConfig stage = new StageConfig(new CaseInsensitiveString("test-stage"), jobConfigs);
        MaterialConfigs materialConfigs = new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig());
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("test-pipeline"), materialConfigs, stage);
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.nextRevision());
        saveRev(cause);

        configFileEditor.addPipeline(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(stage.name()));

        queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), agentService.agents()), "md5-test", new TimeProvider());

        List<JobPlan> plans = jobService.orderedScheduledBuilds();
        assertThat(plans.size(), is(3));
        assertThat(plans.toArray(), hasItemInArray(hasProperty("name", is(RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker("test-job", 1)))));
        assertThat(plans.toArray(), hasItemInArray(hasProperty("name", is(RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker("test-job", 2)))));
        assertThat(plans.toArray(), hasItemInArray(hasProperty("name", is(RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker("test-job", 3)))));
    }

    @Test
    public void shouldPersistTriggerTimeEnvironmentVariable() {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        pipelineConfig.setVariables(env("blahVariable", "blahValue"));
        BuildCause cause = modifySomeFilesAndTriggerAs(pipelineConfig, "cruise-developer");
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        environmentVariables.add(new EnvironmentVariable("blahVariable", "blahOverride"));
        cause.addOverriddenVariables(environmentVariables);
        saveRev(cause);
        queue.schedule(new CaseInsensitiveString(fixture.pipelineName), cause);
        Pipeline pipeline = queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());
        assertThat(pipeline.scheduleTimeVariables(), is(new EnvironmentVariables(Arrays.asList(new EnvironmentVariable("blahVariable", "blahOverride")))));
    }

    @Test
    public void shouldSaveCurrentConfigMD5OnStageWhenSchedulingAPipeline() {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = modifySomeFilesAndTriggerAs(pipelineConfig, "cruise-developer");
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        environmentVariables.add(new EnvironmentVariable("blahVariable", "blahOverride"));
        cause.addOverriddenVariables(environmentVariables
        );
        saveRev(cause);
        queue.schedule(new CaseInsensitiveString(fixture.pipelineName), cause);
        Pipeline pipeline = queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());
        assertThat(pipeline.getFirstStage().getConfigVersion(), is("md5-test"));
    }


    @Test
    public void shouldReturnNullWhenPipelineConfigOriginDoesNotMatchBuildCauseRevision() {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = modifySomeFilesAndTriggerAs(pipelineConfig, "cruise-developer");
        MaterialConfig materialConfig = pipelineConfig.materialConfigs().first();
        cause.getMaterialRevisions().findRevisionFor(materialConfig);
        pipelineConfig.setOrigins(new RepoConfigOrigin(
                new ConfigRepoConfig(materialConfig, "123"), "plug"));
        saveRev(cause);
        queue.schedule(new CaseInsensitiveString(fixture.pipelineName), cause);
        Pipeline pipeline = queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());
        assertThat(pipeline, is(nullValue()));
    }
}
