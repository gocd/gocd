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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.StageNotFoundException;
import com.thoughtworks.go.domain.Users;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.presentation.UserModel;
import com.thoughtworks.go.presentation.UserSearchModel;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.UserEnabledException;
import com.thoughtworks.go.server.security.OnlyKnownUsersAllowedException;
import com.thoughtworks.go.server.service.result.BulkUpdateUsersOperationResult;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.TriState;
import com.thoughtworks.go.util.comparator.AlphaAsciiCollectionComparator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.util.*;

import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL;
import static com.thoughtworks.go.serverhealth.HealthStateType.general;
import static org.apache.commons.lang3.StringUtils.*;

@Service
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private final UserDao userDao;
    private final SecurityService securityService;
    private final GoConfigService goConfigService;
    private final TransactionTemplate transactionTemplate;

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
                result.badRequest("There must be atleast one admin user enabled!");
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

    public boolean canUserTurnOffAutoLogin() {
        return !willDisableAllAdmins(new ArrayList<>());
    }

    private boolean willDisableAllAdmins(List<String> usersToBeDisabled) {
        List<String> enabledUserNames = toUserNames(userDao.enabledUsers());
        enabledUserNames.removeAll(usersToBeDisabled);
        return !userNameListContainsAdmin(enabledUserNames);
    }

    private List<String> toUserNames(List<User> enabledUsers) {
        List<String> enabledUserNames = new ArrayList<>();
        for (User enabledUser : enabledUsers) {
            enabledUserNames.add(enabledUser.getName());
        }
        return enabledUserNames;
    }

    private boolean userNameListContainsAdmin(List<String> enabledUserNames) {
        for (String enabledUserName : enabledUserNames) {
            if (securityService.isUserAdmin(new Username(new CaseInsensitiveString(enabledUserName)))) {
                return true;
            }
        }
        return false;
    }

    public User save(final User user,
                     TriState enabled,
                     TriState emailMe,
                     String email,
                     String checkinAliases,
                     LocalizedOperationResult result) {
        if (enabled.isTrue()) {
            user.enable();
        }

        if (enabled.isFalse()) {
            user.disable();
        }

        if (email != null) {
            user.setEmail(email);
        }

        if (checkinAliases != null) {
            user.setMatcher(checkinAliases);
        }

        if (emailMe.isTrue()) {
            user.setEmailMe(true);
        }

        if (emailMe.isFalse()) {
            user.setEmailMe(false);
        }

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

    public void enable(List<String> usernames, LocalizedOperationResult result) {
        synchronized (enableUserMutex) {
            Set<String> potentialEnabledUsers = new HashSet<>(toUserNames(userDao.enabledUsers()));
            potentialEnabledUsers.addAll(usernames);
            userDao.enableUsers(usernames);
        }
    }

    public long enabledUserCount() {
        return userDao.enabledUserCount();
    }

    public long disabledUserCount() {
        return allUsersForDisplay().size() - enabledUserCount();
    }

    public void modifyRolesAndUserAdminPrivileges(final List<String> users,
                                                  final TriStateSelection adminPrivilege,
                                                  final List<TriStateSelection> roleSelections,
                                                  LocalizedOperationResult result) {
        Users allUsers = userDao.allUsers();
        for (String user : users) {
            if (!allUsers.containsUserNamed(user)) {
                result.badRequest("User '" + user + "' does not exist in the database.");
                return;
            }
        }
        try {
            final GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand();
            command.addCommand(goConfigService.modifyRolesCommand(users, roleSelections));
            command.addCommand(goConfigService.modifyAdminPrivilegesCommand(users, adminPrivilege));
            goConfigService.updateConfig(command);
        } catch (Exception e) {
            result.badRequest("Failed to add role. Reason - '" + e.getMessage() + "'");
        }
    }

    public Set<String> allUsernames() {
        List<UserModel> userModels = allUsersForDisplay();
        Set<String> users = new HashSet<>();
        for (UserModel model : userModels) {
            users.add(model.getUser().getName());
        }
        return users;
    }

    public Collection<String> allRoleNames(CruiseConfig cruiseConfig) {
        List<String> roles = new ArrayList<>();
        for (Role role : allRoles(cruiseConfig)) {
            roles.add(CaseInsensitiveString.str(role.getName()));
        }
        return roles;
    }

    public Collection<String> allRoleNames() {
        return allRoleNames(goConfigService.cruiseConfig());
    }

    public Collection<Role> allRoles(CruiseConfig cruiseConfig) {
        return cruiseConfig.server().security().getRoles();
    }

    public Set<String> usersThatCanOperateOnStage(CruiseConfig cruiseConfig, PipelineConfig pipelineConfig) {
        SortedSet<String> users = new TreeSet<>();
        PipelineConfigs group = cruiseConfig.findGroupOfPipeline(pipelineConfig);
        if (group.hasAuthorizationDefined()) {
            if (group.hasOperationPermissionDefined()) {
                users.addAll(group.getOperateUserNames());
                List<String> roles = group.getOperateRoleNames();
                for (Role role : cruiseConfig.server().security().getRoles()) {
                    if (roles.contains(CaseInsensitiveString.str(role.getName()))) {
                        users.addAll(role.usersOfRole());
                    }
                }
            }
        } else {
            users.addAll(allUsernames());
        }
        return users;
    }

    public Set<String> rolesThatCanOperateOnStage(CruiseConfig cruiseConfig, PipelineConfig pipelineConfig) {
        PipelineConfigs group = cruiseConfig.findGroupOfPipeline(pipelineConfig);
        SortedSet<String> roles = new TreeSet<>();
        if (group.hasAuthorizationDefined()) {
            if (group.hasOperationPermissionDefined()) {
                roles.addAll(group.getOperateRoleNames());
            }
        } else {
            roles.addAll(allRoleNames(cruiseConfig));
        }
        return roles;
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
                    enable(userNames, result);
                } else {
                    disable(userNames, result);
                }

                if (result.isSuccessful()) {
                    String enabledOrDisabled = shouldEnable ? "enabled" : "disabled";
                    result.setMessage(String.format("Users '%s' were %s successfully.", join(userNames, ", "), enabledOrDisabled));
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

    public enum SortableColumn {
        EMAIL {
            @Override
            protected String get(UserModel model) {
                return model.getUser().getEmail();
            }

        },
        USERNAME {
            @Override
            protected String get(UserModel model) {
                return model.getUser().getName();
            }
        },
        ROLES {
            @Override
            public Comparator<UserModel> sorter() {
                return (one, other) -> STRING_COMPARATOR.compare(one.getRoles(), other.getRoles());
            }
        },
        MATCHERS {
            @Override
            public Comparator<UserModel> sorter() {
                return (one, other) -> STRING_COMPARATOR.compare(one.getUser().getMatchers(), other.getUser().getMatchers());
            }
        },
        IS_ADMIN {
            @Override
            public Comparator<UserModel> sorter() {
                return (one, other) -> Boolean.compare(one.isAdmin(), other.isAdmin());
            }
        },
        ENABLED {
            @Override
            public Comparator<UserModel> sorter() {
                return (one, other) -> Boolean.compare(one.isEnabled(), other.isEnabled());
            }
        };

        private static final AlphaAsciiCollectionComparator<String> STRING_COMPARATOR = new AlphaAsciiCollectionComparator<>();


        public Comparator<UserModel> sorter() {
            return Comparator.comparing(this::get);
        }

        protected String get(UserModel model) {
            return null;
        }
    }

    public enum SortDirection {
        ASC {
            @Override
            public Comparator<UserModel> forColumn(final SortableColumn column) {
                return column.sorter();
            }
        },
        DESC {
            @Override
            public Comparator<UserModel> forColumn(final SortableColumn column) {
                return (one, other) -> column.sorter().compare(other, one);
            }
        };

        public abstract Comparator<UserModel> forColumn(SortableColumn column);
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

    public void withEnableUserMutex(Runnable runnable) {
        synchronized (enableUserMutex) {
            runnable.run();
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

    private boolean userExists(User user) {
        User foundUser = userDao.findUser(user.getName());
        return !(foundUser instanceof NullUser);
    }

    public User findUserByName(String username) {
        return userDao.findUser(username);
    }

    //Delete when remove notification filter api v1
    @Deprecated
    public void oldAddNotificationFilter(final long userId, final NotificationFilter filter) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                User user = userDao.load(userId);
                user.addNotificationFilter(filter);
                synchronized (enableUserMutex) {
                    userDao.saveOrUpdate(user);
                }
            }
        });
    }

    public void addNotificationFilter(final long userId, final NotificationFilter filter) {
        validatePipelineAndStageInNotificationFilter(filter);
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
        validatePipelineAndStageInNotificationFilter(notificationFilter);
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

    private void validatePipelineAndStageInNotificationFilter(NotificationFilter filter) {
        if (equalsIgnoreCase(filter.getPipelineName(), "[Any Pipeline]")) {
            return;
        }

        PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(filter.getPipelineName()));
        if (pipelineConfig == null) {
            throw new RecordNotFoundException(EntityType.Pipeline, filter.getPipelineName());
        }

        if (equalsIgnoreCase(filter.getStageName(), "[Any Stage]")) {
            return;
        }

        if (pipelineConfig.getStage(filter.getStageName()) == null) {
            throw new StageNotFoundException(filter.getPipelineName(), filter.getStageName());
        }
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

    public void validate(User user) throws ValidationException {
        user.validateLoginName();
        user.validateMatcher();
        user.validateEmail();
    }

    public List<UserModel> allUsersForDisplay(SortableColumn column, SortDirection direction) {
        List<UserModel> userModels = allUsersForDisplay();
        Comparator<UserModel> userModelComparator = direction.forColumn(column);

        userModels.sort(userModelComparator);
        return userModels;
    }

    private List<UserModel> allUsersForDisplay() {
        Collection<User> users = allUsers();
        ArrayList<UserModel> userModels = new ArrayList<>();
        for (User user : users) {
            String userName = user.getName();

            ArrayList<String> roles = new ArrayList<>();
            for (Role role : goConfigService.rolesForUser(new CaseInsensitiveString(userName))) {
                roles.add(CaseInsensitiveString.str(role.getName()));
            }

            userModels.add(new UserModel(user, roles, securityService.isUserAdmin(new Username(new CaseInsensitiveString(userName)))));
        }
        return userModels;
    }

    public Collection<User> allUsers() {
        Set<User> result = new HashSet<>();
        result.addAll(userDao.allUsers());
        return result;
    }

    public void create(List<UserSearchModel> userSearchModels, HttpLocalizedOperationResult result) {
        if (userSearchModels.isEmpty()) {
            result.badRequest("No users selected.");
            return;
        }
        synchronized (enableUserMutex) {
            for (UserSearchModel userSearchModel : userSearchModels) {
                User user = userSearchModel.getUser();

                if (userExists(user)) {
                    result.conflict(EntityType.User.alreadyExists(user.getName()));
                    return;
                }

                if (user.isAnonymous()) {
                    result.badRequest("Failed to add user. Username '" + user.getName() + "' is not permitted.");
                    return;
                }

                if (validate(result, user)) {
                    return;
                }
                userDao.saveOrUpdate(user);
                result.setMessage("User '" + user.getName() + "' successfully added.");
            }
        }
    }

    public static class AdminAndRoleSelections {
        private final TriStateSelection adminSelection;
        private final List<TriStateSelection> roleSelections;

        public AdminAndRoleSelections(TriStateSelection adminSelection, List<TriStateSelection> roleSelections) {
            this.adminSelection = adminSelection;
            this.roleSelections = roleSelections;
        }

        public TriStateSelection getAdminSelection() {
            return adminSelection;
        }

        public List<TriStateSelection> getRoleSelections() {
            return roleSelections;
        }
    }

    public AdminAndRoleSelections getAdminAndRoleSelections(List<String> users) {
        final SecurityConfig securityConfig = goConfigService.security();
        Set<Role> roles = new HashSet<>(securityConfig.getRoles().getRoleConfigs());
        final List<TriStateSelection> roleSelections = TriStateSelection.forRoles(roles, users);
        final TriStateSelection adminSelection = TriStateSelection.forSystemAdmin(securityConfig.adminsConfig(), roles, new SecurityService.UserRoleMatcherImpl(securityConfig),
            users);
        return new AdminAndRoleSelections(adminSelection, roleSelections);
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
        boolean hasEmailChanged = isNotBlank(newUser.getEmail()) && !StringUtils.equals(originalUser.getEmail(), newUser.getEmail());
        boolean hasDisplayNameChanged = !StringUtils.equals(originalUser.getDisplayName(), newUser.getDisplayName());
        if (hasEmailChanged) {
            LOGGER.debug("User [{}] has an email change. Updating the same in the DB.", originalUser.getName());
        }
        if (hasDisplayNameChanged) {
            LOGGER.debug("User [{}] has a display name change. Updating the same in the DB.", originalUser.getName());
        }
        return hasEmailChanged || hasDisplayNameChanged;
    }
}
