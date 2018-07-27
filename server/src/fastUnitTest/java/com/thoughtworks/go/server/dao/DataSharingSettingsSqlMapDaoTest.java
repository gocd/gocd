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

import com.thoughtworks.go.server.domain.DataSharingSettings;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Date;

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

    private DataSharingSettingsSqlMapDao settingsSqlMapDao;

    @BeforeEach
    void setUp() {
        initMocks(this);
        settingsSqlMapDao = new DataSharingSettingsSqlMapDao(sessionFactory, transactionTemplate, transactionSynchronizationManager);
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
