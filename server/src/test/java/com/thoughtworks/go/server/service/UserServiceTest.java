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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.exception.UncheckedValidationException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.UserEnabledException;
import com.thoughtworks.go.server.security.OnlyKnownUsersAllowedException;
import com.thoughtworks.go.server.service.result.BulkUpdateUsersOperationResult;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TestTransactionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.thoughtworks.go.helper.PipelineConfigMother.createPipelineConfig;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

public class UserServiceTest {
    private UserDao userDao;
    private GoConfigService goConfigService;
    private SecurityService securityService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        userService = new UserService(userDao, securityService, goConfigService, new TestTransactionTemplate(new TestTransactionSynchronizationManager()));
    }

    @Test
    void shouldLoadAllUsernames() {
        User foo = new User("foo", "fOO,Foo", "foo@cruise.com", false);
        User bar = new User("bar", "bAR,Bar", "bar@go.com", true);

        when(userDao.allUsers()).thenReturn(new Users(List.of(foo, bar)));

        Set<String> users = userService.allUsernames();
        assertThat(users).containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    void shouldReturnErrorMessageWhenTheLastAdminIsBeingDisabled() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(userDao.enabledUsers()).thenReturn(List.of(new User("Jake"), new User("Pavan"), new User("Shilpa")));
        configureAdmin("Jake", true);
        configureAdmin("Pavan", true);
        configureAdmin("Shilpa", false);

        userService.disable(List.of("Pavan", "Jake"), result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(HTTP_BAD_REQUEST);
    }

    @Test
    void shouldNotFailToEnableTheSameUser() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(userDao.enabledUsers()).thenReturn(List.of(new User("Jake")));
        userService.enable(List.of("Jake"));

        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        String username = "username";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(userDao.deleteUser(username, "currentUser")).thenReturn(true);
        userService.deleteUser(username, "currentUser", result);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.hasMessage()).isTrue();
    }

    @Test
    void shouldFailWithErrorWhenDeletingAUserFails() {
        String username = "username";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(userDao.deleteUser(username, "currentUser")).thenThrow(new RecordNotFoundException(EntityType.User, username));
        userService.deleteUser(username, "currentUser", result);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.hasMessage()).isTrue();
    }

    @Test
    void shouldDeleteAllSpecifiedUsersSuccessfully() {
        List<String> usernames = List.of("john", "joan");

        User john = new User("john");
        john.disable();
        User joan = new User("joan");
        joan.disable();
        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();

        when(userDao.findUser("john")).thenReturn(john);
        when(userDao.findUser("joan")).thenReturn(joan);
        userService.deleteUsers(usernames, "currentUser", result);

        verify(userDao).deleteUsers(usernames, "currentUser");
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.message()).isEqualTo(EntityType.User.deleteSuccessful(List.of("john", "joan")));
    }

    @Test
    void shouldFailWithErrorEvenIfOneUserDoesNotExistDuringBulkDelete() {
        List<String> usernames = List.of("john", "joan");
        User john = new User("John");
        john.disable();
        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();

        BulkUpdateUsersOperationResult expectedBulkUpdateUsersOperationResult = new BulkUpdateUsersOperationResult();
        expectedBulkUpdateUsersOperationResult.addNonExistentUserName("joan");

        when(userDao.findUser("joan")).thenReturn(new NullUser());
        when(userDao.findUser("john")).thenReturn(john);

        userService.deleteUsers(usernames, "currentUser", result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).contains("Deletion failed because some users do not exist.");
        assertThat(result.getNonExistentUsers()).isEqualTo(expectedBulkUpdateUsersOperationResult.getNonExistentUsers());
        assertThat(result.getEnabledUsers()).isEqualTo(expectedBulkUpdateUsersOperationResult.getEnabledUsers());
    }

    @Test
    void shouldFailWithErrorEvenIfOneUserIsEnabledDuringBulkDelete() {
        List<String> usernames = List.of("john", "joan");
        User john = new User("john");
        User joan = new User("joan");
        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();

        BulkUpdateUsersOperationResult expectedBulkUpdateUsersOperationResult = new BulkUpdateUsersOperationResult();
        expectedBulkUpdateUsersOperationResult.addEnabledUserName("john");
        expectedBulkUpdateUsersOperationResult.addEnabledUserName("joan");

        when(userDao.findUser("joan")).thenReturn(john);
        when(userDao.findUser("john")).thenReturn(joan);

        userService.deleteUsers(usernames, "currentUser", result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("Deletion failed because some users were enabled.");
        assertThat(result.getNonExistentUsers()).isEqualTo(expectedBulkUpdateUsersOperationResult.getNonExistentUsers());
        assertThat(result.getEnabledUsers()).isEqualTo(expectedBulkUpdateUsersOperationResult.getEnabledUsers());
    }

    @Test
    void shouldFailWithErrorIfNoUsersAreProvidedDuringBulkDelete() {
        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();
        userService.deleteUsers(null, "currentUser", result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("No users selected.");
    }

    @Test
    void shouldFailWithErrorIfEmptyUsersAreProvidedDuringBulkDelete() {
        List<String> usernames = new ArrayList<>();

        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();
        userService.deleteUsers(usernames, "currentUser", result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("No users selected.");
    }

    @Test
    void shouldEnableAllSpecifiedUsersSuccessfully() {
        List<String> usernames = List.of("john", "joan");

        User john = new User("john");
        john.disable();
        User joan = new User("joan");
        joan.disable();

        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();

        when(userDao.findUser("john")).thenReturn(john);
        when(userDao.findUser("joan")).thenReturn(joan);
        userService.bulkEnableDisableUsers(usernames, true, result);

        verify(userDao).enableUsers(usernames);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.message()).isEqualTo("Users 'john, joan' were enabled successfully.");
    }

    @Test
    void shouldDisableAllSpecifiedUsersSuccessfully() {
        User admin = new User("admin");

        List<String> usernames = List.of("john", "joan");

        User john = new User("john");
        User joan = new User("joan");

        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();

        when(userDao.findUser("john")).thenReturn(john);
        when(userDao.findUser("joan")).thenReturn(joan);
        when(userDao.enabledUsers()).thenReturn(List.of(joan, joan, admin));
        when(securityService.isUserAdmin(new Username("admin"))).thenReturn(true);

        userService.bulkEnableDisableUsers(usernames, false, result);

        verify(userDao).disableUsers(usernames);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.message()).isEqualTo("Users 'john, joan' were disabled successfully.");
    }

    @Test
    void shouldFailWithErrorIfAllAdminUsersAreTriedToBeDisabled() {
        List<String> usernames = List.of("john", "joan");

        User john = new User("john");
        User joan = new User("joan");

        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();

        when(userDao.findUser("john")).thenReturn(john);
        when(userDao.findUser("joan")).thenReturn(joan);
        when(userDao.enabledUsers()).thenReturn(List.of(joan, joan));

        userService.bulkEnableDisableUsers(usernames, false, result);

        verify(userDao, never()).disableUsers(usernames);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).contains("There must be at least one admin user enabled!");
    }

    @Test
    void shouldFailWithErrorEvenIfOneUserDoesNotExistDuringBulkEnableOrDisableUsers() {
        List<String> usernames = List.of("john", "joan");
        User john = new User("John");
        john.disable();
        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();

        BulkUpdateUsersOperationResult expectedBulkUpdateUsersOperationResult = new BulkUpdateUsersOperationResult();
        expectedBulkUpdateUsersOperationResult.addNonExistentUserName("joan");

        when(userDao.findUser("joan")).thenReturn(new NullUser());
        when(userDao.findUser("john")).thenReturn(john);

        userService.bulkEnableDisableUsers(usernames, true, result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).contains("Update failed because some users do not exist.");
        assertThat(result.getNonExistentUsers()).isEqualTo(expectedBulkUpdateUsersOperationResult.getNonExistentUsers());
        assertThat(result.getEnabledUsers()).isEqualTo(expectedBulkUpdateUsersOperationResult.getEnabledUsers());
    }

    @Test
    void shouldFailWithErrorIfNoUsersAreProvidedDuringBulkEnableOrDisableUsers() {
        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();
        userService.bulkEnableDisableUsers(null, true, result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("No users selected.");
    }

    @Test
    void shouldFailWithErrorIfEmptyUsersAreProvidedDuringBulkEnableOrDisableUsers() {
        List<String> usernames = new ArrayList<>();

        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();
        userService.bulkEnableDisableUsers(usernames, true, result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo("No users selected.");
    }

    @Test
    void shouldFindUserHavingSubscriptionAndPermissionForPipeline() {
        User foo = new User("foo", "fOO,Foo", "foo@cruise.com", false);
        foo.addNotificationFilter(new NotificationFilter("p1", "s1", StageEvent.Passes, true));
        User bar = new User("bar", "bAR,Bar", "bar@go.com", true);
        bar.addNotificationFilter(new NotificationFilter("p1", "s1", StageEvent.Passes, true));
        User quux = new User("quux", "qUUX,Quux", "quux@cruise.go", false);
        quux.addNotificationFilter(new NotificationFilter("p2", "s2", StageEvent.Passes, true));

        when(userDao.findNotificationSubscribingUsers()).thenReturn(new Users(List.of(foo, bar, quux)));
        when(securityService.hasViewPermissionForPipeline(foo.getUsername(), "p1")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(bar.getUsername(), "p1")).thenReturn(false);
        assertThat(userService.findValidSubscribers(new StageConfigIdentifier("p1", "s1"))).containsExactly(foo);
    }

    @Test
    void shouldFindUserSubscribingForAnyPipelineAndThatHasPermission() {
        User foo = new User("foo", "fOO,Foo", "foo@cruise.com", false);
        foo.addNotificationFilter(new NotificationFilter(NotificationFilter.ANY_PIPELINE, NotificationFilter.ANY_STAGE, StageEvent.Passes, true));
        User bar = new User("bar", "bAR,Bar", "bar@go.com", true);
        bar.addNotificationFilter(new NotificationFilter(NotificationFilter.ANY_PIPELINE, NotificationFilter.ANY_STAGE, StageEvent.Passes, true));

        when(userDao.findNotificationSubscribingUsers()).thenReturn(new Users(List.of(foo, bar)));
        when(securityService.hasViewPermissionForPipeline(foo.getUsername(), "p1")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(bar.getUsername(), "p1")).thenReturn(false);
        assertThat(userService.findValidSubscribers(new StageConfigIdentifier("p1", "s1"))).containsExactly(foo);
    }

    @Test
    void shouldFailWithErrorWhenDeletingAnEnabledUserFails() {
        String username = "username";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(userDao.deleteUser(username, "currentUser")).thenThrow(new UserEnabledException());
        userService.deleteUser(username, "currentUser", result);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.hasMessage()).isTrue();
    }

    @Nested
    class AddOrUpdateUser {
        @Test
        void shouldNotDoAnythingIfTheUserIsAnAnonymousUser() {
            User anonymousUser = new User(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()));
            userService.addOrUpdateUser(anonymousUser, new SecurityAuthConfig());
            verifyNoInteractions(userDao);
        }

        @Test
        void shouldThrowIfTheUserDoesNotExistInDbAndUnknownUsersAreNotAllowed() {
            String username = "new-user";
            User user = new User(username);
            when(userDao.findUser(username)).thenReturn(mock(NullUser.class));
            assertThatCode(() -> {
                SecurityAuthConfig authConfig = new SecurityAuthConfig();
                authConfig.setAllowOnlyKnownUsersToLogin(true);
                userService.addOrUpdateUser(user, authConfig);
            })
                .isInstanceOf(OnlyKnownUsersAllowedException.class)
                .hasMessage("Please ask the administrator to add you to GoCD.");

            verify(userDao).findUser(username);
            verifyNoMoreInteractions(userDao);
        }

        @Test
        void shouldAddTheUserToTheDBIfItDoesNotExist() {
            String username = "new-user";
            User user = new User(username);
            when(userDao.findUser(username)).thenReturn(mock(NullUser.class));
            userService.addOrUpdateUser(user, new SecurityAuthConfig());

            verify(userDao).saveOrUpdate(user);
        }

        @Test
        void shouldUpdateTheUserInDBIfDisplayNameIsUpdated() {
            String username = "new-user";
            User originalUserInDB = new User(username, null, "");
            originalUserInDB.setId(1);
            when(userDao.findUser(username)).thenReturn(originalUserInDB);
            User updatedUser = new User(username, "display-name", "");
            userService.addOrUpdateUser(updatedUser, new SecurityAuthConfig());

            verify(userDao).saveOrUpdate(updatedUser);
        }

        @Test
        void shouldUpdateTheUserInDBIfEmailIsUpdated() {
            String username = "new-user";
            User originalUserInDB = new User(username, "", null);
            originalUserInDB.setId(1);
            when(userDao.findUser(username)).thenReturn(originalUserInDB);
            User updatedUser = new User(username, "", "email");
            userService.addOrUpdateUser(updatedUser, new SecurityAuthConfig());

            verify(userDao).saveOrUpdate(updatedUser);
        }

        @Test
        void shouldNotUpdateTheUserIfNeitherTheDisplayNameNorEmailChanged() {
            String username = "new-user";
            User user = new User(username, null, null);
            when(userDao.findUser(username)).thenReturn(user);
            userService.addOrUpdateUser(user, new SecurityAuthConfig());

            verify(userDao).findUser(username);
            verifyNoMoreInteractions(userDao);
        }

        @Test
        void shouldNotUpdateEmailIfNewEmailFromPluginIsNull() {
            String username = "new-user";
            User existingUserWithEmailInDB = new User(username, null, "bob@gocd.org");
            User newUserFromPlugin = new User(username, null, null);
            when(userDao.findUser(username)).thenReturn(existingUserWithEmailInDB);
            userService.addOrUpdateUser(newUserFromPlugin, new SecurityAuthConfig());

            verify(userDao).findUser(username);
            verifyNoMoreInteractions(userDao);
        }
    }

    @Nested
    class AddNotificationFilter {
        @Test
        void shouldBeValidIfPipelineNameIsSetToAnyPipeline() {
            NotificationFilter filter = new NotificationFilter("[Any Pipeline]", null, null, true);
            when(userDao.load(100L)).thenReturn(new User("bob"));

            userService.addNotificationFilter(100L, filter);

            verify(userDao).load(100L);
        }

        @Test
        void shouldErrorOutWhenPipelineWithNameDoesNotExist() {
            String pipelineName = "up42";
            NotificationFilter filter = new NotificationFilter(pipelineName, null, null, true);
            BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("up42");
            when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
            when(userDao.load(100L)).thenReturn(new User("bob"));

            assertThatCode(() -> userService.addNotificationFilter(100L, filter))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("Notification filter validation failed.");
            verifyNoInteractions(userDao);
        }

        @Test
        void shouldBeValidIfPipelineWithNameExistAndStageIsSetToAnyStage() {
            String pipelineName = "up42";
            NotificationFilter filter = new NotificationFilter(pipelineName, "[Any Stage]", null, true);
            BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("up42");
            when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
            when(userDao.load(100L)).thenReturn(new User("bob"));

            userService.addNotificationFilter(100L, filter);

            verify(userDao).load(100L);
        }

        @Test
        void shouldErrorOutWhenPipelineWithNameExistAndStageDoesNotExist() {
            String pipelineName = "up42";
            String stageName = "unit-tests";
            NotificationFilter filter = new NotificationFilter(pipelineName, stageName, null, true);
            BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("up42");
            when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
            when(userDao.load(100L)).thenReturn(new User("bob"));

            assertThatCode(() -> userService.addNotificationFilter(100L, filter))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("Notification filter validation failed.");
            verifyNoInteractions(userDao);
        }

        @Test
        void shouldBeValidWhenPipelineAndStage() {
            String pipelineName = "up42";
            String stageName = "unit-tests";
            NotificationFilter filter = new NotificationFilter(pipelineName, stageName, null, true);
            BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
            cruiseConfig.addPipeline("Group-1", createPipelineConfig(pipelineName, stageName));
            when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
            when(userDao.load(100L)).thenReturn(new User("bob"));

            userService.addNotificationFilter(100L, filter);

            verify(userDao).load(100L);
        }

        @Test
        void shouldAddNotificationFilter() {
            String pipelineName = "up42";
            String stageName = "unit-tests";
            User user = new User("bob");
            BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
            cruiseConfig.addPipeline("Group-1", createPipelineConfig(pipelineName, stageName));
            when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
            when(userDao.load(100L)).thenReturn(user);
            NotificationFilter newFilter = new NotificationFilter(pipelineName, stageName, StageEvent.Breaks, true);

            userService.addNotificationFilter(100L, newFilter);

            assertThat(user.getNotificationFilters())
                .hasSize(1)
                .contains(newFilter);
        }

        @Test
        void shouldErrorOutWhenNotificationFilterWithSameConfigExist() {
            String pipelineName = "up42";
            String stageName = "unit-tests";
            User user = new User("bob");
            user.addNotificationFilter(new NotificationFilter(pipelineName, stageName, StageEvent.Breaks, true));
            BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
            cruiseConfig.addPipeline("Group-1", createPipelineConfig(pipelineName, stageName));
            when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
            when(userDao.load(100L)).thenReturn(user);
            NotificationFilter newFilter = new NotificationFilter(pipelineName, stageName, StageEvent.Breaks, true);

            assertThatCode(() -> userService.addNotificationFilter(100L, newFilter))
                .isInstanceOf(UncheckedValidationException.class)
                .hasMessage("Duplicate notification filter found for: {pipeline: \"up42\", stage: \"unit-tests\", event: \"Breaks\"}");
        }
    }

    @Nested
    class UpdateNotificationFilter {
        String pipelineName = "up42";
        String stageName = "unit-tests";
        private User user;
        private NotificationFilter existingFilter;

        @BeforeEach
        void setUp() {
            user = new User("bob");
            when(userDao.load(100L)).thenReturn(user);
            user.getNotificationFilters().add(notificationFilter(9L, pipelineName, stageName, StageEvent.Fixed));
            existingFilter = notificationFilter(10L, pipelineName, stageName, StageEvent.Breaks);
            user.getNotificationFilters().add(existingFilter);
        }

        @Test
        void shouldBeValidIfPipelineNameIsSetToAnyPipeline() {
            NotificationFilter filter = notificationFilter(10L, "[Any Pipeline]", null, null);

            userService.updateNotificationFilter(100L, filter);

            verify(userDao).load(100L);
        }

        @Test
        void shouldErrorOutWhenPipelineWithNameDoesNotExist() {
            NotificationFilter filter = notificationFilter(10L, pipelineName, null, null);
            BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines();
            when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);

            assertThatCode(() -> userService.updateNotificationFilter(100L, filter))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("Notification filter validation failed.");
            verifyNoInteractions(userDao);
        }

        @Test
        void shouldBeValidIfPipelineWithNameExistAndStageIsSetToAnyStage() {
            existingFilter.setEvent(StageEvent.Cancelled);
            existingFilter.setStageName("[Any Stage]");
            BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("up42");
            when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);

            userService.updateNotificationFilter(100L, existingFilter);

            verify(userDao).load(100L);
        }

        @Test
        void shouldErrorOutWhenPipelineWithNameExistAndStageDoesNotExist() {
            NotificationFilter filter = notificationFilter(10L, pipelineName, stageName, null);
            BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines(pipelineName);
            when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);


            assertThatCode(() -> userService.updateNotificationFilter(100L, filter))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("Notification filter validation failed.");
            verifyNoInteractions(userDao);
        }

        @Test
        void shouldUpdateNotificationFilterWhenPipelineAndStageExist() {
            BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
            cruiseConfig.addPipeline("Group-1", createPipelineConfig("up42", stageName));
            when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
            NotificationFilter updatedFilter = notificationFilter(10L, pipelineName, stageName, StageEvent.Passes);

            userService.updateNotificationFilter(100L, updatedFilter);

            assertThat(user.getNotificationFilters()).hasSize(2).contains(updatedFilter);
        }

        @Test
        void shouldErrorOutWhenNotificationFilterWithSameConfigExist() {
            BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
            cruiseConfig.addPipeline("Group-1", createPipelineConfig("up42", stageName));
            when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
            NotificationFilter updatedFilter = notificationFilter(10L, pipelineName, stageName, StageEvent.Fixed);

            assertThatCode(() -> userService.updateNotificationFilter(100L, updatedFilter))
                .isInstanceOf(UncheckedValidationException.class)
                .hasMessage("Duplicate notification filter found for: {pipeline: \"up42\", stage: \"unit-tests\", event: \"Fixed\"}");
        }

        private NotificationFilter notificationFilter(long id, String pipelineName, String stageName, StageEvent fixed) {
            NotificationFilter notificationFilter = new NotificationFilter(pipelineName, stageName, fixed, true);
            notificationFilter.setId(id);
            return notificationFilter;
        }
    }

    private void configureAdmin(String username, boolean isAdmin) {
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString(username)))).thenReturn(isAdmin);
    }
}
