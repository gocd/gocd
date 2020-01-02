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
package com.thoughtworks.go.server.cronjob;

import com.thoughtworks.go.fixture.ArtifactsDiskIsFull;
import com.thoughtworks.go.fixture.ArtifactsDiskIsLow;
import com.thoughtworks.go.fixture.DatabaseDiskIsFull;
import com.thoughtworks.go.fixture.DatabaseDiskIsLow;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.serverhealth.*;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.util.GoConstants.MEGA_BYTE;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})

public class GoDiskSpaceMonitorTest {
    @Autowired private GoConfigService goConfigService;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private ServerHealthService serverHealthService;
    private GoDiskSpaceMonitor goDiskSpaceMonitor;
    private TestingEmailSender emailSender;
    private SystemDiskSpaceChecker mockDiskSpaceChecker;
    private static final long SHITLOADS_OF_DISK_SPACE = 10000000000L * MEGA_BYTE;
    private long diskSpaceCacheRefresherInterval;
    private StageService stageService;
    private ConfigDbStateRepository configDbStateRepository;

    @Before
    public void setUp() throws Exception {
        serverHealthService.removeAllLogs();
        emailSender = new TestingEmailSender();
        mockDiskSpaceChecker = Mockito.mock(SystemDiskSpaceChecker.class);
        stageService = mock(StageService.class);
        configDbStateRepository = mock(ConfigDbStateRepository.class);
        goDiskSpaceMonitor = new GoDiskSpaceMonitor(goConfigService, systemEnvironment, serverHealthService, emailSender, mockDiskSpaceChecker, mock(ArtifactsService.class),
                stageService, configDbStateRepository);
        goDiskSpaceMonitor.initialize();
        diskSpaceCacheRefresherInterval = systemEnvironment.getDiskSpaceCacheRefresherInterval();
        systemEnvironment.setDiskSpaceCacheRefresherInterval(-1);
    }

    @After
    public void tearDown() throws Exception {
        serverHealthService.removeAllLogs();
        systemEnvironment.setDiskSpaceCacheRefresherInterval(diskSpaceCacheRefresherInterval);
    }

    @Test
    public void shouldStoreAndReportCheckResult() throws Exception {
        ArtifactsDiskIsLow full = new ArtifactsDiskIsLow();
        full.onSetUp();

        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(SHITLOADS_OF_DISK_SPACE);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(full.getLowLimit() + 1L);

        goDiskSpaceMonitor.onTimer();
        assertThat(goDiskSpaceMonitor.isLowOnDisk(), is(true));

        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(SHITLOADS_OF_DISK_SPACE);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(SHITLOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();
        assertThat(goDiskSpaceMonitor.isLowOnDisk(), is(false));
    }

    @Test
    public void shouldRemoveWarningLogAfterArtifactsDiskSpaceIsRecovered() throws Exception {
        ArtifactsDiskIsLow full = new ArtifactsDiskIsLow();
        full.onSetUp();

        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(full.getLowLimit() + 1L);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(SHITLOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState logEntry = findByLogType(ArtifactsDiskSpaceFullChecker.ARTIFACTS_DISK_FULL_ID);
        assertThat(logEntry.getLogLevel(), is(HealthStateLevel.WARNING));

        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(SHITLOADS_OF_DISK_SPACE);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(SHITLOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState after = findByLogType(ArtifactsDiskSpaceFullChecker.ARTIFACTS_DISK_FULL_ID);
        assertThat(after, is(nullValue()));
    }

    @Test
    public void shouldSendMailAboutArtifactsDiskSpaceLowWarningMessage() throws Exception {
        ArtifactsDiskIsLow low = new ArtifactsDiskIsLow();
        low.onSetUp();

        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(low.getLowLimit() + 1L);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(1000 * MEGA_BYTE);
        try {
            goDiskSpaceMonitor.onTimer();
            emailSender.assertHasMessageContaining("Low artifacts disk space warning message from Go Server");
        } finally {
            low.onTearDown();
        }
    }

    @Test
    public void shouldRemoveErrorLogAfterArtifactsDiskSpaceIsRecovered() throws Exception {
        ArtifactsDiskIsLow full = new ArtifactsDiskIsLow();
        full.onSetUp();

        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(full.getLowLimit() - 1L);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(SHITLOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState logEntry = findByLogType(ArtifactsDiskSpaceFullChecker.ARTIFACTS_DISK_FULL_ID);
        assertThat(logEntry.getLogLevel(), is(HealthStateLevel.ERROR));

        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(SHITLOADS_OF_DISK_SPACE);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(SHITLOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState after = findByLogType(ArtifactsDiskSpaceFullChecker.ARTIFACTS_DISK_FULL_ID);
        assertThat(after, is(nullValue()));

    }

    @Test
    public void shouldSendMailAboutArtifactsDiskSpaceFullErrorMessage() throws Exception {
        ArtifactsDiskIsFull full = new ArtifactsDiskIsFull();
        full.onSetUp();
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(full.getLowLimit() - 1L);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(1000 * MEGA_BYTE);

        try {
            goDiskSpaceMonitor.onTimer();
            emailSender.assertHasMessageContaining("No artifacts disk space error message from Go Server");
        } finally {
            full.onTearDown();
        }
    }

    @Test
    public void shouldSendMailAboutDatabaseDiskSpaceLowWarningMessage() throws Exception {
        DatabaseDiskIsLow low = new DatabaseDiskIsLow();
        low.onSetUp();
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(SHITLOADS_OF_DISK_SPACE);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(low.getLowLimit() + 1L);

        try {
            goDiskSpaceMonitor.onTimer();
            emailSender.assertHasMessageContaining("Low database disk space warning message from Go Server");
        } finally {
            low.onTearDown();
        }
    }

    @Test
    public void shouldSendMailAboutDatabaseDiskSpaceFullErrorMessage() throws Exception {
        DatabaseDiskIsFull full = new DatabaseDiskIsFull();
        full.onSetUp();
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(SHITLOADS_OF_DISK_SPACE);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(full.getLowLimit() - 1L);

        try {
            goDiskSpaceMonitor.onTimer();
            emailSender.assertHasMessageContaining("No database disk space error message from Go Server");
        } finally {
            full.onTearDown();
        }
    }

    @Test
    public void shouldRemoveErrorLogAfterDatabaseDiskSpaceIsRecovered() throws Exception {
        DatabaseDiskIsFull full = new DatabaseDiskIsFull();
        full.onSetUp();

        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(SHITLOADS_OF_DISK_SPACE);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(full.getLowLimit() - 1L);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState logEntry = findByLogType(DatabaseDiskSpaceFullChecker.DATABASE_DISK_FULL_ID);
        assertThat(logEntry.getLogLevel(), is(HealthStateLevel.ERROR));

        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(SHITLOADS_OF_DISK_SPACE);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(SHITLOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState after = findByLogType(DatabaseDiskSpaceFullChecker.DATABASE_DISK_FULL_ID);
        assertThat(after, is(nullValue()));
    }

    @Test
    public void shouldRemoveWarningLogAfterDatabaseDiskSpaceIsRecovered() throws Exception {
        DatabaseDiskIsFull full = new DatabaseDiskIsFull();
        full.onSetUp();

        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(SHITLOADS_OF_DISK_SPACE);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(full.getLowLimit() + 1L);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState logEntry = findByLogType(DatabaseDiskSpaceFullChecker.DATABASE_DISK_FULL_ID);
        assertThat(logEntry.getLogLevel(), is(HealthStateLevel.WARNING));

        Mockito.when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(SHITLOADS_OF_DISK_SPACE);
        Mockito.when(mockDiskSpaceChecker.getUsableSpace(systemEnvironment.getDbFolder())).thenReturn(SHITLOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState after = findByLogType(DatabaseDiskSpaceFullChecker.DATABASE_DISK_FULL_ID);
        assertThat(after, is(nullValue()));
    }

    private ServerHealthState findByLogType(HealthStateType healthStateType) {
        for (ServerHealthState serverHealthState : serverHealthService.logs()) {
            if (serverHealthState.getType().equals(healthStateType)) {
                return serverHealthState;
            }
        }
        return null;
    }

}
