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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.Users;
import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.UserEnabledException;
import com.thoughtworks.go.server.security.OnlyKnownUsersAllowedException;
import com.thoughtworks.go.server.service.result.BulkUpdateUsersOperationResult;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.TriState;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL;
import static com.thoughtworks.go.serverhealth.HealthStateType.general;
import static java.lang.String.join;

@Service
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private final UserDao userDao;
    private final SecurityService securityService;
    private final GoConfigService goConfigService;
    private final TransactionTemplate transactionTemplate;

    private final DelegatingValidationContext validationContext = new DelegatingValidationContext(null) {
        @Override
        public CruiseConfig getCruiseConfig() {
            return goConfigService.getCurrentConfig();
        }
    };

    private final Object disableUserMutex = new Object();
    private final Object enableUserMutex = new Object();

    @Autowired
    public UserService(UserDao userDao,
                       SecurityService securityService,
                       GoConfigService goConfigService,
                       TransactionTemplate transactionTemplate) {
        this.userDao = userDao;
        this.securityService = securityService;
        this.goConfigService = goConfigService;
        this.transactionTemplate = transactionTemplate;
    }

    public void disable(final List<String> usersToBeDisabled, LocalizedOperationResult result) {
        synchronized (disableUserMutex) {
            if (willDisableAllAdmins(usersToBeDisabled)) {
                result.badRequest("There must be at least one admin user enabled!");
                return;
            }
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    userDao.disableUsers(usersToBeDisabled);
                }
            });
        }
    }

    private boolean willDisableAllAdmins(List<String> usersToBeDisabled) {
        List<String> enabledUserNames = toUserNames(userDao.enabledUsers());
        enabledUserNames.removeAll(usersToBeDisabled);
        return !userNameListContainsAdmin(enabledUserNames);
    }

    private List<String> toUserNames(List<User> users) {
        return users.stream().map(User::getName).collect(Collectors.toList());
    }

    private boolean userNameListContainsAdmin(List<String> enabledUserNames) {
        return enabledUserNames.stream().anyMatch(name -> securityService.isUserAdmin(new Username(new CaseInsensitiveString(name))));
    }

    public User save(final User user,
                     TriState enabled,
                     TriState emailMe,
                     @Nullable String email,
                     @Nullable String checkinAliases,
                     LocalizedOperationResult result) {

        enabled.ifPresent(user::setEnabled);

        if (email != null) {
            user.setEmail(email);
        }

        if (checkinAliases != null) {
            user.setMatcher(checkinAliases);
        }

        emailMe.ifPresent(user::setEmailMe);

        if (validate(result, user)) {
            return user;
        }

        try {
            saveOrUpdate(user);
        } catch (ValidationException e) {
            result.badRequest("Failed to add user. Validations failed. " + e.getMessage());
        }

        return user;
    }

    public void enable(List<String> usernames) {
        synchronized (enableUserMutex) {
            userDao.enableUsers(usernames);
        }
    }

    public Set<String> allUsernames() {
        return allUsers().stream().map(User::getName).collect(Collectors.toSet());
    }

    public List<User> allUsers() {
        return userDao.allUsers();
    }

    public Collection<String> allRoleNames() {
        List<String> roles = new ArrayList<>();
        for (Role role : allRoles()) {
            roles.add(CaseInsensitiveString.str(role.getName()));
        }
        return roles;
    }

    private Collection<Role> allRoles() {
        return goConfigService.cruiseConfig().server().security().getRoles();
    }

    public User load(long id) {
        return userDao.load(id);
    }

    public void deleteUser(String username, String byWhom, HttpLocalizedOperationResult result) {
        try {
            userDao.deleteUser(username, byWhom);
            result.setMessage(EntityType.User.deleteSuccessful(username));
        } catch (RecordNotFoundException e) {
            result.notFound(EntityType.User.notFoundMessage(username), general(GLOBAL));
        } catch (UserEnabledException e) {
            result.badRequest("User '" + username + "' is not disabled.");
        }
    }

    public void deleteUsers(List<String> userNames, String byWhom, BulkUpdateUsersOperationResult result) {
        synchronized (enableUserMutex) {
            boolean isValid = performUserDeletionValidation(userNames, result);
            if (isValid) {
                userDao.deleteUsers(userNames, byWhom);
                result.setMessage(EntityType.User.deleteSuccessful(userNames));
            }
        }
    }

    public void bulkEnableDisableUsers(List<String> userNames,
                                       boolean shouldEnable,
                                       BulkUpdateUsersOperationResult result) {
        synchronized (enableUserMutex) {
            boolean isValid = performUserUpdateValidation(userNames, result);
            if (isValid) {
                if (shouldEnable) {
                    enable(userNames);
                } else {
                    disable(userNames, result);
                }

                if (result.isSuccessful()) {
                    String enabledOrDisabled = shouldEnable ? "enabled" : "disabled";
                    result.setMessage(String.format("Users '%s' were %s successfully.", join(", ", userNames), enabledOrDisabled));
                }
            }
        }
    }

    private boolean performUserDeletionValidation(List<String> userNames, BulkUpdateUsersOperationResult result) {
        boolean isValid = validateUserSelection(userNames, result);
        if (!isValid) {
            return false;
        }

        for (String userName : userNames) {
            User user = userDao.findUser(userName);
            if (user instanceof NullUser) {
                result.addNonExistentUserName(userName);
                result.unprocessableEntity("Deletion failed because some users do not exist.");
                continue;
            }
            if (user.isEnabled()) {
                result.addEnabledUserName(userName);
                result.unprocessableEntity("Deletion failed because some users were enabled.");
            }
        }

        return result.isSuccessful();
    }

    private boolean performUserUpdateValidation(List<String> userNames, BulkUpdateUsersOperationResult result) {
        boolean isValid = validateUserSelection(userNames, result);
        if (!isValid) {
            return false;
        }

        for (String userName : userNames) {
            User user = userDao.findUser(userName);
            if (user instanceof NullUser) {
                result.addNonExistentUserName(userName);
                result.unprocessableEntity("Update failed because some users do not exist.");
            }
        }

        return result.isSuccessful();
    }

    private boolean validateUserSelection(List<String> userNames, BulkUpdateUsersOperationResult result) {
        if (CollectionUtils.isEmpty(userNames)) {
            result.badRequest("No users selected.");
            return false;
        }
        return true;
    }

    public void addOrUpdateUser(User user, SecurityAuthConfig authConfig) {
        synchronized (enableUserMutex) {
            if (!user.isAnonymous()) {
                LOGGER.debug("User [{}] is not anonymous", user.getName());
                User userFromDB = userDao.findUser(user.getName());
                if (userFromDB instanceof NullUser) {
                    LOGGER.debug("User [{}] is not present in the DB.", user.getName());
                    assertUnknownUsersAreAllowedToLogin(user.getUsername(), authConfig);
                    LOGGER.debug("Adding user [{}] to the DB.", user.getName());
                    userDao.saveOrUpdate(user);
                } else if (hasUserChanged(user, userFromDB)) {
                    userFromDB.setDisplayName(user.getDisplayName());
                    userFromDB.setEmail(user.getEmail());
                    userDao.saveOrUpdate(userFromDB);
                }
            }
        }
    }

    private void assertUnknownUsersAreAllowedToLogin(Username username, SecurityAuthConfig authConfig) {
        if (authConfig.isOnlyKnownUserAllowedToLogin()) {
            throw new OnlyKnownUsersAllowedException(username.getUsername().toString(), "Please ask the administrator to add you to GoCD.");
        }
    }

    public void saveOrUpdate(User user) throws ValidationException {
        validate(user);
        synchronized (enableUserMutex) {
            userDao.saveOrUpdate(user);
        }
    }

    public User findUserByName(String username) {
        return userDao.findUser(username);
    }

    public void addNotificationFilter(final long userId, final NotificationFilter filter) {
        filter.validate(validationContext);

        if (!filter.errors().isEmpty()) {
            throw new UnprocessableEntityException("Notification filter validation failed.");
        }

        User user = userDao.load(userId);
        user.addNotificationFilter(filter);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                synchronized (enableUserMutex) {
                    userDao.saveOrUpdate(user);
                }
            }
        });
    }

    public void updateNotificationFilter(final long userId, final NotificationFilter notificationFilter) {
        notificationFilter.validate(validationContext);

        if (!notificationFilter.errors().isEmpty()) {
            throw new UnprocessableEntityException("Notification filter validation failed.");
        }

        User user = userDao.load(userId);
        user.updateNotificationFilter(notificationFilter);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                synchronized (enableUserMutex) {
                    userDao.saveOrUpdate(user);
                }
            }
        });
    }

    public void removeNotificationFilter(final long userId, final long filterId) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                User user = userDao.load(userId);
                user.removeNotificationFilter(filterId);
                userDao.saveOrUpdate(user);
            }
        });

    }

    public Users findValidSubscribers(final StageConfigIdentifier identifier) {
        Users users = userDao.findNotificationSubscribingUsers();
        return users.filter(user -> user.hasSubscribedFor(identifier.getPipelineName(), identifier.getStageName()) &&
            securityService.hasViewPermissionForPipeline(user.getUsername(), identifier.getPipelineName()));
    }

    @VisibleForTesting
    void validate(User user) throws ValidationException {
        user.validateLoginName();
        user.validateMatcher();
        user.validateEmail();
    }

    private boolean validate(LocalizedOperationResult result, User user) {
        try {
            validate(user);
        } catch (ValidationException e) {
            result.badRequest("Failed to add user. Validations failed. " + e.getMessage());
            return true;
        }
        return false;
    }

    private boolean hasUserChanged(User newUser, User originalUser) {
        boolean hasEmailChanged = newUser.getEmail() != null && !newUser.getEmail().isBlank() && !Strings.CS.equals(originalUser.getEmail(), newUser.getEmail());
        boolean hasDisplayNameChanged = !Strings.CS.equals(originalUser.getDisplayName(), newUser.getDisplayName());
        if (hasEmailChanged) {
            LOGGER.debug("User [{}] has an email change. Updating the same in the DB.", originalUser.getName());
        }
        if (hasDisplayNameChanged) {
            LOGGER.debug("User [{}] has a display name change. Updating the same in the DB.", originalUser.getName());
        }
        return hasEmailChanged || hasDisplayNameChanged;
    }

}
