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

import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.fixture.PipelineWithMultipleStages;
import com.thoughtworks.go.server.controller.StageController;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.web.ResponseCodeView;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.LogFixture;
import org.apache.log4j.Level;
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
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:WEB-INF/spring-rest-servlet.xml"
})
public class StageControllerIntegrationTest {
    @Autowired private PipelineService pipelineService;
    @Autowired private StageController controller;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private PipelineWithMultipleStages fixture;
    private LogFixture logFixture;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private GoConfigFileHelper configHelper;

    @Before
    public void setUp() throws Exception {
        fixture = new PipelineWithMultipleStages(3, materialRepository, transactionTemplate);
        this.configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        logFixture = LogFixture.startListening(Level.INFO);
    }

    @After
    public void teardown() throws Exception {
        fixture.onTearDown();
        logFixture.stopListening();
        configHelper.onTearDown();
    }

    @Test
    public void shouldRunStageIfItHasNotBeenRun() throws Exception {
        Pipeline pipeline = fixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        ModelAndView mav = controller.rerunStage(fixture.pipelineName, pipeline.getLabel(), fixture.ftStage);
        assertThat(((ResponseCodeView) mav.getView()).getStatusCode(), is(HttpServletResponse.SC_OK));
        Pipeline newPipeline = pipelineService.fullPipelineById(pipeline.getId());
        assertThat("Should run " + fixture.ftStage, newPipeline.getStages().hasStage(fixture.ftStage), is(true));
    }

    @Test
    public void shouldReturn404WhenReRunningNonExistantStage() throws Exception {
        Pipeline pipeline = fixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        ModelAndView mav = controller.rerunStage(fixture.pipelineName, pipeline.getLabel(), "doesNotExist");
        ResponseCodeView codeView = (ResponseCodeView) mav.getView();
        assertThat(codeView.getStatusCode(), is(HttpServletResponse.SC_NOT_FOUND));
        assertThat(codeView.getContent(), is("Stage 'doesNotExist' not found in pipeline '"  + fixture.pipelineName + "'"));
    }

    @Test
    public void shouldNotRunStageIfPreviousStageHasNotBeenRun() throws Exception {
        Pipeline pipeline = fixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        String thirdStage = fixture.stageName(3);
        ModelAndView mav = controller.rerunStage(fixture.pipelineName, pipeline.getLabel(), thirdStage);
        Pipeline newPipeline = pipelineService.fullPipelineById(pipeline.getId());
        assertThat("Should not run " + thirdStage, newPipeline.getStages().hasStage(thirdStage), is(false));
        assertThat(((ResponseCodeView) mav.getView()).getStatusCode(), is(HttpServletResponse.SC_BAD_REQUEST));
    }

    @Test
    public void shouldGiveFriendlyErrorMessageForRerun() {
        ModelAndView modelAndView = controller.rerunStage("pipeline", "invalid-label", "stage");
        ResponseCodeView codeView = (ResponseCodeView) modelAndView.getView();
        assertThat(codeView.getStatusCode(), is(HttpServletResponse.SC_BAD_REQUEST));
        assertThat(codeView.getContent(), is("Stage [pipeline/invalid-label/stage] not found"));
    }

}
