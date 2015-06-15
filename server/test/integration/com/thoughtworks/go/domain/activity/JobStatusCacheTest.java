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

package com.thoughtworks.go.domain.activity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.JobConfigIdentifier;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class JobStatusCacheTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private JobStatusCache jobStatusCache;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    
    private PipelineWithTwoStages pipelineFixture;
    private static GoConfigFileHelper configFileHelper = new GoConfigFileHelper();
    
    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        configFileHelper.usingEmptyConfigFileWithLicenseAllowsUnlimitedAgents();
        configFileHelper.usingCruiseConfigDao(goConfigDao);

        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        pipelineFixture.usingConfigHelper(configFileHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        pipelineFixture.onTearDown();
        jobStatusCache.clear();
    }

    @Test
    public void shouldLoadMostRecentInstanceFromDBForTheFirstTime() throws SQLException {
        pipelineFixture.createdPipelineWithAllStagesPassed();

        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStageScheduled();

        JobInstance expect = pipeline.getStages().byName(pipelineFixture.devStage).getJobInstances().first();

        assertThat(jobStatusCache.currentJob(new JobConfigIdentifier(pipelineFixture.pipelineName,
                pipelineFixture.devStage, PipelineWithTwoStages.JOB_FOR_DEV_STAGE)), is(expect));
    }

    @Test
    public void shouldLoadMostRecentInstanceFromDBOnlyOnce() throws SQLException {
        Mockery mockery = new Mockery();
        final StageDao mock = mockery.mock(StageDao.class);
        final JobInstance instance = JobInstanceMother.passed("linux-firefox");

        final List<JobInstance> found = new ArrayList<JobInstance>();
        found.add(instance);
        
        mockery.checking(new Expectations() {
            {
                one(mock).mostRecentJobsForStage("pipeline", "stage");
                will(returnValue(found));
            }
        });

        JobConfigIdentifier identifier = new JobConfigIdentifier(instance.getPipelineName(), instance.getStageName(), instance.getName());
        JobStatusCache cache = new JobStatusCache(mock);
        assertThat(cache.currentJob(identifier).getState(), is(instance.getState()));

        //call currentJob for the second time, should not call jobInstanceDao now
        assertThat(cache.currentJob(identifier).getState(), is(instance.getState()));
    }

    @Test
    public void shouldRefreshCurrentJobWhenNewJobComes() {
        JobInstance job = JobInstanceMother.buildingInstance("cruise", "dev", "linux-firefox", "1");

        jobStatusCache.jobStatusChanged(job);
        assertThat(jobStatusCache.currentJob(job.getIdentifier().jobConfigIdentifier()).getState(), is(JobState.Building));

        JobInstance passing = job.clone();
        passing.changeState(JobState.Completed);

        jobStatusCache.jobStatusChanged(passing);
        assertThat(jobStatusCache.currentJob(passing.getIdentifier().jobConfigIdentifier()).getState(), is(JobState.Completed));
    }

    @Test
    public void shouldHitTheDatabaseOnlyOnceIfTheJobIsNeverScheduledEver() {
        StageDao dao = mock(StageDao.class);
        when(dao.mostRecentJobsForStage("cruise", "dev")).thenReturn(new ArrayList<JobInstance>());

        JobStatusCache cache = new JobStatusCache(dao);
        assertThat(cache.currentJobs(new JobConfigIdentifier("cruise", "dev", "linux-firefox")).isEmpty(), is(true));

        assertThat(cache.currentJobs(new JobConfigIdentifier("cruise", "dev", "linux-firefox")).isEmpty(), is(true));

        Mockito.verify(dao, times(1)).mostRecentJobsForStage("cruise", "dev");
    }

    @Test
    public void shouldSkipNeverRunJobsWhenTryingToDealWithOtherJobs() {
        StageDao dao = mock(StageDao.class);
        JobInstance random = jobInstance("random");
        when(dao.mostRecentJobsForStage("cruise", "dev")).thenReturn(Arrays.asList(random));

        JobStatusCache cache = new JobStatusCache(dao);
        assertThat(cache.currentJobs(new JobConfigIdentifier("cruise", "dev", "linux-firefox")).isEmpty(), is(true));

        assertThat(cache.currentJobs(new JobConfigIdentifier("cruise", "dev", "random")).get(0), is(random));
        Mockito.verify(dao, times(2)).mostRecentJobsForStage("cruise", "dev");
    }

    @Test
    public void shouldRemoveTheNeverRunInstanceWhenTheJobRunsForTheFirstTime() {
        StageDao dao = mock(StageDao.class);
        when(dao.mostRecentJobsForStage("cruise", "dev")).thenReturn(new ArrayList<JobInstance>());

        JobStatusCache cache = new JobStatusCache(dao);
        assertThat(cache.currentJobs(new JobConfigIdentifier("cruise", "dev", "linux-firefox")).isEmpty(), is(true));

        JobInstance instance = jobInstance("linux-firefox");
        cache.jobStatusChanged(instance);

        assertThat(cache.currentJobs(new JobConfigIdentifier("cruise", "dev", "linux-firefox")).get(0), is(instance));
        Mockito.verify(dao, times(1)).mostRecentJobsForStage("cruise", "dev");
    }

    private JobInstance jobInstance(String jobConfigName) {
        JobInstance instance = JobInstanceMother.scheduled(jobConfigName);
        instance.setIdentifier(new JobIdentifier("cruise", 1, "1", "dev", "1", jobConfigName));
        return instance;
    }

    @Test public void shouldReturnNullWhenNoCurrentJob() throws Exception {
        pipelineFixture.createdPipelineWithAllStagesPassed();

        JobConfigIdentifier jobConfigIdentifier = new JobConfigIdentifier(pipelineFixture.pipelineName, pipelineFixture.devStage, "wrong-job");
        JobInstance currentJob = jobStatusCache.currentJob(jobConfigIdentifier);
        assertThat(currentJob, is(nullValue()));
    }

    @Test
    public void shouldFindAllMatchingJobsForJobsThatAreRunOnAllAgents() {
        JobInstance job1 = JobInstanceMother.instanceForRunOnAllAgents("cruise", "dev", "linux-firefox", "1", 1);
        JobInstance job2 = JobInstanceMother.instanceForRunOnAllAgents("cruise", "dev", "linux-firefox", "1", 2);

        jobStatusCache.jobStatusChanged(job1);
        jobStatusCache.jobStatusChanged(job2);

        JobConfigIdentifier config = new JobConfigIdentifier("cruise", "dev", "linux-firefox");

        List<JobInstance> list = jobStatusCache.currentJobs(config);
        assertThat(list, hasItem(job1));
        assertThat(list, hasItem(job2));
        assertThat(list.size(), is(2));
    }

	@Test
	public void shouldFindAllMatchingJobsForJobsThatAreRunMultipleInstance() {
		JobInstance job1 = JobInstanceMother.instanceForRunMultipleInstance("cruise", "dev", "linux-firefox", "1", 1);
		JobInstance job2 = JobInstanceMother.instanceForRunMultipleInstance("cruise", "dev", "linux-firefox", "1", 2);

		jobStatusCache.jobStatusChanged(job1);
		jobStatusCache.jobStatusChanged(job2);

		JobConfigIdentifier config = new JobConfigIdentifier("cruise", "dev", "linux-firefox");

		List<JobInstance> list = jobStatusCache.currentJobs(config);
		assertThat(list, hasItem(job1));
		assertThat(list, hasItem(job2));
		assertThat(list.size(), is(2));
	}

    @Test
    public void shouldExcludeJobFromOtherPipelines() {
        JobInstance job1 = JobInstanceMother.instanceForRunOnAllAgents("cruise", "dev", "linux-firefox", "1", 1);
        JobInstance job2 = JobInstanceMother.instanceForRunOnAllAgents("cruise", "dev", "linux-firefox", "1", 2);

        jobStatusCache.jobStatusChanged(job1);
        jobStatusCache.jobStatusChanged(job2);

        JobInstance otherPipeline = JobInstanceMother.buildingInstance("different-pipeline", "dev", "linux-firefox", "1");
        jobStatusCache.jobStatusChanged(otherPipeline);

        JobConfigIdentifier config = new JobConfigIdentifier("cruise", "dev", "linux-firefox");

        List<JobInstance> list = jobStatusCache.currentJobs(config);
        assertThat(list, not(hasItem(otherPipeline)));
    }

    @Test
    public void shouldExcludeJobFromOtherStages() {
        JobInstance job1 = JobInstanceMother.instanceForRunOnAllAgents("cruise", "dev", "linux-firefox", "1", 1);
        JobInstance job2 = JobInstanceMother.instanceForRunOnAllAgents("cruise", "dev", "linux-firefox", "1", 2);

        jobStatusCache.jobStatusChanged(job1);
        jobStatusCache.jobStatusChanged(job2);

        JobInstance otherStage = JobInstanceMother.buildingInstance("cruise", "different-stage", "linux-firefox", "1");
        jobStatusCache.jobStatusChanged(otherStage);

        JobConfigIdentifier config = new JobConfigIdentifier("cruise", "dev", "linux-firefox");

        List<JobInstance> list = jobStatusCache.currentJobs(config);
        assertThat(list, not(hasItem(otherStage)));
    }

    @Test
    public void shouldExcludeJobFromOtherInstancesOfThisStage() {
        JobInstance job1 = JobInstanceMother.buildingInstance("cruise", "dev", "linux-firefox", "1");
        JobInstance job2 = JobInstanceMother.instanceForRunOnAllAgents("cruise", "dev", "linux-firefox", "2", 1);

        jobStatusCache.jobStatusChanged(job1);
        jobStatusCache.jobStatusChanged(job2);

        JobConfigIdentifier config = new JobConfigIdentifier("cruise", "dev", "linux-firefox");

        List<JobInstance> list = jobStatusCache.currentJobs(config);
        assertThat(list, hasItem(job2));
        assertThat(list, not(hasItem(job1)));
    }

}
