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

package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.JobIdentifierMother;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.dao.JobInstanceSqlMapDao;
import com.thoughtworks.go.server.service.ElasticAgentPluginService;
import com.thoughtworks.go.server.service.StageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class JobStatusListenerTest {
    @Mock
    private JobInstanceSqlMapDao jobInstanceSqlMapDao;
    @Mock
    private StageService stageService;
    @Mock
    private StageStatusTopic stageStatusTopic;
    @Mock
    private ElasticAgentPluginService elasticAgentPluginService;
    @Mock
    private JobStatusTopic jobStatusTopic;

    private JobStatusListener jobStatusListener;

    @BeforeEach
    void setUp() {
        initMocks(this);
        jobStatusListener = new JobStatusListener(jobStatusTopic, stageService, stageStatusTopic, elasticAgentPluginService, jobInstanceSqlMapDao);
    }

    @Test
    void shouldAddAListenerOnJobStatusTopic() {
        jobStatusListener.init();

        verify(jobStatusTopic, times(1)).addListener(jobStatusListener);
    }

    @Test
    void shouldDeleteJobPlanAssociatedEntitiesOnJobCompletion() {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();
        JobInstance jobInstance = JobInstanceMother.completed(jobIdentifier.getBuildName());
        Stage stage = StageMother.passedStageInstance(jobIdentifier.getStageName(), jobIdentifier.getBuildName(), jobIdentifier.getPipelineName());
        stage.setJobInstances(new JobInstances(jobInstance));
        JobStatusMessage jobStatusMessage = new JobStatusMessage(jobIdentifier, JobState.Completed, "agent1");
        when(stageService.findStageWithIdentifier(jobStatusMessage.getStageIdentifier())).thenReturn(stage);

        jobStatusListener.onMessage(jobStatusMessage);

        verify(jobInstanceSqlMapDao, times(1)).deleteJobPlanAssociatedEntities(jobInstance);
    }

    @Test
    void shouldNotDeleteJobPlanAssociatedEntitiesWhenJobIsStillInProgress() {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();
        JobInstance jobInstance = JobInstanceMother.completed(jobIdentifier.getBuildName());
        Stage stage = StageMother.passedStageInstance(jobIdentifier.getStageName(), jobIdentifier.getBuildName(), jobIdentifier.getPipelineName());
        stage.setJobInstances(new JobInstances(jobInstance));
        JobStatusMessage jobStatusMessage = new JobStatusMessage(jobIdentifier, JobState.Building, "agent1");
        when(stageService.findStageWithIdentifier(jobStatusMessage.getStageIdentifier())).thenReturn(stage);

        jobStatusListener.onMessage(jobStatusMessage);

        verifyZeroInteractions(jobInstanceSqlMapDao);
    }

    @Test
    void shouldPostAStageStatusChangeMessageWhenStageIsCompletedBecauseOfJobCompletion() {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();
        JobInstance jobInstance = JobInstanceMother.completed(jobIdentifier.getBuildName());
        Stage stage = StageMother.passedStageInstance(jobIdentifier.getStageName(), jobIdentifier.getBuildName(), jobIdentifier.getPipelineName());
        stage.setJobInstances(new JobInstances(jobInstance));
        JobStatusMessage jobStatusMessage = new JobStatusMessage(jobIdentifier, JobState.Completed, "agent1");
        when(stageService.findStageWithIdentifier(jobStatusMessage.getStageIdentifier())).thenReturn(stage);

        jobStatusListener.onMessage(jobStatusMessage);

        verify(stageStatusTopic, times(1)).post(any());
    }

    @Test
    void shouldNotPostAStageStatusChangeMessageWhenJobIsInProgress() {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();
        JobInstance jobInstance = JobInstanceMother.completed(jobIdentifier.getBuildName());
        Stage stage = StageMother.passedStageInstance(jobIdentifier.getStageName(), jobIdentifier.getBuildName(), jobIdentifier.getPipelineName());
        stage.setJobInstances(new JobInstances(jobInstance));
        JobStatusMessage jobStatusMessage = new JobStatusMessage(jobIdentifier, JobState.Building, "agent1");
        when(stageService.findStageWithIdentifier(jobStatusMessage.getStageIdentifier())).thenReturn(stage);

        jobStatusListener.onMessage(jobStatusMessage);

        verifyZeroInteractions(stageStatusTopic);
    }

    @Test
    void shouldNotPostAStageStatusChangeMessageWhenStageIsRunningAndJobIsCompleted() {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();
        JobInstance jobInstance = JobInstanceMother.completed(jobIdentifier.getBuildName());
        Stage stage = StageMother.withOneScheduledBuild(jobIdentifier.getStageName(), jobIdentifier.getBuildName(), "running_job", 100);
        stage.getJobInstances().add(jobInstance);
        JobStatusMessage jobStatusMessage = new JobStatusMessage(jobIdentifier, JobState.Completed, "agent1");
        when(stageService.findStageWithIdentifier(jobStatusMessage.getStageIdentifier())).thenReturn(stage);

        jobStatusListener.onMessage(jobStatusMessage);

        verifyZeroInteractions(stageStatusTopic);
    }

    @Test
    void shouldPostAMessageToElasticAgentPluginServiceOnJobCompletion() {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();
        JobInstance jobInstance = JobInstanceMother.completed(jobIdentifier.getBuildName());
        Stage stage = StageMother.passedStageInstance(jobIdentifier.getStageName(), jobIdentifier.getBuildName(), jobIdentifier.getPipelineName());
        stage.setJobInstances(new JobInstances(jobInstance));
        JobStatusMessage jobStatusMessage = new JobStatusMessage(jobIdentifier, JobState.Completed, "agent1");
        when(stageService.findStageWithIdentifier(jobStatusMessage.getStageIdentifier())).thenReturn(stage);

        jobStatusListener.onMessage(jobStatusMessage);

        verify(elasticAgentPluginService, times(1)).jobCompleted(jobInstance);
    }

    @Test
    void shouldNotPostAMessageToElasticAgentPluginServiceOnJobIsStillInProgress() {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();
        JobInstance jobInstance = JobInstanceMother.completed(jobIdentifier.getBuildName());
        Stage stage = StageMother.passedStageInstance(jobIdentifier.getStageName(), jobIdentifier.getBuildName(), jobIdentifier.getPipelineName());
        stage.setJobInstances(new JobInstances(jobInstance));
        JobStatusMessage jobStatusMessage = new JobStatusMessage(jobIdentifier, JobState.Building, "agent1");
        when(stageService.findStageWithIdentifier(jobStatusMessage.getStageIdentifier())).thenReturn(stage);

        jobStatusListener.onMessage(jobStatusMessage);

        verifyNoMoreInteractions(elasticAgentPluginService);
    }

    @Test
    void shouldPopulateJobInstanceWithJobPlan() {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();
        JobInstance jobInstance = JobInstanceMother.completed(jobIdentifier.getBuildName());
        Stage stage = StageMother.passedStageInstance(jobIdentifier.getStageName(), jobIdentifier.getBuildName(), jobIdentifier.getPipelineName());
        stage.setJobInstances(new JobInstances(jobInstance));
        JobStatusMessage jobStatusMessage = new JobStatusMessage(jobIdentifier, JobState.Completed, "agent1");
        when(stageService.findStageWithIdentifier(jobStatusMessage.getStageIdentifier())).thenReturn(stage);

        DefaultJobPlan plan = new DefaultJobPlan(null, new ArrayList<>(), null, 100, jobIdentifier, null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
        when(jobInstanceSqlMapDao.loadPlan(jobInstance.getId())).thenReturn(plan);

        assertThat(jobInstance.getPlan()).isNull();

        jobStatusListener.onMessage(jobStatusMessage);

        assertThat(jobInstance.getPlan()).isEqualTo(plan);
        verify(jobInstanceSqlMapDao, times(1)).loadPlan(jobInstance.getId());
    }
}
