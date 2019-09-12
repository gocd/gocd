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

import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.transaction.support.TransactionCallback;

import java.util.Arrays;
import java.util.Set;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UserSqlMapDaoTest {
    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private TransactionTemplate transactionTemplate;
    private UserSqlMapDao dao;
    @Mock
    private AccessTokenDao accessTokenDao;

    @Before
    public void setUp() {
        initMocks(this);
        dao = new UserSqlMapDao(sessionFactory, transactionTemplate, accessTokenDao);
    }

    @Test
    public void shouldGetUserNamesForIds() {
        final User foo = new User("foo");
        foo.setId(1L);
        final User bar = new User("bar");
        bar.setId(2L);
        final User baz = new User("baz");
        bar.setId(3L);
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenReturn(Arrays.asList(foo, bar, baz));

        Set<String> userNames = dao.findUsernamesForIds(s(foo.getId(), bar.getId()));
        assertThat(userNames).hasSize(2);
        assertThat(userNames).contains("foo", "bar");
    }

}
