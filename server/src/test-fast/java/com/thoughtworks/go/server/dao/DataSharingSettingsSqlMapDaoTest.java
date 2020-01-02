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
package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.server.cache.CacheKeyGenerator;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.DataSharingSettings;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class DataSharingSettingsSqlMapDaoTest {
    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private GoCache goCache;

    private DataSharingSettingsSqlMapDao settingsSqlMapDao;
    private HashMap<String, Object> internalTestCache;
    private CacheKeyGenerator cacheKeyGenerator = new CacheKeyGenerator(DataSharingSettingsSqlMapDao.class);

    @BeforeEach
    void setUp() {
        initMocks(this);
        settingsSqlMapDao = new DataSharingSettingsSqlMapDao(sessionFactory, transactionTemplate, goCache);

        internalTestCache = new HashMap<>();
        when(goCache.get(getCacheKey())).then((Answer<DataSharingSettings>) invocation -> (DataSharingSettings) internalTestCache.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> {
            internalTestCache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(goCache).put(anyString(), any(DataSharingSettings.class));
        when(goCache.remove(getCacheKey())).then((Answer<DataSharingSettings>) invocation -> (DataSharingSettings) internalTestCache.remove(invocation.<String>getArgument(0)));
    }

    @Test
    void shouldLoadDataSharingSettingsFromDBOnFirstCall() {
        DataSharingSettings settings = new DataSharingSettings(true, "Bob", new Date());
        when(transactionTemplate.execute(any())).thenReturn(settings);

        assertNull(internalTestCache.get(getCacheKey()));
        DataSharingSettings loaded = settingsSqlMapDao.load();
        assertThat(internalTestCache.get(getCacheKey()), is(settings));

        assertThat(loaded, is(settings));
        verify(transactionTemplate, times(1)).execute(any());
    }

    @Test
    void shouldReturnDataSharingSettingsFromCacheOnSubsequentCalls() {
        DataSharingSettings settings = new DataSharingSettings(true, "Bob", new Date());
        when(transactionTemplate.execute(any())).thenReturn(settings);

        assertNull(internalTestCache.get(getCacheKey()));
        DataSharingSettings loaded = settingsSqlMapDao.load();
        assertThat(internalTestCache.get(getCacheKey()), is(settings));
        assertThat(loaded, is(settings));

        DataSharingSettings loadedAgain = settingsSqlMapDao.load();
        assertThat(internalTestCache.get(getCacheKey()), is(settings));
        assertThat(loadedAgain, is(settings));

        verify(transactionTemplate, times(1)).execute(any());
    }

    @Test
    void invalidateCacheShouldRemoveCachedObjectFromGoCache() {
        assertNull(internalTestCache.get(getCacheKey()));
        DataSharingSettings loaded = settingsSqlMapDao.load();
        assertThat(internalTestCache.get(getCacheKey()), is(loaded));
        settingsSqlMapDao.invalidateCache();
        assertNull(internalTestCache.get(getCacheKey()));
    }

    private String getCacheKey() {
        return cacheKeyGenerator.generate("dataSharing_settings");
    }
}
