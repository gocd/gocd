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
package com.thoughtworks.go.server.service.datasharing;

import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.domain.DataSharingSettings;
import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.server.dao.UsageStatisticsReportingSqlMapDao;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestingClock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;

import static com.thoughtworks.go.server.service.datasharing.DataSharingUsageStatisticsReportingService.USAGE_DATA_IGNORE_LAST_UPDATED_AT;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DataSharingUsageStatisticsReportingServiceTest {
    private DataSharingUsageStatisticsReportingService service;
    @Mock
    private UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao;
    @Mock
    private DataSharingSettingsService dataSharingSettingsService;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private GoCache goCache;
    private String goUsageDataRemoteServerURL;
    private String goUsageDataGetEncryptionKeysURL;
    private DataSharingSettings dataSharingSetting;
    private TestingClock clock = new TestingClock();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        service = new DataSharingUsageStatisticsReportingService(usageStatisticsReportingSqlMapDao, dataSharingSettingsService, systemEnvironment, clock, goCache);
        goUsageDataRemoteServerURL = "https://datasharing.gocd.org";
        goUsageDataGetEncryptionKeysURL = "https://datasharing.gocd.org/encryption_keys";
        when(systemEnvironment.getGoDataSharingServerUrl()).thenReturn(goUsageDataRemoteServerURL);
        when(systemEnvironment.getGoDataSharingGetEncryptionKeysUrl()).thenReturn(goUsageDataGetEncryptionKeysURL);
        when(goCache.getOrDefault(USAGE_DATA_IGNORE_LAST_UPDATED_AT, false)).thenReturn(false);

        dataSharingSetting = new DataSharingSettings();
        when(dataSharingSettingsService.load()).thenReturn(dataSharingSetting);
        when(systemEnvironment.isProductionMode()).thenReturn(true);
    }

    @Test
    public void shouldSetDataSharingServerUrlOnTheLoadedDataSharingUsageReporting() {
        UsageStatisticsReporting existingMetric = new UsageStatisticsReporting()
                .setLastReportedAt(new Timestamp(new Date().getTime()))
                .setServerId("server-id");
        assertNull(existingMetric.getDataSharingServerUrl());
        when(usageStatisticsReportingSqlMapDao.load()).thenReturn(existingMetric);
        UsageStatisticsReporting statisticsReporting = service.get();
        assertThat(statisticsReporting.getDataSharingServerUrl(), is(goUsageDataRemoteServerURL));
    }

    @Test
    public void shouldSetDataSharingGetEncryptionKeysUrlOnTheLoadedDataSharingUsageReporting() {
        UsageStatisticsReporting existingMetric = new UsageStatisticsReporting()
                .setLastReportedAt(new Timestamp(new Date().getTime()))
                .setServerId("server-id");
        assertNull(existingMetric.getDataSharingServerUrl());
        when(usageStatisticsReportingSqlMapDao.load()).thenReturn(existingMetric);
        UsageStatisticsReporting statisticsReporting = service.get();
        assertThat(statisticsReporting.getDataSharingGetEncryptionKeysUrl(), is(goUsageDataGetEncryptionKeysURL));
    }

    @Test
    public void shouldDisallowReportingIfDataSharingSettingsIsNotAllowed() {
        UsageStatisticsReporting existingMetric = new UsageStatisticsReporting()
                .setLastReportedAt(new Timestamp(new Date().getTime()))
                .setServerId("server-id");
        assertTrue(existingMetric.isCanReport());
        when(usageStatisticsReportingSqlMapDao.load()).thenReturn(existingMetric);

        dataSharingSetting = new DataSharingSettings();
        dataSharingSetting.setAllowSharing(false);
        when(dataSharingSettingsService.load()).thenReturn(dataSharingSetting);

        UsageStatisticsReporting statisticsReporting = service.get();
        assertFalse(statisticsReporting.isCanReport());
    }

    @Test
    public void shouldDisallowReportingDataSharingIfAlreadyReportedToday() {
        UsageStatisticsReporting existingMetric = new UsageStatisticsReporting()
                .setLastReportedAt(new Timestamp(new Date().getTime()))
                .setServerId("server-id");
        assertThat(existingMetric.getLastReportedAt().getDate(), is(new Date().getDate()));
        when(usageStatisticsReportingSqlMapDao.load()).thenReturn(existingMetric);

        UsageStatisticsReporting statisticsReporting = service.get();
        assertFalse(statisticsReporting.isCanReport());
    }

    @Test
    public void shouldAllowReportingDataSharingIfDataIsNotReportedToday() {
        Date lastReportedAt = new Date();
        lastReportedAt.setDate(lastReportedAt.getDate() - 1);
        UsageStatisticsReporting existingMetric = new UsageStatisticsReporting()
                .setLastReportedAt(new Timestamp(lastReportedAt.getTime()))
                .setServerId("server-id");
        assertThat(existingMetric.getLastReportedAt().getDate(), is(LocalDate.now().minusDays(1).getDayOfMonth()));
        when(usageStatisticsReportingSqlMapDao.load()).thenReturn(existingMetric);

        UsageStatisticsReporting statisticsReporting = service.get();
        assertTrue(statisticsReporting.isCanReport());
    }

    @Test
    public void shouldDisallowReportingDataSharingIfReportingIsInProgress() {
        Date lastReportedAt = new Date();
        lastReportedAt.setDate(lastReportedAt.getDate() - 1);
        UsageStatisticsReporting existingMetric = new UsageStatisticsReporting()
                .setLastReportedAt(new Timestamp(lastReportedAt.getTime()))
                .setServerId("server-id");
        when(usageStatisticsReportingSqlMapDao.load()).thenReturn(existingMetric);

        UsageStatisticsReporting statisticsReporting = service.get();
        assertTrue(statisticsReporting.isCanReport());
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.startReporting(result);
        assertTrue(result.isSuccessful());
        statisticsReporting = service.get();
        assertFalse(statisticsReporting.isCanReport());
    }

    @Test
    public void shouldAllowReportingDataSharingWhenCurrentInProgressReportingIsStale() {
        Date lastReportedAt = new Date();
        lastReportedAt.setDate(lastReportedAt.getDate() - 1);
        UsageStatisticsReporting existingMetric = new UsageStatisticsReporting()
                .setLastReportedAt(new Timestamp(lastReportedAt.getTime()))
                .setServerId("server-id");
        when(usageStatisticsReportingSqlMapDao.load()).thenReturn(existingMetric);

        UsageStatisticsReporting statisticsReporting = service.get();
        assertTrue(statisticsReporting.isCanReport());
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.startReporting(result);
        assertTrue(result.isSuccessful());

        clock.addSeconds(60 * 31);

        statisticsReporting = service.get();
        assertTrue(statisticsReporting.isCanReport());
    }

    @Test
    public void shouldDisallowReportingForDevelopmentServer() {
        when(systemEnvironment.isProductionMode()).thenReturn(false);
        UsageStatisticsReporting existingMetric = new UsageStatisticsReporting()
                .setLastReportedAt(new Timestamp(new Date().getTime()))
                .setServerId("server-id");
        when(usageStatisticsReportingSqlMapDao.load()).thenReturn(existingMetric);

        UsageStatisticsReporting statisticsReporting = service.get();
        assertFalse(statisticsReporting.isCanReport());
    }

    @Test
    public void shouldFailStartingReportingIfReportingHasAlreadyBeenStarted() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.startReporting(result);
        result = new HttpLocalizedOperationResult();
        service.startReporting(result);

        assertFalse(result.isSuccessful());
        assertThat(result.message(), is("Cannot start usage statistics reporting as it is already in progress."));
    }

    @Test
    public void shouldAllowStartingReportingAgainIfReportingHasAlreadyBeenStartedPriorToHalfAnHour() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.startReporting(result);

        clock.addSeconds(60 * 31);

        result = new HttpLocalizedOperationResult();
        service.startReporting(result);

        assertTrue(result.isSuccessful());
    }

    @Test
    public void shouldFailCompletingReportingIfReportingHasAlreadyBeenCompleted() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.completeReporting(result);

        assertFalse(result.isSuccessful());
        assertThat(result.message(), is("Cannot complete usage statistics reporting as it has already completed."));
    }
}
