/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.transaction;

import java.sql.SQLException;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapExecutor;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.database.DatabaseStrategy;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ibatis.SqlMapClientCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class SqlMapClientDaoSupportTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoCache goCache;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private TransactionSynchronizationManager synchronizationManager;
    @Autowired private SqlMapClient sqlMapClient;
    @Autowired private UserDao userDao;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private DatabaseStrategy databaseStrategy;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private SqlMapClientDaoSupport daoSupport;
    private TransactionCacheAssertionUtil assertionUtil;

    @Before
    public void setUp() throws Exception {
        assertionUtil = new TransactionCacheAssertionUtil(goCache, transactionTemplate);
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        dbHelper.onSetUp();
        goCache.clear();
        daoSupport = new SqlMapClientDaoSupport(goCache, sqlMapClient, systemEnvironment, databaseStrategy);
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldOptOutOfCacheServing_forInsert() {
        assertionUtil.assertCacheBehaviourInTxn(new TransactionCacheAssertionUtil.DoInTxn() {
            public void invoke() {
                userDao.saveOrUpdate(new User("loser", "Massive Loser", "boozer@loser.com"));
            }
        });
        assertThat(userDao.findUser("loser").getEmail(), is("boozer@loser.com"));
    }

    @Test
    @Ignore("Moving to hibernate. Our caching will become obsolete soon. Also we do not delete entities from our application.")
    public void shouldOptOutOfCacheServing_forDelete() {
        User loser = new User("loser");
        userDao.saveOrUpdate(loser);

        assertionUtil.assertCacheBehaviourInTxn(new TransactionCacheAssertionUtil.DoInTxn() {
            public void invoke() {
                daoSupport.getSqlMapClientTemplate().delete("delete-all-users");
            }
        });

        goCache.clear();
        assertThat(userDao.findUser("loser"), is(instanceOf(NullUser.class)));
    }

    @Test
    public void shouldNotOptOutOfCacheServing_whenQueryingObjects() {
        final User loser = new User("loser");
        userDao.saveOrUpdate(loser);
        final User[] loadedUser = new User[1];

        assertThat(assertionUtil.doInTxnWithCachePut(new TransactionCacheAssertionUtil.DoInTxn() {
            public void invoke() {
                loadedUser[0] = userDao.findUser(loser.getName());
            }
        }), is("boozer"));

        assertThat(loadedUser[0].getName(), is("loser"));
    }
    
    @Test
    public void shouldNotOptOutOfCacheServing_whenQueryingList() {
        final User loser = new User("loser");
        userDao.saveOrUpdate(loser);

        final User[] loadedUser = new User[1];

        assertThat(assertionUtil.doInTxnWithCachePut(new TransactionCacheAssertionUtil.DoInTxn() {
            public void invoke() {
                loadedUser[0] = userDao.allUsers().get(0);
            }
        }), is("boozer"));

        assertThat(loadedUser[0].getName(), is("loser"));
    }

    @Test
    public void shouldNotAllowDirectInvocationOfExecute() {
        User loser = new User("loser");
        userDao.saveOrUpdate(loser);

        final User[] loadedUser = new User[1];

        try {
            assertionUtil.doInTxnWithCachePut(new TransactionCacheAssertionUtil.DoInTxn() {
                    public void invoke() {
                        daoSupport.getSqlMapClientTemplate().execute(new SqlMapClientCallback() {
                            public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
                                loadedUser[0] = userDao.allUsers().get(0);
                                return null;
                            }
                        });
                    }
                });
            fail("should not have allowed direct execute invocation");
        } catch (Exception e) {
            assertThat(e, is(instanceOf(UnsupportedOperationException.class)));
            assertThat(e.getMessage(),
                    is("Please call one of the supported methods. Refer " + SqlMapClientDaoSupport.SqlMapClientTemplate.class.getCanonicalName() + " for details. This is to ensure read consistency during transactions."));
        }

        assertThat(loadedUser[0], is(nullValue()));
    }
}
