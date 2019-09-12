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

import com.thoughtworks.go.domain.DataSharingSettings;
import com.thoughtworks.go.server.dao.DataSharingSettingsSqlMapDao;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Timestamp;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class DataSharingSettingsServiceIntegrationTest {
    @Autowired
    private DataSharingSettingsService dataSharingSettingsService;
    @Autowired
    private DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    @BeforeEach
    public void setup() throws Exception {
        dbHelper.onSetUp();
        dataSharingSettingsService.initialize();
    }

    @AfterEach
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldInitializeDataSharingSettingsOnFirstStartup() throws Exception {
        dbHelper.onTearDown();//to start on a clean slate

        assertNull(dataSharingSettingsSqlMapDao.load());
        dataSharingSettingsService.initialize();
        DataSharingSettings dataSharingSettings = dataSharingSettingsSqlMapDao.load();
        assertNotNull(dataSharingSettings);
    }

    @Test
    public void shouldNotReInitializeDataSharingSettingsOnSubsequentStartups() throws Exception {
        dbHelper.onTearDown();//to start on a clean slate

        assertNull(dataSharingSettingsSqlMapDao.load());
        dataSharingSettingsService.initialize();
        DataSharingSettings dataSharingSettings = dataSharingSettingsSqlMapDao.load();
        assertNotNull(dataSharingSettings);
        assertTrue(dataSharingSettings.isAllowSharing());
    }

    @Test
    public void shouldFetchDataSharingSettings() throws Exception {
        DataSharingSettings saved = new DataSharingSettings().setAllowSharing(true)
                .setUpdatedBy("Bob")
                .setUpdatedOn(new Timestamp(new Date().getTime()));
        dataSharingSettingsService.createOrUpdate(saved);

        DataSharingSettings loaded = dataSharingSettingsService.load();
        assertThat(loaded, is(saved));
    }

    @Test
    public void shouldUpdateDataSharingSettings() throws Exception {
        DataSharingSettings existing = dataSharingSettingsService.load();
        assertNotNull(existing);
        assertThat(existing.isAllowSharing(), is(true));
        assertThat(existing.getUpdatedBy(), is("Default"));

        boolean newConsent = false;
        String consentedBy = "Bob";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        dataSharingSettingsService.createOrUpdate(new DataSharingSettings().setAllowSharing(newConsent)
                .setUpdatedBy(consentedBy)
                .setUpdatedOn(new Timestamp(new Date().getTime())));

        DataSharingSettings loaded = dataSharingSettingsService.load();
        assertThat(loaded.isAllowSharing(), is(newConsent));
        assertThat(loaded.getUpdatedBy(), is(consentedBy));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldAllowOnlyOneInstanceOfMetricsSettingsObjectInDB() throws Exception {
        DataSharingSettings dataSharingSettings1 = new DataSharingSettings().setAllowSharing(true)
                .setUpdatedBy("Bob")
                .setUpdatedOn(new Timestamp(new Date().getTime()));
        dataSharingSettingsService.createOrUpdate(dataSharingSettings1);

        DataSharingSettings dataSharingSettings2 = new DataSharingSettings().setAllowSharing(false)
                .setUpdatedBy("John")
                .setUpdatedOn(new Timestamp(new Date().getTime()));
        dataSharingSettingsService.createOrUpdate(dataSharingSettings2);

        DataSharingSettings saved = dataSharingSettingsService.load();

        Assert.assertThat(saved.isAllowSharing(), is(dataSharingSettings2.isAllowSharing()));
        Assert.assertThat(saved.getUpdatedBy(), is(dataSharingSettings2.getUpdatedBy()));
    }

    @Test
    public void shouldReplaceExistingDataSharingSegting() throws Exception {
        DataSharingSettings dataSharingSettings1 = new DataSharingSettings().setAllowSharing(true)
                .setUpdatedBy("Bob")
                .setUpdatedOn(new Timestamp(new Date().getTime()));
        dataSharingSettingsService.createOrUpdate(dataSharingSettings1);

        DataSharingSettings dataSharingSettings2 = new DataSharingSettings().setAllowSharing(false)
                .setUpdatedBy("John")
                .setUpdatedOn(new Timestamp(new Date().getTime()));

        dataSharingSettingsService.createOrUpdate(dataSharingSettings2);
        assertThat(dataSharingSettingsService.load(), is(dataSharingSettings2));
    }
}

