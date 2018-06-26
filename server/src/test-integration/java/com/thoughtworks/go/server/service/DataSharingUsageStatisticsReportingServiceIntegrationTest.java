/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.server.dao.UsageStatisticsReportingSqlMapDao;
import com.thoughtworks.go.server.dao.UsageStatisticsReportingSqlMapDao.DuplicateMetricReporting;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.thoughtworks.go.server.dao.*;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class DataSharingUsageStatisticsReportingServiceIntegrationTest {
    @Autowired
    private DataSharingUsageStatisticsReportingService dataSharingUsageStatisticsReportingService;
    @Autowired
    private UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao;
    @Autowired
    private EntityHashingService entityHashingService;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        dataSharingUsageStatisticsReportingService.initialize();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldInitializeGoStatsReportingOnFirstStartup() throws Exception {
        dbHelper.onTearDown();//to start on a clean slate

        assertNull(usageStatisticsReportingSqlMapDao.load());
        dataSharingUsageStatisticsReportingService.initialize();
        UsageStatisticsReporting usageStatisticsReporting = usageStatisticsReportingSqlMapDao.load();
        assertNotNull(usageStatisticsReporting);
        assertNotNull(usageStatisticsReporting.getServerId());
    }

    @Test
    public void shouldNotUpdateServerIdOfGoStatsReportingOnSubsequentStartups() throws Exception {
        dbHelper.onTearDown();//to start on a clean slate

        assertNull(usageStatisticsReportingSqlMapDao.load());
        dataSharingUsageStatisticsReportingService.initialize();
        UsageStatisticsReporting saved = usageStatisticsReportingSqlMapDao.load();
        assertNotNull(saved);
        dataSharingUsageStatisticsReportingService.initialize();
        UsageStatisticsReporting loaded = usageStatisticsReportingSqlMapDao.load();
        assertThat(saved.getServerId(), is(loaded.getServerId()));
    }

    @Test
    public void shouldFetchUsageReportingContainingDataSharingServerUrl() throws Exception {
        String serverId = "server-id";
        Date lastReportedAt = new Date();
        UsageStatisticsReporting statisticsReporting = new UsageStatisticsReporting(serverId, lastReportedAt);
        usageStatisticsReportingSqlMapDao.saveOrUpdate(statisticsReporting);

        UsageStatisticsReporting loaded = dataSharingUsageStatisticsReportingService.get();

        assertThat(loaded.getServerId(), is(statisticsReporting.getServerId()));
        assertThat(loaded.lastReportedAt().toInstant(), is(statisticsReporting.lastReportedAt().toInstant()));
        assertNull(statisticsReporting.getDataSharingServerUrl());
        assertThat(loaded.getDataSharingServerUrl(), is(new SystemEnvironment().getGoDataSharingServerUrl()));
    }

    @Test
    public void shouldUpdateUsageStatisticsReporting() throws Exception {
        UsageStatisticsReporting existing = usageStatisticsReportingSqlMapDao.load();
        assertNotNull(existing);
        assertNotNull(existing.lastReportedAt());

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        UsageStatisticsReporting reporting = new UsageStatisticsReporting();
        Date lastReportedAt = new Date();
        reporting.setLastReportedAt(lastReportedAt);
        dataSharingUsageStatisticsReportingService.update(reporting, result);

        UsageStatisticsReporting loaded = usageStatisticsReportingSqlMapDao.load();
        assertThat(loaded.lastReportedAt().toInstant(), is(lastReportedAt.toInstant()));
        assertThat(loaded.lastReportedAt().toInstant(), is(not(existing.lastReportedAt())));

        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldThrowErrorOccurredWhileUpdatingUsageStatisticsReportingWithIncorrectTime() throws Exception {
        UsageStatisticsReporting existing = usageStatisticsReportingSqlMapDao.load();
        assertNotNull(existing);
        assertNotNull(existing.lastReportedAt());

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        dataSharingUsageStatisticsReportingService.update(new UsageStatisticsReporting(), result);

        UsageStatisticsReporting loaded = usageStatisticsReportingSqlMapDao.load();
        assertNotNull(loaded);
        assertThat(existing, is(loaded));

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is("Validations failed. Please correct and resubmit."));
    }

    @Test
    public void shouldUpdateMd5SumOfUsageStatisticsReportingUponSave() throws DuplicateMetricReporting {
        String originalMd5 = entityHashingService.md5ForEntity(usageStatisticsReportingSqlMapDao.load());
        assertThat(originalMd5, is(not(nullValue())));
        UsageStatisticsReporting reporting = new UsageStatisticsReporting();
        Date lastReportedAt = new Date();
        lastReportedAt.setTime(lastReportedAt.getTime() + 10000);
        reporting.setLastReportedAt(lastReportedAt);
        dataSharingUsageStatisticsReportingService.update(reporting, new HttpLocalizedOperationResult());

        String md5AfterUpdate = entityHashingService.md5ForEntity(usageStatisticsReportingSqlMapDao.load());
        assertThat(originalMd5, is(not(md5AfterUpdate)));
    }
}
