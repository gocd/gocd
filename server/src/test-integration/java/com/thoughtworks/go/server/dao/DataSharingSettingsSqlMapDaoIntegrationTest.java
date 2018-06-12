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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.DataSharingSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class DataSharingSettingsSqlMapDaoIntegrationTest {
    @Autowired
    private DataSharingSettingsSqlMapDao dataSharingSettingsSqlMapDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldSaveMetricsSettings() throws Exception {
        boolean consent = false;
        String consentedBy = "Bob";
        DataSharingSettings dataSharingSettings = new DataSharingSettings(consent, consentedBy, new Date());

        dataSharingSettingsSqlMapDao.saveOrUpdate(dataSharingSettings);

        dataSharingSettings = dataSharingSettingsSqlMapDao.load();
        assertTrue(dataSharingSettings.hasId());
        assertThat(dataSharingSettings.allowSharing(), is(consent));
        assertThat(dataSharingSettings.updatedBy(), is(consentedBy));
    }

    @Test
    public void shouldUpdateMetricsSettings() throws Exception {
        Date updatedOn = new Date();
        DataSharingSettings dataSharingSettings = new DataSharingSettings(true, "me", updatedOn);
        dataSharingSettingsSqlMapDao.saveOrUpdate(dataSharingSettings);

        DataSharingSettings toBeUpdated = dataSharingSettingsSqlMapDao.load();
        toBeUpdated.setAllowSharing(false);
        toBeUpdated.setUpdatedBy("someone-else");
        dataSharingSettingsSqlMapDao.saveOrUpdate(toBeUpdated);

        DataSharingSettings loaded = dataSharingSettingsSqlMapDao.load();
        assertFalse(loaded.allowSharing());
        assertThat(loaded.updatedBy(), is(toBeUpdated.updatedBy()));
        assertThat(loaded.updatedBy(), not(is(dataSharingSettings.updatedBy())));
        assertThat(loaded.updatedOn().getTime(), is(updatedOn.getTime()));
    }

    @Test
    public void shouldAllowOnlyOneInstanceOfMetricsSettingsObjectInDB() throws Exception {
        DataSharingSettings dataSharingSettings1 = new DataSharingSettings(true, "Bob", new Date());
        dataSharingSettingsSqlMapDao.saveOrUpdate(dataSharingSettings1);

        DataSharingSettings dataSharingSettings2 = new DataSharingSettings(false, "John", new Date());
        dataSharingSettingsSqlMapDao.saveOrUpdate(dataSharingSettings2);

        DataSharingSettings saved = dataSharingSettingsSqlMapDao.load();

        assertThat(saved.allowSharing(), is(dataSharingSettings2.allowSharing()));
        assertThat(saved.updatedBy(), is(dataSharingSettings2.updatedBy()));
    }

    @Test
    public void shouldDisallowSavingMetricsSettingsObjectWithADifferentIdIfAnInstanceAlreadyExistsInDb() throws Exception {
        DataSharingSettings dataSharingSettings1 = new DataSharingSettings(true, "Bob", new Date());
        dataSharingSettingsSqlMapDao.saveOrUpdate(dataSharingSettings1);

        DataSharingSettings dataSharingSettings2 = new DataSharingSettings(false, "John", new Date());
        dataSharingSettings2.setId(100);

        Assertions.assertThrows(DataSharingSettingsSqlMapDao.DuplicateDataSharingSettingsException.class, () -> dataSharingSettingsSqlMapDao.saveOrUpdate(dataSharingSettings2));
    }
}

