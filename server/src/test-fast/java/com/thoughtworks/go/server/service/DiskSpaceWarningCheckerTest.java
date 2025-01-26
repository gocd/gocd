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
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.CurrentGoCDVersion.docsUrl;
import static com.thoughtworks.go.server.service.ArtifactsDiskSpaceFullCheckerTest.mockGoConfigServiceToHaveSiteUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DiskSpaceWarningCheckerTest {
    private EmailSender sender;
    public ServerHealthService serverHealthService;
    private GoConfigService goConfigService;

    @AfterEach
    public void tearDown() {
        new SystemEnvironment().clearProperty(SystemEnvironment.ARTIFACT_WARNING_SIZE_LIMIT);
    }


    @BeforeEach
    public void setUp() {
        sender = mock(EmailSender.class);
        serverHealthService = mock(ServerHealthService.class);
        goConfigService = mock(GoConfigService.class);
        when(goConfigService.artifactsDir()).thenReturn(new File("."));
        when(goConfigService.adminEmail()).thenReturn("admin@cruise.com");
    }

    @Test
    public void shouldUseWarningLimit() {
        new SystemEnvironment().setProperty(SystemEnvironment.ARTIFACT_WARNING_SIZE_LIMIT, "1M");
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setServerConfig(new ServerConfig(".", new SecurityConfig()));

        ArtifactsDiskSpaceWarningChecker fullChecker = new ArtifactsDiskSpaceWarningChecker(new SystemEnvironment(), sender, goConfigService, new SystemDiskSpaceChecker(), serverHealthService);

        assertThat(fullChecker.limitInMb()).isEqualTo(1L);
    }

    @Test
    public void shouldReturnSuccessWhenTheArtifactsFolderIsNotPresent() {
        final GoConfigService service = mock(GoConfigService.class);
        when(service.artifactsDir()).thenReturn(new File("/pavan"));

        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        new ArtifactsDiskSpaceWarningChecker(new SystemEnvironment(), sender, service, new SystemDiskSpaceChecker(), serverHealthService).check(result);
        assertThat(result.getServerHealthState().isSuccess()).isTrue();
    }

    // #2866
    @Test
    public void shouldShowAbsolutePathOfArtifactDirInWarningMessage() throws IOException {
        goConfigService = mockGoConfigServiceToHaveSiteUrl();
        TestingEmailSender sender = new TestingEmailSender();

        SystemEnvironment systemEnvironment = new SystemEnvironment();
        systemEnvironment.setProperty(SystemEnvironment.ARTIFACT_WARNING_SIZE_LIMIT, "1200009M");

        ArtifactsDiskSpaceWarningChecker checker = new ArtifactsDiskSpaceWarningChecker(systemEnvironment, sender, goConfigService, new SystemDiskSpaceChecker(), serverHealthService);

        checker.check(new ServerHealthStateOperationResult());

        assertThat(sender.getSentMessage()).contains(new File(".").getCanonicalPath());
    }

    @Test
    public void shouldFormatLowDiskSpaceWarningMailWithHelpLinksHttpAndSiteUrl() {
        String expectedHelpUrl = docsUrl("/installation/configuring_server_details.html");

        GoConfigService goConfigService = mockGoConfigServiceToHaveSiteUrl();

        SystemDiskSpaceChecker checker = mock(SystemDiskSpaceChecker.class);
        when(checker.getUsableSpace(any(File.class))).thenReturn(1000000L);

        ArtifactsDiskSpaceWarningChecker diskSpaceWarningChecker = new ArtifactsDiskSpaceWarningChecker(new SystemEnvironment(), null, goConfigService, checker, serverHealthService) {
            @Override protected String targetFolderCanonicalPath() {
                return ".";
            }
        };
        SendEmailMessage actual = diskSpaceWarningChecker.createEmail();
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        diskSpaceWarningChecker.check(result);
        assertThat(actual.getBody()).contains(expectedHelpUrl);
        assertThat(result.getServerHealthState().isSuccess()).isTrue();
        assertThat(result.getServerHealthState().getMessage()).isEqualTo("GoCD Server's artifact repository is running low on disk space");
        assertThat(result.getServerHealthState().getType()).isEqualTo(HealthStateType.artifactsDiskFull());
    }
}
