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

import java.sql.SQLException;
import java.util.Date;

import com.thoughtworks.go.config.GoConfigFileDao;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.activity.JobStatusCache;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class RescheduleJobTest {
    @Autowired private GoConfigFileDao goConfigFileDao;
    @Autowired private DatabaseAccessHelper dbHelper;

    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private PipelineWithTwoStages fixture;
    @Autowired private JobInstanceService jobInstanceService;
    @Autowired private ScheduleService scheduleService;
    @Autowired private JobStatusCache jobStatusCache;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    private static final String STAGE_NAME = "mingle";
    private static final String PIPELINE_NAME = "studios";
    private Stage stage;

    @Before
    public void setUp() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigFileDao);
        configHelper.onSetUp();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        configHelper.addPipeline(PIPELINE_NAME, STAGE_NAME);
        stage = dbHelper.saveBuildingStage(PIPELINE_NAME, STAGE_NAME);
    }

    @After
    public void teardown() throws Exception {
        fixture.onTearDown();
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void rescheduleBuildShouldUpdateCache() throws Exception {
        final JobInstance hungJob = stage.getJobInstances().get(0);
        final Pipeline pipeline = dbHelper.getPipelineDao().mostRecentPipeline(PIPELINE_NAME);
        //Need to do this in transaction because of caching
        dbHelper.txTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobInstanceService.save(new StageIdentifier(pipeline.getName(), -2, pipeline.getLabel(), stage.getName(), String.valueOf(stage.getCounter())), stage.getId(), hungJob);
            }
        });

        scheduleService.rescheduleJob(hungJob);
        assertThat(jobStatusCache.currentJob(hungJob.getIdentifier().jobConfigIdentifier()).getState(), is(JobState.Scheduled));
    }

    @Test
    public void rescheduleBuildShouldNotRescheduleIfReloadedJobIsCompleted() throws Exception {
        final JobInstance hungJob = stage.getJobInstances().get(0);
        hungJob.changeState(JobState.Completed, new Date());
        //Need to do this in transaction because of caching
        dbHelper.txTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobInstanceService.save(new StageIdentifier(PIPELINE_NAME, -2, hungJob.getIdentifier().getPipelineLabel(), stage.getName(), String.valueOf(stage.getCounter())), stage.getId(), hungJob);
            }
        });

        assertThat(hungJob.isCompleted(), is(true));
        assertThat(hungJob.isIgnored(), is(false));

        scheduleService.rescheduleJob(hungJob);
        assertThat(jobInstanceService.buildById(hungJob.getId()).isIgnored(), is(false));
    }

    @Test
    public void rescheduleHungBuildShouldScheduleNewBuild() throws Exception {
        JobInstance hungJob = stage.getJobInstances().get(0);
        dbHelper.getBuildInstanceDao().save(stage.getId(), hungJob);
        scheduleService.rescheduleJob(hungJob);

        JobInstance reloaded = dbHelper.getBuildInstanceDao().buildByIdWithTransitions(hungJob.getId());
        assertThat(reloaded.isIgnored(), is(true));
        assertThat(reloaded.getState(), is(JobState.Rescheduled));

        JobPlan newPlan = dbHelper.getBuildInstanceDao().orderedScheduledBuilds().get(0);
        assertThat(newPlan.getJobId(), is(not(hungJob.getId())));
        assertThat(newPlan.getStageName(), is(hungJob.getStageName()));

        JobInstance newJob = dbHelper.getBuildInstanceDao().buildByIdWithTransitions(newPlan.getJobId());
        assertThat(newJob.getState(), is(JobState.Scheduled));
    }

    @Test
    // #2882
    public void rescheduleShouldNotDuplicateResourcesEtc() throws Exception {
        JobInstance job = scheduledJob();
        JobPlan oldPlan = loadJobPlan(job);
        
        scheduleService.rescheduleJob(job);

        JobPlan newPlan = dbHelper.getBuildInstanceDao().orderedScheduledBuilds().get(0);
        assertThat(newPlan.getResources(), is(oldPlan.getResources()));
        assertThat(newPlan.getPropertyGenerators(), is(oldPlan.getPropertyGenerators()));
        assertThat(newPlan.getArtifactPlans(), is(oldPlan.getArtifactPlans()));
    }

    private JobPlan loadJobPlan(JobInstance job) {
        return dbHelper.getBuildInstanceDao().loadPlan(job.getId());
    }

    private JobInstance scheduledJob() throws SQLException {
        JobInstance hungJob = stage.getJobInstances().get(0);
        return jobInstanceService.buildByIdWithTransitions(hungJob.getId());
    }
}