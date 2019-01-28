/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.helper.JobConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.service.builders.BuilderFactory;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.websocket.AgentRemoteHandler;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class BuildAssignmentServiceTest {

    @Mock
    private GoConfigService goConfigService;
    @Mock
    private JobInstanceService jobInstanceService;
    @Mock
    private ScheduleService scheduleService;
    @Mock
    private ElasticAgentPluginService elasticAgentPluginService;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private AgentRemoteHandler agentRemoteHandler;
    @Mock
    private BuilderFactory builderFactory;
    @Mock
    private PipelineService pipelineService;
    @Mock
    private MaintenanceModeService maintenanceModeService;
    @Mock
    private ScheduledPipelineLoader scheduledPipelineLoader;
    @Mock
    private EnvironmentConfigService environmentConfigService;
    @Mock
    private AgentService agentService;
    private BuildAssignmentService buildAssignmentService;
    @Mock
    private TransactionTemplate transactionTemplate;
    private SchedulingContext schedulingContext;
    private ArrayList<JobPlan> jobPlans;
    private AgentConfig elasticAgent;
    private AgentInstance elasticAgentInstance;
    private ElasticProfile elasticProfile1;
    private ElasticProfile elasticProfile2;
    private String elasticProfileId1;
    private String elasticProfileId2;
    private AgentInstance regularAgentInstance;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        buildAssignmentService = new BuildAssignmentService(goConfigService, jobInstanceService, scheduleService, agentService, environmentConfigService, transactionTemplate, scheduledPipelineLoader, pipelineService, builderFactory, agentRemoteHandler, maintenanceModeService, elasticAgentPluginService, systemEnvironment);
        elasticProfileId1 = "elastic.profile.id.1";
        elasticProfileId2 = "elastic.profile.id.2";
        elasticAgent = AgentMother.elasticAgent();
        elasticAgentInstance = AgentInstance.createFromConfig(elasticAgent, new SystemEnvironment(), null);
        regularAgentInstance = AgentInstance.createFromConfig(AgentMother.approvedAgent(), new SystemEnvironment(), null);
        elasticProfile1 = new ElasticProfile(elasticProfileId1, elasticAgent.getElasticPluginId());
        elasticProfile2 = new ElasticProfile(elasticProfileId2, elasticAgent.getElasticPluginId());
        jobPlans = new ArrayList<>();
        HashMap<String, ElasticProfile> profiles = new HashMap<>();
        profiles.put(elasticProfile1.getId(), elasticProfile1);
        profiles.put(elasticProfile2.getId(), elasticProfile2);
        schedulingContext = new DefaultSchedulingContext("me", new Agents(elasticAgent), profiles);
        when(jobInstanceService.orderedScheduledBuilds()).thenReturn(jobPlans);
        when(environmentConfigService.filterJobsByAgent(ArgumentMatchers.eq(jobPlans), any(String.class))).thenReturn(jobPlans);
        when(environmentConfigService.envForPipeline(any(String.class))).thenReturn("");
        when(maintenanceModeService.isMaintenanceMode()).thenReturn(false);
    }

    @Test
    public void shouldMatchAnElasticJobToAnElasticAgentOnlyIfThePluginAgreesToTheAssignment() {
        PipelineConfig pipelineWithElasticJob = PipelineConfigMother.pipelineWithElasticJob(elasticProfileId1);
        JobPlan jobPlan = new InstanceFactory().createJobPlan(pipelineWithElasticJob.first().getJobs().first(), schedulingContext);
        jobPlans.add(jobPlan);
        when(elasticAgentPluginService.shouldAssignWork(elasticAgentInstance.elasticAgentMetadata(), null, jobPlan.getElasticProfile(), jobPlan.getIdentifier())).thenReturn(true);
        buildAssignmentService.onTimer();

        JobPlan matchingJob = buildAssignmentService.findMatchingJob(elasticAgentInstance);
        assertThat(matchingJob, is(jobPlan));
        assertThat(buildAssignmentService.jobPlans().size(), is(0));
    }

    @Test
    public void shouldNotMatchAnElasticJobToAnElasticAgentOnlyIfThePluginIdMatches() {
        PipelineConfig pipelineWithElasticJob = PipelineConfigMother.pipelineWithElasticJob(elasticProfileId1);
        JobPlan jobPlan1 = new InstanceFactory().createJobPlan(pipelineWithElasticJob.first().getJobs().first(), schedulingContext);
        jobPlans.add(jobPlan1);
        when(elasticAgentPluginService.shouldAssignWork(elasticAgentInstance.elasticAgentMetadata(), null, jobPlan1.getElasticProfile(), null)).thenReturn(false);
        buildAssignmentService.onTimer();

        JobPlan matchingJob = buildAssignmentService.findMatchingJob(elasticAgentInstance);
        assertThat(matchingJob, is(nullValue()));
        assertThat(buildAssignmentService.jobPlans().size(), is(1));
    }

    @Test
    public void shouldMatchAnElasticJobToAnElasticAgentOnlyIfThePluginAgreesToTheAssignmentWhenMultipleElasticJobsRequiringTheSamePluginAreScheduled() {
        PipelineConfig pipelineWith2ElasticJobs = PipelineConfigMother.pipelineWithElasticJob(elasticProfileId1, elasticProfileId2);
        JobPlan jobPlan1 = new InstanceFactory().createJobPlan(pipelineWith2ElasticJobs.first().getJobs().first(), schedulingContext);
        JobPlan jobPlan2 = new InstanceFactory().createJobPlan(pipelineWith2ElasticJobs.first().getJobs().last(), schedulingContext);
        jobPlans.add(jobPlan1);
        jobPlans.add(jobPlan2);
        when(elasticAgentPluginService.shouldAssignWork(elasticAgentInstance.elasticAgentMetadata(), null, jobPlan1.getElasticProfile(), jobPlan1.getIdentifier())).thenReturn(false);
        when(elasticAgentPluginService.shouldAssignWork(elasticAgentInstance.elasticAgentMetadata(), null, jobPlan2.getElasticProfile(), jobPlan2.getIdentifier())).thenReturn(true);
        buildAssignmentService.onTimer();


        JobPlan matchingJob = buildAssignmentService.findMatchingJob(elasticAgentInstance);
        assertThat(matchingJob, is(jobPlan2));
        assertThat(buildAssignmentService.jobPlans().size(), is(1));
    }

    @Test
    public void shouldMatchNonElasticJobToNonElasticAgentIfResourcesMatch() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
        pipeline.first().getJobs().add(JobConfigMother.jobWithNoResourceRequirement());
        pipeline.first().getJobs().add(JobConfigMother.elasticJob(elasticProfileId1));
        JobPlan elasticJobPlan = new InstanceFactory().createJobPlan(pipeline.first().getJobs().last(), schedulingContext);
        JobPlan regularJobPlan = new InstanceFactory().createJobPlan(pipeline.first().getJobs().first(), schedulingContext);
        jobPlans.add(elasticJobPlan);
        jobPlans.add(regularJobPlan);
        buildAssignmentService.onTimer();

        JobPlan matchingJob = buildAssignmentService.findMatchingJob(regularAgentInstance);
        assertThat(matchingJob, is(regularJobPlan));
        assertThat(buildAssignmentService.jobPlans().size(), is(1));
        verify(elasticAgentPluginService, never()).shouldAssignWork(any(ElasticAgentMetadata.class), any(String.class), any(ElasticProfile.class), any(JobIdentifier.class));
    }

    @Test
    public void shouldNotMatchJobsDuringMaintenanceMode() {
        when(maintenanceModeService.isMaintenanceMode()).thenReturn(true);
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
        pipeline.first().getJobs().add(JobConfigMother.jobWithNoResourceRequirement());
        pipeline.first().getJobs().add(JobConfigMother.elasticJob(elasticProfileId1));
        JobPlan elasticJobPlan = new InstanceFactory().createJobPlan(pipeline.first().getJobs().last(), schedulingContext);
        JobPlan regularJobPlan = new InstanceFactory().createJobPlan(pipeline.first().getJobs().first(), schedulingContext);
        jobPlans.add(elasticJobPlan);
        jobPlans.add(regularJobPlan);
        buildAssignmentService.onTimer();

        JobPlan matchingJob = buildAssignmentService.findMatchingJob(regularAgentInstance);
        assertNull(matchingJob);
        assertThat(buildAssignmentService.jobPlans().size(), is(0));
        verify(elasticAgentPluginService, never()).shouldAssignWork(any(ElasticAgentMetadata.class), any(String.class), any(ElasticProfile.class), any(JobIdentifier.class));
    }

    @Test
    public void shouldGetMismatchingJobPlansInCaseOfPipelineHasUpdated() {
        StageConfig second = StageConfigMother.stageConfig("second");
        second.getJobs().add(JobConfigMother.jobWithNoResourceRequirement());

        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
        pipeline.get(0).getJobs().add(JobConfigMother.jobWithNoResourceRequirement());
        pipeline.add(second);

        PipelineConfig irrelevantPipeline = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
        irrelevantPipeline.get(0).getJobs().add(JobConfigMother.jobWithNoResourceRequirement());

        JobPlan jobPlan1 = getJobPlan(pipeline.getName(), pipeline.get(0).name(), pipeline.get(0).getJobs().last());
        JobPlan jobPlan2 = getJobPlan(pipeline.getName(), pipeline.get(1).name(), pipeline.get(1).getJobs().first());
        JobPlan jobPlan3 = getJobPlan(irrelevantPipeline.getName(), irrelevantPipeline.get(0).name(), irrelevantPipeline.get(0).getJobs().first());

        //need to get hold of original jobPlans in the tests
        jobPlans = (ArrayList<JobPlan>) buildAssignmentService.jobPlans();

        jobPlans.add(jobPlan1);
        jobPlans.add(jobPlan2);
        jobPlans.add(jobPlan3);

        //delete a stage
        pipeline.remove(1);

        assertThat(jobPlans.size(), is(3));

        when(goConfigService.hasPipelineNamed(pipeline.getName())).thenReturn(true);
        buildAssignmentService.pipelineConfigChangedListener().onEntityConfigChange(pipeline);

        assertThat(jobPlans.size(), is(2));
        assertThat(jobPlans.get(0), is(jobPlan1));
        assertThat(jobPlans.get(1), is(jobPlan3));
    }

    @Test
    public void shouldGetMismatchingJobPlansInCaseOfPipelineHasBeenDeleted() {
        StageConfig second = StageConfigMother.stageConfig("second");
        second.getJobs().add(JobConfigMother.jobWithNoResourceRequirement());

        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
        pipeline.get(0).getJobs().add(JobConfigMother.jobWithNoResourceRequirement());
        pipeline.add(second);

        PipelineConfig irrelevantPipeline = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
        irrelevantPipeline.get(0).getJobs().add(JobConfigMother.jobWithNoResourceRequirement());

        JobPlan jobPlan1 = getJobPlan(pipeline.getName(), pipeline.get(0).name(), pipeline.get(0).getJobs().last());
        JobPlan jobPlan2 = getJobPlan(pipeline.getName(), pipeline.get(1).name(), pipeline.get(1).getJobs().first());
        JobPlan jobPlan3 = getJobPlan(irrelevantPipeline.getName(), irrelevantPipeline.get(0).name(), irrelevantPipeline.get(0).getJobs().first());

        //need to get hold of original jobPlans in the tests
        jobPlans = (ArrayList<JobPlan>) buildAssignmentService.jobPlans();

        jobPlans.add(jobPlan1);
        jobPlans.add(jobPlan2);
        jobPlans.add(jobPlan3);

        when(goConfigService.hasPipelineNamed(pipeline.getName())).thenReturn(false);
        buildAssignmentService.pipelineConfigChangedListener().onEntityConfigChange(pipeline);

        assertThat(jobPlans.size(), is(1));
        assertThat(jobPlans.get(0), is(jobPlan3));
    }

    private JobPlan getJobPlan(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName, JobConfig job) {
        JobPlan jobPlan = new InstanceFactory().createJobPlan(job, schedulingContext);

        jobPlan.getIdentifier().setPipelineName(pipelineName.toString());
        jobPlan.getIdentifier().setStageName(stageName.toString());
        jobPlan.getIdentifier().setBuildName(job.name().toString());

        return jobPlan;
    }
}
