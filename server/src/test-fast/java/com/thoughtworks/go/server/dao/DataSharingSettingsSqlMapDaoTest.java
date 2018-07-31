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

import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.DataSharingSettings;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class DataSharingSettingsSqlMapDaoTest {
    @Mock
    private TransactionSynchronizationManager transactionSynchronizationManager;
    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private GoCache goCache;

    private DataSharingSettingsSqlMapDao settingsSqlMapDao;

    @BeforeEach
    void setUp() {
        initMocks(this);
        settingsSqlMapDao = new DataSharingSettingsSqlMapDao(sessionFactory, transactionTemplate, transactionSynchronizationManager, goCache);

        HashMap<String, Object> internalTestCache = new HashMap<>();
        when(goCache.get(anyString())).then((Answer<DataSharingSettings>) invocation -> (DataSharingSettings) internalTestCache.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> {
            internalTestCache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(goCache).put(anyString(), any(DataSharingSettings.class));
        when(goCache.remove(anyString())).then((Answer<DataSharingSettings>) invocation -> (DataSharingSettings) internalTestCache.remove(invocation.<String>getArgument(0)));
    }

    @Test
    void shouldLoadDataSharingSettingsFromDBOnFirstCall() {
        DataSharingSettings settings = new DataSharingSettings(true, "Bob", new Date());
        when(transactionTemplate.execute(any())).thenReturn(settings);
        DataSharingSettings loaded = settingsSqlMapDao.load();

        assertThat(loaded, is(settings));
        verify(transactionTemplate, times(1)).execute(any());
    }

    @Test
    void shouldReturnDataSharingSettingsFromCacheOnSubsequentCalls() {
        DataSharingSettings settings = new DataSharingSettings(true, "Bob", new Date());
        when(transactionTemplate.execute(any())).thenReturn(settings);

        DataSharingSettings loaded = settingsSqlMapDao.load();
        assertThat(loaded, is(settings));

        DataSharingSettings loadedAgain = settingsSqlMapDao.load();
        assertThat(loadedAgain, is(settings));

        verify(transactionTemplate, times(1)).execute(any());
    }

    @Test
    void shouldInvalidateCacheOnUpdate() throws Exception {
        DataSharingSettings settings = new DataSharingSettings(true, "Bob", new Date());
        when(transactionTemplate.execute(any())).thenReturn(settings);

        //load once
        settingsSqlMapDao.load();

        //update
        settingsSqlMapDao.saveOrUpdate(new DataSharingSettings(false, "Bob", new Date()));

        //load again
        settingsSqlMapDao.load();

        verify(transactionTemplate, times(2)).execute(any());
    }
}
