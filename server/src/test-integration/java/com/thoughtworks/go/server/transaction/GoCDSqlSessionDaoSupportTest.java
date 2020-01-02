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
package com.thoughtworks.go.server.transaction;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class GoCDSqlSessionDaoSupportTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoCache goCache;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private UserDao userDao;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private TransactionCacheAssertionUtil assertionUtil;

    @Before
    public void setUp() throws Exception {
        assertionUtil = new TransactionCacheAssertionUtil(goCache, transactionTemplate);
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        dbHelper.onSetUp();
        goCache.clear();
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldOptOutOfCacheServing_forInsert() {
        assertionUtil.assertCacheBehaviourInTxn(new TransactionCacheAssertionUtil.DoInTxn() {
            @Override
            public void invoke() {
                userDao.saveOrUpdate(new User("loser", "Massive Loser", "boozer@loser.com"));
            }
        });
        assertThat(userDao.findUser("loser").getEmail(), is("boozer@loser.com"));
    }

    @Test
    public void shouldNotOptOutOfCacheServing_whenQueryingObjects() {
        final User loser = new User("loser");
        userDao.saveOrUpdate(loser);
        final User[] loadedUser = new User[1];

        assertThat(assertionUtil.doInTxnWithCachePut(new TransactionCacheAssertionUtil.DoInTxn() {
            @Override
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
            @Override
            public void invoke() {
                loadedUser[0] = userDao.allUsers().get(0);
            }
        }), is("boozer"));

        assertThat(loadedUser[0].getName(), is("loser"));
    }

}
