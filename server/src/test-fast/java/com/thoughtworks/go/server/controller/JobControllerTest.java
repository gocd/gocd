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
package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.policy.SupportedAction;
import com.thoughtworks.go.config.policy.SupportedEntity;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.dto.DurationBean;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.JobAgentMetadataDao;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.util.JsonValue;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.thoughtworks.go.util.JsonUtils.from;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.*;

public class JobControllerTest {

    private JobController jobController;
    private JobInstanceService jobInstanceService;
    private JobInstanceDao jobInstanceDao;
    private GoConfigService jobConfigService;
    private AgentService agentService;
    private StageService stageService;
    private PipelineService pipelineService;
    private MockHttpServletResponse response;
    private SystemEnvironment systemEnvironment;
    private RestfulService restfulService;
    private JobAgentMetadataDao jobAgentMetadataDao;
    private ElasticAgentMetadataStore elasticAgentMetadataStore = ElasticAgentMetadataStore.instance();
    private SecurityService securityService;

    @BeforeEach
    public void setUp() throws Exception {
        jobInstanceService = mock(JobInstanceService.class);
        jobInstanceDao = mock(JobInstanceDao.class);
        jobConfigService = mock(GoConfigService.class);
        agentService = mock(AgentService.class);
        stageService = mock(StageService.class);
        response = new MockHttpServletResponse();
        systemEnvironment = mock(SystemEnvironment.class);
        pipelineService = mock(PipelineService.class);
        restfulService = mock(RestfulService.class);
        jobAgentMetadataDao = mock(JobAgentMetadataDao.class);
        securityService = mock(SecurityService.class);
        jobController = new JobController(jobInstanceService, agentService, jobInstanceDao, jobConfigService,
                pipelineService, restfulService, null, stageService, jobAgentMetadataDao, systemEnvironment, securityService);
    }

    @Test
    void shouldFindTheLatestJobWhenJobStatusIsRequested() {
        JobInstance job = JobInstanceMother.buildEndingWithState(JobState.Rescheduled, JobResult.Unknown, "config");
        job.assign("agent", new Date());

        JobInstance newJob = JobInstanceMother.buildEndingWithState(JobState.Building, JobResult.Unknown, "another_config");
        newJob.setId(2);
        newJob.assign("another_agent", new Date());


        String pipelineName = job.getPipelineName();
        String stageName = job.getStageName();


        when(jobInstanceService.buildByIdWithTransitions(job.getId())).thenReturn(job);
        when(jobInstanceDao.mostRecentJobWithTransitions(job.getIdentifier())).thenReturn(newJob);
        when(agentService.findAgentByUUID(newJob.getAgentUuid())).thenReturn(Agent.blankAgent(newJob.getAgentUuid()));
        when(stageService.getBuildDuration(pipelineName, stageName, newJob)).thenReturn(new DurationBean(newJob.getId(), 5l));

        ModelAndView modelAndView = jobController.handleRequest(pipelineName, stageName, job.getId(), response);

        verify(jobInstanceService).buildByIdWithTransitions(job.getId());
        verify(agentService).findAgentByUUID(newJob.getAgentUuid());
        verify(jobInstanceDao).mostRecentJobWithTransitions(job.getIdentifier());
        verify(stageService).getBuildDuration(pipelineName, stageName, newJob);

        JsonValue json = from(((List) modelAndView.getModel().get("json")).get(0));

        JsonValue buildingInfo = json.getObject("building_info");

        assertThat(buildingInfo.getString("id")).isEqualTo("2");
        assertThat(buildingInfo.getString("last_build_duration")).isEqualTo("5");
    }

    @Nested
    class JobDetail {
        @BeforeEach
        void setupMocksForJobDetail() {
            Pipeline pipeline = PipelineMother.passedPipelineInstance("p1", "s1", "build");
            JobIdentifier jobIdentifier = JobIdentifierMother.jobIdentifier("p1");
            StageIdentifier stageIdentifier = new StageIdentifier("p1", 1, "s1", "1");
            JobInstance jobInstance = JobInstanceMother.jobInstance("building", "one");
            jobInstance.setIdentifier(jobIdentifier);
            jobInstance.setId(12);
            jobInstance.setState(JobState.Building);

            when(jobInstanceService.latestCompletedJobs("p1", "s1", jobInstance.getName())).thenReturn(new JobInstances());
            when(pipelineService.wrapBuildDetails(jobInstance)).thenReturn(pipeline);
            when(pipelineService.resolvePipelineCounter(eq("p1"), anyString())).thenReturn(Optional.of(1));
            when(restfulService.translateStageCounter(any(PipelineIdentifier.class), eq("s1"), anyString())).thenReturn(stageIdentifier);
            when(pipelineService.findPipelineByNameAndCounter("p1", 1)).thenReturn(pipeline);
            when(jobInstanceDao.mostRecentJobWithTransitions(jobIdentifier)).thenReturn(jobInstance);
            when(jobInstanceDao.mostRecentJobWithTransitions(any(JobIdentifier.class))).thenReturn(jobInstance);
            when(jobConfigService.pipelineConfigNamed(any(CaseInsensitiveString.class))).thenReturn(PipelineConfigMother.pipelineConfig("p1"));
            when(stageService.getStageByBuild(jobInstance)).thenReturn(StageMother.passedStageInstance("s1", "plan1", "p1"));

            FeatureToggleService featureToggleService = mock(FeatureToggleService.class);
            when(featureToggleService.isToggleOn(anyString())).thenReturn(true);
            Toggles.initializeWith(featureToggleService);
        }

        @AfterEach
        void tearDown() {
            elasticAgentMetadataStore.clear();
        }

        @Test
        void shouldReturnJobDetail() throws Exception {
            ModelAndView modelAndView = jobController.jobDetail("p1", "1", "s1", "2",
                    "job1");
            assertThat(modelAndView.getModel().isEmpty()).isFalse();
            assertThat(modelAndView.getModel().get("useIframeSandbox")).isEqualTo(false);
            assertThat(modelAndView.getModel().get("websocketEnabled")).isEqualTo(true);
            assertThat(modelAndView.getModel().get("isEditableViaUI")).isEqualTo(false);
            assertThat(modelAndView.getModel().get("isAgentAlive")).isEqualTo(false);
        }

        @Test
        void shouldThrowErrorIfUserPassesANonNumericValueForPipelineCounter() {
            try {
                when(pipelineService.resolvePipelineCounter("p1", "some-string")).thenReturn(Optional.empty());
                jobController.jobDetail("p1", "some-string", "s1", "1", "job");
                fail("Expected an exception to be thrown");
            } catch (Exception e) {
                assertThat(e.getMessage()).isEqualTo("Expected numeric pipelineCounter or latest keyword, but received 'some-string' for [p1/some-string/s1/1/job]");
            }
        }

        @Test
        void shouldThrowErrorIfUserPassesANonNumericValueForStageCounter() {
            try {
                when(pipelineService.resolvePipelineCounter("p1", "1")).thenReturn(Optional.of(1));
                jobController.jobDetail("p1", "1", "s1", "some-string", "job");
                fail("Expected an exception to be thrown");
            } catch (Exception e) {
                assertThat(e.getMessage()).isEqualTo("Expected numeric stageCounter or latest keyword, but received 'some-string' for [p1/1/s1/some-string/job]");
            }
        }

        @Test
        void shouldAcceptLatestAsPipelineCounter() throws Exception {
            ModelAndView modelAndView = jobController.jobDetail("p1", "latest", "s1", "12", "job1");
            assertThat(modelAndView.getModel().isEmpty()).isFalse();
        }

        @Test
        void shouldAcceptLatestAsStageCounter() throws Exception {
            ModelAndView modelAndView = jobController.jobDetail("p1", "1", "s1", "latest", "job1");
            assertThat(modelAndView.getModel().isEmpty()).isFalse();
        }

        @Test
        void shouldConsistElasticAgentInformationForAJobRunningOnAnElasticAgent() throws Exception {
            final GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("cd.go.example.plugin").build();
            elasticAgentMetadataStore.setPluginInfo(new ElasticAgentPluginInfo(descriptor, null,
                    null, null, null, new Capabilities(true, true)));
            ElasticProfile elasticProfile = new ElasticProfile("elastic_id", "cluster_id");
            ClusterProfile clusterProfile = new ClusterProfile("cluster_id", "cd.go.example.plugin");

            when(securityService.doesUserHasPermissions(any(), eq(SupportedAction.VIEW), eq(SupportedEntity.ELASTIC_AGENT_PROFILE), eq("elastic_id"), eq("cluster_id"))).thenReturn(true);
            when(jobAgentMetadataDao.load(12L)).thenReturn(new JobAgentMetadata(12L, elasticProfile, clusterProfile));

            ModelAndView modelAndView = jobController.jobDetail("p1", "1", "s1", "2", "job1");

            assertThat(modelAndView.getModel().get("clusterProfileId")).isEqualTo("cluster_id");
            assertThat(modelAndView.getModel().get("elasticAgentProfileId")).isEqualTo("elastic_id");
            assertThat(modelAndView.getModel().get("doesUserHaveViewAccessToStatusReportPage")).isEqualTo(true);
            assertThat(modelAndView.getModel().get("elasticAgentPluginId")).isEqualTo("cd.go.example.plugin");
        }

        @Test
        void shouldNotHaveElasticAgentInformationIfJobMetadataDoesNotHaveClusterInformation() throws Exception {
            JobAgentMetadata jobAgentMetadata = mock(JobAgentMetadata.class);
            final GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("cd.go.example.plugin").build();
            elasticAgentMetadataStore.setPluginInfo(new ElasticAgentPluginInfo(descriptor, null,
                    null, null, null, new Capabilities(true, true)));

            when(jobAgentMetadataDao.load(12L)).thenReturn(jobAgentMetadata);
            when(jobAgentMetadata.clusterProfile()).thenReturn(null);

            ModelAndView modelAndView = jobController.jobDetail("p1", "1", "s1", "2", "job1");

            assertThat(modelAndView.getModel().get("elasticAgentPluginId")).isNull();
        }
    }
}
