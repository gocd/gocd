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

import com.thoughtworks.go.server.cache.CacheKeyGenerator;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.DataSharingSettings;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Timestamp;
import java.util.Date;
import com.thoughtworks.go.server.dao.*;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;
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
    private EntityHashingService entityHashingService;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoCache goCache;

    private CacheKeyGenerator cacheKeyGenerator = new CacheKeyGenerator(DataSharingSettingsSqlMapDao.class);

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @BeforeEach
    public void setup() throws Exception {
        dbHelper.onSetUp();
        dataSharingSettingsService.initialize();
    }

    @AfterEach
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        goCache.remove(cacheKey());
    }

    @Test
    public void shouldInitializeDataSharingSettingsOnFirstStartup() throws Exception {
        dbHelper.onTearDown();//to start on a clean slate
        goCache.remove(cacheKey());

        assertNull(dataSharingSettingsSqlMapDao.load());
        dataSharingSettingsService.initialize();
        DataSharingSettings dataSharingSettings = dataSharingSettingsSqlMapDao.load();
        assertNotNull(dataSharingSettings);
    }

    @Test
    public void shouldNotReInitializeDataSharingSettingsOnSubsequentStartups() throws Exception {
        dbHelper.onTearDown();//to start on a clean slate
        goCache.remove(cacheKey());

        assertNull(dataSharingSettingsSqlMapDao.load());
        dataSharingSettingsService.initialize();
        DataSharingSettings dataSharingSettings = dataSharingSettingsSqlMapDao.load();
        assertNotNull(dataSharingSettings);
        assertTrue(dataSharingSettings.allowSharing());
    }

    @Test
    public void shouldFetchDataSharingSettings() throws Exception {
        DataSharingSettings saved = new DataSharingSettings(true, "Bob", new Date());
        dataSharingSettingsService.createOrUpdate(saved);

        DataSharingSettings loaded = dataSharingSettingsService.get();

        assertThat(goCache.get(cacheKey()), is(loaded));
        assertThat(loaded, is(saved));
    }

    @Test
    public void shouldUpdateDataSharingSettings() throws Exception {
        DataSharingSettings existing = dataSharingSettingsService.get();
        assertThat(goCache.get(cacheKey()), is(existing));
        assertNotNull(existing);
        assertThat(existing.allowSharing(), is(true));
        assertThat(existing.updatedBy(), is("Default"));

        boolean newConsent = false;
        String consentedBy = "Bob";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        dataSharingSettingsService.createOrUpdate(new DataSharingSettings(newConsent, consentedBy, new Date()));
        assertNull(goCache.get(cacheKey()));

        DataSharingSettings loaded = dataSharingSettingsService.get();
        assertThat(loaded.allowSharing(), is(newConsent));
        assertThat(loaded.updatedBy(), is(consentedBy));
        assertThat(goCache.get(cacheKey()), is(loaded));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldFlushEtagCacheForDataSharingSettingsOnUpdate() {
        DataSharingSettings existing = dataSharingSettingsService.get();
        String settingsEtagCacheKey = existing.getClass().getName() + "." + "data_sharing_settings";
        goCache.put("GO_ETAG_CACHE", settingsEtagCacheKey, "existing-etag-in-cache");

        assertNotNull(goCache.get("GO_ETAG_CACHE", settingsEtagCacheKey));
        dataSharingSettingsService.createOrUpdate(new DataSharingSettings(false, "Bob", new Date()));
        assertNull(goCache.get("GO_ETAG_CACHE", settingsEtagCacheKey));
    }

    @Test
    public void shouldUpdateMd5SumOfDataSharingSettingsUponSave() {
        DataSharingSettings loaded = dataSharingSettingsService.get();

        String originalMd5 = entityHashingService.hashForEntity(loaded);
        assertThat(originalMd5, is(not(nullValue())));

        loaded.setAllowSharing(true);
        loaded.setUpdatedBy("me");
        loaded.setUpdatedOn(Timestamp.from(new Date().toInstant()));
        dataSharingSettingsService.createOrUpdate(loaded);

        String md5AfterUpdate = entityHashingService.hashForEntity(dataSharingSettingsService.get());
        assertThat(originalMd5, is(not(md5AfterUpdate)));
    }

    @Test
    public void shouldAllowOnlyOneInstanceOfMetricsSettingsObjectInDB() throws Exception {
        DataSharingSettings dataSharingSettings1 = new DataSharingSettings(true, "Bob", new Date());
        dataSharingSettingsService.createOrUpdate(dataSharingSettings1);

        DataSharingSettings dataSharingSettings2 = new DataSharingSettings(false, "John", new Date());
        dataSharingSettingsService.createOrUpdate(dataSharingSettings2);

        DataSharingSettings saved = dataSharingSettingsService.get();

        Assert.assertThat(saved.allowSharing(), is(dataSharingSettings2.allowSharing()));
        Assert.assertThat(saved.updatedBy(), is(dataSharingSettings2.updatedBy()));
    }

    @Test
    public void shouldDisallowSavingMetricsSettingsObjectWithADifferentIdIfAnInstanceAlreadyExistsInDb() throws Exception {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectCause(isA(DataSharingSettingsSqlMapDao.DuplicateDataSharingSettingsException.class));

        DataSharingSettings dataSharingSettings1 = new DataSharingSettings(true, "Bob", new Date());
        dataSharingSettingsService.createOrUpdate(dataSharingSettings1);

        DataSharingSettings dataSharingSettings2 = new DataSharingSettings(false, "John", new Date());
        dataSharingSettings2.setId(100);

        dataSharingSettingsService.createOrUpdate(dataSharingSettings2);
    }

    private String cacheKey() {
        return cacheKeyGenerator.generate("dataSharing_settings");
    }

}
