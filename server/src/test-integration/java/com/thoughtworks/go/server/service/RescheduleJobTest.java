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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.activity.JobStatusCache;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class RescheduleJobTest {
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private PipelineWithTwoStages fixture;
    @Autowired
    private JobInstanceService jobInstanceService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private JobStatusCache jobStatusCache;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    public static final String JOB_NAME = "unit";
    private static final String STAGE_NAME = "mingle";
    private static final String PIPELINE_NAME = "studios";
    private Stage stage;

    @Before
    public void setUp() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, temporaryFolder);
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
    public void rescheduleBuildShouldUpdateCache() {
        final JobInstance hungJob = stage.getJobInstances().get(0);
        final Pipeline pipeline = dbHelper.getPipelineDao().mostRecentPipeline(PIPELINE_NAME);
        //Need to do this in transaction because of caching
        dbHelper.txTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobInstanceService.save(new StageIdentifier(pipeline.getName(), -2, pipeline.getLabel(), stage.getName(), String.valueOf(stage.getCounter())), stage.getId(), hungJob);
            }
        });

        scheduleService.rescheduleJob(hungJob);
        assertThat(jobStatusCache.currentJob(hungJob.getIdentifier().jobConfigIdentifier()).getState(), is(JobState.Scheduled));
    }

    @Test
    public void rescheduleBuildShouldNotRescheduleIfReloadedJobIsCompleted() {
        final JobInstance hungJob = stage.getJobInstances().get(0);
        hungJob.changeState(JobState.Completed, new Date());
        //Need to do this in transaction because of caching
        dbHelper.txTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jobInstanceService.save(new StageIdentifier(PIPELINE_NAME, -2, hungJob.getIdentifier().getPipelineLabel(), stage.getName(), String.valueOf(stage.getCounter())), stage.getId(), hungJob);
            }
        });

        assertThat(hungJob.isCompleted(), is(true));
        assertThat(hungJob.isIgnored(), is(false));

        scheduleService.rescheduleJob(hungJob);
        assertThat(jobInstanceService.buildById(hungJob.getId()).isIgnored(), is(false));
    }

    @Test
    public void rescheduleHungBuildShouldScheduleNewBuild()  {
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
    public void shouldRescheduleBuildAlongWithAssociatedEntitiesCorrectly()  {
        dbHelper.cancelStage(stage);

        ResourceConfigs resourceConfigs = new ResourceConfigs(new ResourceConfig("r1"), new ResourceConfig("r2"));
        ArtifactTypeConfigs artifactTypeConfigs = new ArtifactTypeConfigs(Arrays.asList(new BuildArtifactConfig("s1", "d1"), new BuildArtifactConfig("s2", "d2")));
        configHelper.addAssociatedEntitiesForAJob(PIPELINE_NAME, STAGE_NAME, JOB_NAME, resourceConfigs, artifactTypeConfigs);

        dbHelper.schedulePipeline(configHelper.currentConfig().getPipelineConfigByName(new CaseInsensitiveString(PIPELINE_NAME)), new TimeProvider());

        JobPlan oldJobPlan = dbHelper.getBuildInstanceDao().orderedScheduledBuilds().get(0);
        assertThat(oldJobPlan.getResources().size(), is(2));
        assertThat(oldJobPlan.getArtifactPlans().size(), is(2));

        JobInstance oldJobInstance = dbHelper.getBuildInstanceDao().buildById(oldJobPlan.getJobId());
        scheduleService.rescheduleJob(oldJobInstance);

        JobInstance reloadedOldJobInstance = dbHelper.getBuildInstanceDao().buildById(oldJobInstance.getId());
        assertThat(reloadedOldJobInstance.isIgnored(), is(true));
        assertThat(reloadedOldJobInstance.getState(), is(JobState.Rescheduled));

        JobPlan newJobPlan = dbHelper.getBuildInstanceDao().orderedScheduledBuilds().get(1);
        assertThat(newJobPlan.getJobId(), is(not(oldJobInstance.getId())));

        assertThat(newJobPlan.getResources().size(), is(2));
        for (int i = 0; i < newJobPlan.getResources().size(); i++) {
            Resource newResource = newJobPlan.getResources().get(i);
            Resource oldResource = oldJobPlan.getResources().get(i);
            assertThat(newResource.getId(), is(not(oldResource.getId())));
            assertThat(newResource.getName(), is(oldResource.getName()));
            assertThat(ReflectionUtil.getField(newResource, "buildId"), is(newJobPlan.getJobId()));
        }

        assertThat(newJobPlan.getArtifactPlans().size(), is(2));
        for (int i = 0; i < newJobPlan.getArtifactPlans().size(); i++) {
            ArtifactPlan newArtifactPlan = newJobPlan.getArtifactPlans().get(i);
            ArtifactPlan oldArtifactPlan = oldJobPlan.getArtifactPlans().get(i);
            assertThat(newArtifactPlan.getId(), is(not(oldArtifactPlan.getId())));
            assertThat(newArtifactPlan.getArtifactPlanType(), is(oldArtifactPlan.getArtifactPlanType()));
            assertThat(newArtifactPlan.getSrc(), is(oldArtifactPlan.getSrc()));
            assertThat(newArtifactPlan.getDest(), is(oldArtifactPlan.getDest()));
            assertThat(newArtifactPlan.getArtifactPlanType(), is(oldArtifactPlan.getArtifactPlanType()));
            assertThat(ReflectionUtil.getField(newArtifactPlan, "buildId"), is(newJobPlan.getJobId()));
        }

        JobInstance newJobInstance = dbHelper.getBuildInstanceDao().buildById(newJobPlan.getJobId());
        assertThat(newJobInstance.getState(), is(JobState.Scheduled));
    }

    @Test
    // #2882
    public void rescheduleShouldNotDuplicateResourcesEtc() throws Exception {
        JobInstance job = scheduledJob();
        JobPlan oldPlan = loadJobPlan(job);

        scheduleService.rescheduleJob(job);

        JobPlan newPlan = dbHelper.getBuildInstanceDao().orderedScheduledBuilds().get(0);
        assertThat(newPlan.getResources(), is(oldPlan.getResources()));
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
