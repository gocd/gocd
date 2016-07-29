/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.utils.Timeout;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsNull;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static com.thoughtworks.go.helper.ModificationsMother.modifyOneFile;
import static com.thoughtworks.go.util.DataStructureUtils.a;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InstanceFactoryTest {
    private InstanceFactory instanceFactory;
    private Clock clock;

    @Before
    public void setUp() throws Exception {
        instanceFactory = new InstanceFactory();
        this.clock = mock(Clock.class);
    }

    @Test
    public void shouldSetTheConfigVersionOnSchedulingAStage() throws Exception {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("foo-pipeline", "foo-stage", "foo-job");
        DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("loser");
        String md5 = "foo-md5";

        Stage actualStage = instanceFactory.createStageInstance(pipelineConfig, new CaseInsensitiveString("foo-stage"), schedulingContext, md5, clock);

        assertThat(actualStage.getConfigVersion(), is(md5));
    }

    @Test
    public void shouldThrowStageNotFoundExceptionWhenStageDoesNotExist() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs(), new StageConfig(new CaseInsensitiveString("first"), new JobConfigs()));
        try {
            instanceFactory.createStageInstance(pipelineConfig, new CaseInsensitiveString("doesNotExist"), new DefaultSchedulingContext(), "md5", clock);
            fail("Found the stage doesNotExist but, well, it doesn't");
        } catch (StageNotFoundException expected) {
            assertThat(expected.getMessage(), is("Stage 'doesNotExist' not found in pipeline 'cruise'"));
        }
    }

    @Test
    public void shouldCreateAStageInstanceThroughInstanceFactory() {
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("cruise"), new MaterialConfigs(),
                new StageConfig(new CaseInsensitiveString("first"), new JobConfigs(new JobConfig("job1"), new JobConfig("job2"))));

        Stage actualStage = instanceFactory.createStageInstance(pipelineConfig, new CaseInsensitiveString("first"), new DefaultSchedulingContext(), "md5", clock);

        JobInstances jobInstances = new JobInstances();
        jobInstances.add(new JobInstance("job1", clock));
        jobInstances.add(new JobInstance("job2", clock));

        Stage expectedStage = new Stage("first", jobInstances, "Unknown", Approval.SUCCESS, clock);
        assertThat(actualStage, is(expectedStage));
    }

    @Test
    public void shouldCreatePipelineInstanceWithEnvironmentVariablesOverriddenAccordingToScope() {
        StageConfig stageConfig = StageConfigMother.custom("stage", "foo", "bar");
        JobConfig fooConfig = stageConfig.jobConfigByConfigName(new CaseInsensitiveString("foo"));
        fooConfig.addVariable("foo", "foo");
        JobConfig barConfig = stageConfig.jobConfigByConfigName(new CaseInsensitiveString("bar"));
        barConfig.addVariable("foo", "bar");
        MaterialConfigs materialConfigs = MaterialConfigsMother.defaultMaterialConfigs();
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), materialConfigs, stageConfig);
        DefaultSchedulingContext context = new DefaultSchedulingContext("anonymous");

        Pipeline instance = instanceFactory.createPipelineInstance(pipelineConfig, ModificationsMother.forceBuild(pipelineConfig), context, "some-md5", new TimeProvider());

        assertThat(instance.findStage("stage").findJob("foo").getPlan().getVariables(), is(EnvironmentVariablesConfigMother.env("foo", "foo")));
        assertThat(instance.findStage("stage").findJob("bar").getPlan().getVariables(), is(EnvironmentVariablesConfigMother.env("foo", "bar")));
    }

    @Test
    public void shouldSchedulePipelineWithFirstStage() {
        StageConfig stageOneConfig = StageConfigMother.stageConfig("dev", BuildPlanMother.withBuildPlans("functional", "unit"));
        StageConfig stageTwoConfig = StageConfigMother.stageConfig("qa", BuildPlanMother.withBuildPlans("suiteOne", "suiteTwo"));

        MaterialConfigs materialConfigs = MaterialConfigsMother.defaultMaterialConfigs();
        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("mingle"), materialConfigs, stageOneConfig, stageTwoConfig);

        BuildCause buildCause = BuildCause.createManualForced(modifyOneFile(pipelineConfig), Username.ANONYMOUS);
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, buildCause, new DefaultSchedulingContext("test"), "some-md5", new TimeProvider());

        assertThat(pipeline.getName(), is("mingle"));
        assertThat(pipeline.getStages().size(), is(1));
        assertThat(pipeline.getStages().get(0).getName(), is("dev"));
        assertThat(pipeline.getStages().get(0).getJobInstances().get(0).getName(), is("functional"));
    }

    @Test
    public void shouldSetAutoApprovalOnStageInstance() throws Exception {
        StageConfig stageConfig = StageConfigMother.custom("test", Approval.automaticApproval());
        Stage instance = instanceFactory.createStageInstance(stageConfig, new DefaultSchedulingContext("anyone"), "md5", new TimeProvider());
        assertThat(instance.getApprovalType(), is(GoConstants.APPROVAL_SUCCESS));
    }

    @Test
    public void shouldSetManualApprovalOnStageInstance() throws Exception {
        StageConfig stageConfig = StageConfigMother.custom("test", Approval.manualApproval());
        Stage instance = instanceFactory.createStageInstance(stageConfig, new DefaultSchedulingContext("anyone"), "md5", new TimeProvider());
        assertThat(instance.getApprovalType(), is(GoConstants.APPROVAL_MANUAL));
    }

	@Test
	public void shouldSetFetchMaterialsFlagOnStageInstance() throws Exception {
		StageConfig stageConfig = StageConfigMother.custom("test", Approval.automaticApproval());
		stageConfig.setFetchMaterials(false);
		Stage instance = instanceFactory.createStageInstance(stageConfig, new DefaultSchedulingContext("anyone"), "md5", new TimeProvider());
		assertThat(instance.shouldFetchMaterials(), is(false));
	}

    @Test
    public void shouldClear_DatabaseIds_State_and_Result_ForJobObjectHierarchy() {
        Date old = new DateTime().minusDays(2).toDate();
        JobInstance rails = jobInstance(old, "rails", 7, 10);
        JobInstance java = jobInstance(old, "java", 12, 22);
        Stage stage = stage(9, rails, java);
        assertThat(stage.hasRerunJobs(), CoreMatchers.is(false));

        Stage newStage = instanceFactory.createStageForRerunOfJobs(stage, a("rails"), new DefaultSchedulingContext("loser", new Agents()), StageConfigMother.custom("dev", "rails", "java"),
                new TimeProvider(), "md5");
        assertThat(stage.hasRerunJobs(), CoreMatchers.is(false));

        assertThat(newStage.getId(), CoreMatchers.is(-1l));
        assertThat(newStage.getJobInstances().size(), CoreMatchers.is(2));
        assertThat(newStage.isLatestRun(), CoreMatchers.is(true));

        JobInstance newRails = newStage.getJobInstances().getByName("rails");
        assertNewJob(old, newRails);

        JobInstance newJava = newStage.getJobInstances().getByName("java");
        assertCopiedJob(newJava, 12l);
    }

	@Test
    public void should_MaintainRerunOfReferences_InCaseOfMultipleCopyForRerunOperations() {
        Date old = new DateTime().minusDays(2).toDate();
        JobInstance rails = jobInstance(old, "rails", 7, 10);
        JobInstance java = jobInstance(old, "java", 12, 22);
        Stage stage = stage(9, rails, java);
        stage.setCounter(2);

        Stage newStage = instanceFactory.createStageForRerunOfJobs(stage, a("rails"), new DefaultSchedulingContext("loser", new Agents()), StageConfigMother.custom("dev", "rails", "java"),
                new TimeProvider(), "md5");
        newStage.setCounter(3);
        assertThat(newStage.getId(), CoreMatchers.is(-1l));
        assertThat(newStage.getJobInstances().size(), CoreMatchers.is(2));
        assertThat(newStage.isLatestRun(), CoreMatchers.is(true));
        assertThat(newStage.getRerunOfCounter(), CoreMatchers.is(2));

        JobInstance newJava = newStage.getJobInstances().getByName("java");
        assertCopiedJob(newJava, 12l);

        //set id, to assert if original ends up pointing to copied job's id
        newJava.setId(18l);

        newStage = instanceFactory.createStageForRerunOfJobs(newStage, a("rails"), new DefaultSchedulingContext("loser", new Agents()), StageConfigMother.custom("dev", "rails", "java"),
                new TimeProvider(), "md5");
        newStage.setCounter(4);
        assertThat(newStage.getId(), CoreMatchers.is(-1l));
        assertThat(newStage.getJobInstances().size(), CoreMatchers.is(2));
        assertThat(newStage.isLatestRun(), CoreMatchers.is(true));
        assertThat(newStage.getRerunOfCounter(), CoreMatchers.is(2));

        newJava = newStage.getJobInstances().getByName("java");
        assertCopiedJob(newJava, 12l);
    }

    @Test
    public void shouldCloneStageForGivenJobsWithLatestMd5() {
        TimeProvider timeProvider = new TimeProvider() {
            @Override
            public Date currentTime() {
                return new Date();
            }

            public DateTime currentDateTime() {
                throw new UnsupportedOperationException("Not implemented");
            }

            public DateTime timeoutTime(Timeout timeout) {
                throw new UnsupportedOperationException("Not implemented");
            }
        };
        JobInstance firstJob = new JobInstance("first-job", timeProvider);
        JobInstance secondJob = new JobInstance("second-job", timeProvider);
        JobInstances jobInstances = new JobInstances(firstJob, secondJob);
        Stage stage = StageMother.custom("test", jobInstances);
        Stage clonedStage = instanceFactory.createStageForRerunOfJobs(stage, Arrays.asList("first-job"), new DefaultSchedulingContext("loser", new Agents()),
                StageConfigMother.custom("test", "first-job", "second-job"),
                new TimeProvider(),
                "latest");
        assertThat(clonedStage.getConfigVersion(), CoreMatchers.is("latest"));
    }

    @Test
    public void shouldAddEnvironmentVariablesPresentInTheScheduleContextToJobPlan() throws Exception {
        JobConfig jobConfig = new JobConfig("foo");

        EnvironmentVariablesConfig variablesConfig = new EnvironmentVariablesConfig();
        variablesConfig.add("blahVar", "blahVal");
        SchedulingContext context = new DefaultSchedulingContext("Loser");
        context = context.overrideEnvironmentVariables(variablesConfig);
        JobPlan plan = instanceFactory.createJobPlan(jobConfig, context);
        assertThat(plan.getVariables(), hasItem(new EnvironmentVariableConfig("blahVar", "blahVal")));
    }

    @Test
    public void shouldOverrideEnvironmentVariablesPresentInTheScheduleContextToJobPlan() throws Exception {
        EnvironmentVariablesConfig variablesConfig = new EnvironmentVariablesConfig();
        variablesConfig.add("blahVar", "blahVal");
        variablesConfig.add("differentVar", "differentVal");

        JobConfig jobConfig = new JobConfig("foo");
        jobConfig.setVariables(variablesConfig);

        EnvironmentVariablesConfig overridenConfig = new EnvironmentVariablesConfig();
        overridenConfig.add("blahVar", "originalVal");
        overridenConfig.add("secondVar", "secondVal");

        SchedulingContext context = new DefaultSchedulingContext();
        context = context.overrideEnvironmentVariables(overridenConfig);

        JobPlan plan = instanceFactory.createJobPlan(jobConfig, context);

        assertThat(plan.getVariables().size(), is(3));
        assertThat(plan.getVariables(), hasItem(new EnvironmentVariableConfig("blahVar", "blahVal")));
        assertThat(plan.getVariables(), hasItem(new EnvironmentVariableConfig("secondVar", "secondVal")));
        assertThat(plan.getVariables(), hasItem(new EnvironmentVariableConfig("differentVar", "differentVal")));
    }

    @Test
    public void shouldAddEnvironmentVariablesToJobPlan() throws Exception {
        EnvironmentVariablesConfig variablesConfig = new EnvironmentVariablesConfig();
        variablesConfig.add("blahVar", "blahVal");

        JobConfig jobConfig = new JobConfig("foo");
        jobConfig.setVariables(variablesConfig);

        SchedulingContext context = new DefaultSchedulingContext();

        JobPlan plan = instanceFactory.createJobPlan(jobConfig, context);

        assertThat(plan.getVariables(), hasItem(new EnvironmentVariableConfig("blahVar", "blahVal")));
    }

    @Test
    public void shouldCreateJobPlan() {
        Resources resources = new Resources();
        ArtifactPlans artifactPlans = new ArtifactPlans();
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("test"), resources, artifactPlans);
        JobPlan plan = instanceFactory.createJobPlan(jobConfig, new DefaultSchedulingContext());
        assertThat(plan, is((JobPlan) new DefaultJobPlan(resources, artifactPlans, new ArtifactPropertiesGenerators(), -1, new JobIdentifier(), null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null)));
    }

    @Test
    public void shouldReturnBuildInstance() {
        ArtifactPlans artifactPlans = new ArtifactPlans();
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("test"), null, artifactPlans);

        RunOnAllAgents.CounterBasedJobNameGenerator jobNameGenerator = new RunOnAllAgents.CounterBasedJobNameGenerator(CaseInsensitiveString.str(jobConfig.name()));
        JobInstances jobs = instanceFactory.createJobInstance(new CaseInsensitiveString("stage_foo"), jobConfig, new DefaultSchedulingContext(), new TimeProvider(), jobNameGenerator);

        JobInstance jobInstance = jobs.first();
        assertThat(jobConfig.name(), is(new CaseInsensitiveString(jobInstance.getName())));
        assertThat(jobInstance.getState(), is(JobState.Scheduled));
        assertThat(jobInstance.getScheduledDate(), is(notNullValue()));
    }

	@Test
	public void shouldUseRightNameGenerator() {
		StageConfig stageConfig = StageConfigMother.custom("dev", "rails", "java", "html");

		JobConfig railsConfig = stageConfig.getJobs().getJob(new CaseInsensitiveString("rails"));
		railsConfig.setRunOnAllAgents(true);
		railsConfig.addResource("foobar");

		JobConfig javaConfig = stageConfig.getJobs().getJob(new CaseInsensitiveString("java"));
		javaConfig.setRunInstanceCount(2);

		AgentConfig agent1 = new AgentConfig("abcd1234", "host", "127.0.0.2", new Resources(new Resource("foobar")));
		AgentConfig agent2 = new AgentConfig("1234abcd", "ghost", "192.168.1.2", new Resources(new Resource("baz"), new Resource("foobar")));
		AgentConfig agent3 = new AgentConfig("7890abdc", "lost", "10.4.3.55", new Resources(new Resource("crapyagent")));
		DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("loser", new Agents(agent1, agent2, agent3));

		Stage stageInstance = instanceFactory.createStageInstance(stageConfig, schedulingContext, "md5", clock);

		JobInstances jobInstances = stageInstance.getJobInstances();
		assertThat(jobInstances.size(), is(5));
		assertRunOnAllAgentsJobInstance(jobInstances.get(0), "rails-runOnAll-1");
		assertRunOnAllAgentsJobInstance(jobInstances.get(1), "rails-runOnAll-2");
		assertRunMultipleJobInstance(jobInstances.get(2), "java-runInstance-1");
		assertRunMultipleJobInstance(jobInstances.get(3), "java-runInstance-2");
		assertSimpleJobInstance(jobInstances.get(4), "html");
	}

	/*
	SingleJobInstance
	 */

    @Test
    public void shouldCreateASingleJobIfRunOnAllAgentsIsFalse() throws Exception {
        JobConfig jobConfig = new JobConfig("foo");

        SchedulingContext context = mock(SchedulingContext.class);
        when(context.getEnvironmentVariablesConfig()).thenReturn(new EnvironmentVariablesConfig());
        when(context.overrideEnvironmentVariables(any(EnvironmentVariablesConfig.class))).thenReturn(context);

        RunOnAllAgents.CounterBasedJobNameGenerator jobNameGenerator = new RunOnAllAgents.CounterBasedJobNameGenerator(CaseInsensitiveString.str(jobConfig.name()));
        JobInstances jobs = instanceFactory.createJobInstance(new CaseInsensitiveString("someStage"), jobConfig, new DefaultSchedulingContext(), new TimeProvider(), jobNameGenerator);

        assertThat(jobs.toArray(), hasItemInArray(hasProperty("name", is("foo"))));
        assertThat(jobs.toArray(), hasItemInArray(hasProperty("agentUuid", IsNull.nullValue())));
        assertThat(jobs.toArray(), hasItemInArray(hasProperty("runOnAllAgents", is(false))));
        assertThat(jobs.size(), is(1));
    }

	@Test
	public void shouldNotRerun_WhenJobConfigDoesNotExistAnymore_ForSingleInstanceJob() {
		Date old = new DateTime().minusDays(2).toDate();
		JobInstance rails = jobInstance(old, "rails", 7, 10);
		JobInstance java = jobInstance(old, "java", 12, 22);
		Stage stage = stage(9, rails, java);
		Stage newStage = null;

		CannotRerunJobException exception = null;
		try {
			newStage = instanceFactory.createStageForRerunOfJobs(stage, a("rails"), new DefaultSchedulingContext("loser", new Agents()), StageConfigMother.custom("dev", "java"), new TimeProvider(),
					"md5");
			fail("should not schedule when job config does not exist anymore");
		} catch (CannotRerunJobException e) {
			exception = e;
		}
		assertThat(exception.getJobName(), CoreMatchers.is("rails"));
		assertThat(newStage, CoreMatchers.is(nullValue()));
	}

	@Test
	public void shouldClearAgentAssignment_ForSingleInstanceJobType() {
		Date old = new DateTime().minusDays(2).toDate();
		JobInstance rails = jobInstance(old, "rails", 7, 10);
		JobInstance java = jobInstance(old, "java", 12, 22);
		Stage stage = stage(9, rails, java);
		Stage newStage = instanceFactory.createStageForRerunOfJobs(stage, a("rails"), new DefaultSchedulingContext("loser", new Agents()), StageConfigMother.custom("dev", "rails", "java"),
				new TimeProvider(), "md5");
		assertThat(newStage.getJobInstances().getByName("rails").getAgentUuid(), CoreMatchers.is(nullValue()));
		assertThat(newStage.getJobInstances().getByName("java").getAgentUuid(), CoreMatchers.is(not(nullValue())));
	}

	@Test
	public void shouldNotRerun_WhenJobConfigIsChangedToRunMultipleInstance_ForSingleJobInstance() {
		Date old = new DateTime().minusDays(2).toDate();
		StageConfig stageConfig = StageConfigMother.custom("dev", "rails", "java");

		JobConfig railsConfig = stageConfig.getJobs().getJob(new CaseInsensitiveString("rails"));

		DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("loser", new Agents());

		JobInstances jobs = instanceFactory.createJobInstance(new CaseInsensitiveString("dev"), railsConfig, schedulingContext, new TimeProvider(), null);

		Stage stage = createStageInstance(old, jobs);
		Stage newStage = null;

		railsConfig.setRunInstanceCount(10);

		CannotRerunJobException exception = null;
		try {
			newStage = instanceFactory.createStageForRerunOfJobs(stage, a("rails"), schedulingContext, stageConfig, new TimeProvider(), "md5");
			fail("should not schedule since job config changed to run multiple instance");
		} catch (CannotRerunJobException e) {
			exception = e;
		}
		assertThat(exception.getJobName(), is("rails"));
		assertThat(exception.getInformation(), is("Run configuration for job has been changed to 'run multiple instance'."));
		assertThat(newStage, is(nullValue()));
	}

	/*
	RunOnAllAgents tests
	*/

	@Test
	public void shouldCreateAJobForEachAgentIfRunOnAllAgentsIsTrue() throws Exception {
		Agents agents = new Agents();
		agents.add(new AgentConfig("uuid1"));
		agents.add(new AgentConfig("uuid2"));

		JobConfig jobConfig = new JobConfig("foo");
		jobConfig.setRunOnAllAgents(true);

		SchedulingContext context = mock(SchedulingContext.class);
		when(context.getApprovedBy()).thenReturn("chris");
		when(context.findAgentsMatching(new Resources())).thenReturn(agents);
		when(context.getEnvironmentVariablesConfig()).thenReturn(new EnvironmentVariablesConfig());
		when(context.overrideEnvironmentVariables(any(EnvironmentVariablesConfig.class))).thenReturn(context);

		RunOnAllAgents.CounterBasedJobNameGenerator jobNameGenerator = new RunOnAllAgents.CounterBasedJobNameGenerator(CaseInsensitiveString.str(jobConfig.name()));
		JobInstances jobs = instanceFactory.createJobInstance(new CaseInsensitiveString("stageName"), jobConfig, context, new TimeProvider(), jobNameGenerator);

		assertThat(jobs.toArray(), hasItemInArray(hasProperty("name", is("foo-runOnAll-1"))));
		assertThat(jobs.toArray(), hasItemInArray(hasProperty("agentUuid", is("uuid1"))));
		assertThat(jobs.toArray(), hasItemInArray(hasProperty("runOnAllAgents", is(true))));

		assertThat(jobs.toArray(), hasItemInArray(hasProperty("name", is("foo-runOnAll-1"))));
		assertThat(jobs.toArray(), hasItemInArray(hasProperty("agentUuid", is("uuid2"))));
		assertThat(jobs.toArray(), hasItemInArray(hasProperty("runOnAllAgents", is(true))));

		assertThat(jobs.size(), is(2));
	}

	@Test
	public void shouldFailWhenDoesNotFindAnyMatchingAgents() throws Exception {
		JobConfig jobConfig = new JobConfig("foo");
		jobConfig.setRunOnAllAgents(true);

		SchedulingContext context = mock(SchedulingContext.class);
		when(context.getApprovedBy()).thenReturn("chris");
		when(context.findAgentsMatching(new Resources())).thenReturn(new ArrayList<AgentConfig>());
		when(context.getEnvironmentVariablesConfig()).thenReturn(new EnvironmentVariablesConfig());
		when(context.overrideEnvironmentVariables(any(EnvironmentVariablesConfig.class))).thenReturn(context);

		try {
			RunOnAllAgents.CounterBasedJobNameGenerator jobNameGenerator = new RunOnAllAgents.CounterBasedJobNameGenerator(CaseInsensitiveString.str(jobConfig.name()));
			instanceFactory.createJobInstance(new CaseInsensitiveString("myStage"), jobConfig, new DefaultSchedulingContext(), new TimeProvider(), jobNameGenerator);

			fail("should have failed as no agents matched");
		} catch (Exception e) {
			assertThat(e.getMessage(), is("Could not find matching agents to run job [foo] of stage [myStage]."));
		}
	}

	@Test
	public void shouldFailWhenNoAgentsmatchAJob() throws Exception {
		DefaultSchedulingContext context = new DefaultSchedulingContext("raghu/vinay", new Agents());
		JobConfig fooJob = new JobConfig(new CaseInsensitiveString("foo"), new Resources(), new ArtifactPlans());
		fooJob.setRunOnAllAgents(true);
		StageConfig stageConfig = new StageConfig(
				new CaseInsensitiveString("blah-stage"), new JobConfigs(
				fooJob,
				new JobConfig(new CaseInsensitiveString("bar"), new Resources(), new ArtifactPlans())
		)
		);
		try {
			instanceFactory.createStageInstance(stageConfig, context, "md5", new TimeProvider());
			fail("expected exception but not thrown");
		} catch (Exception e) {
			assertThat(e.getMessage(), is("Could not find matching agents to run job [foo] of stage [blah-stage]."));
		}
	}

	@Test
	public void shouldBomb_ForRerun_OfASingleInstanceJobType_WhichWasEarlierRunOnAll_WithTwoRunOnAllInstancesSelectedForRerun() {
		Date old = new DateTime().minusDays(2).toDate();
		StageConfig stageConfig = StageConfigMother.custom("dev", "rails", "java");

		JobConfig railsConfig = stageConfig.getJobs().getJob(new CaseInsensitiveString("rails"));
		railsConfig.setRunOnAllAgents(true);
		railsConfig.addResource("foobar");

		DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("loser", new Agents(
				new AgentConfig("abcd1234", "host", "127.0.0.2", new Resources(new Resource("foobar"))),
				new AgentConfig("1234abcd", "ghost", "192.168.1.2", new Resources(new Resource("baz"), new Resource("foobar"))),
				new AgentConfig("7890abdc", "lost", "10.4.3.55", new Resources(new Resource("crapyagent")))));

		RunOnAllAgents.CounterBasedJobNameGenerator jobNameGenerator = new RunOnAllAgents.CounterBasedJobNameGenerator(CaseInsensitiveString.str(railsConfig.name()));
		JobInstances jobs = instanceFactory.createJobInstance(new CaseInsensitiveString("dev"), railsConfig, schedulingContext, new TimeProvider(), jobNameGenerator);

		Stage stage = createStageInstance(old, jobs);

		railsConfig.setRunOnAllAgents(false);

		try {
			instanceFactory.createStageForRerunOfJobs(stage, a("rails-runOnAll-1", "rails-runOnAll-2"), schedulingContext, stageConfig, new TimeProvider(), "md5");
			fail("should have failed when multiple run on all agents jobs are selected when job-config does not have run on all flag anymore");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), CoreMatchers.is("Cannot schedule multiple instances of job named 'rails'."));
		}
	}

	@Test
	public void should_NOT_ClearAgentAssignment_ForRerun_OfASingleInstanceJobType_WhichWasEarlierRunOnAll() {
		Date old = new DateTime().minusDays(2).toDate();
		StageConfig stageConfig = StageConfigMother.custom("dev", "rails", "java");

		JobConfig railsConfig = stageConfig.getJobs().getJob(new CaseInsensitiveString("rails"));
		railsConfig.setRunOnAllAgents(true);
		railsConfig.addResource("foobar");

		DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("loser", new Agents(
				new AgentConfig("abcd1234", "host", "127.0.0.2", new Resources(new Resource("foobar"))),
				new AgentConfig("1234abcd", "ghost", "192.168.1.2", new Resources(new Resource("baz"), new Resource("foobar"))),
				new AgentConfig("7890abdc", "lost", "10.4.3.55", new Resources(new Resource("crapyagent")))));

		RunOnAllAgents.CounterBasedJobNameGenerator jobNameGenerator = new RunOnAllAgents.CounterBasedJobNameGenerator(CaseInsensitiveString.str(railsConfig.name()));
		JobInstances jobs = instanceFactory.createJobInstance(new CaseInsensitiveString("dev"), railsConfig, schedulingContext, new TimeProvider(), jobNameGenerator);
		Stage stage = createStageInstance(old, jobs);

		railsConfig.setRunOnAllAgents(false);

		Stage newStage = instanceFactory.createStageForRerunOfJobs(stage, a("rails-runOnAll-1"), schedulingContext, stageConfig, new TimeProvider(), "md5");

		assertThat(newStage.getJobInstances().size(), CoreMatchers.is(3));

		JobInstance newRailsJob = newStage.getJobInstances().getByName("rails");
		assertNewJob(old, newRailsJob);
		assertThat(newRailsJob.getAgentUuid(), CoreMatchers.is("abcd1234"));

		JobInstance copiedRailsJob = newStage.getJobInstances().getByName("rails-runOnAll-2");
		assertCopiedJob(copiedRailsJob, 102l);
		assertThat(copiedRailsJob.getAgentUuid(), CoreMatchers.is("1234abcd"));

		JobInstance copiedJavaJob = newStage.getJobInstances().getByName("java");
		assertCopiedJob(copiedJavaJob, 12l);
		assertThat(copiedJavaJob.getAgentUuid(), CoreMatchers.is(not(nullValue())));
	}

	@Test
	public void shouldClearAgentAssignment_ForRunOnAllAgentsJobType() {
		Date old = new DateTime().minusDays(2).toDate();
		JobInstance rails = jobInstance(old, "rails", 7, 10);
		JobInstance java = jobInstance(old, "java", 12, 22);
		Stage stage = stage(9, rails, java);
		StageConfig stageConfig = StageConfigMother.custom("dev", "rails", "java");
		JobConfig railsConfig = stageConfig.getJobs().getJob(new CaseInsensitiveString("rails"));
		railsConfig.setRunOnAllAgents(true);
		railsConfig.addResource("foobar");
		Stage newStage = instanceFactory.createStageForRerunOfJobs(stage, a("rails"), new DefaultSchedulingContext("loser", new Agents(
				new AgentConfig("abcd1234", "host", "127.0.0.2", new Resources(new Resource("foobar"))),
				new AgentConfig("1234abcd", "ghost", "192.168.1.2", new Resources(new Resource("baz"), new Resource("foobar"))),
				new AgentConfig("7890abdc", "lost", "10.4.3.55", new Resources(new Resource("crapyagent"))))), stageConfig, new TimeProvider(), "md5");

		assertThat(newStage.getJobInstances().size(), CoreMatchers.is(3));

		JobInstance newRailsFirstJob = newStage.getJobInstances().getByName("rails-runOnAll-1");
		assertNewJob(old, newRailsFirstJob);
		assertThat(newRailsFirstJob.getAgentUuid(), CoreMatchers.is("abcd1234"));

		JobInstance newRailsSecondJob = newStage.getJobInstances().getByName("rails-runOnAll-2");
		assertNewJob(old, newRailsSecondJob);
		assertThat(newRailsSecondJob.getAgentUuid(), CoreMatchers.is("1234abcd"));

		JobInstance copiedJavaJob = newStage.getJobInstances().getByName("java");
		assertCopiedJob(copiedJavaJob, 12l);
		assertThat(copiedJavaJob.getAgentUuid(), CoreMatchers.is(not(nullValue())));
	}

	@Test
	public void shouldNotRerun_WhenJobConfigDoesNotExistAnymore_ForRunOnAllAgentsJobInstance() {
		Date old = new DateTime().minusDays(2).toDate();
		StageConfig stageConfig = StageConfigMother.custom("dev", "rails", "java");

		JobConfig railsConfig = stageConfig.getJobs().getJob(new CaseInsensitiveString("rails"));
		railsConfig.setRunOnAllAgents(true);
		railsConfig.addResource("foobar");

		DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("loser", new Agents(
				new AgentConfig("abcd1234", "host", "127.0.0.2", new Resources(new Resource("foobar"))),
				new AgentConfig("1234abcd", "ghost", "192.168.1.2", new Resources(new Resource("baz"), new Resource("foobar"))),
				new AgentConfig("7890abdc", "lost", "10.4.3.55", new Resources(new Resource("crapyagent")))));

		RunOnAllAgents.CounterBasedJobNameGenerator jobNameGenerator = new RunOnAllAgents.CounterBasedJobNameGenerator(CaseInsensitiveString.str(railsConfig.name()));
		JobInstances jobs = instanceFactory.createJobInstance(new CaseInsensitiveString("dev"), railsConfig, schedulingContext, new TimeProvider(), jobNameGenerator);

		Stage stage = createStageInstance(old, jobs);
		Stage newStage = null;

		CannotRerunJobException exception = null;
		try {
			newStage = instanceFactory.createStageForRerunOfJobs(stage, a("rails-runOnAll-1"), new DefaultSchedulingContext("loser", new Agents()), StageConfigMother.custom("dev", "java"),
					new TimeProvider(), "md5");
			fail("should not schedule when job config does not exist anymore");
		} catch (CannotRerunJobException e) {
			exception = e;
		}
		assertThat(exception.getJobName(), CoreMatchers.is("rails"));
		assertThat(newStage, CoreMatchers.is(nullValue()));
	}

	@Test
	public void shouldNotRerun_WhenJobConfigIsChangedToRunMultipleInstance_ForRunOnAllAgentsJobInstance() {
		Date old = new DateTime().minusDays(2).toDate();
		StageConfig stageConfig = StageConfigMother.custom("dev", "rails", "java");

		JobConfig railsConfig = stageConfig.getJobs().getJob(new CaseInsensitiveString("rails"));
		railsConfig.setRunOnAllAgents(true);
		railsConfig.addResource("foobar");

		AgentConfig agent1 = new AgentConfig("abcd1234", "host", "127.0.0.2", new Resources(new Resource("foobar")));
		AgentConfig agent2 = new AgentConfig("1234abcd", "ghost", "192.168.1.2", new Resources(new Resource("baz"), new Resource("foobar")));
		AgentConfig agent3 = new AgentConfig("7890abdc", "lost", "10.4.3.55", new Resources(new Resource("crapyagent")));
		DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("loser", new Agents(agent1, agent2, agent3));

		RunOnAllAgents.CounterBasedJobNameGenerator jobNameGenerator = new RunOnAllAgents.CounterBasedJobNameGenerator(CaseInsensitiveString.str(railsConfig.name()));
		JobInstances jobs = instanceFactory.createJobInstance(new CaseInsensitiveString("dev"), railsConfig, schedulingContext, new TimeProvider(), jobNameGenerator);

		Stage stage = createStageInstance(old, jobs);
		Stage newStage = null;

		railsConfig.setRunOnAllAgents(false);
		railsConfig.setRunInstanceCount(10);

		CannotRerunJobException exception = null;
		try {
			newStage = instanceFactory.createStageForRerunOfJobs(stage, a("rails-runOnAll-1"), schedulingContext, stageConfig, new TimeProvider(), "md5");
			fail("should not schedule since job config changed to run multiple instance");
		} catch (CannotRerunJobException e) {
			exception = e;
		}
		assertThat(exception.getJobName(), is("rails"));
		assertThat(exception.getInformation(), is("Run configuration for job has been changed to 'run multiple instance'."));
		assertThat(newStage, is(nullValue()));
	}

	/*
	RunMultipleInstance tests
	 */

	@Test
	public void shouldCreateJobInstancesCorrectly_RunMultipleInstance() {
		Date old = new DateTime().minusDays(2).toDate();
		StageConfig stageConfig = StageConfigMother.custom("dev", "rails", "java");

		JobConfig railsConfig = stageConfig.getJobs().getJob(new CaseInsensitiveString("rails"));
		railsConfig.setRunInstanceCount(3);

		DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("loser", new Agents());

		RunMultipleInstance.CounterBasedJobNameGenerator jobNameGenerator = new RunMultipleInstance.CounterBasedJobNameGenerator(CaseInsensitiveString.str(railsConfig.name()));
		JobInstances jobs = instanceFactory.createJobInstance(new CaseInsensitiveString("dev"), railsConfig, schedulingContext, new TimeProvider(), jobNameGenerator);

		assertThat(jobs.get(0).getName(), is("rails-runInstance-1"));
		assertEnvironmentVariable(jobs.get(0), 0, "GO_JOB_RUN_INDEX", "1");
		assertEnvironmentVariable(jobs.get(0), 1, "GO_JOB_RUN_COUNT", "3");
		assertThat(jobs.get(1).getName(), is("rails-runInstance-2"));
		assertEnvironmentVariable(jobs.get(1), 0, "GO_JOB_RUN_INDEX", "2");
		assertEnvironmentVariable(jobs.get(1), 1, "GO_JOB_RUN_COUNT", "3");
		assertThat(jobs.get(2).getName(), is("rails-runInstance-3"));
		assertEnvironmentVariable(jobs.get(2), 0, "GO_JOB_RUN_INDEX", "3");
		assertEnvironmentVariable(jobs.get(2), 1, "GO_JOB_RUN_COUNT", "3");

		Stage stage = createStageInstance(old, jobs);

		JobInstances jobInstances = stage.getJobInstances();
		assertThat(jobInstances.size(), is(4));
		assertRunMultipleJobInstance(jobInstances.get(0), "rails-runInstance-1");
		assertRunMultipleJobInstance(jobInstances.get(1), "rails-runInstance-2");
		assertRunMultipleJobInstance(jobInstances.get(2), "rails-runInstance-3");
		assertThat(jobInstances.get(3).getName(), is("java"));
	}

	@Test
	public void shouldCreateJobInstancesCorrectly_RunMultipleInstance_Rerun() {
		Date old = new DateTime().minusDays(2).toDate();
		StageConfig stageConfig = StageConfigMother.custom("dev", "rails", "java");

		JobConfig railsConfig = stageConfig.getJobs().getJob(new CaseInsensitiveString("rails"));
		railsConfig.setRunInstanceCount(3);

		DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("loser", new Agents());

		RunMultipleInstance.CounterBasedJobNameGenerator jobNameGenerator = new RunMultipleInstance.CounterBasedJobNameGenerator(CaseInsensitiveString.str(railsConfig.name()));
		JobInstances jobs = instanceFactory.createJobInstance(new CaseInsensitiveString("dev"), railsConfig, schedulingContext, new TimeProvider(), jobNameGenerator);

		assertThat(jobs.get(0).getName(), is("rails-runInstance-1"));
		assertEnvironmentVariable(jobs.get(0), 0, "GO_JOB_RUN_INDEX", "1");
		assertEnvironmentVariable(jobs.get(0), 1, "GO_JOB_RUN_COUNT", "3");
		assertThat(jobs.get(1).getName(), is("rails-runInstance-2"));
		assertEnvironmentVariable(jobs.get(1), 0, "GO_JOB_RUN_INDEX", "2");
		assertEnvironmentVariable(jobs.get(1), 1, "GO_JOB_RUN_COUNT", "3");
		assertThat(jobs.get(2).getName(), is("rails-runInstance-3"));
		assertEnvironmentVariable(jobs.get(2), 0, "GO_JOB_RUN_INDEX", "3");
		assertEnvironmentVariable(jobs.get(2), 1, "GO_JOB_RUN_COUNT", "3");

		Stage stage = createStageInstance(old, jobs);

		Stage stageForRerun = instanceFactory.createStageForRerunOfJobs(stage, a("rails-runInstance-1", "rails-runInstance-2"), schedulingContext, stageConfig, clock, "md5");
		JobInstances jobsForRerun = stageForRerun.getJobInstances();

		assertThat(jobsForRerun.get(0).getName(), is("rails-runInstance-3"));
		assertEnvironmentVariable(jobsForRerun.get(0), 0, "GO_JOB_RUN_INDEX", "3");
		assertEnvironmentVariable(jobsForRerun.get(0), 1, "GO_JOB_RUN_COUNT", "3");
		assertThat(jobsForRerun.get(2).getName(), is("rails-runInstance-1"));
		assertEnvironmentVariable(jobsForRerun.get(2), 0, "GO_JOB_RUN_INDEX", "1");
		assertEnvironmentVariable(jobsForRerun.get(2), 1, "GO_JOB_RUN_COUNT", "3");
		assertThat(jobsForRerun.get(3).getName(), is("rails-runInstance-2"));
		assertEnvironmentVariable(jobsForRerun.get(3), 0, "GO_JOB_RUN_INDEX", "2");
		assertEnvironmentVariable(jobsForRerun.get(3), 1, "GO_JOB_RUN_COUNT", "3");

		assertThat(jobsForRerun.size(), is(4));
		assertRunMultipleJobInstance(jobsForRerun.get(0), "rails-runInstance-3");
		assertThat(jobsForRerun.get(1).getName(), is("java"));
		assertReRunMultipleJobInstance(jobsForRerun.get(2), "rails-runInstance-1");
		assertReRunMultipleJobInstance(jobsForRerun.get(3), "rails-runInstance-2");
	}

	@Test
	public void shouldNotRerun_WhenJobConfigDoesNotExistAnymore_ForRunMultipleInstance() {
		Date old = new DateTime().minusDays(2).toDate();
		StageConfig stageConfig = StageConfigMother.custom("dev", "rails", "java");

		JobConfig railsConfig = stageConfig.getJobs().getJob(new CaseInsensitiveString("rails"));
		railsConfig.setRunInstanceCount(3);

		DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("loser", new Agents());

		RunMultipleInstance.CounterBasedJobNameGenerator jobNameGenerator = new RunMultipleInstance.CounterBasedJobNameGenerator(CaseInsensitiveString.str(railsConfig.name()));
		JobInstances jobs = instanceFactory.createJobInstance(new CaseInsensitiveString("dev"), railsConfig, schedulingContext, new TimeProvider(), jobNameGenerator);

		Stage stage = createStageInstance(old, jobs);
		Stage newStage = null;

		CannotRerunJobException exception = null;
		try {
			newStage = instanceFactory.createStageForRerunOfJobs(stage, a("rails-runInstance-1"), schedulingContext, StageConfigMother.custom("dev", "java"),
					new TimeProvider(), "md5");
			fail("should not schedule when job config does not exist anymore");
		} catch (CannotRerunJobException e) {
			exception = e;
		}
		assertThat(exception.getJobName(), is("rails"));
		assertThat(exception.getInformation(), is("Configuration for job doesn't exist."));
		assertThat(newStage, is(nullValue()));
	}

	@Test
	public void shouldNotRerun_WhenJobRunConfigIsChanged_ForRunMultipleInstance() {
		Date old = new DateTime().minusDays(2).toDate();
		StageConfig stageConfig = StageConfigMother.custom("dev", "rails", "java");

		JobConfig railsConfig = stageConfig.getJobs().getJob(new CaseInsensitiveString("rails"));
		railsConfig.setRunInstanceCount(3);

		DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("loser", new Agents());

		RunMultipleInstance.CounterBasedJobNameGenerator jobNameGenerator = new RunMultipleInstance.CounterBasedJobNameGenerator(CaseInsensitiveString.str(railsConfig.name()));
		JobInstances jobs = instanceFactory.createJobInstance(new CaseInsensitiveString("dev"), railsConfig, schedulingContext, new TimeProvider(), jobNameGenerator);

		Stage stage = createStageInstance(old, jobs);
		Stage newStage = null;

		railsConfig.setRunOnAllAgents(true);
		railsConfig.setRunInstanceCount(0);

		CannotRerunJobException exception = null;
		try {
			newStage = instanceFactory.createStageForRerunOfJobs(stage, a("rails-runInstance-1"), schedulingContext, stageConfig, new TimeProvider(), "md5");
			fail("should not schedule since job config changed to run multiple instance");
		} catch (CannotRerunJobException e) {
			exception = e;
		}
		assertThat(exception.getJobName(), is("rails"));
		assertThat(exception.getInformation(), is("Run configuration for job has been changed to 'run on all agents'."));
		assertThat(newStage, is(nullValue()));

		railsConfig.setRunOnAllAgents(false);

		try {
			newStage = instanceFactory.createStageForRerunOfJobs(stage, a("rails-runInstance-1"), schedulingContext, stageConfig, new TimeProvider(), "md5");
			fail("should not schedule since job config changed to run multiple instance");
		} catch (CannotRerunJobException e) {
			exception = e;
		}
		assertThat(exception.getJobName(), is("rails"));
		assertThat(exception.getInformation(), is("Run configuration for job has been changed to 'simple'."));
		assertThat(newStage, is(nullValue()));
	}

	private Stage stage(long id, JobInstance... jobs) {
        Stage stage = new Stage("dev", new JobInstances(jobs), "anonymous", "manual", new TimeProvider());
        stage.setId(id);
        return stage;
    }

	private Stage createStageInstance(Date old, JobInstances jobs) {
		int jobId = 100;
		for (JobInstance job : jobs) {
			passJob(new Date(), ++jobId, jobId * 10, job);
		}

		JobInstance java = jobInstance(old, "java", 12, 22);
		jobs.add(java);

		return stage(9, jobs.toArray(new JobInstance[0]));
	}

    private JobInstance jobInstance(final Date date, final String jobName, final int id, int transitionIdStart) {
        JobInstance jobInstance = new JobInstance(jobName, new TimeProvider() {
            @Override
            public Date currentTime() {
                return date;
            }
        });
        jobInstance.setAgentUuid(UUID.randomUUID().toString());
        return passJob(date, id, transitionIdStart, jobInstance);
    }

    private JobInstance passJob(Date date, int id, int transitionIdStart, JobInstance jobInstance) {
        jobInstance.setId(id);

        jobInstance.changeState(JobState.Completed, date);

        for (JobStateTransition jobStateTransition : jobInstance.getTransitions()) {
            jobStateTransition.setId(++transitionIdStart);
        }
        jobInstance.setResult(JobResult.Passed);

        return jobInstance;
    }

    private void assertCopiedJob(JobInstance newJava, final long originalId) {
        assertThat(newJava.getId(), CoreMatchers.is(-1l));
        assertThat(newJava.getTransitions().isEmpty(), CoreMatchers.is(false));
        assertThat(newJava.getResult(), CoreMatchers.is(JobResult.Passed));
        assertThat(newJava.getState(), CoreMatchers.is(JobState.Completed));
        assertThat(newJava.getTransitions().byState(JobState.Scheduled).getId(), CoreMatchers.is(-1l));
        assertThat(newJava.getTransitions().byState(JobState.Completed).getId(), CoreMatchers.is(-1l));
        assertThat(newJava.getOriginalJobId(), CoreMatchers.is(originalId));
        assertThat(newJava.isRerun(), CoreMatchers.is(false));
        assertThat(newJava.isCopy(), CoreMatchers.is(true));
    }

    private void assertNewJob(Date old, JobInstance newRails) {
        JobStateTransition newSchedulingTransition = assertNewJob(newRails);
        assertThat(newSchedulingTransition.getStateChangeTime().after(old), CoreMatchers.is(true));
    }

    private JobStateTransition assertNewJob(JobInstance newRails) {
        assertThat(newRails.getId(), CoreMatchers.is(-1l));
        assertThat(newRails.getTransitions().size(), CoreMatchers.is(1));
        JobStateTransition newSchedulingTransition = newRails.getTransitions().byState(JobState.Scheduled);
        assertThat(newSchedulingTransition.getId(), CoreMatchers.is(-1l));
        assertThat(newRails.getResult(), CoreMatchers.is(JobResult.Unknown));
        assertThat(newRails.getState(), CoreMatchers.is(JobState.Scheduled));
        assertThat(newRails.isRerun(), CoreMatchers.is(true));
        return newSchedulingTransition;
    }

	private void assertSimpleJobInstance(JobInstance jobInstance, String jobName) {
		assertThat(jobInstance.getName(), is(jobName));
		assertThat(jobInstance.isRunOnAllAgents(), is(false));
		assertThat(jobInstance.isRunMultipleInstance(), is(false));
		assertThat(jobInstance.isRerun(), is(false));
	}

	private void assertRunOnAllAgentsJobInstance(JobInstance jobInstance, String jobName) {
		assertThat(jobInstance.getName(), is(jobName));
		assertThat(jobInstance.isRunOnAllAgents(), is(true));
		assertThat(jobInstance.isRunMultipleInstance(), is(false));
		assertThat(jobInstance.isRerun(), is(false));
	}

	private void assertRunMultipleJobInstance(JobInstance jobInstance, String jobName) {
		assertThat(jobInstance.getName(), is(jobName));
		assertThat(jobInstance.isRunMultipleInstance(), is(true));
		assertThat(jobInstance.isRunOnAllAgents(), is(false));
		assertThat(jobInstance.isRerun(), is(false));
	}

	private void assertReRunMultipleJobInstance(JobInstance jobInstance, String jobName) {
		assertThat(jobInstance.getName(), is(jobName));
		assertThat(jobInstance.isRunMultipleInstance(), is(true));
		assertThat(jobInstance.isRunOnAllAgents(), is(false));
		assertThat(jobInstance.isRerun(), is(true));
	}

	private void assertEnvironmentVariable(JobInstance jobInstance, int index, String name, String value) {
		assertThat(jobInstance.getPlan().getVariables().get(index).getName(), is(name));
		assertThat(jobInstance.getPlan().getVariables().get(index).getValue(), is(value));
	}
}
