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

import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.DefaultSchedulingContext;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeProvider;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class RestfulServiceTest {
    @Autowired private RestfulService restfulService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoConfigDao cruiseConfigDao;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private StageDao stageDao;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private InstanceFactory instanceFactory;

    private PipelineWithTwoStages fixture;
    private GoConfigFileHelper configHelper ;

    @Before
    public void setUp() throws Exception {
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        configHelper = new GoConfigFileHelper().usingCruiseConfigDao(cruiseConfigDao);
        configHelper.onSetUp();
        dbHelper.onSetUp();
        fixture.usingThreeJobs();
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        fixture.onTearDown();
    }

    @Test
    public void shouldShouldTranslateLatestPipelineLabel() throws Exception {
        fixture.createdPipelineWithAllStagesPassed();
        Pipeline latestPipleine = fixture.createdPipelineWithAllStagesPassed();
        final JobIdentifier jobIdentifier1 = new JobIdentifier(latestPipleine.getName(), JobIdentifier.LATEST, fixture.devStage, JobIdentifier.LATEST, PipelineWithTwoStages.JOB_FOR_DEV_STAGE);
        JobIdentifier jobIdentifier = restfulService.findJob(jobIdentifier1.getPipelineName(), jobIdentifier1.getPipelineLabel(), jobIdentifier1.getStageName(), jobIdentifier1.getStageCounter(), jobIdentifier1.getBuildName());
        assertThat(jobIdentifier.getPipelineLabel(), is(latestPipleine.getLabel()));
    }

    @Test
    public void shouldTranslateLatestToRealPipelineLabel() throws Exception {
        fixture.createdPipelineWithAllStagesPassed();
        Pipeline latestPipleine = fixture.createdPipelineWithAllStagesPassed();
        JobIdentifier jobIdentifier = restfulService.findJob(latestPipleine.getName(), JobIdentifier.LATEST, fixture.devStage, JobIdentifier.LATEST, PipelineWithTwoStages.JOB_FOR_DEV_STAGE);
        assertThat(jobIdentifier.getPipelineLabel(), is(latestPipleine.getLabel()));
    }


    @Test
    public void shouldTranslateLatestToRealStageCounter() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();

        JobIdentifier jobIdentifier = restfulService.findJob(pipeline.getName(), pipeline.getLabel(), fixture.devStage, JobIdentifier.LATEST, PipelineWithTwoStages.JOB_FOR_DEV_STAGE);
        assertThat(Integer.valueOf(jobIdentifier.getStageCounter()), is(pipeline.getStages().byName(fixture.devStage).getCounter()));
    }

    @Test
    public void shouldTranslateEmtpyToLatestStageCounter() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        JobIdentifier jobIdentifier = restfulService.findJob(pipeline.getName(), pipeline.getLabel(), fixture.devStage, "", PipelineWithTwoStages.JOB_FOR_DEV_STAGE);
        assertThat(Integer.valueOf(jobIdentifier.getStageCounter()), is(pipeline.getStages().byName(fixture.devStage).getCounter()));
    }

    @Test
    public void canSupportQueryingUsingPipelineNameWithDifferentCase() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();

        JobIdentifier jobIdentifier = restfulService.findJob(pipeline.getName().toUpperCase(), JobIdentifier.LATEST, fixture.devStage, "", PipelineWithTwoStages.JOB_FOR_DEV_STAGE);

        assertThat(jobIdentifier.getPipelineName(), is(pipeline.getName()));
    }

    @Test
    public void canSupportQueryingUsingStageNameWithDifferentCase() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();

        JobIdentifier jobIdentifier = restfulService.findJob(pipeline.getName(), pipeline.getLabel(), fixture.devStage.toUpperCase(), "", PipelineWithTwoStages.JOB_FOR_DEV_STAGE);

        assertThat(jobIdentifier.getStageName(), is(fixture.devStage));
    }

    @Ignore("Fix as a part of #3335 - case sensitivity needs to be fixed much wider than this")
    @Test
    public void canSupportQueryingUsingJobNameWithDifferentCase() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        final JobIdentifier jobIdentifier1 = new JobIdentifier(pipeline.getName(), JobIdentifier.LATEST, fixture.devStage, "", PipelineWithTwoStages.JOB_FOR_DEV_STAGE);

        JobIdentifier jobIdentifier = restfulService.findJob(jobIdentifier1.getPipelineName(), jobIdentifier1.getPipelineLabel(), jobIdentifier1.getStageName(), jobIdentifier1.getStageCounter(), jobIdentifier1.getBuildName().toUpperCase());

        assertThat(jobIdentifier.getBuildName(), Is.is(PipelineWithTwoStages.JOB_FOR_DEV_STAGE));
    }

    @Test
    public void shouldFindJobByPipelineCounter() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage stage = pipeline.getStages().byName(fixture.devStage);
        JobInstance job = stage.getJobInstances().first();

        JobIdentifier result = restfulService.findJob(pipeline.getName(), String.valueOf(pipeline.getCounter()), stage.getName(), String.valueOf(stage.getCounter()), job.getName(), job.getId());
        JobIdentifier expect = new JobIdentifier(pipeline, stage, job);
        assertThat(result, is(expect));
    }

    @Test
    public void shouldFindOriginalWhenJobCopiedForRerun() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage stage = pipeline.getStages().byName(fixture.devStage);
        JobInstance job = stage.findJob(PipelineWithTwoStages.JOB_FOR_DEV_STAGE);
        Stage rerunStage = instanceFactory.createStageForRerunOfJobs(stage, a(PipelineWithTwoStages.DEV_STAGE_SECOND_JOB), new DefaultSchedulingContext("loser", new Agents()),
                fixture.pipelineConfig().getStage(
                        new CaseInsensitiveString(fixture.devStage)), new TimeProvider(), "md5");
        stageDao.saveWithJobs(pipeline, rerunStage);
        dbHelper.passStage(rerunStage);

        JobIdentifier result = restfulService.findJob(pipeline.getName(), String.valueOf(pipeline.getCounter()), stage.getName(), String.valueOf(rerunStage.getCounter()), job.getName());
        JobIdentifier expect = new JobIdentifier(pipeline, stage, job);
        assertThat(result, is(expect));

        long copiedJobId = rerunStage.getJobInstances().getByName(job.getName()).getId();
        assertThat(copiedJobId, is(not(job.getId())));//sanity check(its a copy, not the same)

        result = restfulService.findJob(pipeline.getName(), String.valueOf(pipeline.getCounter()), stage.getName(), String.valueOf(rerunStage.getCounter()), job.getName());
        assertThat(result, is(expect));//still, the job identifier returned should be the same(because other one was a copy)

        result = restfulService.findJob(pipeline.getName(), String.valueOf(pipeline.getCounter()), stage.getName(), String.valueOf(rerunStage.getCounter()), job.getName(), copiedJobId);
        assertThat(result, is(not(expect)));//since caller knows the buildId, honor it(caller knows what she is doing)
        assertThat(result, is(new JobIdentifier(rerunStage.getIdentifier(), job.getName(), copiedJobId)));
    }

    @Test
    public void shouldReturnJobWithJobIdWhenSpecifyPipelineCounter() throws Exception {
        configHelper.setPipelineLabelTemplate(fixture.pipelineName, "label-${COUNT}");
        Pipeline oldPipeline = fixture.createdPipelineWithAllStagesPassed();
        fixture.createdPipelineWithAllStagesPassed();

        Stage stage = oldPipeline.getStages().byName(fixture.devStage);
        JobInstance job = stage.getJobInstances().first();

        JobIdentifier result = restfulService.findJob(oldPipeline.getName(), String.valueOf(oldPipeline.getCounter()), stage.getName(), String.valueOf(stage.getCounter()), job.getName(), null);
        JobIdentifier expect = new JobIdentifier(oldPipeline, stage, job);
        assertThat(result, is(expect));
    }

    @Test
    public void shouldReturnJobWithLabelWhenSpecifyPipelineLabel() throws Exception {
        configHelper.setPipelineLabelTemplate(fixture.pipelineName, "label-${COUNT}");
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage stage = pipeline.getStages().byName(fixture.devStage);
        JobInstance job = stage.getJobInstances().first();

        JobIdentifier result = restfulService.findJob(pipeline.getName(), pipeline.getLabel(),
                stage.getName(), String.valueOf(stage.getCounter()), job.getName(), job.getId());
        JobIdentifier expect = new JobIdentifier(pipeline, stage, job);
        assertThat(result, is(expect));
    }

    @Test
    public void shouldReturnLatestJobWhenMultiplePipelinesWithSameLabel() throws Exception {
        configHelper.setPipelineLabelTemplate(fixture.pipelineName, "label-${COUNT}");
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Pipeline newPipeline = createPipelineWithSameLabelButNoCounter(pipeline);

        Stage stage = newPipeline.getStages().byName(fixture.devStage);
        JobInstance job = stage.getJobInstances().first();

        JobIdentifier result = restfulService.findJob(newPipeline.getName(), newPipeline.getLabel(),
                stage.getName(), String.valueOf(stage.getCounter()), job.getName(), null);
        JobIdentifier expect = new JobIdentifier(pipeline, stage, job);
        assertThat(result, is(expect));
    }

    @Test
    public void shouldTranslateLatestStageCounter() {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        StageIdentifier stageIdentifier = restfulService.translateStageCounter(pipeline.getIdentifier(),
                fixture.devStage, "latest");
        assertThat(stageIdentifier, is(new StageIdentifier(pipeline, pipeline.getStages().byName(fixture.devStage))));
    }

    private Pipeline createPipelineWithSameLabelButNoCounter(Pipeline pipeline) {
        pipeline.setCounter(null);
        return dbHelper.save(pipeline);
    }

}
