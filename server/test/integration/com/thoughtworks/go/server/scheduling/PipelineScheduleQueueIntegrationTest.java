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

package com.thoughtworks.go.server.scheduling;

import java.util.List;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.server.service.PipelineScheduleQueue;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.TimeProvider;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import static com.thoughtworks.go.helper.ModificationsMother.forceBuild;
import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFilesAndTriggerAs;
import static com.thoughtworks.go.helper.ModificationsMother.multipleModifications;
import static com.thoughtworks.go.util.GoConfigFileHelper.env;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class PipelineScheduleQueueIntegrationTest {
    @Autowired private GoConfigService goConfigService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineScheduleQueue queue;
    @Autowired private JobInstanceService jobService;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private GoConfigFileHelper configFileEditor;

    private PipelineWithTwoStages fixture;
    private BuildCause newCause;

    @Before
    public void setup() throws Exception {
        configFileEditor = new GoConfigFileHelper();
        configFileEditor.onSetUp();
        dbHelper.onSetUp();
        configFileEditor.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        fixture.usingDbHelper(dbHelper).usingConfigHelper(configFileEditor).onSetUp();
        newCause = BuildCause.createWithEmptyModifications();
    }

    @After
    public void teardown() throws Exception {
        fixture.onTearDown();
        dbHelper.onTearDown();
        configFileEditor.onTearDown();
        FileUtil.deleteFolder(goConfigService.artifactsDir());
        queue.clear();
    }

    @Test
    public void shouldReturnNullBuildCauseIfPipelineHasNoHistory() {
        assertThat(queue.mostRecentScheduled("cruise").hasNeverRun(), is(true));
    }

    @Test
    public void shouldReturnMostRecentScheduledBuildCauseIfExists() {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();

        BuildCause actual = queue.mostRecentScheduled(fixture.pipelineName);
        assertThat(actual, is(pipeline.getBuildCause()));
    }

    @Test
    public void shouldReturnToBeScheduledBuildCauseIfExists() {
        BuildCause beforeSchedule = queue.toBeScheduled().get(fixture.pipelineName);
        assertThat(beforeSchedule, is(nullValue()));

        BuildCause buildCause = BuildCause.createWithEmptyModifications();
        queue.schedule(fixture.pipelineName, buildCause);

        BuildCause afterSchedule = queue.toBeScheduled().get(fixture.pipelineName);
        assertThat(afterSchedule, is(buildCause));
    }

    @Test
    public void shouldChangeToBeScheduledBuildCauseToAlreadyScheduledAfterBeenFinished() throws Exception {
        BuildCause buildCause = BuildCause.createWithEmptyModifications();
        queue.schedule("cruise", buildCause);

        queue.finishSchedule("cruise", buildCause, newCause);

        assertThat(queue.mostRecentScheduled("cruise"), is(buildCause));
        assertThat(queue.toBeScheduled().size(), is(0));
    }

    @Test
    public void shouldScheduleBuildCauseConsideringPriority() throws Exception {
        BuildCause buildCause = BuildCause.createWithModifications(multipleModifications(), "");
        queue.schedule("cruise", buildCause);

        queue.schedule("cruise", BuildCause.createManualForced());
        assertThat(queue.toBeScheduled().get("cruise").isForced(), is(true));
    }

    @Test
    public void shouldClearToBeScheduledIfPipelineIsDeleted() {
        queue.schedule("cruise", BuildCause.createWithEmptyModifications());
        queue.clearPipeline("cruise");
        assertThat(queue.toBeScheduled().get("cruise"), is(nullValue()));
    }

    @Test
    public void shouldClearMostRecentScheduledIfPipelineIsDeleted() {
        BuildCause buildCause = BuildCause.createWithEmptyModifications();
        queue.schedule("cruise", buildCause);
        queue.finishSchedule("cruise", buildCause, newCause);
        queue.clearPipeline("cruise");
        assertThat(queue.mostRecentScheduled("cruise").hasNeverRun(), is(true));
    }

    @Test
    public void shouldReturnFalseIfThereIsBuildCauseWithoutModifications() throws Exception {
        queue.schedule("cruise", BuildCause.createWithEmptyModifications());
        assertThat(queue.hasBuildCause("cruise"), is(false));
    }

    @Test
    public void shouldReturnFalseIfThereIsBuildCause() throws Exception {
        queue.schedule("cruise", BuildCause.createWithModifications(multipleModifications(), ""));
        assertThat(queue.hasBuildCause("cruise"), is(true));
    }

    @Test
    public void shouldReturnTrueIfThereIsForcedBuildCause() throws Exception {
        queue.schedule("cruise", BuildCause.createManualForced());
        assertThat(queue.hasForcedBuildCause("cruise"), is(true));
    }

    @Test
    public void shouldCreatePipelineIfBuildCauseIsNotTrumped() throws Exception {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = modifySomeFiles(pipelineConfig);
        saveRev(cause);
        queue.schedule(fixture.pipelineName, cause);

        assertThat(queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider()), is(not(nullValue())));
    }

    private void saveRev(final BuildCause cause) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.save(cause.getMaterialRevisions());
            }
        });
    }

    @Test public void shouldCreateStageWithApproverFromBuildCauseForCreatePipeline() throws Exception {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = modifySomeFilesAndTriggerAs(pipelineConfig, "cruise-developer");
        saveRev(cause);
        queue.schedule(fixture.pipelineName, cause);

        Pipeline pipeline = queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());
        Stage stage = pipeline.getStages().first();
        assertThat(stage.getApprovedBy(), is("cruise-developer"));
    }

    @Test
    public void shouldReturnNullIfBuildCauseIsTrumped() throws Exception {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.currentRevision());
        queue.schedule(fixture.pipelineName, cause);
        queue.finishSchedule(fixture.pipelineName, cause, cause);
        
        assertThat(queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider()), is(nullValue()));
    }

    @Test public void shouldBeCanceledWhenSameBuildCause() throws Exception {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.currentRevision());
        queue.finishSchedule(fixture.pipelineName, cause, cause);
        queue.schedule(fixture.pipelineName, cause);

        assertThat(fixture.pipelineName, is(scheduledOn(queue)));
        assertThat(queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider()), is(nullValue()));
        assertThat(fixture.pipelineName, is(not(scheduledOn(queue))));
    }

    @Test public void shouldFinishSchedule() throws Exception {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.currentRevision());
        queue.schedule(fixture.pipelineName, cause);

        BuildCause newCause = modifySomeFiles(pipelineConfig, "somethingElse");
        queue.finishSchedule(fixture.pipelineName, cause, newCause);

        assertThat(queue.hasBuildCause(fixture.pipelineName), is(false));
        assertThat(queue.mostRecentScheduled(fixture.pipelineName), is(newCause));
    }

    @Test public void shouldNotBeCanceledWhenForcingBuildTwice() throws Exception {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = forceBuild(pipelineConfig);
        saveRev(cause);

        queue.finishSchedule(fixture.pipelineName, cause, newCause);
        queue.schedule(fixture.pipelineName, cause);

        assertThat(pipelineDao.mostRecentLabel(fixture.pipelineName), is(nullValue()));

        assertThat(fixture.pipelineName, is(scheduledOn(queue)));
        assertThat(queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider()), is(not(nullValue())));
        assertThat(pipelineDao.mostRecentLabel(fixture.pipelineName), is("label-1"));
   }

    private TypeSafeMatcher<String> scheduledOn(final PipelineScheduleQueue queue) {
        return new TypeSafeMatcher<String>() {
            public boolean matchesSafely(String item) {
                return queue.toBeScheduled().containsKey(item);
            }

            public void describeTo(Description description) {
                description.appendText("to be scheduled");
            }
        };
    }

    @Test
    public void shouldSaveBuildPlansWhenScheduling() throws Exception {
        JobConfigs jobConfigs = new JobConfigs();
        Resources resources = new Resources(new Resource("resource1"));
        ArtifactPlans artifactPlans = new ArtifactPlans();
        ArtifactPropertiesGenerators generators = new ArtifactPropertiesGenerators();
        generators.add(new ArtifactPropertiesGenerator("property-name", "artifact-path", "artifact-xpath"));
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("test-job"), resources, artifactPlans, generators);
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
        assertThat(plan.getArtifactPlans(), is((List<ArtifactPlan>)artifactPlans));
        assertThat(plan.getPropertyGenerators(), is((List<ArtifactPropertiesGenerator>)generators));
        assertThat(plan.getResources(), is((List<Resource>)resources));
    }

    @Test
    public void shouldLogWithInfoIfPipelineISScheduled() throws Exception {
        LogFixture logging = LogFixture.startListening();

        JobConfigs jobConfigs = new JobConfigs();
        Resources resources = new Resources(new Resource("resource1"));
        ArtifactPlans artifactPlans = new ArtifactPlans();
        ArtifactPropertiesGenerators generators = new ArtifactPropertiesGenerators();
        generators.add(new ArtifactPropertiesGenerator("property-name", "artifact-path", "artifact-xpath"));
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("test-job"), resources, artifactPlans, generators);
        jobConfigs.add(jobConfig);

        StageConfig stage = new StageConfig(new CaseInsensitiveString("test-stage"), jobConfigs);
        MaterialConfigs materialConfigs = new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig());
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("test-pipeline"), materialConfigs, stage);
        configFileEditor.addPipeline(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(stage.name()));
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.nextRevision());
        saveRev(cause);

        queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());

        assertThat(logging.getLog(), containsString("[Pipeline Schedule] Successfully scheduled pipeline test-pipeline, buildCause:[ModificationBuildCause: triggered by " + cause.getMaterialRevisions().latestRevision() +"]"));
        logging.stopListening();
    }

    @Test
    public void shouldCreateJobsMatchingRealAgentsIfRunOnAllAgentsIsSet() throws Exception {

        JobConfigs jobConfigs = new JobConfigs();
        ArtifactPlans artifactPlans = new ArtifactPlans();
        ArtifactPropertiesGenerators generators = new ArtifactPropertiesGenerators();
        generators.add(new ArtifactPropertiesGenerator("property-name", "artifact-path", "artifact-xpath"));
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("test-job"), new Resources(), artifactPlans, generators);
        jobConfig.setRunOnAllAgents(true);
        jobConfigs.add(jobConfig);

        StageConfig stage = new StageConfig(new CaseInsensitiveString("test-stage"), jobConfigs);
        MaterialConfigs materialConfigs = new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig());
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("test-pipeline"), materialConfigs, stage);
        BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.nextRevision());
        saveRev(cause);

        configFileEditor.addAgent("localhost", "uuid1");
        configFileEditor.addAgent("localhost", "uuid2");
        configFileEditor.addAgent("localhost", "uuid3");
        configFileEditor.addAgentToEnvironment("env", "uuid1");
        configFileEditor.addPipeline(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(stage.name()));

        queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), configFileEditor.currentConfig().agents()), "md5-test", new TimeProvider());

        List<JobPlan> plans = jobService.orderedScheduledBuilds();
        assertThat(plans.toArray(), hasItemInArray(hasProperty("name", is(RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker("test-job", 1)))));
        assertThat(plans.toArray(), hasItemInArray(hasProperty("name", is(RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker("test-job", 2)))));
        assertThat(plans.toArray(), hasItemInArray(hasProperty("name", is(RunOnAllAgents.CounterBasedJobNameGenerator.appendMarker("test-job", 3)))));
        assertThat(plans.size(), is(3));
    }

	@Test
	public void shouldCreateMultipleJobsIfRunMultipleInstanceIsSet() throws Exception {
		JobConfigs jobConfigs = new JobConfigs();
		ArtifactPropertiesGenerators generators = new ArtifactPropertiesGenerators();
		generators.add(new ArtifactPropertiesGenerator("property-name", "artifact-path", "artifact-xpath"));
		JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("test-job"), new Resources(), new ArtifactPlans(), generators);
		jobConfig.setRunInstanceCount(3);
		jobConfigs.add(jobConfig);

		StageConfig stage = new StageConfig(new CaseInsensitiveString("test-stage"), jobConfigs);
		MaterialConfigs materialConfigs = new MaterialConfigs(MaterialConfigsMother.dependencyMaterialConfig());
		PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("test-pipeline"), materialConfigs, stage);
		BuildCause cause = modifySomeFiles(pipelineConfig, ModificationsMother.nextRevision());
		saveRev(cause);

		configFileEditor.addPipeline(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(stage.name()));

		queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), configFileEditor.currentConfig().agents()), "md5-test", new TimeProvider());

		List<JobPlan> plans = jobService.orderedScheduledBuilds();
		assertThat(plans.size(), is(3));
		assertThat(plans.toArray(), hasItemInArray(hasProperty("name", is(RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker("test-job", 1)))));
		assertThat(plans.toArray(), hasItemInArray(hasProperty("name", is(RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker("test-job", 2)))));
		assertThat(plans.toArray(), hasItemInArray(hasProperty("name", is(RunMultipleInstance.CounterBasedJobNameGenerator.appendMarker("test-job", 3)))));
	}

    @Test
    public void shouldPersistTriggerTimeEnvironmentVariable() {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        pipelineConfig.setVariables(env("blahVariable","blahValue"));
        BuildCause cause = modifySomeFilesAndTriggerAs(pipelineConfig, "cruise-developer");
        EnvironmentVariablesConfig environmentVariablesConfig = new EnvironmentVariablesConfig();
        environmentVariablesConfig.add(new EnvironmentVariableConfig("blahVariable", "blahOverride"));
        cause.addOverriddenVariables(environmentVariablesConfig);
        saveRev(cause);
        queue.schedule(fixture.pipelineName, cause);
        Pipeline pipeline = queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());
        assertThat(pipeline.scheduleTimeVariables(),is(env("blahVariable","blahOverride")));
    }

    @Test
    public void shouldSaveCurrentConfigMD5OnStageWhenSchedulingAPipeline() {
        PipelineConfig pipelineConfig = fixture.pipelineConfig();
        BuildCause cause = modifySomeFilesAndTriggerAs(pipelineConfig, "cruise-developer");
        EnvironmentVariablesConfig environmentVariablesConfig = new EnvironmentVariablesConfig();
        environmentVariablesConfig.add(new EnvironmentVariableConfig("blahVariable", "blahOverride"));
        cause.addOverriddenVariables(environmentVariablesConfig);
        saveRev(cause);
        queue.schedule(fixture.pipelineName, cause);
        Pipeline pipeline = queue.createPipeline(cause, pipelineConfig, new DefaultSchedulingContext(cause.getApprover(), new Agents()), "md5-test", new TimeProvider());
        assertThat(pipeline.getFirstStage().getConfigVersion(), is("md5-test"));
    }
}
