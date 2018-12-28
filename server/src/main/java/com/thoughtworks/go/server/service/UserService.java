/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.Users;
import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.presentation.UserModel;
import com.thoughtworks.go.presentation.UserSearchModel;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.UserEnabledException;
import com.thoughtworks.go.server.exceptions.UserNotFoundException;
import com.thoughtworks.go.server.security.OnlyKnownUsersAllowedException;
import com.thoughtworks.go.server.service.result.BulkDeletionFailureResult;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.TriState;
import com.thoughtworks.go.util.comparator.AlphaAsciiCollectionComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.util.*;

import static com.thoughtworks.go.i18n.LocalizedMessage.resourceAlreadyExists;
import static com.thoughtworks.go.i18n.LocalizedMessage.resourceNotFound;
import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL;
import static com.thoughtworks.go.serverhealth.HealthStateType.general;

@Service
public class UserService {
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

    public void deleteAll() {
        userDao.deleteAll();
    }

    public void disable(final List<String> usersToBeDisabled, LocalizedOperationResult result) {
        synchronized (disableUserMutex) {
            if (willDisableAllAdmins(usersToBeDisabled)) {
                result.badRequest("Did not disable any of the selected users. Ensure that all configured admins are not being disabled.");
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

    public void deleteUser(String username, HttpLocalizedOperationResult result) {
        try {
            userDao.deleteUser(username);
            result.setMessage(LocalizedMessage.resourceDeleteSuccessful("user", username));
        } catch (UserNotFoundException e) {
            result.notFound(resourceNotFound("User", username), general(GLOBAL));
        } catch (UserEnabledException e) {
            result.badRequest("User '" + username + "' is not disabled.");
        }
    }

    public BulkDeletionFailureResult deleteUsers(List<String> userNames, HttpLocalizedOperationResult result) {
        if (userNames == null || userNames.isEmpty()) {
            result.badRequest("No users selected.");
            return new BulkDeletionFailureResult();
        }
        synchronized (enableUserMutex) {
            BulkDeletionFailureResult bulkDeletionFailureResult = deletionValidation(userNames);

            if (bulkDeletionFailureResult.isEmpty()) {
                userDao.deleteUsers(userNames);
                result.setMessage(LocalizedMessage.resourcesDeleteSuccessful("Users", userNames));
            } else {
                result.unprocessableEntity("Deletion failed because some users were either enabled or do not exist.");
            }
            return bulkDeletionFailureResult;
        }
    }

    private BulkDeletionFailureResult deletionValidation(List<String> userNames) {
        BulkDeletionFailureResult bulkDeletionFailureResult = new BulkDeletionFailureResult();
        for (String userName : userNames) {
            User user = userDao.findUser(userName);
            if (user instanceof NullUser) {
                bulkDeletionFailureResult.addNonExistentUserName(userName);
                continue;
            }
            if (user.isEnabled()) {
                bulkDeletionFailureResult.addEnabledUserName(userName);
            }
        }
        return bulkDeletionFailureResult;
    }

    public enum SortableColumn {
        EMAIL {
            protected String get(UserModel model) {
                return model.getUser().getEmail();
            }

        },
        USERNAME {
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

    public void addUserIfDoesNotExist(User user) {
        synchronized (enableUserMutex) {
            if (!(user.isAnonymous() || userExists(user))) {
                assertUnknownUsersAreAllowedToLogin(user.getUsername());

                userDao.saveOrUpdate(user);
            }
        }
    }

    public void withEnableUserMutex(Runnable runnable) {
        synchronized (enableUserMutex) {
            runnable.run();
        }
    }

    private void assertUnknownUsersAreAllowedToLogin(Username username) {
        if (goConfigService.isOnlyKnownUserAllowedToLogin()) {
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

    public void addNotificationFilter(final long userId, final NotificationFilter filter) {
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
                    result.conflict(resourceAlreadyExists("user", user.getName()));
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

}
