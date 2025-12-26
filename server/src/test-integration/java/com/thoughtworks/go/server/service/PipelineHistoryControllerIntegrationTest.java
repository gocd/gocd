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
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.fixture.PipelineWithMultipleStages;
import com.thoughtworks.go.server.controller.PipelineHistoryController;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.SessionUtilsHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ClearSingleton.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PipelineHistoryControllerIntegrationTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoConfigService goConfigService;
    @Autowired private PipelineHistoryController controller;
    @Autowired private ScheduleHelper scheduleHelper;
	@Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private PipelineWithMultipleStages pipelineFixture;
    private HttpServletResponse response;
    private HttpServletRequest request;
    private final GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        pipelineFixture = new PipelineWithMultipleStages(3, materialRepository, transactionTemplate, tempDir);
        configHelper.usingCruiseConfigDao(goConfigDao);
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        goConfigService.forceNotifyListeners();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    public void teardown() throws Exception {
        pipelineFixture.onTearDown();
    }

    @Test
    public void shouldHaveGroupsInJson() {
        pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        Map<String, Object> jsonMap = requestPipelineHistoryPage();
        List<?> groups = (List<?>) jsonMap.get("groups");
        assertThat(groups.size()).isEqualTo(1);
    }

    @Test
    public void canForceShouldBeFalseForUnauthorizedAccess() {
        configHelper.addSecurityWithAdminConfig();
        pipelineFixture.configStageAsManualApprovalWithApprovedUsers(pipelineFixture.devStage, "user");

        pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();

        Map<String, Object> jsonMap = requestPipelineHistoryPage();
        assertThat(getItemInJson(jsonMap, "canForce")).isEqualTo("false");
    }

    @Test
    public void canForceShouldBeTrueForAuthorizedUser() {
        configHelper.addSecurityWithAdminConfig();
        pipelineFixture.configStageAsManualApprovalWithApprovedUsers(pipelineFixture.devStage, "userA");

        SessionUtilsHelper.loginAs("userA");
        Map<String, Object> jsonMap = requestPipelineHistoryPage();
        assertThat(getItemInJson(jsonMap, "canForce")).isEqualTo("true");
    }

    @Test
    public void canPauseShouldBeFalseForUnauthorizedAccess() {
        configHelper.addSecurityWithAdminConfig();
        configHelper.setOperatePermissionForGroup("defaultGroup", "jez");

        Map<String, Object> jsonMap = requestPipelineHistoryPage();
        assertThat(getItemInJson(jsonMap, "canPause")).isEqualTo("false");
    }

    @Test
    public void canPauseShouldBeTrueForAuthorizedAccess() {
        configHelper.addSecurityWithAdminConfig();
        configHelper.setOperatePermissionForGroup("defaultGroup", CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()));

        Map<String, Object> jsonMap = requestPipelineHistoryPage();
        assertThat(getItemInJson(jsonMap, "canPause")).isEqualTo("true");
    }

    @Test
    public void hasModificationShouldBeTrueIfThereIsBuildCauseInBuffer() throws Exception {
        pipelineFixture.createNewCheckin();
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(pipelineFixture.pipelineName);
        Map<String, Object> jsonMap = requestPipelineHistoryPage();
        assertThat(getItemInJson(jsonMap, "showForceBuildButton")).isEqualTo("true");
    }

    @Test
    public void shouldCreateGroupIfPipelineHasModificationEvenNoPipelineHistory() throws Exception {
        pipelineFixture.createNewCheckin();
        scheduleHelper.autoSchedulePipelinesWithRealMaterials(pipelineFixture.pipelineName);
        Map<String, Object> jsonMap = requestPipelineHistoryPage();
        List<?> groups = (List<?>) jsonMap.get("groups");
        assertThat(groups.size()).isEqualTo(1);
    }

    private String getItemInJson(Map<String, Object> jsonMap, String key) {
        return (String) jsonMap.get(key);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requestPipelineHistoryPage() {
        ModelAndView modelAndView = controller.list(pipelineFixture.pipelineName, 10, 0, null, response);
        return (Map<String, Object>) modelAndView.getModel().get("json");
    }

}
