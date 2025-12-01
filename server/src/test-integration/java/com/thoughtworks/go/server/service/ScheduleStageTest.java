/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.fixture.PipelineWithMultipleStages;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.newsecurity.SessionUtilsHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ClearSingleton.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ScheduleStageTest {
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoConfigDao dao;
    @Autowired
    private StageDao stageDao;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private PipelineWithMultipleStages pipelineFixture;
    private GoConfigFileHelper configHelper;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        configHelper = new GoConfigFileHelper().usingCruiseConfigDao(dao);
        pipelineFixture = new PipelineWithMultipleStages(3, materialRepository, transactionTemplate, tempDir);
        pipelineFixture.usingThreeJobs();
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
        pipelineFixture.onTearDown();
    }

    @Test
    public void shouldRerunStageUsingPipelineCounter() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        Stage oldStage = pipeline.getStages().byName(pipelineFixture.devStage);

        scheduleService.rerunStage(pipeline.getName(), pipeline.getCounter(), pipelineFixture.devStage);
        Stage stage = dbHelper.getStageDao().mostRecentStage(
                new StageConfigIdentifier(pipeline.getName(), pipelineFixture.devStage));
        assertThat(stage.getCounter()).isEqualTo(oldStage.getCounter() + 1);
    }

    @Test
    public void shouldResolveEnvironmentVariablesForStateReRun() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();

        EnvironmentVariablesConfig pipelineVariables = new EnvironmentVariablesConfig();
        pipelineVariables.add("pipelineEnv", "pipelineFoo");
        pipelineVariables.add("stageEnv", "pipelineBar");
        pipelineVariables.add("jobEnv", "pipelineBaz");
        configHelper.addEnvironmentVariableToPipeline(pipelineFixture.pipelineName, pipelineVariables);

        EnvironmentVariablesConfig stageVariables = new EnvironmentVariablesConfig();
        stageVariables.add("stageEnv", "stageBar");
        stageVariables.add("jobEnv", "stageBaz");
        configHelper.addEnvironmentVariableToStage(pipelineFixture.pipelineName, pipelineFixture.devStage, stageVariables);

        EnvironmentVariablesConfig jobVariables = new EnvironmentVariablesConfig();
        jobVariables.add("jobEnv", "jobBaz");
        configHelper.addEnvironmentVariableToJob(pipelineFixture.pipelineName, pipelineFixture.devStage, PipelineWithTwoStages.JOB_FOR_DEV_STAGE, jobVariables);

        Stage stage = scheduleService.rerunStage(pipeline.getName(), pipeline.getCounter(), pipelineFixture.devStage);

        dbHelper.passStage(stage);

        EnvironmentVariables expectedVariableOrder = new EnvironmentVariables();
        expectedVariableOrder.add("pipelineEnv", "pipelineFoo");
        expectedVariableOrder.add("stageEnv", "stageBar");
        expectedVariableOrder.add("jobEnv", "jobBaz");

        JobInstances jobInstances = stage.getJobInstances();
        assertThat(jobInstances.getByName(PipelineWithTwoStages.JOB_FOR_DEV_STAGE).getPlan().getVariables()).isEqualTo(expectedVariableOrder);
    }

    @Test
    public void shouldResolveEnvironmentVariablesForJobReRun() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();

       Stage oldStage = stageDao.stageById(pipeline.getStages().byName(pipelineFixture.devStage).getId());

        EnvironmentVariablesConfig pipelineVariables = new EnvironmentVariablesConfig();
        pipelineVariables.add("pipelineEnv", "pipelineFoo");
        pipelineVariables.add("stageEnv", "pipelineBar");
        pipelineVariables.add("jobEnv", "pipelineBaz");
        configHelper.addEnvironmentVariableToPipeline(pipelineFixture.pipelineName, pipelineVariables);

        EnvironmentVariablesConfig stageVariables = new EnvironmentVariablesConfig();
        stageVariables.add("stageEnv", "stageBar");
        stageVariables.add("jobEnv", "stageBaz");
        configHelper.addEnvironmentVariableToStage(pipelineFixture.pipelineName, pipelineFixture.devStage, stageVariables);

        EnvironmentVariablesConfig jobVariables = new EnvironmentVariablesConfig();
        jobVariables.add("jobEnv", "jobBaz");
        configHelper.addEnvironmentVariableToJob(pipelineFixture.pipelineName, pipelineFixture.devStage, PipelineWithTwoStages.JOB_FOR_DEV_STAGE, jobVariables);

        Stage stage = scheduleService.rerunJobs(oldStage, List.of(PipelineWithTwoStages.JOB_FOR_DEV_STAGE), new HttpOperationResult());

        EnvironmentVariables expectedVariableOrder = new EnvironmentVariables();
        expectedVariableOrder.add("pipelineEnv", "pipelineFoo");
        expectedVariableOrder.add("stageEnv", "stageBar");
        expectedVariableOrder.add("jobEnv", "jobBaz");

        JobInstances jobInstances = stage.getJobInstances();
        assertThat(jobInstances.getByName(PipelineWithTwoStages.JOB_FOR_DEV_STAGE).getPlan().getVariables()).isEqualTo(expectedVariableOrder);
    }

    @Test
    public void shouldRerunOnlyGivenJobsFromExistingStage() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();

        Stage stage = scheduleService.rerunStage(pipeline.getName(), pipeline.getCounter(), pipelineFixture.devStage);

        dbHelper.passStage(stage);

        assertThat(stage.hasRerunJobs()).isFalse();

        Stage oldStage = stageDao.stageById(pipeline.getStages().byName(pipelineFixture.devStage).getId());

        assertThat(oldStage.hasRerunJobs()).isFalse();

        HttpOperationResult result = new HttpOperationResult();
        Stage newStage = scheduleService.rerunJobs(oldStage, List.of("foo", "foo3"), result);
        Stage loadedLatestStage = dbHelper.getStageDao().findStageWithIdentifier(newStage.getIdentifier());
        assertThat(loadedLatestStage.isLatestRun()).isTrue();

        assertThat(loadedLatestStage.getJobInstances().getByName("foo").getResult()).isEqualTo(JobResult.Unknown);
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").getState()).isEqualTo(JobState.Scheduled);
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").getOriginalJobId()).isNull();
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").isRerun()).isTrue();
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").isCopy()).isFalse();

        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").getResult()).isEqualTo(JobResult.Passed);
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").getState()).isEqualTo(JobState.Completed);
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").getOriginalJobId()).isEqualTo(oldStage.getJobInstances().getByName("foo2").getId());
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").isRerun()).isFalse();
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").isCopy()).isTrue();

        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").getResult()).isEqualTo(JobResult.Unknown);
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").getState()).isEqualTo(JobState.Scheduled);
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").getOriginalJobId()).isNull();
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").isRerun()).isTrue();
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").isCopy()).isFalse();


        assertThat(loadedLatestStage).isEqualTo(newStage);
        assertThat(loadedLatestStage.hasRerunJobs()).isTrue();
        assertThat(loadedLatestStage.getCounter()).isEqualTo(oldStage.getCounter() + 2);
        assertThat(loadedLatestStage.getIdentifier().getPipelineCounter()).isEqualTo(oldStage.getIdentifier().getPipelineCounter());
        assertThat(result.canContinue()).isTrue();
    }

    @Test
    public void shouldConsiderCopyOfRerunJobACopyAndNotRerun() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();

        Stage stage = scheduleService.rerunStage(pipeline.getName(), pipeline.getCounter(), pipelineFixture.devStage);

        assertThat(stage.hasRerunJobs()).isFalse();

        dbHelper.passStage(stage);

        Stage oldStage = stageDao.stageById(pipeline.getStages().byName(pipelineFixture.devStage).getId());

        assertThat(oldStage.hasRerunJobs()).isFalse();

        HttpOperationResult result = new HttpOperationResult();
        Stage newStage = scheduleService.rerunJobs(oldStage, List.of("foo", "foo3"), result);
        Stage loadedLatestStage = dbHelper.getStageDao().findStageWithIdentifier(newStage.getIdentifier());

        assertThat(loadedLatestStage.hasRerunJobs()).isTrue();

        dbHelper.passStage(loadedLatestStage);
        JobInstances jobsBeforeLatestRerunJobs = loadedLatestStage.getJobInstances();

        newStage = scheduleService.rerunJobs(loadedLatestStage, List.of("foo2"), result);

        loadedLatestStage = dbHelper.getStageDao().findStageWithIdentifier(newStage.getIdentifier());

        assertThat(loadedLatestStage.getJobInstances().getByName("foo").getResult()).isEqualTo(JobResult.Passed);
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").getState()).isEqualTo(JobState.Completed);
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").getOriginalJobId()).isEqualTo(jobsBeforeLatestRerunJobs.getByName("foo").getId());
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").isRerun()).isFalse();
        assertThat(loadedLatestStage.getJobInstances().getByName("foo").isCopy()).isTrue();

        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").getResult()).isEqualTo(JobResult.Unknown);
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").getState()).isEqualTo(JobState.Scheduled);
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").getOriginalJobId()).isNull();
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").isRerun()).isTrue();
        assertThat(loadedLatestStage.getJobInstances().getByName("foo2").isCopy()).isFalse();

        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").getResult()).isEqualTo(JobResult.Passed);
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").getState()).isEqualTo(JobState.Completed);
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").getOriginalJobId()).isEqualTo(jobsBeforeLatestRerunJobs.getByName("foo3").getId());
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").isRerun()).isFalse();
        assertThat(loadedLatestStage.getJobInstances().getByName("foo3").isCopy()).isTrue();

        assertThat(loadedLatestStage).isEqualTo(newStage);
        assertThat(loadedLatestStage.hasRerunJobs()).isTrue();
        assertThat(loadedLatestStage.getCounter()).isEqualTo(oldStage.getCounter() + 3);
        assertThat(loadedLatestStage.getIdentifier().getPipelineCounter()).isEqualTo(oldStage.getIdentifier().getPipelineCounter());
        assertThat(result.canContinue()).isTrue();
    }

    @Test
    public void shouldFailRerunWhenJobConfigDoesNotExist() {
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStageScheduled();
        Stage oldStage = pipeline.getStages().byName(pipelineFixture.devStage);
        dbHelper.pass(pipeline);

        configHelper.removeJob(pipeline.getName(), pipelineFixture.devStage, "foo3");

        HttpOperationResult result = new HttpOperationResult();

        Stage newStage = scheduleService.rerunJobs(oldStage, List.of("foo3"), result);

        assertThat(result.canContinue()).isFalse();
        assertThat(result.message()).contains("Cannot rerun job 'foo3'. Configuration for job doesn't exist.");
        assertThat(newStage).isNull();
    }

    @Test
    public void shouldRerunJobsWithUserAsApprover() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        Stage oldStage = pipeline.getStages().byName(pipelineFixture.devStage);

        SessionUtilsHelper.loginAs("looser");
        HttpOperationResult result = new HttpOperationResult();
        Stage newStage = scheduleService.rerunJobs(oldStage, List.of("foo", "foo3"), result);
        Stage loadedLatestStage = dbHelper.getStageDao().findStageWithIdentifier(newStage.getIdentifier());
        assertThat(loadedLatestStage.getApprovedBy()).isEqualTo("looser");
        assertThat(oldStage.getApprovedBy()).isNotEqualTo("looser");
        assertThat(result.canContinue()).isTrue();
    }

    @Test
    public void shouldFailWhenStageAlreadyActive() {
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStageScheduled();
        Stage oldStage = pipeline.getStages().byName(pipelineFixture.devStage);

        HttpOperationResult result = new HttpOperationResult();
        Stage newStage = scheduleService.rerunJobs(oldStage, List.of("foo", "foo3"), result);

        assertThat(result.canContinue()).isFalse();
        assertThat(result.message()).contains("Pipeline[name='" + pipeline.getName() + "', counter='" + pipeline.getCounter() + "', label='" + pipeline.getLabel() + "'] is still in progress");
        assertThat(result.message()).contains("Cannot schedule");
        assertThat(newStage).isNull();
    }

    @Test
    public void shouldNotRunStageIfItsPreviousStageHasNotBeenRun() {
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        String theThirdStage = pipelineFixture.stageName(3);
        try {
            scheduleService.rerunStage(pipeline.getName(), pipeline.getCounter(), theThirdStage);
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo(String.format(
                    "Can not run stage [%s] in pipeline [%s] because its previous stage has not been run.",
                    theThirdStage, pipeline.getName()));
        }
    }

    @Test
    public void shouldNotScheduleAStageIfAnyStageForThatPipelineIsAlreadyRunning() throws Exception {
        pipelineFixture.createdPipelineWithAllStagesPassed();
        final List<Exception> exceptions = new ArrayList<>();
        Thread t1 = new Thread(rerunStage(exceptions, pipelineFixture.devStage));
        Thread t2 = new Thread(rerunStage(exceptions, pipelineFixture.ftStage));

        t1.start();
        t2.start();

        t1.join();
        t2.join();
        assertThat(exceptions.size()).isEqualTo(1);
    }

    private Runnable rerunStage(final List<Exception> exceptions, final String ftStage) {
        return () -> {
            try {
                scheduleService.rerunStage(pipelineFixture.pipelineName, pipelineFixture.pipelineCounter(), ftStage);
            } catch (Exception e) {
                exceptions.add(e);
            }
        };
    }
}
