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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.fixture.PipelineWithMultipleStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ScheduleStageTest {
    @Autowired private ScheduleService scheduleService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoConfigDao dao;
    @Autowired private StageDao stageDao;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    
    private PipelineWithMultipleStages fixture;
    private GoConfigFileHelper configHelper;

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper().usingCruiseConfigDao(dao);
        fixture = new PipelineWithMultipleStages(3, materialRepository, transactionTemplate);
        fixture.usingThreeJobs();
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @After
    public void tearDown() throws Exception {
        fixture.onTearDown();
    }

    @Test
    public void shouldRerunStageUsingPipelineCounter() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage oldStage = pipeline.getStages().byName(fixture.devStage);

        scheduleService.rerunStage(pipeline.getName(), String.valueOf(pipeline.getCounter()), fixture.devStage);
        Stage stage = dbHelper.getStageDao().mostRecentStage(
                new StageConfigIdentifier(pipeline.getName(), fixture.devStage));
        assertThat(stage.getCounter(), is(oldStage.getCounter() + 1));
    }

    @Test
    public void shouldResolveEnvironmentVariablesForStateReRun() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();

        EnvironmentVariablesConfig pipelineVariables = new EnvironmentVariablesConfig();
        pipelineVariables.add("pipelineEnv", "pipelineFoo");
        pipelineVariables.add("stageEnv", "pipelineBar");
        pipelineVariables.add("jobEnv", "pipelineBaz");
        configHelper.addEnvironmentVariableToPipeline(fixture.pipelineName, pipelineVariables);

        EnvironmentVariablesConfig stageVariables = new EnvironmentVariablesConfig();
        stageVariables.add("stageEnv", "stageBar");
        stageVariables.add("jobEnv", "stageBaz");
        configHelper.addEnvironmentVariableToStage(fixture.pipelineName, fixture.devStage, stageVariables);

        EnvironmentVariablesConfig jobVariables = new EnvironmentVariablesConfig();
        jobVariables.add("jobEnv", "jobBaz");
        configHelper.addEnvironmentVariableToJob(fixture.pipelineName, fixture.devStage, fixture.JOB_FOR_DEV_STAGE, jobVariables);

        Stage stage = scheduleService.rerunStage(pipeline.getName(), String.valueOf(pipeline.getCounter()), fixture.devStage);

        dbHelper.passStage(stage);

        EnvironmentVariablesConfig expectedVariableOrder = new EnvironmentVariablesConfig();
        expectedVariableOrder.add("pipelineEnv", "pipelineFoo");
        expectedVariableOrder.add("stageEnv", "stageBar");
        expectedVariableOrder.add("jobEnv", "jobBaz");

        JobInstances jobInstances = stage.getJobInstances();
        assertThat(jobInstances.getByName(fixture.JOB_FOR_DEV_STAGE).getPlan().getVariables(), is(expectedVariableOrder));
    }
    
     @Test
    public void shouldResolveEnvironmentVariablesForJobReRun() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();

       Stage oldStage = stageDao.stageByIdWithBuilds(pipeline.getStages().byName(fixture.devStage).getId());

        EnvironmentVariablesConfig pipelineVariables = new EnvironmentVariablesConfig();
        pipelineVariables.add("pipelineEnv", "pipelineFoo");
        pipelineVariables.add("stageEnv", "pipelineBar");
        pipelineVariables.add("jobEnv", "pipelineBaz");
        configHelper.addEnvironmentVariableToPipeline(fixture.pipelineName, pipelineVariables);

        EnvironmentVariablesConfig stageVariables = new EnvironmentVariablesConfig();
        stageVariables.add("stageEnv", "stageBar");
        stageVariables.add("jobEnv", "stageBaz");
        configHelper.addEnvironmentVariableToStage(fixture.pipelineName, fixture.devStage, stageVariables);

        EnvironmentVariablesConfig jobVariables = new EnvironmentVariablesConfig();
        jobVariables.add("jobEnv", "jobBaz");
        configHelper.addEnvironmentVariableToJob(fixture.pipelineName, fixture.devStage, fixture.JOB_FOR_DEV_STAGE, jobVariables);

        Stage stage = scheduleService.rerunJobs(oldStage, a(fixture.JOB_FOR_DEV_STAGE), new HttpOperationResult());

        EnvironmentVariablesConfig expectedVariableOrder = new EnvironmentVariablesConfig();
        expectedVariableOrder.add("pipelineEnv", "pipelineFoo");
        expectedVariableOrder.add("stageEnv", "stageBar");
        expectedVariableOrder.add("jobEnv", "jobBaz");

        JobInstances jobInstances = stage.getJobInstances();
        assertThat(jobInstances.getByName(fixture.JOB_FOR_DEV_STAGE).getPlan().getVariables(), is(expectedVariableOrder));
    }

    @Test
    public void shouldRerunOnlyGivenJobsFromExistingStage() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();

        Stage stage = scheduleService.rerunStage(pipeline.getName(), String.valueOf(pipeline.getCounter()), fixture.devStage);

        dbHelper.passStage(stage);

        assertThat(stage.hasRerunJobs(), is(false));

        Stage oldStage = stageDao.stageByIdWithBuilds(pipeline.getStages().byName(fixture.devStage).getId());

        assertThat(oldStage.hasRerunJobs(), is(false));

        HttpOperationResult result = new HttpOperationResult();
        Stage newStage = scheduleService.rerunJobs(oldStage, a("foo", "foo3"), result);
        Stage loadedLatestStage = dbHelper.getStageDao().findStageWithIdentifier(newStage.getIdentifier());
        assertThat(loadedLatestStage.isLatestRun(), is(true));

        assertThat(loadedLatestStage.getJobInstances().getByName("foo").getResult(), is(JobResult.Unknown));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").getState(), is(JobState.Scheduled));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").getOriginalJobId(), is(nullValue()));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").isRerun(), is(true));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").isCopy(), is(false));

        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").getResult(), is(JobResult.Passed));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").getState(), is(JobState.Completed));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").getOriginalJobId(), is(oldStage.getJobInstances().getByName("foo2").getId()));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").isRerun(), is(false));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").isCopy(), is(true));

        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").getResult(), is(JobResult.Unknown));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").getState(), is(JobState.Scheduled));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").getOriginalJobId(), is(nullValue()));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").isRerun(), is(true));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").isCopy(), is(false));


        assertThat(loadedLatestStage, is(newStage));
        assertThat(loadedLatestStage.hasRerunJobs(), is(true));
        assertThat(loadedLatestStage.getCounter(), is(oldStage.getCounter() + 2));
        assertThat(loadedLatestStage.getIdentifier().getPipelineCounter(), is(oldStage.getIdentifier().getPipelineCounter()));
        assertThat(result.canContinue(), is(true));
    }

    @Test
    public void shouldConsiderCopyOfRerunJobACopyAndNotRerun() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();

        Stage stage = scheduleService.rerunStage(pipeline.getName(), String.valueOf(pipeline.getCounter()), fixture.devStage);

        assertThat(stage.hasRerunJobs(), is(false));

        dbHelper.passStage(stage);

        Stage oldStage = stageDao.stageByIdWithBuilds(pipeline.getStages().byName(fixture.devStage).getId());

        assertThat(oldStage.hasRerunJobs(), is(false));

        HttpOperationResult result = new HttpOperationResult();
        Stage newStage = scheduleService.rerunJobs(oldStage, a("foo", "foo3"), result);
        Stage loadedLatestStage = dbHelper.getStageDao().findStageWithIdentifier(newStage.getIdentifier());

        assertThat(loadedLatestStage.hasRerunJobs(), is(true));

        dbHelper.passStage(loadedLatestStage);
        JobInstances jobsBeforeLatestRerunJobs = loadedLatestStage.getJobInstances();

        newStage = scheduleService.rerunJobs(loadedLatestStage, a("foo2"), result);

        loadedLatestStage = dbHelper.getStageDao().findStageWithIdentifier(newStage.getIdentifier());

        assertThat(loadedLatestStage.getJobInstances().getByName("foo").getResult(), is(JobResult.Passed));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").getState(), is(JobState.Completed));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").getOriginalJobId(), is(jobsBeforeLatestRerunJobs.getByName("foo").getId()));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").isRerun(), is(false));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").isCopy(), is(true));

        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").getResult(), is(JobResult.Unknown));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").getState(), is(JobState.Scheduled));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").getOriginalJobId(), is(nullValue()));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").isRerun(), is(true));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").isCopy(), is(false));

        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").getResult(), is(JobResult.Passed));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").getState(), is(JobState.Completed));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").getOriginalJobId(), is(jobsBeforeLatestRerunJobs.getByName("foo3").getId()));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").isRerun(), is(false));
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").isCopy(), is(true));

        assertThat(loadedLatestStage, is(newStage));
        assertThat(loadedLatestStage.hasRerunJobs(), is(true));
        assertThat(loadedLatestStage.getCounter(), is(oldStage.getCounter() + 3));
        assertThat(loadedLatestStage.getIdentifier().getPipelineCounter(), is(oldStage.getIdentifier().getPipelineCounter()));
        assertThat(result.canContinue(), is(true));
    }

    @Test
    public void shouldFailRerunWhenJobConfigDoesNotExist() throws Exception {
        Pipeline pipeline = fixture.createPipelineWithFirstStageScheduled();
        Stage oldStage = pipeline.getStages().byName(fixture.devStage);
        dbHelper.pass(pipeline);

        configHelper.removeJob(pipeline.getName(), fixture.devStage, "foo3");

        HttpOperationResult result = new HttpOperationResult();

        Stage newStage = scheduleService.rerunJobs(oldStage, a("foo3"), result);

        assertThat(result.canContinue(), is(false));
        assertThat(result.message(), containsString("Cannot rerun job 'foo3'. Configuration for job doesn't exist."));
        assertThat(newStage, is(nullValue()));
    }

    @Test
    public void shouldRerunJobsWithUserAsApprover() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage oldStage = pipeline.getStages().byName(fixture.devStage);

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(new User("loser", "pass", true, true, true, true, new GrantedAuthority[]{}), null));
        HttpOperationResult result = new HttpOperationResult();
        Stage newStage = scheduleService.rerunJobs(oldStage, a("foo", "foo3"), result);
        Stage loadedLatestStage = dbHelper.getStageDao().findStageWithIdentifier(newStage.getIdentifier());
        assertThat(loadedLatestStage.getApprovedBy(), is("loser"));
        assertThat(oldStage.getApprovedBy(), is(not("loser")));
        assertThat(result.canContinue(), is(true));
    }

    @Test
    public void shouldFailWhenStageAlreadyActive() throws Exception {
        Pipeline pipeline = fixture.createPipelineWithFirstStageScheduled();
        Stage oldStage = pipeline.getStages().byName(fixture.devStage);

        HttpOperationResult result = new HttpOperationResult();
        Stage newStage = scheduleService.rerunJobs(oldStage, a("foo", "foo3"), result);

        assertThat(result.canContinue(), is(false));
        assertThat(result.message(), containsString("Pipeline[name='" + pipeline.getName() + "', counter='" + pipeline.getCounter() + "', label='" + pipeline.getLabel() + "'] is still in progress"));
        assertThat(result.message(), containsString("Cannot schedule"));
        assertThat(newStage, is(nullValue()));
    }

    @Test
    public void shouldNotRunStageIfItsPreviousStageHasNotBeenRun() throws Exception {
        Pipeline pipeline = fixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        String theThirdStage = fixture.stageName(3);
        try {
            scheduleService.rerunStage(pipeline.getName(), pipeline.getLabel(), theThirdStage);
        } catch (Exception e) {
            assertThat(e.getMessage(), is(String.format(
                    "Can not run stage [%s] in pipeline [%s] because its previous stage has not been run.",
                    theThirdStage, pipeline.getName())));
        }
    }

    @Test
    public void shouldNotScheduleAStageIfAnyStageForThatPipelineIsAlreadyRunning() throws Exception {
        fixture.createdPipelineWithAllStagesPassed();
        final List<Exception> exceptions = new ArrayList<Exception>();
        Thread t1 = new Thread(rerunStage(exceptions, fixture.devStage));
        Thread t2 = new Thread(rerunStage(exceptions, fixture.ftStage));

        t1.start();
        t2.start();

        t1.join();
        t2.join();
        assertThat(exceptions.size(), is(1));
    }

    private Runnable rerunStage(final List<Exception> exceptions, final String ftStage) {
        return new Runnable() {
            public void run() {
                try {
                    scheduleService.rerunStage(fixture.pipelineName, fixture.pipelineLabel(), ftStage);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
        };
    }
}
