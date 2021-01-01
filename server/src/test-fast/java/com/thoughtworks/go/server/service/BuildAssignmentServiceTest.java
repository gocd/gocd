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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.plugin.access.exceptions.SecretResolutionFailureException;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.server.domain.ElasticAgentMetadata;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import com.thoughtworks.go.server.messaging.JobStatusMessage;
import com.thoughtworks.go.server.messaging.JobStatusTopic;
import com.thoughtworks.go.server.service.builders.BuilderFactory;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.thoughtworks.go.helper.MaterialsMother.packageMaterial;
import static com.thoughtworks.go.helper.MaterialsMother.pluggableSCMMaterial;
import static com.thoughtworks.go.server.service.BuildAssignmentService.GO_AGENT_RESOURCES;
import static com.thoughtworks.go.server.service.BuildAssignmentService.GO_PIPELINE_GROUP_NAME;
import static com.thoughtworks.go.util.command.EnvironmentVariableContext.GO_ENVIRONMENT_NAME;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class BuildAssignmentServiceTest {

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
    @Mock
    private SecretParamResolver secretParamResolver;
    @Mock
    private JobStatusTopic jobStatusTopic;
    @Mock
    private ConsoleService consoleService;

    private BuildAssignmentService buildAssignmentService;
    private TransactionTemplate transactionTemplate;
    private SchedulingContext schedulingContext;
    private ArrayList<JobPlan> jobPlans;
    private Agent elasticAgent;
    private AgentInstance elasticAgentInstance;
    private ElasticProfile elasticProfile1;
    private ElasticProfile elasticProfile2;
    private String elasticProfileId1;
    private String elasticProfileId2;
    private AgentInstance regularAgentInstance;

    @BeforeEach
    void setUp() throws Exception {
        initMocks(this);
        transactionTemplate = dummy();
        buildAssignmentService = new BuildAssignmentService(goConfigService, jobInstanceService, scheduleService, agentService,
                environmentConfigService, transactionTemplate, scheduledPipelineLoader, pipelineService, builderFactory,
                maintenanceModeService, elasticAgentPluginService, systemEnvironment, secretParamResolver,
                jobStatusTopic, consoleService);
        elasticProfileId1 = "elastic.profile.id.1";
        elasticProfileId2 = "elastic.profile.id.2";
        elasticAgent = AgentMother.elasticAgent();
        elasticAgentInstance = AgentInstance.createFromAgent(elasticAgent, new SystemEnvironment(), null);
        regularAgentInstance = AgentInstance.createFromAgent(AgentMother.approvedAgent(), new SystemEnvironment(), null);
        elasticProfile1 = new ElasticProfile(elasticProfileId1, "prod-cluster");
        elasticProfile2 = new ElasticProfile(elasticProfileId2, "prod-cluster");
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
    void shouldMatchAnElasticJobToAnElasticAgentOnlyIfThePluginAgreesToTheAssignment() {
        PipelineConfig pipelineWithElasticJob = PipelineConfigMother.pipelineWithElasticJob(elasticProfileId1);
        JobPlan jobPlan = new InstanceFactory().createJobPlan(pipelineWithElasticJob.first().getJobs().first(), schedulingContext);
        jobPlans.add(jobPlan);
        when(elasticAgentPluginService.shouldAssignWork(elasticAgentInstance.elasticAgentMetadata(), null, jobPlan.getElasticProfile(), jobPlan.getClusterProfile(), jobPlan.getIdentifier())).thenReturn(true);
        buildAssignmentService.onTimer();

        JobPlan matchingJob = buildAssignmentService.findMatchingJob(elasticAgentInstance);
        assertThat(matchingJob).isEqualTo(jobPlan);
        assertThat(buildAssignmentService.jobPlans().size()).isEqualTo(0);
    }

    @Test
    void shouldNotMatchAnElasticJobToAnElasticAgentOnlyIfThePluginIdMatches() {
        PipelineConfig pipelineWithElasticJob = PipelineConfigMother.pipelineWithElasticJob(elasticProfileId1);
        JobPlan jobPlan1 = new InstanceFactory().createJobPlan(pipelineWithElasticJob.first().getJobs().first(), schedulingContext);
        jobPlans.add(jobPlan1);
        when(elasticAgentPluginService.shouldAssignWork(elasticAgentInstance.elasticAgentMetadata(), null, jobPlan1.getElasticProfile(), jobPlan1.getClusterProfile(), null)).thenReturn(false);
        buildAssignmentService.onTimer();

        JobPlan matchingJob = buildAssignmentService.findMatchingJob(elasticAgentInstance);
        assertThat(matchingJob).isNull();
        assertThat(buildAssignmentService.jobPlans().size()).isEqualTo(1);
    }

    @Test
    void shouldMatchAnElasticJobToAnElasticAgentOnlyIfThePluginAgreesToTheAssignmentWhenMultipleElasticJobsRequiringTheSamePluginAreScheduled() {
        PipelineConfig pipelineWith2ElasticJobs = PipelineConfigMother.pipelineWithElasticJob(elasticProfileId1, elasticProfileId2);
        JobPlan jobPlan1 = new InstanceFactory().createJobPlan(pipelineWith2ElasticJobs.first().getJobs().first(), schedulingContext);
        JobPlan jobPlan2 = new InstanceFactory().createJobPlan(pipelineWith2ElasticJobs.first().getJobs().last(), schedulingContext);
        jobPlans.add(jobPlan1);
        jobPlans.add(jobPlan2);
        when(elasticAgentPluginService.shouldAssignWork(elasticAgentInstance.elasticAgentMetadata(), null, jobPlan1.getElasticProfile(), jobPlan1.getClusterProfile(), jobPlan1.getIdentifier())).thenReturn(false);
        when(elasticAgentPluginService.shouldAssignWork(elasticAgentInstance.elasticAgentMetadata(), null, jobPlan2.getElasticProfile(), jobPlan2.getClusterProfile(), jobPlan2.getIdentifier())).thenReturn(true);
        buildAssignmentService.onTimer();

        JobPlan matchingJob = buildAssignmentService.findMatchingJob(elasticAgentInstance);
        assertThat(matchingJob).isEqualTo(jobPlan2);
        assertThat(buildAssignmentService.jobPlans().size()).isEqualTo(1);
    }

    @Test
    void shouldMatchNonElasticJobToNonElasticAgentIfResourcesMatch() {
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
        pipeline.first().getJobs().add(JobConfigMother.jobWithNoResourceRequirement());
        pipeline.first().getJobs().add(JobConfigMother.elasticJob(elasticProfileId1));
        JobPlan elasticJobPlan = new InstanceFactory().createJobPlan(pipeline.first().getJobs().last(), schedulingContext);
        JobPlan regularJobPlan = new InstanceFactory().createJobPlan(pipeline.first().getJobs().first(), schedulingContext);
        jobPlans.add(elasticJobPlan);
        jobPlans.add(regularJobPlan);
        buildAssignmentService.onTimer();

        JobPlan matchingJob = buildAssignmentService.findMatchingJob(regularAgentInstance);
        assertThat(matchingJob).isEqualTo(regularJobPlan);
        assertThat(buildAssignmentService.jobPlans().size()).isEqualTo(1);
        verify(elasticAgentPluginService, never()).shouldAssignWork(any(ElasticAgentMetadata.class), any(String.class), any(ElasticProfile.class), any(ClusterProfile.class), any(JobIdentifier.class));
    }

    @Test
    void shouldNotMatchJobsDuringMaintenanceMode() {
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
        assertThat(matchingJob).isNull();
        assertThat(buildAssignmentService.jobPlans().size()).isEqualTo(0);
        verify(elasticAgentPluginService, never()).shouldAssignWork(any(ElasticAgentMetadata.class), any(String.class), any(ElasticProfile.class), any(ClusterProfile.class), any(JobIdentifier.class));
    }

    @Test
    void shouldGetMismatchingJobPlansInCaseOfPipelineHasUpdated() {
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

        assertThat(jobPlans.size()).isEqualTo(3);

        when(goConfigService.hasPipelineNamed(pipeline.getName())).thenReturn(true);
        buildAssignmentService.pipelineConfigChangedListener().onEntityConfigChange(pipeline);

        assertThat(jobPlans.size()).isEqualTo(2);
        assertThat(jobPlans.get(0)).isEqualTo(jobPlan1);
        assertThat(jobPlans.get(1)).isEqualTo(jobPlan3);
    }

    @Test
    void shouldGetMismatchingJobPlansInCaseOfPipelineHasBeenDeleted() {
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

        assertThat(jobPlans.size()).isEqualTo(1);
        assertThat(jobPlans.get(0)).isEqualTo(jobPlan3);
    }

    @Nested
    class AssignWorkToAgent {
        @Test
        void shouldResolveSecretParamsFromEnvironmentConfig() {
            BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig();
            environmentConfig.addEnvironmentVariable("GIT_USERNAME", "bob");
            environmentConfig.addEnvironmentVariable("GIT_TOKEN", "{{SECRET:[secret_config_id][GIT_TOKEN]}}");

            final PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
            pipelineConfig.get(0).getJobs().add(JobConfigMother.jobWithNoResourceRequirement());

            final AgentInstance agentInstance = mock(AgentInstance.class);
            when(agentInstance.getResourceConfigs()).thenReturn(mock(ResourceConfigs.class));
            agentInstance.getResourceConfigs().add(new ResourceConfig("resource-1"));
            agentInstance.getResourceConfigs().add(new ResourceConfig("resource-2"));

            final Pipeline pipeline = mock(Pipeline.class);
            final JobPlan jobPlan1 = getJobPlan(pipelineConfig.getName(), pipelineConfig.get(0).name(), pipelineConfig.get(0).getJobs().last());

            when(agentInstance.isRegistered()).thenReturn(true);
            when(agentInstance.getAgent()).thenReturn(mock(Agent.class));
            when(agentInstance.firstMatching(anyList())).thenReturn(jobPlan1);
            when(pipeline.getBuildCause()).thenReturn(BuildCause.createNeverRun());
            when(environmentConfigService.filterJobsByAgent(any(), any())).thenReturn(singletonList(jobPlan1));
            when(scheduledPipelineLoader.pipelineWithPasswordAwareBuildCauseByBuildId(anyLong())).thenReturn(pipeline);
            when(scheduleService.updateAssignedInfo(anyString(), any())).thenReturn(false);
            when(goConfigService.artifactStores()).thenReturn(new ArtifactStores());
            when(environmentConfigService.environmentForPipeline(anyString())).thenReturn(environmentConfig);
            doAnswer(invocation -> {
                BasicEnvironmentConfig config = invocation.getArgument(0);
                config.getSecretParams().findFirst("GIT_TOKEN").ifPresent(param -> param.setValue("some-token"));
                return config;
            }).when(secretParamResolver).resolve(environmentConfig);

            BuildWork work = (BuildWork) buildAssignmentService.assignWorkToAgent(agentInstance);
            EnvironmentVariableContext environmentVariableContext = work.getAssignment().initialEnvironmentVariableContext();
            assertThat(environmentVariableContext.getProperty("GIT_USERNAME")).isEqualTo("bob");
            assertThat(environmentVariableContext.getProperty("GIT_TOKEN")).isEqualTo("some-token");
            assertThat(environmentVariableContext.getProperty(GO_AGENT_RESOURCES)).isEqualTo(agentInstance.getResourceConfigs().getCommaSeparatedResourceNames());
            assertThat(environmentVariableContext.getSecureEnvironmentVariables())
                    .contains(new EnvironmentVariableContext.EnvironmentVariable("GIT_TOKEN", "some-token"));
        }

        @Test
        void shouldResolveSecretParamsInMaterials() {
            final GitMaterial gitMaterial = MaterialsMother.gitMaterial("http://foo.com");
            gitMaterial.setUserName("bob");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][GIT_PASSWORD]}}");
            final Modification modification = new Modification("user", null, null, null, "rev1");
            final MaterialRevisions materialRevisions = new MaterialRevisions(new MaterialRevision(gitMaterial, modification));

            final PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
            pipelineConfig.get(0).getJobs().add(JobConfigMother.jobWithNoResourceRequirement());

            final AgentInstance agentInstance = mock(AgentInstance.class);

            final Pipeline pipeline = mock(Pipeline.class);
            final JobPlan jobPlan1 = getJobPlan(pipelineConfig.getName(), pipelineConfig.get(0).name(), pipelineConfig.get(0).getJobs().last());

            when(agentInstance.isRegistered()).thenReturn(true);
            when(agentInstance.getAgent()).thenReturn(mock(Agent.class));
            when(agentInstance.firstMatching(anyList())).thenReturn(jobPlan1);
            when(pipeline.getBuildCause()).thenReturn(BuildCause.createWithModifications(materialRevisions, "bob"));
            when(environmentConfigService.filterJobsByAgent(any(), any())).thenReturn(singletonList(jobPlan1));
            when(scheduledPipelineLoader.pipelineWithPasswordAwareBuildCauseByBuildId(anyLong())).thenReturn(pipeline);
            when(scheduleService.updateAssignedInfo(anyString(), any())).thenReturn(false);
            when(goConfigService.artifactStores()).thenReturn(new ArtifactStores());
            doAnswer(invocation -> {
                BuildAssignment assignment = invocation.getArgument(0);
                assignment.getSecretParams().findFirst("GIT_PASSWORD").ifPresent(param -> param.setValue("some-password"));
                return assignment;
            }).when(secretParamResolver).resolve(any(BuildAssignment.class));

            BuildWork work = (BuildWork) buildAssignmentService.assignWorkToAgent(agentInstance);

            assertThat(gitMaterial.hasSecretParams()).isTrue();
            verify(scheduleService, never()).failJob(any(JobInstance.class));
            verifyZeroInteractions(consoleService);
            verifyZeroInteractions(jobStatusTopic);
            ScmMaterial material = (ScmMaterial) work.getAssignment().materialRevisions().getMaterialRevision(0).getMaterial();
            assertThat(material.passwordForCommandLine()).isEqualTo("some-password");
            assertThat(work.getAssignment().initialEnvironmentVariableContext().hasProperty(GO_AGENT_RESOURCES)).isFalse();
        }

        @Test
        void shouldFailJobIfSecretsResolutionFails() throws Exception {
            final MaterialRevisions materialRevisions = new MaterialRevisions();
            final PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
            pipelineConfig.get(0).getJobs().add(JobConfigMother.jobWithNoResourceRequirement());
            final AgentInstance agentInstance = mock(AgentInstance.class);
            final Pipeline pipeline = mock(Pipeline.class);
            final JobPlan jobPlan1 = getJobPlan(pipelineConfig.getName(), pipelineConfig.get(0).name(), pipelineConfig.get(0).getJobs().last());
            JobInstance jobInstance = mock(JobInstance.class);

            when(agentInstance.isRegistered()).thenReturn(true);
            when(agentInstance.firstMatching(anyList())).thenReturn(jobPlan1);
            when(pipeline.getBuildCause()).thenReturn(BuildCause.createWithModifications(materialRevisions, "bob"));
            when(scheduledPipelineLoader.pipelineWithPasswordAwareBuildCauseByBuildId(anyLong())).thenReturn(pipeline);
            when(goConfigService.artifactStores()).thenReturn(new ArtifactStores());
            when(jobInstanceService.buildById(jobPlan1.getJobId())).thenReturn(jobInstance);
            when(agentInstance.getUuid()).thenReturn("agent_uuid");
            when(jobInstance.getState()).thenReturn(JobState.Completed);
            doThrow(new SecretResolutionFailureException("Failed resolving params for keys: 'key1'"))
                    .when(secretParamResolver).resolve(any(BuildAssignment.class));

            assertThatCode(() -> buildAssignmentService.assignWorkToAgent(agentInstance))
                    .isInstanceOf(SecretResolutionFailureException.class);

            InOrder inOrder = inOrder(scheduleService, jobStatusTopic, consoleService);
            inOrder.verify(consoleService, times(2)).appendToConsoleLog(eq(jobPlan1.getIdentifier()), anyString());
            inOrder.verify(scheduleService).failJob(jobInstance);
            inOrder.verify(jobStatusTopic).post(new JobStatusMessage(jobPlan1.getIdentifier(), JobState.Completed, "agent_uuid"));
        }

        @Test
        void shouldFailJobIfEnvironmentVariableInEnvironmentConfigCanNotReferToSecretConfig() throws Exception {
            final PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
            pipelineConfig.get(0).getJobs().add(JobConfigMother.jobWithNoResourceRequirement());
            final AgentInstance agentInstance = mock(AgentInstance.class);
            final Pipeline pipeline = mock(Pipeline.class);
            final JobPlan jobPlan1 = getJobPlan(pipelineConfig.getName(), pipelineConfig.get(0).name(), pipelineConfig.get(0).getJobs().last());
            JobInstance jobInstance = mock(JobInstance.class);

            when(jobInstance.getState()).thenReturn(JobState.Completed);
            when(agentInstance.isRegistered()).thenReturn(true);
            when(agentInstance.getUuid()).thenReturn("agent_uuid");
            when(agentInstance.getAgent()).thenReturn(mock(Agent.class));
            when(agentInstance.firstMatching(anyList())).thenReturn(jobPlan1);
            when(pipeline.getBuildCause()).thenReturn(BuildCause.createNeverRun());
            when(environmentConfigService.filterJobsByAgent(any(), any())).thenReturn(singletonList(jobPlan1));
            when(scheduledPipelineLoader.pipelineWithPasswordAwareBuildCauseByBuildId(anyLong())).thenReturn(pipeline);
            when(scheduleService.updateAssignedInfo(anyString(), any())).thenReturn(false);
            when(goConfigService.artifactStores()).thenReturn(new ArtifactStores());
            when(environmentConfigService.environmentForPipeline(anyString())).thenReturn(new BasicEnvironmentConfig());
            when(jobInstanceService.buildById(anyLong())).thenReturn(jobInstance);
            doThrow(new RulesViolationException("Failed resolving params for keys: 'key1'"))
                    .when(secretParamResolver).resolve(any(EnvironmentConfig.class));

            assertThatCode(() -> buildAssignmentService.assignWorkToAgent(agentInstance))
                    .isInstanceOf(RulesViolationException.class);

            InOrder inOrder = inOrder(scheduleService, jobStatusTopic, consoleService);
            inOrder.verify(consoleService, times(1)).appendToConsoleLog(eq(jobPlan1.getIdentifier()), anyString());
            inOrder.verify(scheduleService).failJob(jobInstance);
            inOrder.verify(jobStatusTopic).post(new JobStatusMessage(jobPlan1.getIdentifier(), JobState.Completed, "agent_uuid"));
        }

        @Test
        void shouldResolveSecretsInPluggableScmMaterialAndPackageMaterialBeforeCreatingAssignment() {
            final GitMaterial gitMaterial = MaterialsMother.gitMaterial("http://foo.com");
            gitMaterial.setUserName("bob");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][GIT_PASSWORD]}}");
            PluggableSCMMaterial pluggableSCMMaterial = pluggableSCMMaterial();
            pluggableSCMMaterial.getScmConfig().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][SCM_PASSWORD]}}"));
            PackageMaterial packageMaterial = packageMaterial();
            packageMaterial.getPackageDefinition().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][PKG_PASSWORD]}}"));
            final Modification modification = new Modification("user", null, null, null, "rev1");
            final MaterialRevisions materialRevisions = new MaterialRevisions(new MaterialRevision(gitMaterial, modification));
            materialRevisions.addRevision(new MaterialRevision(pluggableSCMMaterial, new Modification("user2", null, null, null, "rev")));
            materialRevisions.addRevision(new MaterialRevision(packageMaterial, new Modification("user3", null, null, null, "rev-pkg")));

            final PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
            pipelineConfig.get(0).getJobs().add(JobConfigMother.jobWithNoResourceRequirement());

            final AgentInstance agentInstance = mock(AgentInstance.class);

            final Pipeline pipeline = mock(Pipeline.class);
            final JobPlan jobPlan1 = getJobPlan(pipelineConfig.getName(), pipelineConfig.get(0).name(), pipelineConfig.get(0).getJobs().last());

            when(agentInstance.isRegistered()).thenReturn(true);
            when(agentInstance.getAgent()).thenReturn(mock(Agent.class));
            when(agentInstance.firstMatching(anyList())).thenReturn(jobPlan1);
            when(pipeline.getBuildCause()).thenReturn(BuildCause.createWithModifications(materialRevisions, "bob"));
            when(environmentConfigService.filterJobsByAgent(any(), any())).thenReturn(singletonList(jobPlan1));
            when(scheduledPipelineLoader.pipelineWithPasswordAwareBuildCauseByBuildId(anyLong())).thenReturn(pipeline);
            when(scheduleService.updateAssignedInfo(anyString(), any())).thenReturn(false);
            when(goConfigService.artifactStores()).thenReturn(new ArtifactStores());
            doAnswer(invocation -> {
                BuildAssignment assignment = invocation.getArgument(0);
                assignment.getSecretParams().findFirst("GIT_PASSWORD").ifPresent(param -> param.setValue("some-password"));
                return assignment;
            }).when(secretParamResolver).resolve(any(BuildAssignment.class));
            doAnswer(invocation -> {
                List<Material> materials = invocation.getArgument(0);
                ((PluggableSCMMaterial) materials.get(0)).getScmConfig().getConfiguration().get(0).getSecretParams().get(0).setValue("some-scm-password");
                ((PackageMaterial) materials.get(1)).getPackageDefinition().getConfiguration().get(0).getSecretParams().get(0).setValue("some-pkg-password");
                return materials;
            }).when(secretParamResolver).resolve(anyList());

            InOrder inOrder = inOrder(goConfigService, secretParamResolver);

            BuildWork work = (BuildWork) buildAssignmentService.assignWorkToAgent(agentInstance);

            inOrder.verify(secretParamResolver).resolve(asList(pluggableSCMMaterial, packageMaterial));
            inOrder.verify(goConfigService).artifactStores();
            inOrder.verify(secretParamResolver).resolve(any(BuildAssignment.class));

            assertThat(gitMaterial.hasSecretParams()).isTrue();
            ScmMaterial material = (ScmMaterial) work.getAssignment().materialRevisions().getMaterialRevision(0).getMaterial();
            assertThat(material.passwordForCommandLine()).isEqualTo("some-password");
            PluggableSCMMaterial material1 = (PluggableSCMMaterial) work.getAssignment().materialRevisions().getMaterialRevision(1).getMaterial();
            assertThat(material1.getScmConfig().getConfiguration().get(0).getResolvedValue()).isEqualTo("some-scm-password");
            PackageMaterial material2 = (PackageMaterial) work.getAssignment().materialRevisions().getMaterialRevision(2).getMaterial();
            assertThat(material2.getPackageDefinition().getConfiguration().get(0).getResolvedValue()).isEqualTo("some-pkg-password");
        }
    }

    @Test
    void shouldGetEnvironmentVariableContextIncludingGO_ENVIRONMENT_NAMEVariable() {
        String pipelineName = "pipeline1";
        String environmentName = "uat_environment";

        when(environmentConfigService.environmentForPipeline(pipelineName)).thenReturn(new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName)));
        EnvironmentVariableContext context = buildAssignmentService.buildEnvVarContext(pipelineName);

        assertThat(context.getProperties().size()).isEqualTo(2);
        assertThat(context.getProperty(GO_ENVIRONMENT_NAME)).isEqualTo(environmentName);
    }

    @Test
    void shouldSetEnvironmentVariableContextIncludingGO_PIPELINE_GROUP_NAMEVariable() {
        String pipelineName = "pipeline1";
        String pipelineGroupName = "pipeline-group1";
        String environmentName = "uat_environment";

        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName))).thenReturn(pipelineGroupName);
        when(environmentConfigService.environmentForPipeline(pipelineName)).thenReturn(new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName)));
        EnvironmentVariableContext context = buildAssignmentService.buildEnvVarContext(pipelineName);

        assertThat(context.getProperties().size()).isEqualTo(2);
        assertThat(context.getProperty(GO_PIPELINE_GROUP_NAME)).isEqualTo(pipelineGroupName);
    }

    @Test
    void shouldNotSetEnvPropertyWhenNoEnvironmentBelongingToSpecifiedPipelineExists() {
        String pipelineName = "pipeline1";

        when(environmentConfigService.environmentForPipeline(pipelineName)).thenReturn(null);
        EnvironmentVariableContext context = buildAssignmentService.buildEnvVarContext(pipelineName);

        assertThat(context.getProperties().size()).isEqualTo(1);
        assertThat(context.getProperty(GO_ENVIRONMENT_NAME)).isNullOrEmpty();
    }

    @Test
    void shouldFailTheJobWhenRulesViolationErrorOccursForElasticConfiguration() throws IOException, IllegalArtifactLocationException {
        PipelineConfig pipelineWithElasticJob = PipelineConfigMother.pipelineWithElasticJob(elasticProfileId1);
        JobPlan jobPlan = new InstanceFactory().createJobPlan(pipelineWithElasticJob.first().getJobs().first(), schedulingContext);
        jobPlans.add(jobPlan);
        JobInstance jobInstance = mock(JobInstance.class);

        doThrow(new RulesViolationException("some rules related violation message"))
                .when(elasticAgentPluginService).shouldAssignWork(elasticAgentInstance.elasticAgentMetadata(), null, jobPlan.getElasticProfile(), jobPlan.getClusterProfile(), jobPlan.getIdentifier());
        when(jobInstance.getState()).thenReturn(JobState.Scheduled);
        when(jobInstanceService.buildById(anyLong())).thenReturn(jobInstance);

        buildAssignmentService.onTimer();

        assertThatCode(() -> {
            JobPlan matchingJob = buildAssignmentService.findMatchingJob(elasticAgentInstance);

            assertThat(matchingJob).isNull();
            assertThat(buildAssignmentService.jobPlans()).containsExactly(jobPlan);
        }).doesNotThrowAnyException();

        InOrder inOrder = inOrder(jobInstanceService, scheduleService, consoleService, jobStatusTopic);
        inOrder.verify(jobInstanceService).buildById(jobPlan.getJobId());
        inOrder.verify(consoleService).appendToConsoleLog(jobPlan.getIdentifier(), "\nThis job was failed by GoCD. This job is configured to run on an elastic agent, there were errors while resolving secrets for the the associated elastic configurations.\nReasons: some rules related violation message");
        inOrder.verify(scheduleService).failJob(jobInstance);
        inOrder.verify(jobStatusTopic).post(new JobStatusMessage(jobPlan.getIdentifier(), JobState.Scheduled, elasticAgentInstance.getUuid()));
    }

    @Test
    void shouldFailTheJobWhenSecretResolutionErrorOccursForElasticConfiguration() throws IOException, IllegalArtifactLocationException {
        PipelineConfig pipelineWithElasticJob = PipelineConfigMother.pipelineWithElasticJob(elasticProfileId1);
        JobPlan jobPlan = new InstanceFactory().createJobPlan(pipelineWithElasticJob.first().getJobs().first(), schedulingContext);
        jobPlans.add(jobPlan);
        JobInstance jobInstance = mock(JobInstance.class);

        doThrow(new SecretResolutionFailureException("some secret resolution related failure message"))
                .when(elasticAgentPluginService).shouldAssignWork(elasticAgentInstance.elasticAgentMetadata(), null, jobPlan.getElasticProfile(), jobPlan.getClusterProfile(), jobPlan.getIdentifier());
        when(jobInstance.getState()).thenReturn(JobState.Scheduled);
        when(jobInstanceService.buildById(anyLong())).thenReturn(jobInstance);

        buildAssignmentService.onTimer();

        assertThatCode(() -> {
            JobPlan matchingJob = buildAssignmentService.findMatchingJob(elasticAgentInstance);

            assertThat(matchingJob).isNull();
            assertThat(buildAssignmentService.jobPlans()).containsExactly(jobPlan);
        }).doesNotThrowAnyException();

        InOrder inOrder = inOrder(jobInstanceService, scheduleService, consoleService, jobStatusTopic);
        inOrder.verify(jobInstanceService).buildById(jobPlan.getJobId());
        inOrder.verify(consoleService).appendToConsoleLog(jobPlan.getIdentifier(), "\nThis job was failed by GoCD. This job is configured to run on an elastic agent, there were errors while resolving secrets for the the associated elastic configurations.\nReasons: some secret resolution related failure message");
        inOrder.verify(scheduleService).failJob(jobInstance);
        inOrder.verify(jobStatusTopic).post(new JobStatusMessage(jobPlan.getIdentifier(), JobState.Scheduled, elasticAgentInstance.getUuid()));
    }

    private JobPlan getJobPlan(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName, JobConfig job) {
        JobPlan jobPlan = new InstanceFactory().createJobPlan(job, schedulingContext);

        jobPlan.getIdentifier().setPipelineName(pipelineName.toString());
        jobPlan.getIdentifier().setStageName(stageName.toString());
        jobPlan.getIdentifier().setBuildName(job.name().toString());
        jobPlan.getIdentifier().setPipelineCounter(1);

        return jobPlan;
    }

    private TransactionTemplate dummy() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        return new TransactionTemplate(new org.springframework.transaction.support.TransactionTemplate(transactionManager));
    }
}
