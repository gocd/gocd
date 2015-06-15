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

import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageEvent;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.testinfo.TestSuite;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.dao.sparql.ShineDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.InMemoryEmailNotificationTopic;
import com.thoughtworks.go.server.messaging.StageNotificationListener;
import com.thoughtworks.go.server.messaging.StageResultMessage;
import com.thoughtworks.go.server.messaging.StageResultTopic;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class StageNotificationServiceIntegrationTest {
    @Autowired private PipelineDao pipelineDao;
    @Autowired private StageDao stageDao;
    @Autowired private JobInstanceDao jobInstanceDao;
    @Autowired private DataSource dataSource;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private UserDao userDao;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private PipelineService pipelineService;
    @Autowired private UserService userService;
    @Autowired private GoConfigService goConfigService;
    @Autowired private StageResultTopic stageResultTopic;
    @Autowired private ServerConfigService serverConfigService;
    @Autowired private TransactionTemplate transactionTemplate;

    private ShineDao shineDao;
    private PipelineWithTwoStages pipelineFixture;

    private StageNotificationService stageNotificationService;

    private StageNotificationListener stageNotificationListener;
    private InMemoryEmailNotificationTopic inMemoryEmailNotificationTopic;
    private static GoConfigFileHelper configFileHelper = new GoConfigFileHelper();
    private StageService stageService;

    @Before
    public void setUp() throws Exception {

        stageService = mock(StageService.class);
        shineDao = mock(ShineDao.class);
        stageNotificationService = new StageNotificationService(pipelineService, userService,inMemoryEmailNotificationTopic, systemEnvironment, stageService, serverConfigService, shineDao);
        stageNotificationListener = new StageNotificationListener(stageNotificationService, goConfigService, stageResultTopic);

        dbHelper.onSetUp();
        configFileHelper.onSetUp();
        configFileHelper.usingEmptyConfigFileWithLicenseAllowsUnlimitedAgents();
        configFileHelper.usingCruiseConfigDao(goConfigDao);

        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        pipelineFixture.usingConfigHelper(configFileHelper).usingDbHelper(dbHelper).onSetUp();
        configFileHelper.addSecurityWithAdminConfig();

        inMemoryEmailNotificationTopic = new InMemoryEmailNotificationTopic();
        stageNotificationService.setEmailNotificationTopic(inMemoryEmailNotificationTopic);
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        pipelineFixture.onTearDown();
        inMemoryEmailNotificationTopic.reset();
    }

    private Stage mockStageServiceWithStage(Pipeline pipeline) {
        Stage ftStage = pipeline.getStages().byName(pipelineFixture.ftStage);
        when(stageService.findStageWithIdentifier(ftStage.getIdentifier())).thenReturn(ftStage);
        when(shineDao.failedTestsFor(ftStage.getIdentifier())).thenReturn(new ArrayList<TestSuite>());
        return ftStage;
    }

    @Test
    public void shouldOnlySendEmailToMatchedUser() throws SQLException {
        String jezMail = prepareOneMatchedUser();
        String chrisMail = prepareOneNotMatchedUser();

        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        Stage ftStage = mockStageServiceWithStage(pipeline);

        stageNotificationListener.onMessage(new StageResultMessage(ftStage.getIdentifier(), StageEvent.Passes, Username.BLANK));

        String subject = "Stage [" + ftStage.getIdentifier().stageLocator() + "]" + " passed";

        assertThat(inMemoryEmailNotificationTopic.getSubject(jezMail), is(subject));
        assertThat(inMemoryEmailNotificationTopic.getBody(jezMail), containsString("Sent by Go on behalf of jez"));

        assertThat(inMemoryEmailNotificationTopic.emailCount(chrisMail), is(0));
    }


    private String prepareOneNotMatchedUser() {
        String chrisMail = "chris@cruise.com";
        User chris = new User("chris", new String[]{"will not be matched"}, chrisMail, true);
        chris.addNotificationFilter(new NotificationFilter(pipelineFixture.pipelineName, pipelineFixture.ftStage, StageEvent.All, true));
        userDao.saveOrUpdate(chris);
        return chrisMail;
    }

    @Test
    public void shouldSendEmailWithCancelledInfo() throws SQLException {
        String jezMail = prepareOneMatchedUser();
        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        Stage ftStage = mockStageServiceWithStage(pipeline);
        stageNotificationListener.onMessage(
                new StageResultMessage(ftStage.getIdentifier(), StageEvent.Passes,
                        new Username(new CaseInsensitiveString("chris"))));

        assertThat(inMemoryEmailNotificationTopic.getBody(jezMail),
                containsString("The stage was cancelled by chris."));
    }

    private String prepareOneMatchedUser() {
        return prepareOneMatchedUser(StageEvent.All);
    }

    private String prepareOneMatchedUser(StageEvent event) {
        String jezMail = "jez@cruise.com";
        User jez = new User("jez", new String[]{"lgao"}, jezMail, true);
        jez.addNotificationFilter(new NotificationFilter(pipelineFixture.pipelineName, pipelineFixture.ftStage, event, true));
        userDao.saveOrUpdate(jez);
        return jezMail;
    }

    @Test
    public void shouldSendEmailWithLink() throws SQLException {
        String jezMail = prepareOneMatchedUser();

        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        Stage ftStage = mockStageServiceWithStage(pipeline);

        stageNotificationListener.onMessage(
                new StageResultMessage(ftStage.getIdentifier(), StageEvent.Passes, Username.BLANK));

        String body = inMemoryEmailNotificationTopic.getBody(jezMail);
        String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        int port = systemEnvironment.getServerPort();
        assertThat(body, containsString(String.format("http://%s:%s/go/pipelines/%s/%s/%s/%s", ipAddress, port,
                pipelineFixture.pipelineName, pipeline.getCounter(), pipelineFixture.ftStage, ftStage.getCounter())));
    }

    @Test
    public void shouldSendEmailWhenStageBreaks() throws SQLException {
        String jezMail = prepareOneMatchedUser(StageEvent.Breaks);

        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        Stage ftStage = mockStageServiceWithStage(pipeline);

        stageNotificationListener.onMessage(
                new StageResultMessage(ftStage.getIdentifier(), StageEvent.Breaks, Username.BLANK));

        String subject = "Stage [" + ftStage.getIdentifier().stageLocator() + "]" + " is broken";

        assertThat(inMemoryEmailNotificationTopic.getSubject(jezMail), is(subject));
        assertThat(inMemoryEmailNotificationTopic.getBody(jezMail), containsString("Sent by Go on behalf of jez"));
    }

    @Test
    public void shouldSendEmailWhenStageFixed() throws SQLException {
        String jezMail = prepareOneMatchedUser(StageEvent.Fixed);

        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesCompleted(JobResult.Failed);
        Stage ftStage = mockStageServiceWithStage(pipeline);
        stageNotificationListener.onMessage(
                new StageResultMessage(ftStage.getIdentifier(), StageEvent.Fixed, Username.BLANK));

        String subject = "Stage [" + ftStage.getIdentifier().stageLocator() + "]" + " is fixed";

        assertThat(inMemoryEmailNotificationTopic.getSubject(jezMail), is(subject));
        assertThat(inMemoryEmailNotificationTopic.getBody(jezMail), containsString("Sent by Go on behalf of jez"));
    }

    @Test
    public void shouldSendEmailWhenStageContinueFail() throws SQLException {
        String jezMail = prepareOneMatchedUser(StageEvent.Fails);

        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesCompleted(JobResult.Failed);
        Stage ftStage = mockStageServiceWithStage(pipeline);
        stageNotificationListener.onMessage(
                new StageResultMessage(ftStage.getIdentifier(), StageEvent.Fails, Username.BLANK));

        String subject = "Stage [" + ftStage.getIdentifier().stageLocator() + "]" + " failed";

        assertThat(inMemoryEmailNotificationTopic.getSubject(jezMail), is(subject));
        assertThat(inMemoryEmailNotificationTopic.getBody(jezMail), containsString("Sent by Go on behalf of jez"));
    }



}
