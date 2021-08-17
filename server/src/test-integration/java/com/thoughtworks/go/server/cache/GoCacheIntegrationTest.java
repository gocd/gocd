/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.cache;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.UserSqlMapDao;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class GoCacheIntegrationTest {
    @Autowired
    private GoCache goCache;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private TransactionSynchronizationManager transactionSynchronizationManager;
    @Autowired
    private SqlSessionFactory sqlMapClient;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private UserSqlMapDao userSqlMapDao;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @BeforeEach
    public void setUp() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        dbHelper.onSetUp();
        goCache.clear();
    }

    @AfterEach
    public void tearDown() throws Exception {
        goCache.clear();
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void put_shouldNotUpdateCacheWhenInTransaction() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                Object o = new Object();
                goCache.put("someKey", o);
            }
        });
        assertNull(goCache.get("someKey"));
    }

    @Test
    public void shouldStartServingThingsOutOfCacheOnceTransactionCompletes() {
        final SqlSessionDaoSupport daoSupport = new SqlSessionDaoSupport() {

        };

        daoSupport.setSqlSessionFactory(sqlMapClient);
        goCache.put("foo", "bar");
        final String[] valueInCleanTxn = new String[1];
        final String[] valueInDirtyTxn = new String[1];
        final String[] valueInAfterCommit = new String[1];
        final String[] valueInAfterCompletion = new String[1];

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                valueInCleanTxn[0] = (String) goCache.get("foo");
                User user = new User("loser", "Massive Loser", "boozer@loser.com");
                userSqlMapDao.saveOrUpdate(user);
                valueInDirtyTxn[0] = (String) goCache.get("foo");
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        valueInAfterCommit[0] = (String) goCache.get("foo");
                    }

                    @Override
                    public void afterCompletion(int status) {
                        valueInAfterCompletion[0] = (String) goCache.get("foo");
                    }
                });
            }
        });
        assertThat(valueInCleanTxn[0], is("bar"));
        assertThat(valueInDirtyTxn[0], is(nullValue()));
        assertThat(valueInAfterCommit[0], is("bar"));
        assertThat(valueInAfterCompletion[0], is("bar"));
    }

}
