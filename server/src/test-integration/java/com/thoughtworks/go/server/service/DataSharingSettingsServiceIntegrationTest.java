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

import com.thoughtworks.go.domain.DataSharingSettings;
import com.thoughtworks.go.server.dao.DataSharingSettingsSqlMapDao;
import com.thoughtworks.go.server.dao.DataSharingSettingsSqlMapDao.DuplicateDataSharingSettingsException;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

import com.thoughtworks.go.server.dao.*;
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
public class DataSharingSettingsServiceIntegrationTest {
    @Autowired
    private DataSharingSettingsService dataSharingSettingsService;
    @Autowired
    private DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao;
    @Autowired
    private EntityHashingService entityHashingService;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        dataSharingSettingsService.initialize();
    }

    @After
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
        assertTrue(dataSharingSettings.allowSharing());
    }

    @Test
    public void shouldFetchDataSharingSettings() throws Exception {
        DataSharingSettings saved = new DataSharingSettings(true, "Bob", new Date());
        dataSharingSettingsSqlMapDao.saveOrUpdate(saved);

        DataSharingSettings loaded = dataSharingSettingsService.get();

        assertThat(loaded, is(saved));
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
        dataSharingSettingsService.update(new DataSharingSettings(newConsent, consentedBy, new Date()));

        DataSharingSettings loaded = dataSharingSettingsSqlMapDao.load();
        assertThat(loaded.allowSharing(), is(newConsent));
        assertThat(loaded.updatedBy(), is(consentedBy));

        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldUpdateMd5SumOfDataSharingSettingsUponSave() throws DuplicateDataSharingSettingsException {
        String originalMd5 = entityHashingService.md5ForEntity(dataSharingSettingsSqlMapDao.load());
        assertThat(originalMd5, is(not(nullValue())));
        dataSharingSettingsService.update(new DataSharingSettings(true, "me", new Date()));

        String md5AfterUpdate = entityHashingService.md5ForEntity(dataSharingSettingsSqlMapDao.load());
        assertThat(originalMd5, is(not(md5AfterUpdate)));
    }
}
