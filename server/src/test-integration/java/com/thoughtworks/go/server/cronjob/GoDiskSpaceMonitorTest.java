/*
 * Copyright 2024 Thoughtworks, Inc.
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
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;

import static com.thoughtworks.go.util.GoConstants.MEGA_BYTE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath:/applicationContext-global.xml",
    "classpath:/applicationContext-dataLocalAccess.xml",
    "classpath:/testPropertyConfigurer.xml",
    "classpath:/spring-all-servlet.xml",
})

public class GoDiskSpaceMonitorTest {
    private static final long LOADS_OF_DISK_SPACE = 10000000000L * MEGA_BYTE;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private SystemEnvironment systemEnvironment;
    @Autowired
    private ServerHealthService serverHealthService;
    private GoDiskSpaceMonitor goDiskSpaceMonitor;
    private TestingEmailSender emailSender;
    private SystemDiskSpaceChecker mockDiskSpaceChecker;
    private long diskSpaceCacheRefresherInterval;
    private StageService stageService;
    private ConfigDbStateRepository configDbStateRepository;

    @BeforeEach
    public void setUp() throws Exception {
        serverHealthService.removeAllLogs();
        emailSender = new TestingEmailSender();
        mockDiskSpaceChecker = mock(SystemDiskSpaceChecker.class);
        stageService = mock(StageService.class);
        configDbStateRepository = mock(ConfigDbStateRepository.class);
        goDiskSpaceMonitor = new GoDiskSpaceMonitor(goConfigService, systemEnvironment, serverHealthService, emailSender, mockDiskSpaceChecker, mock(ArtifactsService.class),
            stageService, configDbStateRepository);
        goDiskSpaceMonitor.initialize();
        diskSpaceCacheRefresherInterval = systemEnvironment.getDiskSpaceCacheRefresherInterval();
        systemEnvironment.setDiskSpaceCacheRefresherInterval(-1);
    }

    @AfterEach
    public void tearDown() throws Exception {
        serverHealthService.removeAllLogs();
        systemEnvironment.setDiskSpaceCacheRefresherInterval(diskSpaceCacheRefresherInterval);
    }

    @Test
    @ExtendWith(ArtifactsDiskIsLow.class)
    public void shouldStoreAndReportCheckResult() {
        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(LOADS_OF_DISK_SPACE);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(ArtifactsDiskIsLow.limitBytes() + 1L);

        goDiskSpaceMonitor.onTimer();
        assertThat(goDiskSpaceMonitor.isLowOnDisk(), is(true));

        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(LOADS_OF_DISK_SPACE);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(LOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();
        assertThat(goDiskSpaceMonitor.isLowOnDisk(), is(false));
    }

    @Test
    @ExtendWith(ArtifactsDiskIsLow.class)
    public void shouldRemoveWarningLogAfterArtifactsDiskSpaceIsRecovered() {
        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(ArtifactsDiskIsLow.limitBytes() + 1L);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(LOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState logEntry = findByLogType(ArtifactsDiskSpaceFullChecker.ARTIFACTS_DISK_FULL_ID);
        assertThat(logEntry.getLogLevel(), is(HealthStateLevel.WARNING));

        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(LOADS_OF_DISK_SPACE);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(LOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState after = findByLogType(ArtifactsDiskSpaceFullChecker.ARTIFACTS_DISK_FULL_ID);
        assertThat(after, is(nullValue()));
    }

    @Test
    @ExtendWith(ArtifactsDiskIsLow.class)
    public void shouldSendMailAboutArtifactsDiskSpaceLowWarningMessage() {
        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(ArtifactsDiskIsLow.limitBytes() + 1L);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(1000 * MEGA_BYTE);
        goDiskSpaceMonitor.onTimer();
        emailSender.assertHasMessageContaining("Low artifacts disk space warning message from Go Server");
    }

    @Test
    @ExtendWith(ArtifactsDiskIsLow.class)
    public void shouldRemoveErrorLogAfterArtifactsDiskSpaceIsRecovered() {
        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(ArtifactsDiskIsLow.limitBytes() - 1L);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(LOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState logEntry = findByLogType(ArtifactsDiskSpaceFullChecker.ARTIFACTS_DISK_FULL_ID);
        assertThat(logEntry.getLogLevel(), is(HealthStateLevel.ERROR));

        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(LOADS_OF_DISK_SPACE);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(LOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState after = findByLogType(ArtifactsDiskSpaceFullChecker.ARTIFACTS_DISK_FULL_ID);
        assertThat(after, is(nullValue()));

    }

    @Test
    @ExtendWith(ArtifactsDiskIsFull.class)
    public void shouldSendMailAboutArtifactsDiskSpaceFullErrorMessage() {
        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(ArtifactsDiskIsFull.limitBytes() - 1L);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(1000 * MEGA_BYTE);

        goDiskSpaceMonitor.onTimer();
        emailSender.assertHasMessageContaining("No artifacts disk space error message from Go Server");
    }

    @Test
    @ExtendWith(DatabaseDiskIsLow.class)
    public void shouldSendMailAboutDatabaseDiskSpaceLowWarningMessage() {
        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(LOADS_OF_DISK_SPACE);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(DatabaseDiskIsLow.limitBytes() + 1L);

        goDiskSpaceMonitor.onTimer();
        emailSender.assertHasMessageContaining("Low database disk space warning message from Go Server");
    }

    @Test
    @ExtendWith(DatabaseDiskIsFull.class)
    public void shouldSendMailAboutDatabaseDiskSpaceFullErrorMessage() {
        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(LOADS_OF_DISK_SPACE);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(DatabaseDiskIsFull.limitBytes() - 1L);
        goDiskSpaceMonitor.onTimer();
        emailSender.assertHasMessageContaining("No database disk space error message from Go Server");
    }

    @Test
    @ExtendWith(DatabaseDiskIsFull.class)
    public void shouldRemoveErrorLogAfterDatabaseDiskSpaceIsRecovered() {
        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(LOADS_OF_DISK_SPACE);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(DatabaseDiskIsFull.limitBytes() - 1L);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState logEntry = findByLogType(DatabaseDiskSpaceFullChecker.DATABASE_DISK_FULL_ID);
        assertThat(logEntry.getLogLevel(), is(HealthStateLevel.ERROR));

        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(LOADS_OF_DISK_SPACE);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(LOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState after = findByLogType(DatabaseDiskSpaceFullChecker.DATABASE_DISK_FULL_ID);
        assertThat(after, is(nullValue()));
    }

    @Test
    @ExtendWith(DatabaseDiskIsFull.class)
    public void shouldRemoveWarningLogAfterDatabaseDiskSpaceIsRecovered() {
        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(LOADS_OF_DISK_SPACE);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(DatabaseDiskIsFull.limitBytes() + 1L);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState logEntry = findByLogType(DatabaseDiskSpaceFullChecker.DATABASE_DISK_FULL_ID);
        assertThat(logEntry.getLogLevel(), is(HealthStateLevel.WARNING));

        when(mockDiskSpaceChecker.getUsableSpace(goConfigService.artifactsDir())).thenReturn(LOADS_OF_DISK_SPACE);
        when(mockDiskSpaceChecker.getUsableSpace(new File(SystemEnvironment.DB_BASE_DIR))).thenReturn(LOADS_OF_DISK_SPACE);

        goDiskSpaceMonitor.onTimer();

        ServerHealthState after = findByLogType(DatabaseDiskSpaceFullChecker.DATABASE_DISK_FULL_ID);
        assertThat(after, is(nullValue()));
    }

    private ServerHealthState findByLogType(HealthStateType healthStateType) {
        for (ServerHealthState serverHealthState : serverHealthService.logsSorted()) {
            if (serverHealthState.getType().equals(healthStateType)) {
                return serverHealthState;
            }
        }
        return null;
    }

}
