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
import org.springframework.util.StopWatch;

import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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

    @Test
    public void shouldRemoveEnabledUserCountFromCacheWhenAUserIsSaved() throws Exception {
        makeSureThatCacheIsInitialized();

        userDao.saveOrUpdate(new User("some-random-user"));

        assertThatEnabledUserCacheHasBeenCleared();
    }

    @Test
    public void shouldRemoveEnabledUserCountFromCacheWhenAUserIsDisabled() throws Exception {
        userDao.saveOrUpdate(new User("some-random-user"));
        makeSureThatCacheIsInitialized();

        userDao.disableUsers(Arrays.asList("some-random-user"));

        assertThatEnabledUserCacheHasBeenCleared();
    }

    @Test
    public void shouldRemoveEnabledUserCountFromCacheWhenAUserIsEnabled() throws Exception {
        userDao.saveOrUpdate(new User("some-random-user"));
        makeSureThatCacheIsInitialized();

        userDao.enableUsers(Arrays.asList("some-random-user"));

        assertThatEnabledUserCacheHasBeenCleared();
    }

    @Test
    public void shouldNOTRemoveEnabledUserCountFromCacheWhenFindUserHappens() throws Exception {
        makeSureThatCacheIsInitialized();

        userDao.findUser("some-random-user");

        assertThatEnabledUserCacheExists();
    }

    @Test
    public void shouldNOTRemoveEnabledUserCountFromCacheWhenAllUsersAreLoaded() throws Exception {
        makeSureThatCacheIsInitialized();

        userDao.allUsers();

        assertThatEnabledUserCacheExists();
    }

    @Test
    public void shouldNOTRemoveEnabledUserCountFromCacheWhenEnabledUsersAreLoaded() throws Exception {
        makeSureThatCacheIsInitialized();

        userDao.enabledUsers();

        assertThatEnabledUserCacheExists();
    }

    @Test
    public void shouldNOTRemoveEnabledUserCountFromCacheWhenFindUsernamesForIds() throws Exception {
        userDao.saveOrUpdate(new User("some-random-user"));
        User user = userDao.findUser("some-random-user");

        HashSet<Long> userIds = new HashSet<>();
        userIds.add(user.getId());

        makeSureThatCacheIsInitialized();

        userDao.findUsernamesForIds(userIds);

        assertThatEnabledUserCacheExists();
    }

    @Test
    public void shouldNOTRemoveEnabledUserCountFromCacheWhenUserIsLoaded() throws Exception {
        userDao.saveOrUpdate(new User("some-random-user"));
        User user = userDao.findUser("some-random-user");

        makeSureThatCacheIsInitialized();

        userDao.load(user.getId());

        assertThatEnabledUserCacheExists();
    }

    @Test(timeout = 60000)
    public void enabledUserCacheShouldBeThreadSafe() throws Exception {
        ThreadSafetyChecker threadSafetyChecker = new ThreadSafetyChecker(10000);

        threadSafetyChecker.addOperation(new ThreadSafetyChecker.Operation() {
            @Override
            public void execute(int runIndex) {
                StopWatch stopWatch = new StopWatch("enabledUserCount");
                stopWatch.start("enabledUserCount");
                userDao.enabledUserCount();
                stopWatch.stop();
//                System.out.println(stopWatch.shortSummary());
            }
        });

        threadSafetyChecker.addOperation(new ThreadSafetyChecker.Operation() {
            @Override
            public void execute(int runIndex) {
                StopWatch stopWatch = new StopWatch("saveOrUpdate");
                stopWatch.start("saveOrUpdate");
                userDao.saveOrUpdate(new User("some-random-user " + runIndex));
                stopWatch.stop();
//                System.out.println(stopWatch.shortSummary());
            }
        });

        threadSafetyChecker.addOperation(new ThreadSafetyChecker.Operation() {
            @Override
            public void execute(int runIndex) {
                StopWatch stopWatch = new StopWatch("enableUsers");
                stopWatch.start("enableUsers");
                userDao.enableUsers(Arrays.asList("some-random-user " + runIndex));
                stopWatch.stop();
//                System.out.println(stopWatch.shortSummary());
            }
        });

        threadSafetyChecker.run(250);
    }

    private void assertThatEnabledUserCacheHasBeenCleared() {
        assertThat(goCache.get(UserSqlMapDao.ENABLED_USER_COUNT_CACHE_KEY), is(nullValue()));
    }

    private void assertThatEnabledUserCacheExists() {
        assertThat(goCache.get(UserSqlMapDao.ENABLED_USER_COUNT_CACHE_KEY), is(not(nullValue())));
    }

    private void makeSureThatCacheIsInitialized() {
        userDao.enabledUserCount();
        assertThatEnabledUserCacheExists();
    }
}
