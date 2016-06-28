/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.DefaultSchedulingContext;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.domain.StageEvent;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.Users;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.testinfo.TestStatus;
import com.thoughtworks.go.domain.testinfo.TestSuite;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.dao.sparql.ShineDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.InMemoryEmailNotificationTopic;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.thoughtworks.go.server.service.StageNotificationService.MATERIAL_SECTION_HEADER;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class StageNotificationServiceTest {
    private PipelineService pipelineService;
    private UserService userService;
    private SystemEnvironment systemEnvironment;
    private StageService stageService;
    private StageNotificationService stageNotificationService;
    private Pipeline pipeline;
    private InMemoryEmailNotificationTopic inMemoryEmailNotificationTopic;
    private StageIdentifier stageIdentifier;
    private ServerConfigService serverConfigService;
    private GoConfigService goConfigService;
    private ShineDao shineDao;
    private InstanceFactory instanceFactory;

    @Before
    public void setUp() {
        pipelineService = mock(PipelineService.class);
        userService = mock(UserService.class);
        systemEnvironment = mock(SystemEnvironment.class);
        stageService = mock(StageService.class);
        inMemoryEmailNotificationTopic = new InMemoryEmailNotificationTopic();
        serverConfigService = mock(ServerConfigService.class);
        goConfigService = mock(GoConfigService.class);
        shineDao = mock(ShineDao.class);
        stageNotificationService = new StageNotificationService(pipelineService, userService, inMemoryEmailNotificationTopic, systemEnvironment, stageService, serverConfigService, shineDao);
        stageIdentifier = new StageIdentifier("go", 1, "go-1", "dev", "2");
        instanceFactory = new InstanceFactory();
    }

    @Test
    public void shouldSendEmailWithFailureDetails() throws Exception {
        final String expectedBaseUrl = String.format("http://test.host:8153");
        String jezMail = prepareOneMatchedUser();
        final Date date = new Date();
        stubPipelineAndStage(date);
        final TestSuite suite1 = new TestSuite("com.thoughtworks.go.FailOne");
        suite1.addTest("shouldCompile", TestStatus.Error, new JobIdentifier(stageIdentifier, "compile"));
        suite1.addTest("shouldPass", TestStatus.Failure, new JobIdentifier(stageIdentifier, "test"));
        suite1.addTest("shouldPass", TestStatus.Failure, new JobIdentifier(stageIdentifier, "twist"));
        suite1.addTest("shouldCompile2", TestStatus.Failure, new JobIdentifier(stageIdentifier, "compile"));
        final TestSuite suite2 = new TestSuite("com.thoughtworks.go.FailTwo");
        suite2.addTest("shouldCompile", TestStatus.Error, new JobIdentifier(stageIdentifier, "test"));
        suite2.addTest("shouldTest", TestStatus.Failure, new JobIdentifier(stageIdentifier, "test"));

        when(serverConfigService.siteUrlFor(anyString(), eq(false))).thenAnswer(new Answer<String>() {
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return morphURl((String)args[0], expectedBaseUrl);
            }
        });
        when(systemEnvironment.isShineEnabled()).thenReturn(true);
        when(shineDao.failedTestsFor(stageIdentifier)).thenReturn(Arrays.asList(suite1, suite2));
        stageNotificationService.sendNotifications(stageIdentifier, StageEvent.Fails, new Username(new CaseInsensitiveString("loser")));

        String body = inMemoryEmailNotificationTopic.getBody(jezMail);
        assertThat(body, containsString(StageNotificationService.FAILED_TEST_SECTION));

        String restOfThebody = textAfter(body, StageNotificationService.FAILED_TEST_SECTION);
        String failuresText = restOfThebody.substring(0, restOfThebody.indexOf(StageNotificationService.MATERIAL_SECTION_HEADER));
        assertEquals("\n\nThe following tests failed in pipeline 'go' (instance 'go-1'):\n\n"
                + "* com.thoughtworks.go.FailOne\n"
                + "   shouldCompile\n"
                + "     Errored on 'compile' (" + expectedBaseUrl + "/go/tab/build/detail/go/1/dev/2/compile)\n"
                + "   shouldCompile2\n"
                + "     Failed on 'compile' (" + expectedBaseUrl + "/go/tab/build/detail/go/1/dev/2/compile)\n"
                + "   shouldPass\n"
                + "     Failed on 'test' (" + expectedBaseUrl + "/go/tab/build/detail/go/1/dev/2/test)\n"
                + "     Failed on 'twist' (" + expectedBaseUrl + "/go/tab/build/detail/go/1/dev/2/twist)\n"
                + "\n\n* com.thoughtworks.go.FailTwo\n"
                + "   shouldCompile\n"
                + "     Errored on 'test' (" + expectedBaseUrl + "/go/tab/build/detail/go/1/dev/2/test)\n"
                + "   shouldTest\n"
                + "     Failed on 'test' (" + expectedBaseUrl + "/go/tab/build/detail/go/1/dev/2/test)\n\n\n", failuresText);
    }

    private String morphURl(String url, String expectedBaseUrl) throws URISyntaxException {
        URI uri = new URI(url);
        String blah = String.format("%s%s", expectedBaseUrl, uri.getPath());
        return blah;
    }

    @Test
    public void shouldNotHaveFailedTestsSectionWhenThereAreNoFailedTests() {
        String jezMail = prepareOneMatchedUser();
        stubPipelineAndStage(new Date());
        when(systemEnvironment.isShineEnabled()).thenReturn(true);
        when(shineDao.failedTestsFor(stageIdentifier)).thenReturn(new ArrayList<TestSuite>());

        stageNotificationService.sendNotifications(stageIdentifier, StageEvent.Fails, new Username(new CaseInsensitiveString("loser")));

        String body = inMemoryEmailNotificationTopic.getBody(jezMail);
        assertThat(body, not(containsString(StageNotificationService.FAILED_TEST_SECTION)));
    }

    @Test
    public void shouldHaveFailedTestsSectionWhenShineIsEnabledAndThereAreFailedTests() {
        String mail = prepareOneMatchedUser();
        stubPipelineAndStage(new Date());
        when(systemEnvironment.isShineEnabled()).thenReturn(true);
        ArrayList<TestSuite> testSuites = new ArrayList<>();
        testSuites.add(new TestSuite("blah"));
        when(shineDao.failedTestsFor(stageIdentifier)).thenReturn(testSuites);

        stageNotificationService.sendNotifications(stageIdentifier, StageEvent.Fails, new Username(new CaseInsensitiveString("loser")));

        String body = inMemoryEmailNotificationTopic.getBody(mail);
        assertThat(body, containsString(StageNotificationService.FAILED_TEST_SECTION));
    }

    @Test
    public void shouldNotHaveFailedTestsSectionWhenShineIsDisabled() {
        String mail = prepareOneMatchedUser();
        stubPipelineAndStage(new Date());
        when(systemEnvironment.isShineEnabled()).thenReturn(false);

        stageNotificationService.sendNotifications(stageIdentifier, StageEvent.Fails, new Username(new CaseInsensitiveString("loser")));

        String body = inMemoryEmailNotificationTopic.getBody(mail);
        assertThat(body, not(containsString(StageNotificationService.FAILED_TEST_SECTION)));
    }


    @Test
    public void shouldSendEmailWithModificationInfo() throws SQLException {
        String jezMail = prepareOneMatchedUser();
        final Date date = new Date();
        stubPipelineAndStage(date);
        stageNotificationService.sendNotifications(stageIdentifier, StageEvent.Fails, new Username(new CaseInsensitiveString("loser")));

        String body = inMemoryEmailNotificationTopic.getBody(jezMail);
        assertThat(body, containsString(MATERIAL_SECTION_HEADER));
        String materialBody = textAfter(body, MATERIAL_SECTION_HEADER);
        assertEquals("\n\nSubversion: http://some/svn/url\n"
                + String.format("revision: 123, modified by lgao on %s\n", date)
                + "Fixing the not checked in files\n"
                + "added build.xml\n"
                + "deleted some.xml\n\n"
                + "Sent by Go on behalf of jez", materialBody);
    }

    @Test
    public void shouldNotComputeFailedTestSuitesWhenThereAreNoSubscribers() throws Exception {
        when(userService.findValidSubscribers(stageIdentifier.stageConfigIdentifier())).thenReturn(new Users(new ArrayList<User>()));

        stageNotificationService.sendNotifications(stageIdentifier, StageEvent.Fails, new Username(new CaseInsensitiveString("loser")));
        verifyZeroInteractions(shineDao);
        verifyZeroInteractions(stageService);
        verifyZeroInteractions(pipelineService);
    }

    private String textAfter(String body, String sectionName) {
        return body.substring(body.indexOf(sectionName) + sectionName.length());
    }

    private void stubPipelineAndStage(Date date) {
        final PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("go", "dev", "compile", "test", "twist");
        final Modification svnModification = new Modification("lgao", "Fixing the not checked in files", "jez@cruise.com", date, "123");
        svnModification.createModifiedFile("build.xml", "some_dir", ModifiedAction.added);
        svnModification.createModifiedFile("some.xml", "other_dir", ModifiedAction.deleted);

        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig,
                new ManualBuild(new Username(new CaseInsensitiveString("loser"))).onModifications(new MaterialRevisions(new MaterialRevision(MaterialsMother.createMaterialsFromMaterialConfigs(pipelineConfig.materialConfigs()).get(0), svnModification)),
                        false, null),
                new DefaultSchedulingContext("loser"), "md5-test", new TimeProvider());
        Stage stage = pipeline.getStages().get(0);
        when(stageService.findStageWithIdentifier(stageIdentifier)).thenReturn(stage);
        stage.setPipelineId(100L);
        when(pipelineService.fullPipelineById(100)).thenReturn(pipeline);
    }

    private String prepareOneMatchedUser() {
        String jezMail = "jez@cruise.com";
        User jez = new User("jez", new String[]{"lgao"}, jezMail, true);
        jez.setNotificationFilters(Arrays.asList(new NotificationFilter("go", "dev", StageEvent.All, true)));
        when(userService.findValidSubscribers(new StageConfigIdentifier("go", "dev"))).thenReturn(new Users(Arrays.asList(jez)));
        return jezMail;
    }

}
