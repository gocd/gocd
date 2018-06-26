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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.thoughtworks.go.server.dao.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
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
    private DataSharingSettingsService dataSharingSettingsService;
    @Autowired
    private UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        dataSharingUsageStatisticsReportingService.initialize();
        dataSharingSettingsService.initialize();
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
    public void shouldUpdateUsageStatisticsReporting() {
        UsageStatisticsReporting existing = usageStatisticsReportingSqlMapDao.load();
        assertNotNull(existing);
        assertNotNull(existing.lastReportedAt());

        dataSharingUsageStatisticsReportingService.updateLastReportedTime();

        UsageStatisticsReporting loaded = usageStatisticsReportingSqlMapDao.load();
        assertThat(loaded.lastReportedAt().toInstant(), is(not(existing.lastReportedAt())));
    }
}
