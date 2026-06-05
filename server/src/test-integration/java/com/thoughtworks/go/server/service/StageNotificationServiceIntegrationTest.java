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

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.InMemoryEmailNotificationTopic;
import com.thoughtworks.go.server.messaging.StageNotificationListener;
import com.thoughtworks.go.server.messaging.StageStatusMessage;
import com.thoughtworks.go.server.messaging.StageStatusTopic;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath:/applicationContext-global.xml",
    "classpath:/applicationContext-dataLocalAccess.xml",
    "classpath:/testPropertyConfigurer.xml",
    "classpath:/spring-all-servlet.xml",
})
public class StageNotificationServiceIntegrationTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private UserDao userDao;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private PipelineService pipelineService;
    @Autowired private UserService userService;
    @Autowired private GoConfigService goConfigService;
    @Autowired private StageDao stageDao;
    @Autowired private StageStatusTopic stageStatusTopic;
    @Autowired private ServerConfigService serverConfigService;
    @Autowired private TransactionTemplate transactionTemplate;

    private static final String TEST_MATCHER_USER = "lgao";
    private PipelineWithTwoStages pipelineFixture;

    private StageNotificationListener stageNotificationListener;
    private final InMemoryEmailNotificationTopic inMemoryEmailNotificationTopic = new InMemoryEmailNotificationTopic();
    private final GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private StageService stageService;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {

        stageService = mock(StageService.class);
        StageNotificationService stageNotificationService = new StageNotificationService(pipelineService, userService, inMemoryEmailNotificationTopic, stageService, serverConfigService);
        stageNotificationListener = new StageNotificationListener(stageNotificationService, goConfigService, stageDao, stageStatusTopic);

        configHelper.usingCruiseConfigDao(goConfigDao);

        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        configHelper.enableSecurity();
    }

    @AfterEach
    public void teardown() throws Exception {
        pipelineFixture.onTearDown();
        inMemoryEmailNotificationTopic.reset();
    }

    private Stage mockStageServiceWithStage(Pipeline pipeline) {
        Stage ftStage = pipeline.getStages().byName(pipelineFixture.ftStage);
        when(stageService.findStageWithIdentifier(ftStage.getIdentifier())).thenReturn(ftStage);
        return ftStage;
    }

    @Test
    void shouldOnlySendEmailToMatchedUserFilteredByDb() {
        String jezMail = prepareMatchedUserJezInterestedInOwnCheckinsForAllEvents();
        String jeffMail = prepareDontEmailMeUserJeff();
        prepareOneEmailMeNoAddressUser();

        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        Stage ftStage = mockStageServiceWithStage(pipeline);

        stageNotificationListener.onMessage(new StageStatusMessage(ftStage.getIdentifier(), StageState.Passed, StageResult.Passed));

        String subject = "Stage [" + ftStage.getIdentifier().stageLocator() + "]" + " passed";

        assertThat(inMemoryEmailNotificationTopic.getSubject(jezMail)).isEqualTo(subject);
        assertThat(inMemoryEmailNotificationTopic.getBody(jezMail)).contains("Sent by Go on behalf of jez");

        assertThat(inMemoryEmailNotificationTopic.emailCount(jeffMail)).isEqualTo(0);
    }

    @Test
    void shouldOnlySendEmailToMatchedUserFilteredByMatcherAndEvents() {
        String jezMail = prepareMatchedUserJezInterestedInOwnCheckinsForAllEvents();
        String allEvents = prepareMatchedUserInterestedInAllEventsAllModifications();
        String chrisMail = prepareBadMatcherUserChris();

        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        Stage ftStage = mockStageServiceWithStage(pipeline);

        stageNotificationListener.onMessage(new StageStatusMessage(ftStage.getIdentifier(), StageState.Passed, StageResult.Passed));

        String subject = "Stage [" + ftStage.getIdentifier().stageLocator() + "]" + " passed";

        assertThat(inMemoryEmailNotificationTopic.getSubject(jezMail)).isEqualTo(subject);
        assertThat(inMemoryEmailNotificationTopic.getBody(jezMail)).contains("Sent by Go on behalf of jez");

        assertThat(inMemoryEmailNotificationTopic.emailCount(allEvents)).isEqualTo(1);
        assertThat(inMemoryEmailNotificationTopic.emailCount(chrisMail)).isEqualTo(0);
    }

    private String prepareMatchedUserJezInterestedInOwnCheckinsForAllEvents() {
        return prepareMatchedUserJezInterestedInOwnCheckinsFor(StageEvent.All);
    }

    private String prepareMatchedUserJezInterestedInOwnCheckinsFor(StageEvent event) {
        String jezMail = "jez@cruise.com";
        User jez = new User("jez", TEST_MATCHER_USER, jezMail, true);
        jez.addNotificationFilter(new NotificationFilter(pipelineFixture.pipelineName, pipelineFixture.ftStage, event, true));
        userDao.saveOrUpdate(jez);
        return jezMail;
    }

    private String prepareMatchedUserInterestedInAllEventsAllModifications() {
        String email = "all-modifications@cruise.com";
        User user = new User("all-mods", null, email, true);
        user.addNotificationFilter(new NotificationFilter(pipelineFixture.pipelineName, pipelineFixture.ftStage, StageEvent.All, false));
        userDao.saveOrUpdate(user);
        return email;
    }

    private String prepareBadMatcherUserChris() {
        String chrisMail = "chris@cruise.com";
        User chris = new User("chris", "will not be matched", chrisMail, true);
        chris.addNotificationFilter(new NotificationFilter(pipelineFixture.pipelineName, pipelineFixture.ftStage, StageEvent.All, true));
        userDao.saveOrUpdate(chris);
        return chrisMail;
    }

    private String prepareDontEmailMeUserJeff() {
        String chrisMail = "jeff@cruise.com";
        User chris = new User("jeff", "will not be matched", chrisMail, false);
        chris.addNotificationFilter(new NotificationFilter(pipelineFixture.pipelineName, pipelineFixture.ftStage, StageEvent.All, true));
        userDao.saveOrUpdate(chris);
        return chrisMail;
    }

    private void prepareOneEmailMeNoAddressUser() {
        User chris = new User("noEmailAddress", "will not be matched", null, true);
        chris.addNotificationFilter(new NotificationFilter(pipelineFixture.pipelineName, pipelineFixture.ftStage, StageEvent.All, true));
        userDao.saveOrUpdate(chris);
    }

    @Test
    public void shouldSendEmailWithCancelledInfo() {
        String jezMail = prepareMatchedUserJezInterestedInOwnCheckinsForAllEvents();
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        Stage ftStage = mockStageServiceWithStage(pipeline);
        stageNotificationListener.onMessage(new StageStatusMessage(ftStage.getIdentifier(), StageState.Cancelled, StageResult.Cancelled, new Username(cis("chris"))));

        assertThat(inMemoryEmailNotificationTopic.getBody(jezMail)).contains("The stage was cancelled by chris.");
    }

    @Test
    public void shouldSendEmailWithLink() {
        String jezMail = prepareMatchedUserJezInterestedInOwnCheckinsForAllEvents();

        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        Stage ftStage = mockStageServiceWithStage(pipeline);

        stageNotificationListener.onMessage(new StageStatusMessage(ftStage.getIdentifier(), StageState.Passed, StageResult.Passed));

        String body = inMemoryEmailNotificationTopic.getBody(jezMail);
        assertThat(body.lines()).first(InstanceOfAssertFactories.STRING)
            .matches(String.format("(?m)See details: http://.*:%s/go/pipelines/%s/%s/%s/%s",
                systemEnvironment.getServerPort(),
                pipelineFixture.pipelineName,
                pipeline.getCounter(),
                pipelineFixture.ftStage,
                ftStage.getCounter()));
    }

    @Test
    public void shouldSendEmailWhenStageBreaks() {
        String jezMail = prepareMatchedUserJezInterestedInOwnCheckinsFor(StageEvent.Breaks);

        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        Stage ftStage = mockStageServiceWithStage(pipeline);

        stageNotificationListener.onMessage(new StageStatusMessage(ftStage.getIdentifier(), StageState.Failed, StageResult.Failed));

        String subject = "Stage [" + ftStage.getIdentifier().stageLocator() + "]" + " is broken";

        assertThat(inMemoryEmailNotificationTopic.getSubject(jezMail)).isEqualTo(subject);
        assertThat(inMemoryEmailNotificationTopic.getBody(jezMail)).contains("Sent by Go on behalf of jez");
    }

    @Test
    public void shouldSendEmailWhenStageFixed() {
        String jezMail = prepareMatchedUserJezInterestedInOwnCheckinsFor(StageEvent.Fixed);

        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesCompleted(JobResult.Failed);
        Stage ftStage = mockStageServiceWithStage(pipeline);
        stageNotificationListener.onMessage(new StageStatusMessage(ftStage.getIdentifier(), StageState.Failed, StageResult.Failed));
        stageNotificationListener.onMessage(new StageStatusMessage(ftStage.getIdentifier(), StageState.Passed, StageResult.Passed));

        String subject = "Stage [" + ftStage.getIdentifier().stageLocator() + "]" + " is fixed";

        assertThat(inMemoryEmailNotificationTopic.getSubject(jezMail)).isEqualTo(subject);
        assertThat(inMemoryEmailNotificationTopic.getBody(jezMail)).contains("Sent by Go on behalf of jez");
    }

    @Test
    public void shouldSendEmailWhenStageContinueFail() {
        String jezMail = prepareMatchedUserJezInterestedInOwnCheckinsFor(StageEvent.Fails);

        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesCompleted(JobResult.Failed);
        Stage ftStage = mockStageServiceWithStage(pipeline);
        stageNotificationListener.onMessage(new StageStatusMessage(ftStage.getIdentifier(), StageState.Failed, StageResult.Failed));

        String subject = "Stage [" + ftStage.getIdentifier().stageLocator() + "]" + " failed";

        assertThat(inMemoryEmailNotificationTopic.getSubject(jezMail)).isEqualTo(subject);
        assertThat(inMemoryEmailNotificationTopic.getBody(jezMail)).contains("Sent by Go on behalf of jez");
    }
}
