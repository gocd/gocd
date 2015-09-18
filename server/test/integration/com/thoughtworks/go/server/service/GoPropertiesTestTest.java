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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.JobStateTransition;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PropertyDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import static com.thoughtworks.go.server.dao.DatabaseAccessHelper.AGENT_UUID;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GoPropertiesTestTest {
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoConfigDao cruiseConfigDao;
    private PipelineWithTwoStages fixture;
    private GoConfigFileHelper configHelper;
    @Autowired private BuildRepositoryService buildRepositoryService;
    @Autowired private PropertyDao propertyDao;
    private static final String HOSTNAME = "10.18.0.1";
    @Autowired private StageService stageService;
    @Autowired private PipelineService pipelineDao;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(cruiseConfigDao);
        configHelper.onSetUp();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        configHelper.addAgent(HOSTNAME, AGENT_UUID);
    }

    @After
    public void tearDown() throws Exception {
        fixture.onTearDown();
        configHelper.onTearDown();
    }


    @Test
    public void shouldGeneratePropertiesAfterBuildCompleted() throws Exception {
        Pipeline newPipeline = fixture.createPipelineWithFirstStageAssigned(AGENT_UUID);
        JobInstance job = completeStageAndTrigger(newPipeline.getFirstStage());
        assertCommonBuildProperties(newPipeline, job);
        assertThat(propertyDao.value(job.getId(), GoConstants.CRUISE_AGENT), is(HOSTNAME));
    }

    @Test
    public void shouldGeneratePropertiesForCancelledBuild() throws Exception {
        Pipeline newPipeline = fixture.createPipelineWithFirstStageAssigned(AGENT_UUID);
        final Stage firstStage = newPipeline.getFirstStage();
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            public void doInTransactionWithoutResult(TransactionStatus status) {
                stageService.cancelStage(stageService.stageById(firstStage.getId()));
            }
        });
        Pipeline pipelineFromDb = pipelineDao.mostRecentFullPipelineByName(newPipeline.getName());
        JobInstance job = pipelineFromDb.getFirstStage().getJobInstances().first();
        assertCommonBuildProperties(newPipeline, job);
        assertThat(propertyDao.value(job.getId(), GoConstants.CRUISE_AGENT), is(HOSTNAME));
    }

    @Test
    public void shouldGeneratePropertiesForCancellingScheduledBuild() throws Exception {
        Pipeline newPipeline = fixture.createPipelineWithFirstStageScheduled();
        final Stage firstStage = newPipeline.getFirstStage();
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            public void doInTransactionWithoutResult(TransactionStatus status) {
                stageService.cancelStage(stageService.stageById(firstStage.getId()));
            }
        });

        Pipeline pipelineFromDb = pipelineDao.mostRecentFullPipelineByName(newPipeline.getName());
        JobInstance job = pipelineFromDb.getFirstStage().getJobInstances().first();
        assertCommonBuildProperties(newPipeline, job);
        assertThat(propertyDao.value(job.getId(), GoConstants.CRUISE_AGENT), is(""));
    }

    private void assertCommonBuildProperties(Pipeline newPipeline, JobInstance job) {
        assertThat(propertyDao.value(job.getId(), GoConstants.CRUISE_PIPELINE_LABEL), is(newPipeline.getLabel()));
        assertThat(propertyDao.value(job.getId(), GoConstants.CRUISE_PIPELINE_COUNTER),
                is(String.valueOf(newPipeline.getCounter())));
        assertThat(propertyDao.value(job.getId(), GoConstants.CRUISE_JOB_DURATION),
                is(job.getCurrentBuildDuration()));
        assertThat(propertyDao.value(job.getId(), GoConstants.CRUISE_RESULT),
                is(job.getResult().toString()));
        assertThat(propertyDao.value(job.getId(), GoConstants.CRUISE_STAGE_COUNTER),
                is(job.getStageCounter()));


        for (JobStateTransition transition : job.getTransitions()) {
            String transitionKey = PropertiesService.getTransitionKey(transition.getCurrentState());
            assertThat(propertyDao.value(job.getId(), transitionKey), is(
                    DateUtils.formatISO8601(transition.getStateChangeTime())));
        }
    }

    private JobInstance completeStageAndTrigger(Stage oldFtStage) throws Exception {
        JobInstance job = oldFtStage.getJobInstances().first();
        buildRepositoryService.completing(job.getIdentifier(), JobResult.Passed, AGENT_UUID);
        reportJobPassed(job);
        return dbHelper.getBuildInstanceDao().buildByIdWithTransitions(job.getId());
    }

    private void reportJobPassed(JobInstance jobInstance) throws Exception {
        buildRepositoryService.updateStatusFromAgent(jobInstance.getIdentifier(), JobState.Completed, AGENT_UUID);
    }
}