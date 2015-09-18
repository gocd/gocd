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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.fixture.PipelineWithMultipleStages;
import com.thoughtworks.go.server.controller.PipelineHistoryController;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.json.JsonList;
import com.thoughtworks.go.util.json.JsonMap;
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

import static com.thoughtworks.go.fixture.IntegrationTestsFixture.login;
import static com.thoughtworks.go.fixture.IntegrationTestsFixture.resetSecurityContext;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:WEB-INF/spring-rest-servlet.xml"
})
public class PipelineHistoryControllerIntegrationTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoConfigService goConfigService;
    @Autowired private PipelineHistoryController controller;
    @Autowired private ScheduleHelper scheduleHelper;
	@Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    
    private PipelineWithMultipleStages fixture;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Before
    public void setUp() throws Exception {
        fixture = new PipelineWithMultipleStages(3, materialRepository, transactionTemplate);
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        dbHelper.onSetUp();
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        goConfigService.forceNotifyListeners();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        fixture.onTearDown();
        configHelper.onTearDown();
        resetSecurityContext();
    }

    @Test
    public void shouldHaveGroupsInJson() throws Exception {
        fixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        JsonMap jsonMap = requestPipelineHistoryPage();
        JsonList groups = jsonMap.getJsonList("groups");
        assertThat(groups.size(), is(1));
    }

    @Test
    public void canForceShouldBeFalseForUnauthorizedAccess() throws Exception {
        configHelper.addSecurityWithAdminConfig();
        fixture.configStageAsManualApprovalWithApprovedUsers(fixture.devStage, "user");

        fixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();

        JsonMap jsonMap = requestPipelineHistoryPage();
        assertThat(getItemInJson(jsonMap, "canForce"), is("false"));
    }

    @Test
    public void canForceShouldBeTrueForAuthorizedUser() throws Exception {
        configHelper.addSecurityWithAdminConfig();
        fixture.configStageAsManualApprovalWithApprovedUsers(fixture.devStage, "userA");

        login("userA", "");
        JsonMap jsonMap = requestPipelineHistoryPage();
        assertThat(getItemInJson(jsonMap, "canForce"), is("true"));
    }

    @Test
    public void canPauseShouldBeFalseForUnauthorizedAccess() throws Exception {
        configHelper.addSecurityWithAdminConfig();
        configHelper.setOperatePermissionForGroup("defaultGroup", "jez");

        JsonMap jsonMap = requestPipelineHistoryPage();
        assertThat(getItemInJson(jsonMap, "canPause"), is("false"));
    }

    @Test
    public void canPauseShouldBeTrueForAuthorizedAccess() throws Exception {
        configHelper.addSecurityWithAdminConfig();
        configHelper.setOperatePermissionForGroup("defaultGroup", CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()));

        JsonMap jsonMap = requestPipelineHistoryPage();
        assertThat(getItemInJson(jsonMap, "canPause"), is("true"));
    }

    @Test
    public void hasModificationShouldBeTrueIfThereIsBuildCauseInBuffer() throws Exception {
        fixture.createNewCheckin();
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(fixture.pipelineName);
        JsonMap jsonMap = requestPipelineHistoryPage();
        assertThat(getItemInJson(jsonMap, "showForceBuildButton"), is("true"));
    }

    @Test
    public void shouldCreateGroupIfPipelineHasModificationEvenNoPipelineHistory() throws Exception {
        fixture.createNewCheckin();
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(fixture.pipelineName);
        JsonMap jsonMap = requestPipelineHistoryPage();
        JsonList groups = jsonMap.getJsonList("groups");
        assertThat("Should create group for the coming pipeline", groups.size(), is(1));
    }

    private String getItemInJson(JsonMap jsonMap, String key) {
        return jsonMap.getJsonString(key).withoutQuote();
    }

    private JsonMap requestPipelineHistoryPage() throws Exception {
        ModelAndView modelAndView = controller.list(fixture.pipelineName, 10, 0, response, request);
        return (JsonMap) modelAndView.getModel().get("json");
    }

}