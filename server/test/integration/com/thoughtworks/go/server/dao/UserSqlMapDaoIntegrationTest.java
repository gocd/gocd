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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.UserEnabledException;
import com.thoughtworks.go.server.exceptions.UserNotFoundException;
import org.hamcrest.Matchers;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItems;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class UserSqlMapDaoIntegrationTest {
    @Autowired private UserSqlMapDao userDao;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private SessionFactory sessionFactory;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        userDao.deleteAll();
    }

    @After
    public void teardown() throws Exception {
        userDao.deleteAll();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldSaveUsersIntoDatabase() throws Exception {
        User user = anUser();
        userDao.saveOrUpdate(user);

        User savedUser = userDao.findUser("user");
        assertThat(savedUser, is(user));
        assertThat(userDao.load(savedUser.getId()), is(user));
    }

    @Test
    public void shouldSaveLoginAsDisplayNameIfDisplayNameIsNotPresent(){
        User user = new User("loser");
        userDao.saveOrUpdate(user);

        assertThat(userDao.findUser("loser").getDisplayName(), is("loser"));
    }

    @Test
    public void shouldNotUpdateDisplayNameToNullOrBlank(){
        User user = new User("loser", "moocow", "moocow@example.com");
        userDao.saveOrUpdate(user);
        user.setDisplayName("");
        userDao.saveOrUpdate(user);
        assertThat(userDao.findUser("loser").getDisplayName(), is("loser"));
    }

    @Test
    public void findUserShouldNotFailForNonExistentUser() {
        assertThat(userDao.findUser("first"), is((User) new NullUser()));
        User first = user("first");
        userDao.saveOrUpdate(first);
        assertThat(userDao.findUser("first"), is(first));
    }

    @Test
    public void shouldFailWhenAttemptingToSaveAnonymousUser() {
        User user = user(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()));
        try {
            userDao.saveOrUpdate(user);
            fail("should not allow saving of anonymous user");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("User name 'anonymous' is not permitted."));
        }
    }

    @Test
    public void shouldFailWhenAttemptingToUpdateAnonymousUser() {
        User user = user("foo");
        userDao.saveOrUpdate(user);
        user.setName("anonymous");
        try {
            userDao.saveOrUpdate(user);
            fail("should not allow updating anonymous user");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("User name 'anonymous' is not permitted."));
        }
    }

    @Test
    public void shouldFindUserAgnosticOfCase() {
        userDao.saveOrUpdate(user("first"));
        User lowerFirst = userDao.findUser("first");
        assertThat(lowerFirst.getName(), is("first"));
        User upperFirst = userDao.findUser("FIRST");
        assertThat(upperFirst.getName(), is("first"));
        assertThat(upperFirst.getId(), is(lowerFirst.getId()));
    }


    @Test
    public void shouldDeleteAllUsers() {
        User firstUser = user("first");
        User secondUser = user("second");

        userDao.saveOrUpdate(firstUser);
        userDao.saveOrUpdate(secondUser);

        userDao.deleteAll();
        assertThat(userDao.allUsers().size(), is(0));
    }

    @Test
    public void shouldDisableUsers() {
        User firstUser = user("first");
        User secondUser = user("second");
        User thirdUser = user("third");

        userDao.saveOrUpdate(firstUser);
        userDao.saveOrUpdate(secondUser);
        userDao.saveOrUpdate(thirdUser);

        userDao.disableUsers(Arrays.asList("first", "second"));
        assertThat(userDao.findUser(firstUser.getName()).isEnabled(), is(false));
        assertThat(userDao.findUser(secondUser.getName()).isEnabled(), is(false));
        assertThat(userDao.findUser(thirdUser.getName()).isEnabled(), is(true));
    }

    @Test
    public void shouldReturnOnlyEnabledUsers() throws Exception {
        userDao.saveOrUpdate(user("first"));
        userDao.saveOrUpdate(user("second"));

        User disabled = user("disabled");
        disabled.disable();
        userDao.saveOrUpdate(disabled);

        List<User> enabledUsers = userDao.enabledUsers();
        assertThat(enabledUsers.size(), is(2));
        assertThat(enabledUsers, hasItem(user("first")));
        assertThat(enabledUsers, hasItem(user("second")));
    }

    @Test
    public void shouldUpdateUserWhenUserExist() throws Exception {
        User user = anUser();
        userDao.saveOrUpdate(user);
        user = userDao.findUser(user.getName());
        user.setEmail("user2@mail.com");
        user.setMatcher("abc");
        user.setEmailMe(false);
        userDao.saveOrUpdate(user);
        assertThat(userDao.findUser("user"), is(user));
        assertThat(user.getNotificationFilters().size(), is(0));
    }

    @Test
    public void shouldUpdateUserWithEnabledStatusWhenUserExist(){
        User user = new User("user", "my name", "user2@mail.com");
        userDao.saveOrUpdate(user);
        final User foundUser = userDao.findUser("user");
        assertThat(foundUser.isEnabled(), is(true));

        foundUser.disable();
        userDao.saveOrUpdate(foundUser);
        assertThat(userDao.findUser("user").isEnabled(), is(false));
    }

    @Test
    public void shouldFindUserWhenExist() throws Exception {
        User user = anUser();
        userDao.saveOrUpdate(user);

        User settingFromDb = userDao.findUser("user");

        assertThat(settingFromDb.getName(), is(user.getName()));
        assertThat(settingFromDb.getMatcher(), is(user.getMatcher()));
        assertThat(settingFromDb.getEmail(), is(user.getEmail()));

    }

    @Test
    public void shouldFindAllUsers() throws Exception {
        User user = new User("user", new String[]{"*.*,user"}, "user@mail.com", true);
        User loser = new User("loser", new String[]{"loser", "user"}, "loser@mail.com", true);
        User boozer = new User("boozer", new String[]{"boozer"}, "boozer@mail.com", true);
        userDao.saveOrUpdate(user);
        userDao.saveOrUpdate(loser);
        userDao.saveOrUpdate(boozer);

        List<User> allUsers = userDao.allUsers();

        assertThat(allUsers, hasItems(user, loser, boozer));
        assertThat(allUsers.size(), is(3));
    }

    @Test
    public void shouldSaveAndLoadNotificationFilterInOrder() {
        User user = anUser();
        NotificationFilter filter1 = new NotificationFilter("PipelineName", "StageName", StageEvent.Fixed, false);
        NotificationFilter filter2 = new NotificationFilter("PipelineName", "StageName", StageEvent.Fails, false);
        user.setNotificationFilters(Arrays.asList(filter1, filter2));

        userDao.saveOrUpdate(user);

        user = userDao.findUser("user");
        assertThat(user.getNotificationFilters().size(), is(2));
        assertThat(user.getNotificationFilters().get(0), is(filter1));
        assertThat(user.getNotificationFilters().get(1).getId(), is(filter2.getId()));
    }


    @Test
    public void shouldGetCountForEnabledUsersOnly() {
        User user = new User("user", new String[]{"*.*,user"}, "user@mail.com", true);
        User loser = new User("loser", new String[]{"loser", "user"}, "loser@mail.com", true);
        User boozer = new User("boozer", new String[]{"boozer"}, "boozer@mail.com", true);
        userDao.saveOrUpdate(user);
        userDao.saveOrUpdate(loser);
        userDao.saveOrUpdate(boozer);
        assertThat(userDao.enabledUserCount(), is(3));
        userDao.disableUsers(Arrays.asList("loser"));
        assertThat(userDao.enabledUserCount(), is(2));
        userDao.enableUsers(Arrays.asList("loser"));
        assertThat(userDao.enabledUserCount(), is(3));
    }

    @Test
    public void shouldClearCountCacheOnSaveOrStatusChange() {
        User user = new User("user", new String[]{"*.*,user"}, "user@mail.com", true);
        userDao.saveOrUpdate(user);
        assertThat(userDao.enabledUserCount(), is(1));
        User loser = new User("loser", "Loser", "loser@mail.com");
        loser.disable();
        User foo = new User("foo", "Foo", "foo@mail.com");
        userDao.saveOrUpdate(loser);
        userDao.saveOrUpdate(foo);
        userDao.saveOrUpdate(user);
        assertThat(userDao.enabledUserCount(), is(2));
        userDao.enableUsers(Arrays.asList(loser.getName()));
        assertThat(userDao.enabledUserCount(), is(3));
        userDao.disableUsers(Arrays.asList(user.getName()));
        assertThat(userDao.enabledUserCount(), is(2));
        userDao.saveOrUpdate(new User("bozer", "Bozer", "bozer@emai.com"));
        assertThat(userDao.enabledUserCount(), is(3));
    }

    @Test
    public void shouldFetchEnabledUsersCount() {
        User user = new User("user", new String[]{"*.*,user"}, "user@mail.com", true);
        userDao.saveOrUpdate(user);
        assertThat(userDao.enabledUserCount(), is(1));
        userDao.saveOrUpdate(new User("loser", new String[]{"loser", "user"}, "loser@mail.com", true));
        assertThat(userDao.enabledUserCount(), is(2));
    }

    private User saveUser(final String user) {
        User user1 = user(user);
        userDao.saveOrUpdate(user1);
        return user1;
    }

    @Test
    public void findUserShouldIgnoreCaseOfUserName() {
        String userName = "USER";
        saveUser(userName.toLowerCase());
        User found = userDao.findUser(userName);

        found.disable();
        userDao.saveOrUpdate(found);
        assertThat(userDao.findUser(userName).isEnabled(), is(false));
        assertThat(userDao.findUser(userName.toLowerCase()).isEnabled(), is(false));
    }

    @Test
    public void shouldLoadSubscribersOfNotification(){
        User user1 = new User("user1");
        user1.addNotificationFilter(new NotificationFilter("pipeline", "stage", StageEvent.Fails, true));
        userDao.saveOrUpdate(user1);

        User user2 = new User("user2");
        user2.addNotificationFilter(new NotificationFilter("pipeline", "stage", StageEvent.Fails, true));
        user2.addNotificationFilter(new NotificationFilter("pipeline", "stage", StageEvent.Breaks, true));
        user2.addNotificationFilter(new NotificationFilter("pipeline", "stage", StageEvent.Passes, true));
        userDao.saveOrUpdate(user2);

        User user3 = new User("user3");
        user3.addNotificationFilter(new NotificationFilter("p1", "s1", StageEvent.Fails, true));
        userDao.saveOrUpdate(user3);

        User user4 = new User("user4");
        userDao.saveOrUpdate(user4);

        Users subscribedUsers = userDao.findNotificationSubscribingUsers();
        userDao.findNotificationSubscribingUsers();
        assertThat(subscribedUsers.size(), is(3));
        assertThat(subscribedUsers.containsAll(Arrays.asList(user1, user2, user3)), is(true));

        Collections.sort(subscribedUsers, new Comparator<User>() {
            @Override
            public int compare(User user1, User user2) {
                return user1.getName().compareTo(user2.getName());
            }
        });

        assertThat(subscribedUsers.get(0).getNotificationFilters().size(), is(1));
        assertThat(subscribedUsers.get(1).getNotificationFilters().size(), is(3));
        assertThat(subscribedUsers.get(2).getNotificationFilters().size(), is(1));
    }


    @Test
    public void shouldDeleteNotificationOnAUser(){
        User user = new User("user1");
        user.addNotificationFilter(new NotificationFilter("pipeline1", "stage", StageEvent.Fails, true));
        user.addNotificationFilter(new NotificationFilter("pipeline2", "stage", StageEvent.Fails, true));
        userDao.saveOrUpdate(user);
        user = userDao.findUser(user.getName());
        NotificationFilter filter1 = user.getNotificationFilters().get(0);
        long filter1Id = filter1.getId();
        NotificationFilter filter2 = user.getNotificationFilters().get(1);
        user.removeNotificationFilter(filter1.getId());
        userDao.saveOrUpdate(user);
        user = userDao.findUser(user.getName());
        assertThat(user.getNotificationFilters().size(), is(1));
        assertThat(user.getNotificationFilters().contains(filter2), is(true));
        assertThat(sessionFactory.openSession().get(NotificationFilter.class, filter1Id), is(Matchers.nullValue()));
    }

    @Test
    public void shouldDeleteUser() {
        String userName = "user1";
        User user = new User(userName);
        user.disable();
        userDao.saveOrUpdate(user);
        boolean result = userDao.deleteUser(userName);
        assertThat(result, is(true));
    }

    @Test
    public void shouldDeleteNotificationsAlongWithUser() {
        User user = new User("user1");
        user.addNotificationFilter(new NotificationFilter("pipelineName", "stageName", StageEvent.All, true));
        user.disable();
        userDao.saveOrUpdate(user);
        Users users = userDao.findNotificationSubscribingUsers();
        assertThat(users, hasItem(user));

        boolean result = userDao.deleteUser(user.getName());

        assertThat(result, is(true));
        users = userDao.findNotificationSubscribingUsers();
        assertThat(users, is(empty()));
    }

    @Test
    @ExpectedException(UserNotFoundException.class)
    public void shouldThrowExceptionWhenUserIsNotFound() {
        String userName = "invaliduser";
        userDao.deleteUser(userName);
    }

    @Test
    @ExpectedException(UserEnabledException.class)
    public void shouldThrowExceptionWhenUserIsNotDisabled() {
        String userName = "enabledUser";
        User user = new User(userName);
        userDao.saveOrUpdate(user);
        userDao.deleteUser(userName);
    }

    @Test
    public void shouldAddNewUserWhenDeleteQueryForTheUserHasCachedANullUser() {
        String userName = "invalidForNowUser";
        try {
            userDao.deleteUser(userName);
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e instanceof UserNotFoundException, is(true));
        }
        User addingTheUserNow = new User(userName);
        addingTheUserNow.disable();
        userDao.saveOrUpdate(addingTheUserNow);
        User retrievedUser = userDao.findUser(userName);
        assertThat(retrievedUser instanceof NullUser, is(false));
        assertThat(retrievedUser, is(addingTheUserNow));
        assertThat(userDao.deleteUser(userName), is(true));
    }

    private User anUser() {
        return user("user");
    }

    private User user(String username) {
        return new User(username, username, new String[]{"*.*"}, username + "@mail.com", true);
    }
}
