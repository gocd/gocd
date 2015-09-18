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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.CachedCurrentActivityService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.XmlUtils;
import com.thoughtworks.go.util.json.Json;
import com.thoughtworks.go.util.json.JsonList;
import com.thoughtworks.go.util.json.JsonMap;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.jdom.Document;
import org.jdom.xpath.XPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/spring-tabs-servlet.xml",
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})

public class PipelineStatusControllerTest {
    @Autowired private PipelineStatusController pipelineStatusController;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private GoConfigService configService;
	@Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    
    private String pipelineName1 = "connectfour";
    private String pipelineName2 = "connectfive";
    private static final String STAGE1 = "dev";
    private static final String STAGE2 = "qa";
    private GoConfigFileHelper configHelper;
    private String buildName = "unit";
    @Autowired private CachedCurrentActivityService currentActivityService;
    private PipelineWithTwoStages pipelineFixture;
    public JsonMap map;


    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();

        configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();

        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();

        configService.forceNotifyListeners();
    }

    @After
    public void teardown() throws Exception {
        pipelineFixture.onTearDown();
        dbHelper.onTearDown();

    }

    @Test
    public void shouldReturnCorrectContextPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/go");

        request.setRequestURI("/go/tab/cruise/cctray.xml");
        assertThat(pipelineStatusController.getFullContextPath(request), is("http://localhost:80/go"));

        request.setServerName("go-server");
        request.setServerPort(8153);
        request.setRequestURI("/go/cctray.xml");
        assertThat(pipelineStatusController.getFullContextPath(request), is("http://go-server:8153/go"));
    }

    @Test
    public void shouldReturnEmptyArrayWhenNoPipelines() throws Exception {
        ModelAndView modelAndView = pipelineStatusController.list(null, false, new MockHttpServletResponse(),
                new MockHttpServletRequest());
        JsonList pipelinesJsonList = getPipelinesJsonFromModel(modelAndView);
        final JsonList emptyArray = new JsonList();
        assertTrue(pipelinesJsonList.contains(emptyArray));
    }

    @Test
    public void shouldReturnJsonWith404WhenNoPipelineFoundWithGivingName() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest req = new MockHttpServletRequest();
        pipelineStatusController.list("not-Exist-Pipeline", false, response, req);
        assertThat(response.getStatus(), is(HttpServletResponse.SC_NOT_FOUND));
    }

    @Test
    public void shouldContainMostRecentStageInPipelineJson() throws Exception {
        configHelper.onSetUp();
        configHelper.addPipeline(pipelineName1, STAGE1, buildName);
        configHelper.addPipeline(pipelineName2, STAGE2, buildName);

        Stage stage1InPipeline1 = StageMother.passedStageInstance(STAGE1, buildName, "pipeline-name");
        final Pipeline pipeline1 = PipelineMother.pipeline(pipelineName1, stage1InPipeline1);
        savePipelineWithStages(pipeline1);

        Stage stage2InPipeline1 = StageMother.passedStageInstance(STAGE2, buildName, "pipeline-name");
        Pipeline pipeline2 = PipelineMother.pipeline(pipelineName2, stage2InPipeline1);
        savePipelineWithStages(pipeline2);

        ModelAndView modelAndView = pipelineStatusController.list(null, false, new MockHttpServletResponse(),
                new MockHttpServletRequest());
        JsonList pipelinesJsonList = getPipelinesJsonFromModel(modelAndView);

        assertThat(pipelinesJsonList, containsPipeline(pipeline1));
        assertThat(pipelinesJsonList, containsPipeline(pipeline2));

        final JsonList stagesJson1 = ((JsonMap) pipelinesJsonList.get(1)).getJsonList("stages");
        assertThat(stagesJson1, containsStage(pipeline1.getStages().byName(STAGE1)));

        final JsonList stagesJson2 = ((JsonMap) pipelinesJsonList.get(0)).getJsonList("stages");
        assertThat(stagesJson2, containsStage(pipeline2.getStages().byName(STAGE2)));
    }

    private void savePipelineWithStages(final Pipeline pipeline1) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.save(pipeline1.getBuildCause().getMaterialRevisions());
                pipelineDao.saveWithStages(pipeline1);
            }
        });
    }

    @Test
    public void shouldReturnErrorIfTriggerPipelineWithoutName() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/force");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView modelAndView = pipelineStatusController.handleError(request, response, new Exception());
        String json = modelAndView.getModelMap().get("json").toString();
        assertThat(json, containsString("Cannot schedule: missed pipeline name"));
    }

    @Test
    public void shouldReturnErrorIfException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("pausePipeline.json");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView modelAndView = pipelineStatusController.handleError(request, response, new Exception("pause error"));
        String json = modelAndView.getModelMap().get("json").toString();
        assertThat(json, containsString("pause error"));
    }

    private void assertHasProject(Document document, String projectName) throws Exception {
        List devStageProjects = XPath.selectNodes(document,
                String.format("/Projects/Project[@name=\"%s\"]", projectName));
        assertThat(projectName + " should be in cctray feed \nActual xml: \n" + XmlUtils.asXml(document),
                devStageProjects.size(), Matchers.is(1));
    }

    private JsonList getPipelinesJsonFromModel(ModelAndView modelAndView) {
        JsonMap jsonMap = (JsonMap) modelAndView.getModel().get("json");
        return (JsonList) jsonMap.get("pipelines");
    }

    private TypeSafeMatcher<JsonList> containsPipeline(final Pipeline pipeline) {

        return new TypeSafeMatcher<JsonList>() {

            public boolean matchesSafely(JsonList pipelineList) {
                for (Json json : pipelineList) {
                    JsonMap map = (JsonMap) json;
                    if (map.hasEntry("name", pipeline.getName())) {
                        return true;

                    }
                }
                return false;
            }

            public void describeTo(Description description) {
                description.appendText(pipeline.getName() + " does not exist in the json");
            }
        };
    }

    private TypeSafeMatcher<JsonList> containsStage(Stage stage) {
        final long id = stage.getId();
        final String stageName = stage.getName();

        return new TypeSafeMatcher<JsonList>() {
            public boolean matchesSafely(JsonList jsonList) {
                for (Json json : jsonList) {
                    map = (JsonMap) json;
                    if (map.hasEntry("id", String.valueOf(id))
                            && map.hasEntry("stageName", stageName)) {
                        return true;
                    }
                }
                return false;
            }

            public void describeTo(Description description) {
                description.appendText("to contain stage with: ");
                description.appendText("stageName=" + stageName);
                description.appendText(", id=" + id);
                description.appendText(" but id=" + map.get("id"));
                description.appendText(" and stageName=" + map.get("stageName"));
            }
        };
    }
}
