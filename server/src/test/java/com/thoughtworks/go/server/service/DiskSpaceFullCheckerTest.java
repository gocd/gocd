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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.SiteUrl;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.thoughtworks.go.CurrentGoCDVersion.docsUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class DiskSpaceFullCheckerTest {
    private EmailSender sender;
    private GoConfigService goConfigService;

    @AfterEach
    public void tearDown() {
        new SystemEnvironment().clearProperty(SystemEnvironment.ARTIFACT_FULL_SIZE_LIMIT);
        verifyNoMoreInteractions(sender);
    }

    @BeforeEach
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

        ArtifactsDiskSpaceFullChecker fullChecker = createChecker();
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        fullChecker.check(result);
        assertThat(result.canContinue()).isTrue();
    }

    @Test
    public void shouldNotSendMoreThanOneEmail() {
        CruiseConfig cruiseConfig = simulateFullDisk();
        ArtifactsDiskSpaceFullChecker fullChecker = createChecker();
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        fullChecker.check(result);
        assertThat(result.canContinue()).isFalse();
        verify(sender).sendEmail(any());
    }

    @Test
    public void shouldSendEmailsAgainIfDiskSpaceIsFixedAndFallsBelowAgain() {
        CruiseConfig cruiseConfig = simulateFullDisk();

        ArtifactsDiskSpaceFullChecker fullChecker = createChecker();
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        fullChecker.check(result);
        assertThat(result.canContinue()).isFalse();

        simulateEmptyDisk();

        result = new ServerHealthStateOperationResult();
        fullChecker.check(result);
        assertThat(result.canContinue()).isTrue();

        simulateFullDisk();

        result = new ServerHealthStateOperationResult();
        fullChecker.check(result);
        assertThat(result.canContinue()).isFalse();
        verify(sender, times(2)).sendEmail(any());
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

        ArtifactsDiskSpaceFullChecker fullChecker = createChecker();

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        fullChecker.check(result);
        assertThat(result.getServerHealthState().isSuccess()).isFalse();
        assertThat(result.getServerHealthState().getMessage()).isEqualTo("GoCD Server has run out of artifacts disk space. Scheduling has been stopped");
        assertThat(result.getServerHealthState().getType()).isEqualTo(HealthStateType.artifactsDiskFull());
        verify(sender).sendEmail(any());
    }

    @Test
    public void shouldFormatLowDiskSpaceWarningMailWithHelpLinksHttpAndSiteUrl() {
        String expectedHelpUrl = docsUrl("/installation/configuring_server_details.html");
        ServerConfig serverConfig = new ServerConfig(null, null, new SiteUrl("http://test.host"), new SecureSiteUrl("https://test.host"));
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
        assertThat(actual.getBody()).contains(expectedHelpUrl);
    }

    private ArtifactsDiskSpaceFullChecker createChecker() {
        return new ArtifactsDiskSpaceFullChecker(new SystemEnvironment(), sender, goConfigService, new SystemDiskSpaceChecker());
    }
}
