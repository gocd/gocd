/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.UserEnabledException;
import com.thoughtworks.go.server.service.AccessTokenFilter;
import com.thoughtworks.go.server.service.AccessTokenService;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class UserSqlMapDaoIntegrationTest {
    @Autowired
    private UserSqlMapDao userDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private AccessTokenService accessTokenService;

    @BeforeEach
    public void setup() throws Exception {
        dbHelper.onSetUp();
    }

    @AfterEach
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldSaveUsersIntoDatabase() throws Exception {
        User user = anUser();
        userDao.saveOrUpdate(user);

        User savedUser = userDao.findUser("user");
        assertThat(savedUser).isEqualTo(user);
        assertThat(userDao.load(savedUser.getId())).isEqualTo(user);
    }

    @Test
    public void shouldSaveLoginAsDisplayNameIfDisplayNameIsNotPresent() {
        User user = new User("loser");
        userDao.saveOrUpdate(user);

        assertThat(userDao.findUser("loser").getDisplayName()).isEqualTo("loser");
    }

    @Test
    public void shouldNotUpdateDisplayNameToNullOrBlank() {
        User user = new User("loser", "moocow", "moocow@example.com");
        userDao.saveOrUpdate(user);
        user.setDisplayName("");
        userDao.saveOrUpdate(user);
        assertThat(userDao.findUser("loser").getDisplayName()).isEqualTo("loser");
    }

    @Test
    public void findUserShouldNotFailForNonExistentUser() {
        assertThat(userDao.findUser("first")).isEqualTo(new NullUser());
        User first = user("first");
        userDao.saveOrUpdate(first);
        assertThat(userDao.findUser("first")).isEqualTo(first);
    }

    @Test
    public void shouldFailWhenAttemptingToSaveAnonymousUser() {
        User user = user(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()));
        try {
            userDao.saveOrUpdate(user);
            fail("should not allow saving of anonymous user");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("User name 'anonymous' is not permitted.");
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
            assertThat(e.getMessage()).isEqualTo("User name 'anonymous' is not permitted.");
        }
    }

    @Test
    public void shouldFindUserAgnosticOfCase() {
        userDao.saveOrUpdate(user("first"));
        User lowerFirst = userDao.findUser("first");
        assertThat(lowerFirst.getName()).isEqualTo("first");
        User upperFirst = userDao.findUser("FIRST");
        assertThat(upperFirst.getName()).isEqualTo("first");
        assertThat(upperFirst.getId()).isEqualTo(lowerFirst.getId());
    }

    @Test
    public void shouldDisableUsers() {
        User firstUser = user("first");
        User secondUser = user("second");
        User thirdUser = user("third");

        userDao.saveOrUpdate(firstUser);
        userDao.saveOrUpdate(secondUser);
        userDao.saveOrUpdate(thirdUser);

        userDao.disableUsers(List.of("first", "second"));
        assertThat(userDao.findUser(firstUser.getName()).isEnabled()).isFalse();
        assertThat(userDao.findUser(secondUser.getName()).isEnabled()).isFalse();
        assertThat(userDao.findUser(thirdUser.getName()).isEnabled()).isTrue();
    }

    @Test
    public void shouldReturnOnlyEnabledUsers() throws Exception {
        userDao.saveOrUpdate(user("first"));
        userDao.saveOrUpdate(user("second"));

        User disabled = user("disabled");
        disabled.disable();
        userDao.saveOrUpdate(disabled);

        List<User> enabledUsers = userDao.enabledUsers();
        assertThat(enabledUsers.size()).isEqualTo(2);
        assertThat(enabledUsers).contains(user("first"));
        assertThat(enabledUsers).contains(user("second"));
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
        assertThat(userDao.findUser("user")).isEqualTo(user);
        assertThat(user.getNotificationFilters().size()).isEqualTo(0);
    }

    @Test
    public void shouldUpdateUserWithEnabledStatusWhenUserExist() {
        User user = new User("user", "my name", "user2@mail.com");
        userDao.saveOrUpdate(user);
        final User foundUser = userDao.findUser("user");
        assertThat(foundUser.isEnabled()).isTrue();

        foundUser.disable();
        userDao.saveOrUpdate(foundUser);
        assertThat(userDao.findUser("user").isEnabled()).isFalse();
    }

    @Test
    public void shouldFindUserWhenExist() throws Exception {
        User user = anUser();
        userDao.saveOrUpdate(user);

        User settingFromDb = userDao.findUser("user");

        assertThat(settingFromDb.getName()).isEqualTo(user.getName());
        assertThat(settingFromDb.getMatcher()).isEqualTo(user.getMatcher());
        assertThat(settingFromDb.getEmail()).isEqualTo(user.getEmail());

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

        assertThat(allUsers).contains(user, loser, boozer);
        assertThat(allUsers.size()).isEqualTo(3);
    }

    @Test
    public void shouldSaveAndLoadNotificationFilterInOrder() {
        User user = anUser();
        NotificationFilter filter1 = new NotificationFilter("PipelineName", "StageName", StageEvent.Fixed, false);
        NotificationFilter filter2 = new NotificationFilter("PipelineName", "StageName", StageEvent.Fails, false);
        user.setNotificationFilters(List.of(filter1, filter2));

        userDao.saveOrUpdate(user);

        user = userDao.findUser("user");
        assertThat(user.getNotificationFilters().size()).isEqualTo(2);
        assertThat(user.getNotificationFilters().get(0)).isEqualTo(filter1);
        assertThat(user.getNotificationFilters().get(1).getId()).isEqualTo(filter2.getId());
    }


    @Test
    public void shouldGetCountForEnabledUsersOnly() {
        User user = new User("user", new String[]{"*.*,user"}, "user@mail.com", true);
        User loser = new User("loser", new String[]{"loser", "user"}, "loser@mail.com", true);
        User boozer = new User("boozer", new String[]{"boozer"}, "boozer@mail.com", true);
        userDao.saveOrUpdate(user);
        userDao.saveOrUpdate(loser);
        userDao.saveOrUpdate(boozer);
        assertThat(userDao.enabledUserCount()).isEqualTo(3L);
        userDao.disableUsers(List.of("loser"));
        assertThat(userDao.enabledUserCount()).isEqualTo(2L);
        userDao.enableUsers(List.of("loser"));
        assertThat(userDao.enabledUserCount()).isEqualTo(3L);
    }

    @Test
    public void shouldClearCountCacheOnSaveOrStatusChange() {
        User user = new User("user", new String[]{"*.*,user"}, "user@mail.com", true);
        userDao.saveOrUpdate(user);
        assertThat(userDao.enabledUserCount()).isEqualTo(1L);
        User loser = new User("loser", "Loser", "loser@mail.com");
        loser.disable();
        User foo = new User("foo", "Foo", "foo@mail.com");
        userDao.saveOrUpdate(loser);
        userDao.saveOrUpdate(foo);
        userDao.saveOrUpdate(user);
        assertThat(userDao.enabledUserCount()).isEqualTo(2L);
        userDao.enableUsers(List.of(loser.getName()));
        assertThat(userDao.enabledUserCount()).isEqualTo(3L);
        userDao.disableUsers(List.of(user.getName()));
        assertThat(userDao.enabledUserCount()).isEqualTo(2L);
        userDao.saveOrUpdate(new User("bozer", "Bozer", "bozer@emai.com"));
        assertThat(userDao.enabledUserCount()).isEqualTo(3L);
    }

    @Test
    public void shouldFetchEnabledUsersCount() {
        User user = new User("user", new String[]{"*.*,user"}, "user@mail.com", true);
        userDao.saveOrUpdate(user);
        assertThat(userDao.enabledUserCount()).isEqualTo(1L);
        userDao.saveOrUpdate(new User("loser", new String[]{"loser", "user"}, "loser@mail.com", true));
        assertThat(userDao.enabledUserCount()).isEqualTo(2L);
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
        assertThat(userDao.findUser(userName).isEnabled()).isFalse();
        assertThat(userDao.findUser(userName.toLowerCase()).isEnabled()).isFalse();
    }

    @Test
    public void shouldLoadOnlyActiveSubscribersOfNotification() {
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


        User disabledUser = new User("user4");
        disabledUser.disable();
        disabledUser.addNotificationFilter(new NotificationFilter("p1", "s1", StageEvent.Fails, true));
        userDao.saveOrUpdate(disabledUser);

        User user5 = new User("user5");
        userDao.saveOrUpdate(user5);

        Users subscribedUsers = userDao.findNotificationSubscribingUsers();
        userDao.findNotificationSubscribingUsers();
        subscribedUsers.sort(Comparator.comparing(User::getName));

        assertThat(subscribedUsers.size()).isEqualTo(3);
        assertThat(subscribedUsers.containsAll(List.of(user1, user2, user3))).isTrue();

        assertThat(subscribedUsers.get(0).getNotificationFilters().size()).isEqualTo(1);
        assertThat(subscribedUsers.get(1).getNotificationFilters().size()).isEqualTo(3);
        assertThat(subscribedUsers.get(2).getNotificationFilters().size()).isEqualTo(1);
    }

    @Test
    public void shouldDeleteNotificationOnAUser() {
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
        assertThat(user.getNotificationFilters().size()).isEqualTo(1);
        assertThat(user.getNotificationFilters().contains(filter2)).isTrue();
        assertThat(sessionFactory.openSession().get(NotificationFilter.class, filter1Id)).isNull();
    }

    @Test
    public void shouldDeleteUser() {
        String userName = "user1";
        User user = new User(userName);
        user.disable();
        userDao.saveOrUpdate(user);
        boolean result = userDao.deleteUser(userName, "currentUser");
        assertThat(result).isTrue();
    }

    @Test
    public void shouldDeleteNotificationsAlongWithUser() {
        final User user = new User("user1");
        user.disable();
        user.addNotificationFilter(new NotificationFilter("pipelineName", "stageName", StageEvent.All, true));
        userDao.saveOrUpdate(user);
        assertThat(getAllNotificationFilter()).hasSize(1);

        boolean result = userDao.deleteUser(user.getName(), "currentUser");

        assertThat(result).isTrue();
        assertThat(getAllNotificationFilter()).isEmpty();
    }

    private List<NotificationFilter> getAllNotificationFilter() {
        return sessionFactory.openSession().createCriteria(NotificationFilter.class)
                .list();
    }

    @Test
    public void shouldThrowExceptionWhenUserIsNotFound() {
        String userName = "invaliduser";
        assertThatThrownBy(() -> userDao.deleteUser(userName, "currentUser"))
                .isInstanceOf(RecordNotFoundException.class);
    }

    @Test
    public void shouldThrowExceptionWhenUserIsNotDisabled() {
        String userName = "enabledUser";
        User user = new User(userName);
        userDao.saveOrUpdate(user);
        assertThatThrownBy(() -> userDao.deleteUser(userName, "currentUser"))
                .isInstanceOf(UserEnabledException.class);
    }

    @Test
    public void shouldAddNewUserWhenDeleteQueryForTheUserHasCachedANullUser() {
        String userName = "invalidForNowUser";
        try {
            userDao.deleteUser(userName, "currentUser");
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e instanceof RecordNotFoundException).isTrue();
        }
        User addingTheUserNow = new User(userName);
        addingTheUserNow.disable();
        userDao.saveOrUpdate(addingTheUserNow);
        User retrievedUser = userDao.findUser(userName);
        assertThat(retrievedUser instanceof NullUser).isFalse();
        assertThat(retrievedUser).isEqualTo(addingTheUserNow);
        assertThat(userDao.deleteUser(userName, "currentUser")).isTrue();
    }

    @Test
    public void shouldDeleteUsersAlongWithTokensAssociatedWithThem() {
        User john = new User("john");
        john.disable();
        User joan = new User("joan");
        joan.disable();

        List<String> userNames = List.of("john", "joan");

        userDao.saveOrUpdate(john);
        userDao.saveOrUpdate(joan);

        accessTokenService.create("my token", john.getName(), "blah");
        accessTokenService.create("my token", joan.getName(), "blah");

        assertThat(accessTokenService.findAllTokensForUser(john.getName(), AccessTokenFilter.all)).hasSize(1);
        assertThat(accessTokenService.findAllTokensForUser(joan.getName(), AccessTokenFilter.all)).hasSize(1);

        boolean result = userDao.deleteUsers(userNames, "currentUser");
        assertThat(result).isTrue();

        assertThat(accessTokenService.findAllTokensForUser(john.getName(), AccessTokenFilter.all)).hasSize(0);
        assertThat(accessTokenService.findAllTokensForUser(joan.getName(), AccessTokenFilter.all)).hasSize(0);

        Users users = userDao.allUsers();
        assertThat(users).isEmpty();
    }

    private User anUser() {
        return user("user");
    }

    private User user(String username) {
        return new User(username, username, new String[]{"*.*"}, username + "@mail.com", true);
    }
}
