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
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.InMemoryEmailNotificationTopic;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static com.thoughtworks.go.server.service.StageNotificationService.MATERIAL_SECTION_HEADER;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class StageNotificationServiceTest {
    private PipelineService pipelineService;
    private UserService userService;
    private SystemEnvironment systemEnvironment;
    private StageService stageService;
    private StageNotificationService stageNotificationService;
    private InMemoryEmailNotificationTopic inMemoryEmailNotificationTopic;
    private StageIdentifier stageIdentifier;
    private ServerConfigService serverConfigService;
    private InstanceFactory instanceFactory;

    @Before
    public void setUp() {
        pipelineService = mock(PipelineService.class);
        userService = mock(UserService.class);
        systemEnvironment = mock(SystemEnvironment.class);
        stageService = mock(StageService.class);
        inMemoryEmailNotificationTopic = new InMemoryEmailNotificationTopic();
        serverConfigService = mock(ServerConfigService.class);
        stageNotificationService = new StageNotificationService(pipelineService, userService, inMemoryEmailNotificationTopic, systemEnvironment, stageService, serverConfigService);
        stageIdentifier = new StageIdentifier("go", 1, "go-1", "dev", "2");
        instanceFactory = new InstanceFactory();
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
        when(userService.findValidSubscribers(stageIdentifier.stageConfigIdentifier())).thenReturn(new Users(new ArrayList<>()));

        stageNotificationService.sendNotifications(stageIdentifier, StageEvent.Fails, new Username(new CaseInsensitiveString("loser")));
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
                new ManualBuild(new Username(new CaseInsensitiveString("loser"))).onModifications(new MaterialRevisions(new MaterialRevision(new MaterialConfigConverter().toMaterials(pipelineConfig.materialConfigs()).get(0), svnModification)),
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
