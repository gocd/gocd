/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.fixture.PipelineWithMultipleStages;
import com.thoughtworks.go.server.controller.StageController;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.web.ResponseCodeView;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml"
})
public class StageControllerIntegrationTest {
    @Autowired private PipelineService pipelineService;
    @Autowired private StageController controller;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PipelineWithMultipleStages fixture;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private GoConfigFileHelper configHelper;

    @Before
    public void setUp() throws Exception {
        fixture = new PipelineWithMultipleStages(3, materialRepository, transactionTemplate, temporaryFolder);
        this.configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.addHeader("Confirm", "true");
    }

    @After
    public void teardown() throws Exception {
        fixture.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldRunStageIfItHasNotBeenRun() throws Exception {
        Pipeline pipeline = fixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        ModelAndView mav = controller.rerunStage(fixture.pipelineName, pipeline.getCounter().toString(), fixture.ftStage, response, request);
        assertThat(((ResponseCodeView) mav.getView()).getStatusCode(), is(HttpServletResponse.SC_OK));
        Pipeline newPipeline = pipelineService.fullPipelineById(pipeline.getId());
        assertThat("Should run " + fixture.ftStage, newPipeline.getStages().hasStage(fixture.ftStage), is(true));
    }

    @Test
    public void shouldReturn404WhenReRunningNonExistantStage() throws Exception {
        Pipeline pipeline = fixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        ModelAndView mav = controller.rerunStage(fixture.pipelineName, pipeline.getCounter().toString(), "doesNotExist", response, request);
        ResponseCodeView codeView = (ResponseCodeView) mav.getView();
        assertThat(codeView.getStatusCode(), is(HttpServletResponse.SC_NOT_FOUND));
        assertThat(codeView.getContent(), is("Stage 'doesNotExist' not found in pipeline '"  + fixture.pipelineName + "'"));
    }

    @Test
    public void shouldNotRunStageIfPreviousStageHasNotBeenRun() throws Exception {
        Pipeline pipeline = fixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        String thirdStage = fixture.stageName(3);
        ModelAndView mav = controller.rerunStage(fixture.pipelineName, pipeline.getCounter().toString(), thirdStage, response, request);
        Pipeline newPipeline = pipelineService.fullPipelineById(pipeline.getId());
        assertThat("Should not run " + thirdStage, newPipeline.getStages().hasStage(thirdStage), is(false));
        assertThat(((ResponseCodeView) mav.getView()).getStatusCode(), is(HttpServletResponse.SC_BAD_REQUEST));
    }

    @Test
    public void shouldGiveFriendlyErrorMessageForRerun() {
        ModelAndView modelAndView = controller.rerunStage("pipeline", "invalid-label", "stage", response, request);
        ResponseCodeView codeView = (ResponseCodeView) modelAndView.getView();
        assertThat(codeView.getStatusCode(), is(HttpServletResponse.SC_BAD_REQUEST));
        assertThat(codeView.getContent(), is("Error while rerunning [pipeline/invalid-label/stage]. Received non-numeric pipeline counter 'invalid-label'."));
    }

    @Test
    public void shouldGiveFriendlyErrorMessageForRerunWhenPipelineCounterIsNotFound() {
        ModelAndView modelAndView = controller.rerunStage("pipeline", "9999", "stage", response, request);
        ResponseCodeView codeView = (ResponseCodeView) modelAndView.getView();
        assertThat(codeView.getStatusCode(), is(HttpServletResponse.SC_NOT_FOUND));
        assertThat(codeView.getContent(), is("Pipeline instance [pipeline/9999] not found"));
    }

    @Test
    public void shouldReturnBadRequestIfRequiredHeadersAreMissing() {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("Confirm", "false");

        ModelAndView modelAndView = controller.rerunStage("pipeline", "invalid-label", "stage", response, mockHttpServletRequest);
        ResponseCodeView codeView = (ResponseCodeView) modelAndView.getView();

        assertThat(codeView.getStatusCode(), is(HttpServletResponse.SC_BAD_REQUEST));
        assertThat(codeView.getContent(), is("Missing required header 'Confirm'"));
    }

    @Test
    public void shouldHandleLatestPipelineCounter(){
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        ModelAndView modelAndView = controller.rerunStage(fixture.pipelineName, "latest", fixture.ftStage, response, request);
        assertThat(((ResponseCodeView) modelAndView.getView()).getStatusCode(), is(HttpServletResponse.SC_OK));
        Pipeline newPipeline = pipelineService.fullPipelineById(pipeline.getId());
        assertThat("Should run " + fixture.ftStage, newPipeline.getStages().hasStage(fixture.ftStage), is(true));
    }
}
