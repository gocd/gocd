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

package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.service.StageService;
import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.GoConstants;
import org.jmock.Expectations;
import org.jmock.Mockery;
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
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class JobStatusListenerTest  {
    @Autowired private StageService stageService;
	@Autowired private DatabaseAccessHelper dbHelper;
	@Autowired private ScheduleHelper scheduleHelper;
	@Autowired private GoConfigDao goConfigDao;
	@Autowired private GoCache goCache;

    private static final String PIPELINE_NAME = "mingle";
    private static final String STAGE_NAME = "dev";
    private static final String JOB_NAME = "unit";
    private static final String UUID = "AGENT1";
    private Pipeline savedPipeline;

    private Mockery mockery;
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

        mockery = new ClassMockery();
        stageStatusTopic = mockery.mock(StageStatusTopic.class);
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        goCache.clear();
        configHelper.onTearDown();
    }


    @Test
    public void shouldSendStageCompletedMessage() {
        dbHelper.pass(savedPipeline);
        listener = new JobStatusListener(new JobStatusTopic(null), stageService, stageStatusTopic);
        final StageStatusMessage stagePassed = new StageStatusMessage(jobIdentifier.getStageIdentifier(), StageState.Passed, StageResult.Passed);

        mockery.checking(new Expectations() {
            {
                one(stageStatusTopic).post(stagePassed);
            }
        });

        listener.onMessage(new JobStatusMessage(jobIdentifier, JobState.Completed, AGENT1.getUuid()));
        mockery.assertIsSatisfied();
    }

    @Test
    public void shouldNotSendStageCompletedMessage() {
        listener = new JobStatusListener(new JobStatusTopic(null), stageService, stageStatusTopic);
        mockery.checking(new Expectations() {
            {
                never(stageStatusTopic).post(with(any(StageStatusMessage.class)));
            }
        });

        listener.onMessage(new JobStatusMessage(jobIdentifier, JobState.Completed, AGENT1.getUuid()));
        mockery.assertIsSatisfied();
    }
}
