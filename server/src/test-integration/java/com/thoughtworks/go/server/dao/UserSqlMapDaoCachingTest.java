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

import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.domain.StageEvent;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.cache.GoCache;
import org.hamcrest.Matchers;
import org.hibernate.SessionFactory;
import org.hibernate.stat.SecondLevelCacheStatistics;
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
public class UserSqlMapDaoCachingTest {
    @Autowired
    private UserSqlMapDao userDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoCache goCache;
    @Autowired
    private SessionFactory sessionFactory;

    @Before
    public void setup() throws Exception {
        sessionFactory.getStatistics().clear();
        dbHelper.onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        sessionFactory.getStatistics().clear();
    }

    @Test
    public void shouldCacheUserOnFind() {
        User first = new User("first");
        first.addNotificationFilter(new NotificationFilter("pipline", "stage1", StageEvent.Fails, true));
        first.addNotificationFilter(new NotificationFilter("pipline", "stage2", StageEvent.Fails, true));
        int originalUserCacheSize = sessionFactory.getStatistics().getSecondLevelCacheStatistics(User.class.getCanonicalName()).getEntries().size();
        int originalNotificationsCacheSize = sessionFactory.getStatistics().getSecondLevelCacheStatistics(User.class.getCanonicalName() + ".notificationFilters").getEntries().size();
        userDao.saveOrUpdate(first);
        long userId = userDao.findUser("first").getId();
        assertThat(sessionFactory.getStatistics().getSecondLevelCacheStatistics(User.class.getCanonicalName()).getEntries().size(), is(originalUserCacheSize + 1));
        SecondLevelCacheStatistics notificationFilterCollectionCache = sessionFactory.getStatistics().getSecondLevelCacheStatistics(User.class.getCanonicalName() + ".notificationFilters");
        assertThat(notificationFilterCollectionCache.getEntries().size(), is(originalNotificationsCacheSize + 1));
        assertThat(notificationFilterCollectionCache.getEntries().get(userId), is(Matchers.notNullValue()));
    }

}
