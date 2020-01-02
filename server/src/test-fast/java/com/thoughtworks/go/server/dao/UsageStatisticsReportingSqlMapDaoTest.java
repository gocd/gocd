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

import com.thoughtworks.go.domain.UsageStatisticsReporting;
import com.thoughtworks.go.server.cache.CacheKeyGenerator;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
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

class UsageStatisticsReportingSqlMapDaoTest {
    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private TransactionSynchronizationManager synchronizationManager;
    @Mock
    private GoCache goCache;

    private UsageStatisticsReportingSqlMapDao reportingSqlMapDao;
    private HashMap<String, Object> internalTestCache;
    private CacheKeyGenerator cacheKeyGenerator = new CacheKeyGenerator(UsageStatisticsReportingSqlMapDao.class);

    @BeforeEach
    void setUp() {
        initMocks(this);
        reportingSqlMapDao = new UsageStatisticsReportingSqlMapDao(sessionFactory, transactionTemplate, synchronizationManager, goCache);

        internalTestCache = new HashMap<>();
        when(goCache.get(getCacheKey())).then((Answer<UsageStatisticsReporting>) invocation -> (UsageStatisticsReporting) internalTestCache.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> {
            internalTestCache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(goCache).put(anyString(), any(UsageStatisticsReporting.class));
        when(goCache.remove(getCacheKey())).then((Answer<UsageStatisticsReporting>) invocation -> (UsageStatisticsReporting) internalTestCache.remove(invocation.<String>getArgument(0)));
    }

    @Test
    void shouldLoadUsageStatisticsReportingFromDBOnFirstCall() {
        UsageStatisticsReporting usageStatisticsReporting = new UsageStatisticsReporting("server-id", new Date());
        when(transactionTemplate.execute(any())).thenReturn(usageStatisticsReporting);

        assertNull(internalTestCache.get(getCacheKey()));
        UsageStatisticsReporting loaded = reportingSqlMapDao.load();
        assertThat(internalTestCache.get(getCacheKey()), is(usageStatisticsReporting));

        assertThat(loaded, is(usageStatisticsReporting));
        verify(transactionTemplate, times(1)).execute(any());
    }

    @Test
    void shouldReturnUsageStatisticsReportingFromCacheOnSubsequentCalls() {
        UsageStatisticsReporting usageStatisticsReporting = new UsageStatisticsReporting("server-id", new Date());
        when(transactionTemplate.execute(any())).thenReturn(usageStatisticsReporting);

        assertNull(internalTestCache.get(getCacheKey()));
        UsageStatisticsReporting loaded = reportingSqlMapDao.load();
        assertThat(internalTestCache.get(getCacheKey()), is(usageStatisticsReporting));
        assertThat(loaded, is(usageStatisticsReporting));

        UsageStatisticsReporting loadedAgain = reportingSqlMapDao.load();
        assertThat(internalTestCache.get(getCacheKey()), is(usageStatisticsReporting));
        assertThat(loadedAgain, is(usageStatisticsReporting));

        verify(transactionTemplate, times(1)).execute(any());
    }

    @Test
    void shouldInvalidateCacheOnUpdate() throws Exception {
        UsageStatisticsReporting usageStatisticsReporting = new UsageStatisticsReporting("server-id", new Date());
        when(transactionTemplate.execute(any())).thenReturn(usageStatisticsReporting);

        assertNull(internalTestCache.get(getCacheKey()));
        //load once
        reportingSqlMapDao.load();
        assertThat(internalTestCache.get(getCacheKey()), is(usageStatisticsReporting));

        //update
        UsageStatisticsReporting updated = new UsageStatisticsReporting("server-id", new Date());
        reportingSqlMapDao.saveOrUpdate(updated);

        //load again
        reportingSqlMapDao.load();
        assertThat(internalTestCache.get(getCacheKey()), is(updated));

        verify(transactionTemplate, times(2)).execute(any());
    }

    private String getCacheKey() {
        return cacheKeyGenerator.generate("dataSharing_reporting");
    }
}
