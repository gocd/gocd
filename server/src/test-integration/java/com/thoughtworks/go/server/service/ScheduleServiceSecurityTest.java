/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static javax.servlet.http.HttpServletResponse.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ScheduleServiceSecurityTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private ScheduleService scheduleService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PipelineWithTwoStages fixture;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Before
    public void setUp() throws Exception {
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);

        dbHelper.onSetUp();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, temporaryFolder);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        fixture.onTearDown();
    }

    @Test
    public void shouldReturnAppropriateHttpResultIfUserDoesNotHaveOperatePermission() throws Exception {
        configHelper.enableSecurity();
        configHelper.addAdmins("admin");
        configHelper.setOperatePermissionForGroup("defaultGroup", "jez");
        Pipeline pipeline = fixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        Username anonymous = new Username(new CaseInsensitiveString("anonymous"));
        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        Stage resultStage = scheduleService.cancelAndTriggerRelevantStages(pipeline.getStages().byName(fixture.ftStage).getId(), anonymous, operationResult);

        assertThat(resultStage, is(nullValue()));
        assertThat(operationResult.isSuccessful(), is(false));
        assertThat(operationResult.httpCode(), is(SC_FORBIDDEN));
    }

    @Test
    public void shouldReturnAppropriateHttpResultIfTheStageIsInvalid() throws Exception {
        configHelper.enableSecurity();
        configHelper.setOperatePermissionForGroup("defaultGroup", "jez");
        Username jez = new Username(new CaseInsensitiveString("jez"));
        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        Stage resultStage = scheduleService.cancelAndTriggerRelevantStages(-23l, jez, operationResult);

        assertThat(resultStage, is(nullValue()));
        assertThat(operationResult.isSuccessful(), is(false));
        assertThat(operationResult.httpCode(), is(SC_NOT_FOUND));
    }

    @Test
    public void shouldNotThrowExceptionIfUserHasOperatePermission() throws Exception {
        configHelper.enableSecurity();
        Username user = SessionUtils.currentUsername();
        configHelper.setOperatePermissionForGroup("defaultGroup", user.getUsername().toString());
        Pipeline pipeline = fixture.createPipelineWithFirstStagePassedAndSecondStageRunning();

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();

        Stage stageForCancellation = pipeline.getStages().byName(fixture.ftStage);
        Stage resultStage = scheduleService.cancelAndTriggerRelevantStages(stageForCancellation.getId(), user, operationResult);

        assertThat(resultStage, is(not(nullValue())));
        assertThat(operationResult.isSuccessful(), is(true));
        assertThat(operationResult.httpCode(), is(SC_OK));
        //TODO: Check why stage result is not persisted after stage is cancelled
//        Stage mostRecent = stageDao.mostRecentStage(new StageConfigIdentifier(fixture.pipelineName, fixture.ftStage));
//        assertThat(mostRecent.getResult(), is(StageResult.Cancelled));
    }

}
