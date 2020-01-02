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
package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceSqlMapDao;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.service.ElasticAgentPluginService;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.server.service.StageService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.GoConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.helper.BuildPlanMother.withBuildPlans;
import static com.thoughtworks.go.helper.ModificationsMother.modifyOneFile;
import static com.thoughtworks.go.helper.PipelineMother.withSingleStageWithMaterials;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class JobStatusListenerIntegrationTest {
    @Autowired
    private StageService stageService;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private ScheduleHelper scheduleHelper;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private GoCache goCache;
    @Autowired
    private ElasticAgentPluginService elasticAgentPluginService;
    @Autowired
    private JobInstanceSqlMapDao jobInstanceSqlMapDao;
    @Autowired
    private JobInstanceService jobInstanceService;

    private static final String PIPELINE_NAME = "mingle";
    private static final String STAGE_NAME = "dev";
    private static final String JOB_NAME = "unit";
    private static final String UUID = "AGENT1";
    private Pipeline savedPipeline;

    JobIdentifier jobIdentifier = new JobIdentifier(PIPELINE_NAME, "1", STAGE_NAME, "1", JOB_NAME);
    private static final AgentIdentifier AGENT1 = new AgentIdentifier(UUID, "IPADDRESS", UUID);
    private JobStatusListener listener;
    private StageStatusTopic stageStatusTopic;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();


    @Before
    public void setUp() throws Exception {
        goCache.clear();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        PipelineConfig pipelineConfig = withSingleStageWithMaterials(PIPELINE_NAME, STAGE_NAME, withBuildPlans(JOB_NAME));
        configHelper.addPipeline(PIPELINE_NAME, STAGE_NAME);
        savedPipeline = scheduleHelper.schedule(pipelineConfig, BuildCause.createWithModifications(modifyOneFile(pipelineConfig), ""), GoConstants.DEFAULT_APPROVED_BY);
        JobInstance job = savedPipeline.getStages().first().getJobInstances().first();
        job.setAgentUuid(UUID);

        stageStatusTopic = mock(StageStatusTopic.class);
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        goCache.clear();
        configHelper.onTearDown();
    }


    @Test
    public void shouldSendStageCompletedMessage() {
        final ElasticAgentPluginService spyOfElasticAgentPluginService = spy(this.elasticAgentPluginService);
        dbHelper.pass(savedPipeline);
        jobIdentifier.setBuildId(savedPipeline.getFirstStage().getJobInstances().get(0).getId());
        listener = new JobStatusListener(new JobStatusTopic(null), stageService, stageStatusTopic, spyOfElasticAgentPluginService, jobInstanceSqlMapDao, jobInstanceService);
        final StageStatusMessage stagePassed = new StageStatusMessage(jobIdentifier.getStageIdentifier(), StageState.Passed, StageResult.Passed);

        listener.onMessage(new JobStatusMessage(jobIdentifier, JobState.Completed, AGENT1.getUuid()));
        verify(stageStatusTopic).post(stagePassed);
        verify(spyOfElasticAgentPluginService).jobCompleted(any(JobInstance.class));
    }

    @Test
    public void shouldNotSendStageCompletedMessage() {
        final ElasticAgentPluginService spyOfElasticAgentPluginService = spy(this.elasticAgentPluginService);
        dbHelper.pass(savedPipeline);
        jobIdentifier.setBuildId(savedPipeline.getFirstStage().getJobInstances().get(0).getId());

        listener = new JobStatusListener(new JobStatusTopic(null), stageService, stageStatusTopic, spyOfElasticAgentPluginService, jobInstanceSqlMapDao, jobInstanceService);

        listener.onMessage(new JobStatusMessage(jobIdentifier, JobState.Building, AGENT1.getUuid()));

        verify(stageStatusTopic, never()).post(any(StageStatusMessage.class));
        verify(spyOfElasticAgentPluginService, never()).jobCompleted(any(JobInstance.class));
    }

    @Test
    public void shouldSendStageCompletedMessageForCancelledStage() {
        dbHelper.cancelStage(savedPipeline.getStages().get(0));
        jobIdentifier.setBuildId(savedPipeline.getFirstStage().getJobInstances().get(0).getId());
        listener = new JobStatusListener(new JobStatusTopic(null), stageService, stageStatusTopic, mock(ElasticAgentPluginService.class), jobInstanceSqlMapDao, jobInstanceService);
        final StageStatusMessage stageCancelled = new StageStatusMessage(jobIdentifier.getStageIdentifier(), StageState.Cancelled, StageResult.Cancelled);

        listener.onMessage(new JobStatusMessage(jobIdentifier, JobState.Completed, AGENT1.getUuid()));
        verify(stageStatusTopic).post(stageCancelled);
    }
}
