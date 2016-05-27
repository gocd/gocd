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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.domain.ServerSiteUrlConfig;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class DiskSpaceFullCheckerTest {
    private EmailSender sender;
    private GoConfigService goConfigService;

    @After
    public void tearDown() throws Exception {
        new SystemEnvironment().clearProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT);
        verifyNoMoreInteractions(sender);
    }

    @Before
    public void setUp() {
        sender = mock(EmailSender.class);
        goConfigService = ArtifactsDiskSpaceFullCheckerTest.mockGoConfigServiceToHaveSiteUrl();
        when(goConfigService.artifactsDir()).thenReturn(new File("."));
        when(goConfigService.adminEmail()).thenReturn("admin@tw.com");
    }

    @Test
    public void shouldReturnTrueIfTheArtifactFolderHasSizeLimit() {
        new SystemEnvironment().setProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT, "1M");
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setServerConfig(new ServerConfig(".", new SecurityConfig()));

        ArtifactsDiskSpaceFullChecker fullChecker = createChecker(cruiseConfig);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        fullChecker.check(result);
        assertThat(result.canContinue(), is(true));
    }

    @Test
    public void shouldNotSendMoreThanOneEmail() {
        CruiseConfig cruiseConfig = simulateFullDisk();
        ArtifactsDiskSpaceFullChecker fullChecker = createChecker(cruiseConfig);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        fullChecker.check(result);
        assertThat(result.canContinue(), is(false));
        verify(sender).sendEmail(any(SendEmailMessage.class));
    }

    @Test
    public void shouldSendEmailsAgainIfDiskSpaceIsFixedAndFallsBelowAgain() {
        CruiseConfig cruiseConfig = simulateFullDisk();

        ArtifactsDiskSpaceFullChecker fullChecker = createChecker(cruiseConfig);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        fullChecker.check(result);
        assertThat(result.canContinue(), is(false));

        simulateEmptyDisk();

        result = new ServerHealthStateOperationResult();
        fullChecker.check(result);
        assertThat(result.canContinue(), is(true));

        simulateFullDisk();

        result = new ServerHealthStateOperationResult();
        fullChecker.check(result);
        assertThat(result.canContinue(), is(false));
        verify(sender, times(2)).sendEmail(any(SendEmailMessage.class));
    }

    private CruiseConfig simulateFullDisk() {
        new SystemEnvironment().setProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT, "1200009M");
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setServerConfig(new ServerConfig(".", new SecurityConfig()));
        return cruiseConfig;
    }

    private void simulateEmptyDisk() {
        new SystemEnvironment().setProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT, "1M");
    }

    @Test
    public void shouldReturnFalseIfTheArtifactFolderExceedSizeLimit() {
        CruiseConfig cruiseConfig = simulateFullDisk();

        ArtifactsDiskSpaceFullChecker fullChecker = createChecker(cruiseConfig);

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        fullChecker.check(result);
        assertThat(result.getServerHealthState().isSuccess(), is(false));
        assertThat(result.getServerHealthState().getMessage(),
                is("Go Server has run out of artifacts disk space. Scheduling has been stopped"));
        assertThat(result.getServerHealthState().getType(), is(HealthStateType.artifactsDiskFull()));
        verify(sender).sendEmail(any(SendEmailMessage.class));
    }

    @Test
    public void shouldFormatLowDiskSpaceWarningMailWithHelpLinksHttpAndSiteUrl() throws URISyntaxException {
        String expectedHelpUrl = "http://www.go.cd/documentation/user/current/installation/configuring_server_details.html";
        ServerConfig serverConfig = new ServerConfig(null, null, new ServerSiteUrlConfig("http://test.host"), new ServerSiteUrlConfig("https://test.host"));
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setServerConfig(serverConfig);

        GoConfigService goConfigService = mock(GoConfigService.class);
        when(goConfigService.artifactsDir()).thenReturn(null);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(goConfigService.adminEmail()).thenReturn("admin@email.com");

        ArtifactsDiskSpaceFullChecker diskSpaceFullChecker = new ArtifactsDiskSpaceFullChecker(new SystemEnvironment(), null, goConfigService, null) {
            @Override protected String targetFolderCanonicalPath() {
                return "";
            }
        };
        SendEmailMessage actual = diskSpaceFullChecker.createEmail();
        assertThat(actual.getBody(), Matchers.containsString(expectedHelpUrl));
    }

    private ArtifactsDiskSpaceFullChecker createChecker(CruiseConfig cruiseConfig) {
        return new ArtifactsDiskSpaceFullChecker(new SystemEnvironment(), sender, goConfigService, new SystemDiskSpaceChecker());
    }
}
