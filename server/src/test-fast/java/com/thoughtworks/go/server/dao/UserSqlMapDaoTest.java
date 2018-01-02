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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.service.StubGoCache;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.support.TransactionCallback;

import java.util.Arrays;
import java.util.Set;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class UserSqlMapDaoTest {
    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private HibernateTemplate mockHibernateTemplate;
    @Mock
    private TransactionSynchronizationManager transactionSynchronizationManager;
    private StubGoCache goCache;
    private UserSqlMapDao dao;

    @Before
    public void setUp() {
        initMocks(this);
        goCache = new StubGoCache(new TestTransactionSynchronizationManager());
        dao = new UserSqlMapDao(sessionFactory, transactionTemplate, goCache, transactionSynchronizationManager);
    }

    @Test
    public void shouldGetUserNamesForIds() {
        final User foo = new User("foo");
        foo.setId(1);
        final User bar = new User("bar");
        bar.setId(2);
        final User baz = new User("baz");
        bar.setId(3);
        when(transactionTemplate.execute(Matchers.<TransactionCallback>any())).thenReturn(Arrays.asList(foo, bar, baz));

        Set<String> userNames = dao.findUsernamesForIds(s(foo.getId(), bar.getId()));
        assertThat(userNames.size(), is(2));
        assertThat(userNames, hasItems("foo", "bar"));
    }

    @Test
    public void shouldCacheTheEnabledUserCountIfItIsNotInTheCache() throws Exception {
        UserSqlMapDao daoSpy = spy(dao);
        doReturn(mockHibernateTemplate).when(daoSpy).hibernateTemplate();
        doReturn(10).when(mockHibernateTemplate).execute(Matchers.<HibernateCallback<Object>>any());

        Integer firstEnabledUserCount = daoSpy.enabledUserCount();
        Integer secondEnabledUserCount = daoSpy.enabledUserCount();

        assertThat(firstEnabledUserCount, is(10));
        assertThat(secondEnabledUserCount, is(10));

        assertThat(goCache.get(UserSqlMapDao.ENABLED_USER_COUNT_CACHE_KEY), is(10));
        verify(mockHibernateTemplate, times(1)).execute(any(HibernateCallback.class));
    }

    @Test
    public void shouldUseTheCachedValueForEnabledUserCountIfItExists() throws Exception {
        goCache.put(UserSqlMapDao.ENABLED_USER_COUNT_CACHE_KEY, 10);

        Integer firstEnabledUserCount = dao.enabledUserCount();
        Integer secondEnabledUserCount = dao.enabledUserCount();

        assertThat(firstEnabledUserCount, is(10));
        assertThat(secondEnabledUserCount, is(10));

        assertThat(goCache.get(UserSqlMapDao.ENABLED_USER_COUNT_CACHE_KEY), is(10));
        verify(mockHibernateTemplate, times(0)).execute(any(HibernateCallback.class));
    }

    @Test
    public void shouldDoADoubleCheckOfCacheBeforeLoadingFromTheDB() throws Exception {
        GoCache cache = mock(GoCache.class);
        UserSqlMapDao userSqlMapDaoSpy = spy(new UserSqlMapDao(sessionFactory, transactionTemplate, cache, transactionSynchronizationManager));

        doReturn(mockHibernateTemplate).when(userSqlMapDaoSpy).hibernateTemplate();
        doReturn(10).when(mockHibernateTemplate).execute(Matchers.<HibernateCallback<Object>>any());

        Integer firstEnabledUserCount = userSqlMapDaoSpy.enabledUserCount();

        assertThat(firstEnabledUserCount, is(10));
        verify(mockHibernateTemplate, times(1)).execute(any(HibernateCallback.class));
        verify(cache, times(2)).get(UserSqlMapDao.ENABLED_USER_COUNT_CACHE_KEY);
        verify(cache, times(1)).put(UserSqlMapDao.ENABLED_USER_COUNT_CACHE_KEY, 10);
    }
}
