/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.UserSqlMapDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TriState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath:/applicationContext-global.xml",
    "classpath:/applicationContext-dataLocalAccess.xml",
    "classpath:/testPropertyConfigurer.xml",
    "classpath:/spring-all-servlet.xml",
})
public class UserServiceIntegrationTest {
    @Autowired
    private UserSqlMapDao userDao;
    @Autowired
    private UserService userService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoCache goCache;

    private final GoConfigFileHelper configHelper = new GoConfigFileHelper(ConfigFileFixture.ONE_PIPELINE);

    @BeforeEach
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        goCache.clear();
    }

    @AfterEach
    public void teardown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
        goCache.clear();
    }

    @Test
    public void shouldSaveUser() throws ValidationException {
        User user = new User("name1", "regx", "user@mail.com", true);
        userService.saveOrUpdate(user);
        User savedUser = userDao.findUser("name1");
        assertThat(savedUser).isEqualTo(user);
        assertThat(userService.load(savedUser.getId())).isEqualTo(user);
    }

    @Test
    public void shouldUpdateWhenUserAlreadyExist() throws ValidationException {
        addUser(new User("name1", "regx", "user@mail.com", true));
        User updatedUser = userService.findUserByName("name1");
        updatedUser.setEmail("user2@mail.com");
        updatedUser.setMatcher("regx2");

        userService.saveOrUpdate(updatedUser);

        User user = userDao.findUser("name1");
        assertThat(user).isEqualTo(updatedUser);
        assertThat(user.getId()).isEqualTo(updatedUser.getId());
    }

    @Test
    public void addOrUpdateUser_shouldAddUserIfDoesNotExist() {
        assertThat(userDao.findUser("new_user")).isInstanceOf(NullUser.class);
        SecurityAuthConfig authConfig = new SecurityAuthConfig();
        userService.addOrUpdateUser(new User("new_user"), authConfig);
        User loadedUser = userDao.findUser("new_user");
        assertThat(loadedUser).isEqualTo(new User("new_user", "new_user", ""));
        assertThat(loadedUser).isNotInstanceOf(NullUser.class);
    }

    @Test
    public void addOrUpdateUser_shouldNotAddUserIfExistsAndIsNotUpdated() {
        User user = new User("old_user");
        addUser(user);
        SecurityAuthConfig authConfig = new SecurityAuthConfig();
        userService.addOrUpdateUser(user, authConfig);
    }

    @Test
    public void addOrUpdateUser_shouldUpdateUserIfExistsAndEitherEmailOrDisplayNameChanged() {
        String name = "old_user";
        User user = new User(name);
        addUser(user);
        User updatedUser = new User(name, name, "");
        SecurityAuthConfig authConfig = new SecurityAuthConfig();
        userService.addOrUpdateUser(updatedUser, authConfig);

        User loadedUser = userDao.findUser(name);
        assertThat(loadedUser).isEqualTo(updatedUser);
        assertThat(loadedUser).isNotInstanceOf(NullUser.class);
    }

    @Test
    public void addOrUpdateUser_shouldNotAddUserIfAnonymous() {
        SecurityAuthConfig authConfig = new SecurityAuthConfig();
        userService.addOrUpdateUser(new User(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername())), authConfig);
        assertThat(userDao.findUser(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()))).isInstanceOf(NullUser.class);
        assertThat(userDao.findUser(Username.ANONYMOUS.getDisplayName())).isInstanceOf(NullUser.class);
    }

    @Test
    public void shouldValidateUser() {
        try {
            userService.validate(new User("username", "committer", "mail.com", false));
            fail("should have thrown when email is invalid");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    public void shouldNotSaveUserWhenValidationFailed() {
        try {
            userService.saveOrUpdate(new User("username", "committer", "mail.com", false));
            fail("should have thrown when email is invalid");
        } catch (ValidationException e) {
            assertThat(userService.findUserByName("username")).isInstanceOf(NullUser.class);
        }
    }

    @Test
    public void shouldAddNotificationFilterForExistingUser() throws ValidationException {
        User user = new User("jez", "jez", "user@mail.com", true);
        userService.saveOrUpdate(user);
        user = userDao.findUser(user.getName());
        NotificationFilter filter = new NotificationFilter("pipeline1", "stage", StageEvent.Fixed, false);
        userService.addNotificationFilter(user.getId(), filter);
        user = userService.findUserByName("jez");
        assertThat(user.getNotificationFilters().size()).isEqualTo(1);
        assertThat(user.getNotificationFilters()).contains(filter);
    }

    @Test
    public void shouldRemoveNotificationFilterForUser() {
        User user = new User("jez", "jez", "user@mail.com", true);
        addUser(user);
        user = userDao.findUser(user.getName());
        NotificationFilter filter = new NotificationFilter("pipeline1", "stage", StageEvent.Fixed, false);
        userService.addNotificationFilter(user.getId(), filter);
        user = userService.findUserByName(user.getName());
        assertThat(user.getNotificationFilters().size()).isEqualTo(1);
        long deletedNotificationId = user.getNotificationFilters().get(0).getId();
        userService.removeNotificationFilter(user.getId(), deletedNotificationId);
        assertThat(userService.findUserByName(user.getName()).getNotificationFilters().size()).isEqualTo(0);
    }

    @Test
    public void shouldNotAddDuplicateNotificationFilter() {
        User user = new User("jez", "jez", "user@mail.com", true);
        NotificationFilter filter = new NotificationFilter("pipeline1", "stage", StageEvent.Fixed, false);
        addUserWithNotificationFilter(user, filter);
        user = userDao.findUser(user.getName());

        try {
            userService.addNotificationFilter(user.getId(), filter);
            fail("shouldNotAddDuplicateNotificationFilter");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("Duplicate notification filter");
        }
    }

    @Test
    public void shouldNotAddUnnecessaryNotificationFilter() throws ValidationException {
        User user = new User("jez", "jez", "user@mail.com", true);
        userService.saveOrUpdate(user);
        user = userDao.findUser(user.getName());
        userService.addNotificationFilter(user.getId(), new NotificationFilter("pipeline1", "stage", StageEvent.Fixed, false));

        try {
            userService.addNotificationFilter(user.getId(), new NotificationFilter("pipeline1", "stage", StageEvent.Fixed, false));
            fail("shouldNotAddUnnecessaryNotificationFilter");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("Duplicate notification filter");
        }
    }

    @Test
    public void shouldLoadUsersWhoSubscribedNotificationOnStage() {
        User tom = new User("tom", "tom", "tom@mail.com", true);
        addUserWithNotificationFilter(tom, new NotificationFilter("p1", "s1", StageEvent.Breaks, true));

        User jez = new User("jez", "jez", "user@mail.com", true);
        addUserWithNotificationFilter(jez,
            new NotificationFilter("pipeline1", "stage", StageEvent.All, false),
            new NotificationFilter("mingle", "dev", StageEvent.All, false));

        Users users = userService.findValidSubscribers(new StageConfigIdentifier("pipeline1", "stage"));
        assertThat(users.size()).isEqualTo(1);
        assertThat(users.get(0)).isEqualTo(jez);
        assertThat(users.get(0).getNotificationFilters().size()).isEqualTo(2);
    }

    @Test
    public void shouldLoadAuthorizedUser() {
        givingJezViewPermissionToMingle();

        User tom = new User("tom", "tom", "tom@mail.com", true);
        User jez = new User("jez", "jez", "user@mail.com", true);
        addUserWithNotificationFilter(jez,
            new NotificationFilter("mingle", "dev", StageEvent.All, false));
        addUserWithNotificationFilter(tom,
            new NotificationFilter("mingle", "dev", StageEvent.All, false));

        Users users = userService.findValidSubscribers(new StageConfigIdentifier("mingle", "dev"));
        assertThat(users.size()).isEqualTo(1);
        assertThat(users.get(0)).isEqualTo(jez);
        assertThat(users.get(0).getNotificationFilters().size()).isEqualTo(1);
    }

    @Test
    public void disableUsers_shouldDisableUsers() {
        addUser(new User("user_one"));
        addUser(new User("user_two"));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.disable(List.of("user_one"), result);

        assertThat(result.isSuccessful()).isTrue();
        List<User> users = userService.allUsers();
        assertThat(users.get(0).isEnabled()).isFalse();
        assertThat(users.get(1).isEnabled()).isTrue();
    }

    @Test
    public void shouldEnableUsers() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        User user1 = new User("user_one");
        user1.disable();
        addUser(user1);

        createDisabledUser("user_two");

        userService.enable(List.of("user_one"));

        assertThat(result.isSuccessful()).isTrue();
        List<User> users = userService.allUsers();
        assertThat(users.get(0).isEnabled()).isTrue();
        assertThat(users.get(1).isEnabled()).isFalse();
    }

    @Test
    public void shouldUpdateEnabledStateToFalse() {
        User user = new User("user-1");
        user.enable();
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.FALSE, TriState.UNSET, null, null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    public void shouldUpdateEnabledStateToTrue() {
        User user = new User("user-1");
        user.disable();
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.TRUE, TriState.UNSET, null, null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    public void shouldNotUpdateEnabledStateWhenAskedToBeLeftUnset() {
        User user = new User("user-1");
        user.disable();
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, null, null, result);
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    public void updateShouldUpdateEmailMeStateToTrue() {
        User user = new User("user-1");
        user.enable();
        user.setEmailMe(false);
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.TRUE, null, null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.isEmailMe()).isTrue();
    }


    @Test
    public void updateShouldUpdateEmailMeStateToFalse() {
        User user = new User("user-1");
        user.enable();
        user.setEmailMe(true);
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.FALSE, null, null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.isEmailMe()).isFalse();
    }

    @Test
    public void updateShouldUpdateEmailMeStateWHenAskedToBeLeftUnset() {
        User user = new User("user-1");
        user.enable();
        user.setEmailMe(true);
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, null, null, result);
        assertThat(user.isEmailMe()).isTrue();
    }

    @Test
    public void updateShouldUpdateEmail() {
        User user = new User("user-1");
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, "foo@example.com", null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.getEmail()).isEqualTo("foo@example.com");

        result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.TRUE, TriState.UNSET, "", null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.getEmail()).isEqualTo("");
    }

    @Test
    public void updateShouldNotUpdateEmailWhenNull() {
        User user = new User("user-1");
        user.setEmail("foo@example.com");
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, null, null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.getEmail()).isEqualTo("foo@example.com");
    }

    @Test
    public void updateShouldUpdateMatcher() {
        User user = new User("user-1");
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, null, "foo,bar", result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.getMatcher()).isEqualTo("foo,bar");

        result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, null, "", result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.getMatcher()).isEqualTo("");
    }

    @Test
    public void updateShouldNotUpdateMatcherWhenNull() {
        User user = new User("user-1");
        user.setMatcher("foo,bar");
        addUser(user);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.save(user, TriState.UNSET, TriState.UNSET, null, null, result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(user.getMatcher()).isEqualTo("foo,bar");
    }

    private void createDisabledUser(String username) {
        User user = new User(username);
        user.disable();
        addUser(user);
    }

    private void givingJezViewPermissionToMingle() {
        configHelper.enableSecurity();
        configHelper.addPipeline("mingle", "dev");
        configHelper.setViewPermissionForGroup("defaultGroup", "jez");
        configHelper.addSecurityWithAdminConfig();
    }

    private void addUser(User user) {
        userDao.saveOrUpdate(user);
    }

    private void addUserWithNotificationFilter(User user, NotificationFilter... filters) {
        for (NotificationFilter filter : filters) {
            user.addNotificationFilter(filter);
        }
        addUser(user);
    }
}
