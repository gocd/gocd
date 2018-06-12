/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.DataSharingSettings;
import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.server.dao.DataSharingSettingsSqlMapDao.DuplicateDataSharingSettingsException;
import com.thoughtworks.go.server.dao.UsageStatisticsReportingSqlMapDao;
import com.thoughtworks.go.server.dao.DataSharingSettingsSqlMapDao;
import com.thoughtworks.go.server.dao.UsageStatisticsReportingSqlMapDao.DuplicateMetricReporting;
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
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class DataSharingServiceIntegrationTest {
    @Autowired
    private DataSharingService dataSharingService;
    @Autowired
    private UsageStatisticsReportingSqlMapDao usageStatisticsReportingSqlMapDao;
    @Autowired
    private DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao;
    @Autowired
    private EntityHashingService entityHashingService;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        dataSharingService.initialize();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldInitializeGoStatsReportingOnFirstStartup() throws Exception {
        dbHelper.onTearDown();//to start on a clean slate

        assertNull(usageStatisticsReportingSqlMapDao.load());
        dataSharingService.initialize();
        UsageStatisticsReporting usageStatisticsReporting = usageStatisticsReportingSqlMapDao.load();
        assertNotNull(usageStatisticsReporting);
        assertNotNull(usageStatisticsReporting.getServerId());
    }

    @Test
    public void shouldNotUpdateServerIdOfGoStatsReportingOnSubsequentStartups() throws Exception {
        dbHelper.onTearDown();//to start on a clean slate

        assertNull(usageStatisticsReportingSqlMapDao.load());
        dataSharingService.initialize();
        UsageStatisticsReporting saved = usageStatisticsReportingSqlMapDao.load();
        assertNotNull(saved);
        dataSharingService.initialize();
        UsageStatisticsReporting loaded = usageStatisticsReportingSqlMapDao.load();
        assertThat(saved.getServerId(), is(loaded.getServerId()));
    }

    @Test
    public void shouldInitializeDataSharingSettingsOnFirstStartup() throws Exception {
        dbHelper.onTearDown();//to start on a clean slate

        assertNull(dataSharingSettingsSqlMapDao.load());
        dataSharingService.initialize();
        DataSharingSettings dataSharingSettings = dataSharingSettingsSqlMapDao.load();
        assertNotNull(dataSharingSettings);
    }

    @Test
    public void shouldNotReInitializeDataSharingSettingsOnSubsequentStartups() throws Exception {
        dbHelper.onTearDown();//to start on a clean slate

        assertNull(dataSharingSettingsSqlMapDao.load());
        dataSharingService.initialize();
        DataSharingSettings dataSharingSettings = dataSharingSettingsSqlMapDao.load();
        assertNotNull(dataSharingSettings);
        assertTrue(dataSharingSettings.allowSharing());
    }

    @Test
    public void shouldFetchDataSharingSettings() throws Exception {
        DataSharingSettings saved = new DataSharingSettings(true, "Bob", new Date());
        dataSharingSettingsSqlMapDao.saveOrUpdate(saved);

        DataSharingSettings loaded = dataSharingSettingsSqlMapDao.load();

        assertThat(loaded, is(saved));
    }

    @Test
    public void shouldUpdateUsageStatistisReporting() throws Exception {
        UsageStatisticsReporting existing = usageStatisticsReportingSqlMapDao.load();
        assertNotNull(existing);
        assertThat(existing.lastReportedAt(), is(new Timestamp(0)));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        UsageStatisticsReporting reporting = new UsageStatisticsReporting();
        Date lastReportedAt = new Date();
        reporting.setLastReportedAt(lastReportedAt);
        dataSharingService.updateUsageStatisticsReporting(reporting, result);

        UsageStatisticsReporting loaded = usageStatisticsReportingSqlMapDao.load();
        assertThat(loaded.lastReportedAt().toInstant(), is(lastReportedAt.toInstant()));

        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldThrowErrorOccuredWhileUpdatingUsageStatisticsReportingWithIncorrectTime() throws Exception {
        UsageStatisticsReporting existing = usageStatisticsReportingSqlMapDao.load();
        assertThat(existing.lastReportedAt(), is(new Timestamp(0)));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        dataSharingService.updateUsageStatisticsReporting(new UsageStatisticsReporting(), result);

        UsageStatisticsReporting loaded = usageStatisticsReportingSqlMapDao.load();
        assertThat(loaded.lastReportedAt(), is(new Timestamp(0)));

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is("Validations failed. Please correct and resubmit."));
    }

    @Test
    public void shouldUpdateDataSharingSettings() throws Exception {
        DataSharingSettings existing = dataSharingSettingsSqlMapDao.load();
        assertNotNull(existing);
        assertThat(existing.allowSharing(), is(true));
        assertThat(existing.updatedBy(), is("Default"));

        boolean newConsent = false;
        String consentedBy = "Bob";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        dataSharingService.updateDataSharingSettings(new DataSharingSettings(newConsent, consentedBy, new Date()));

        DataSharingSettings loaded = dataSharingSettingsSqlMapDao.load();
        assertThat(loaded.allowSharing(), is(newConsent));
        assertThat(loaded.updatedBy(), is(consentedBy));

        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldUpdateMd5SumOfDataSharingSettingsUponSave() throws DuplicateDataSharingSettingsException {
        String originalMd5 = entityHashingService.md5ForEntity(dataSharingSettingsSqlMapDao.load());
        assertThat(originalMd5, is(not(nullValue())));
        dataSharingService.updateDataSharingSettings(new DataSharingSettings(true, "me", new Date()));

        String md5AfterUpdate = entityHashingService.md5ForEntity(dataSharingSettingsSqlMapDao.load());
        assertThat(originalMd5, is(not(md5AfterUpdate)));
    }

    @Test
    public void shouldUpdateMd5SumOfUsageStatisticsReportingUponSave() throws DuplicateMetricReporting {
        String originalMd5 = entityHashingService.md5ForEntity(usageStatisticsReportingSqlMapDao.load());
        assertThat(originalMd5, is(not(nullValue())));
        UsageStatisticsReporting reporting = new UsageStatisticsReporting();
        reporting.setLastReportedAt(new Date());
        dataSharingService.updateUsageStatisticsReporting(reporting, new HttpLocalizedOperationResult());

        String md5AfterUpdate = entityHashingService.md5ForEntity(usageStatisticsReportingSqlMapDao.load());
        assertThat(originalMd5, is(not(md5AfterUpdate)));
    }
}
