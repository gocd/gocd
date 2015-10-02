/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.presentation.models.JobDetailPresentationModel;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.GoConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class JobControllerIntegrationTest {

    private JobController controller;
    private MockHttpServletResponse response;
    private MockHttpServletRequest request;
    @Autowired private JobInstanceService jobInstanceService;
    @Autowired private JobDetailService jobDetailService;
    @Autowired private GoConfigService goConfigService;
    @Autowired private PipelineService pipelineService;
    @Autowired private RestfulService restfulService;
    @Autowired private ArtifactsService artifactService;
    @Autowired private PropertiesService propertiesService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private StageService stageService;
    @Autowired private Localizer localizer;
    @Autowired private TransactionTemplate transactionTemplate;

    private GoConfigFileHelper configHelper;
    private PipelineWithTwoStages fixture;

    @Before
    public void setUp() throws Exception {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao);
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        controller = new JobController(jobInstanceService, jobDetailService, goConfigService,
                pipelineService, restfulService, artifactService, propertiesService, stageService, localizer);
    }

    @After
    public void tearDown() throws Exception {
        fixture.onTearDown();
    }

    @Test
    public void shouldSupportPipelineCounter() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage stage = pipeline.getFirstStage();
        JobInstance job = stage.getFirstJob();
        ModelAndView modelAndView = controller.jobDetail(pipeline.getName(), String.valueOf(pipeline.getCounter()),
                stage.getName(), String.valueOf(stage.getCounter()), job.getName());
        assertThat(presenter(modelAndView).getBuildLocator(), is(job.getIdentifier().buildLocator()));
    }

    @Test
    public void shouldSupportLatest() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage stage = pipeline.getFirstStage();
        JobInstance job = stage.getFirstJob();
        ModelAndView modelAndView = controller.jobDetail(pipeline.getName(), JobIdentifier.LATEST,
                stage.getName(), JobIdentifier.LATEST, job.getName());
        assertThat(presenter(modelAndView).getBuildLocator(), is(job.getIdentifier().buildLocator()));
    }

    @Test
    public void shouldReturnErrorMessageWhenFailedToFindJob() throws Exception {
        try {
            controller.jobDetail(fixture.pipelineName, "1", fixture.devStage, "1", "invalid-job");
        } catch (Exception e) {
            ModelAndView modelAndView = controller.handle(request, response, e);
            assertThat((String) modelAndView.getModel().get(GoConstants.ERROR_FOR_PAGE),
                    containsString("invalid-job not found"));
        }
    }

    @Test
    public void shouldFindJobByPipelineCounterEvenMultiplePipelinesHaveSameLabel() throws Exception {
        fixture.configLabelTemplateUsingMaterialRevision();
        Pipeline oldPipeline = fixture.createdPipelineWithAllStagesPassed();
        Pipeline newPipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage stage = oldPipeline.getFirstStage();
        JobInstance job = stage.getFirstJob();
        ModelAndView modelAndView = controller.jobDetail(oldPipeline.getName(),
                String.valueOf(oldPipeline.getCounter()),
                stage.getName(), String.valueOf(stage.getCounter()), job.getName());
        assertThat(presenter(modelAndView).getBuildLocator(), is(job.getIdentifier().buildLocator()));
    }

    @Test
    public void shouldCreateJobPresentationModelWithRightStage() throws Exception {
        controller = new JobController(jobInstanceService, jobDetailService, goConfigService,
                pipelineService, restfulService, artifactService, propertiesService, stageService, localizer);
        fixture.configLabelTemplateUsingMaterialRevision();
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage devStage = pipeline.getStages().byName("dev");
        JobInstance job = devStage.getFirstJob();

        ModelAndView modelAndView = controller.jobDetail(pipeline.getName(), String.valueOf(pipeline.getCounter()), devStage.getName(), String.valueOf(devStage.getCounter()), job.getName());

        assertThat(presenter(modelAndView).getStageLocator(), is(devStage.stageLocator()));
        assertThat(presenter(modelAndView).getStage(), is(devStage));
    }

    private JobDetailPresentationModel presenter(ModelAndView modelAndView) {
        return (JobDetailPresentationModel) modelAndView.getModel().get("presenter");
    }

}
