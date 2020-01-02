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

import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.server.cache.CacheKeyGenerator;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.thoughtworks.go.server.dao.*;

import java.sql.Timestamp;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
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
    @Autowired
    private GoCache goCache;

    private CacheKeyGenerator cacheKeyGenerator = new CacheKeyGenerator(UsageStatisticsReportingSqlMapDao.class);

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        dataSharingUsageStatisticsReportingService.initialize();
        dataSharingSettingsService.initialize();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        goCache.remove(cacheKey());
    }

    @Test
    public void shouldInitializeGoStatsReportingOnFirstStartup() throws Exception {
        dbHelper.onTearDown();//to start on a clean slate
        goCache.remove(cacheKey());

        assertNull(usageStatisticsReportingSqlMapDao.load());
        dataSharingUsageStatisticsReportingService.initialize();
        UsageStatisticsReporting usageStatisticsReporting = usageStatisticsReportingSqlMapDao.load();
        assertNotNull(usageStatisticsReporting);
        assertNotNull(usageStatisticsReporting.getServerId());
        assertThat(usageStatisticsReporting.lastReportedAt(), is(new Timestamp(0)));
    }

    @Test
    public void shouldNotUpdateServerIdOfGoStatsReportingOnSubsequentStartups() throws Exception {
        dbHelper.onTearDown();//to start on a clean slate
        goCache.remove(cacheKey());

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
        UsageStatisticsReporting existing = dataSharingUsageStatisticsReportingService.get();
        assertNotNull(existing);
        Timestamp existingReportedTime = existing.lastReportedAt();
        assertNotNull(existingReportedTime);
        dataSharingUsageStatisticsReportingService.startReporting(new HttpLocalizedOperationResult());

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        dataSharingUsageStatisticsReportingService.completeReporting(result);
        assertTrue(result.isSuccessful());

        UsageStatisticsReporting loaded = dataSharingUsageStatisticsReportingService.get();

        assertThat(loaded.lastReportedAt().getTime(), greaterThan(existingReportedTime.getTime()));
    }

    private String cacheKey() {
        return cacheKeyGenerator.generate("dataSharing_reporting");
    }
}
