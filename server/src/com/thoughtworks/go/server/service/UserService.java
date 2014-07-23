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

package com.thoughtworks.go.server.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.GoConfigFileDao;
import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.Users;
import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.presentation.UserModel;
import com.thoughtworks.go.presentation.UserSearchModel;
import com.thoughtworks.go.presentation.UserSourceType;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.UserEnabledException;
import com.thoughtworks.go.server.exceptions.UserNotFoundException;
import com.thoughtworks.go.server.persistence.OauthRepository;
import com.thoughtworks.go.server.security.OnlyKnownUsersAllowedException;
import com.thoughtworks.go.server.security.UserLicenseLimitExceededException;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.util.Filter;
import com.thoughtworks.go.util.comparator.AlphaAsciiCollectionComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

@Service
public class UserService {
    private final UserDao userDao;
    private final SecurityService securityService;
    private final GoConfigService goConfigService;
    private final GoLicenseService licenseService;
    private final OauthRepository oauthRepository;
    private final TransactionSynchronizationManager transactionSynchronizationManager;
    private final TransactionTemplate transactionTemplate;

    private final Object disableUserMutex = new Object();
    private final Object enableUserMutex = new Object();

    @Autowired
    public UserService(UserDao userDao, SecurityService securityService, GoConfigService goConfigService, GoLicenseService licenseService, TransactionTemplate transactionTemplate,
                       TransactionSynchronizationManager transactionSynchronizationManager, OauthRepository oauthRepository) {
        this.userDao = userDao;
        this.securityService = securityService;
        this.goConfigService = goConfigService;
        this.licenseService = licenseService;
        this.transactionTemplate = transactionTemplate;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.oauthRepository = oauthRepository;
    }

    public void deleteAll() {
        userDao.deleteAll();
    }

    public void disable(final List<String> usersToBeDisabled, LocalizedOperationResult result) {
        synchronized (disableUserMutex) {
            if (willDisableAllAdmins(usersToBeDisabled)) {
                result.badRequest(LocalizedMessage.string("CANNOT_DISABLE_LAST_ADMIN"));
                return;
            }
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    userDao.disableUsers(usersToBeDisabled);
                    oauthRepository.deleteUsersOauthGrants(usersToBeDisabled);
                }
            });
        }
    }

    public boolean canUserTurnOffAutoLogin() {
        return !willDisableAllAdmins(new ArrayList<String>());
    }

    private boolean willDisableAllAdmins(List<String> usersToBeDisabled) {
        List<String> enabledUserNames = toUserNames(userDao.enabledUsers());
        enabledUserNames.removeAll(usersToBeDisabled);
        return !userNameListContainsAdmin(enabledUserNames);
    }

    private List<String> toUserNames(List<User> enabledUsers) {
        List<String> enabledUserNames = new ArrayList<String>();
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

    public void enable(List<String> usernames, LocalizedOperationResult result) {
        synchronized (enableUserMutex) {
            Set<String> potentialEnabledUsers = new HashSet<String>(toUserNames(userDao.enabledUsers()));
            potentialEnabledUsers.addAll(usernames);
            if (licenseService.maximumUsersAllowed() < potentialEnabledUsers.size()) {
                result.badRequest(LocalizedMessage.string("DID_NOT_ENABLE_SELECTED_USERS"));
                return;
            }
            userDao.enableUsers(usernames);
        }
    }

    public int enabledUserCount() {
        return userDao.enabledUserCount();
    }

    public int disabledUserCount() {
        return allUsersForDisplay().size() - enabledUserCount();
    }

    public void modifyRolesAndUserAdminPrivileges(final List<String> users, final TriStateSelection adminPrivilege, final List<TriStateSelection> roleSelections, LocalizedOperationResult result) {
        Users allUsers = userDao.allUsers();
        for (String user : users) {
            if (!allUsers.containsUserNamed(user)) {
                result.badRequest(LocalizedMessage.string("USER_DOES_NOT_EXIST_IN_DB", user));
                return;
            }
        }
        try {
            final GoConfigFileDao.CompositeConfigCommand command = new GoConfigFileDao.CompositeConfigCommand();
            command.addCommand(goConfigService.modifyRolesCommand(users, roleSelections));
            command.addCommand(goConfigService.modifyAdminPrivilegesCommand(users, adminPrivilege));
            goConfigService.updateConfig(command);
        } catch (Exception e) {
            result.badRequest(LocalizedMessage.string("INVALID_ROLE_NAME", e.getMessage()));
        }
    }

    public Set<String> allUsernames() {
        List<UserModel> userModels = allUsersForDisplay();
        Set<String> users = new HashSet<String>();
        for (UserModel model : userModels) {
            users.add(model.getUser().getName());
        }
        return users;
    }

    public Collection<String> allRoleNames(CruiseConfig cruiseConfig) {
        List<String> roles = new ArrayList<String>();
        for (Role role : allRoles(cruiseConfig)) {
            roles.add(CaseInsensitiveString.str(role.getName()));
        }
        return roles;
    }

    public Collection<Role> allRoles(CruiseConfig cruiseConfig) {
        return cruiseConfig.server().security().getRoles();
    }

    public Set<String> usersThatCanOperateOnStage(CruiseConfig cruiseConfig, PipelineConfig pipelineConfig) {
        SortedSet<String> users = new TreeSet<String>();
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
        SortedSet<String> roles = new TreeSet<String>();
        if (group.hasAuthorizationDefined()) {
            if (group.hasOperationPermissionDefined()) {
                roles.addAll(group.getOperateRoleNames());
            }
        } else {
            roles.addAll(allRoleNames(cruiseConfig));
        }
        return roles;
    }

    public void disableLicenseExpiryWarning(long userId) {
        User user = userDao.load(userId);
        user.disableLicenseExpiryWarning();
        userDao.saveOrUpdate(user);
    }

    public User load(long id) {
        return userDao.load(id);
    }

    public void deleteUser(String username, HttpLocalizedOperationResult result) {
        try {
            userDao.deleteUser(username);
            result.setMessage(LocalizedMessage.string("USER_DELETE_SUCCESSFUL", username));
        } catch (UserNotFoundException e) {
            result.notFound(LocalizedMessage.string("USER_NOT_FOUND", username), HealthStateType.general(HealthStateScope.GLOBAL));
        } catch (UserEnabledException e) {
            result.badRequest(LocalizedMessage.string("USER_NOT_DISABLED", username));
        }
    }

    public static enum SortableColumn {
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
                return new Comparator<UserModel>() {
                    public int compare(UserModel one, UserModel other) {
                        return STRING_COMPARATOR.compare(one.getRoles(), other.getRoles());
                    }
                };
            }
        },
        MATCHERS {
            @Override
            public Comparator<UserModel> sorter() {
                return new Comparator<UserModel>() {
                    public int compare(UserModel one, UserModel other) {
                        return STRING_COMPARATOR.compare(one.getUser().getMatchers(), other.getUser().getMatchers());
                    }
                };
            }
        },
        IS_ADMIN {
            @Override
            public Comparator<UserModel> sorter() {
                return new Comparator<UserModel>() {
                    public int compare(UserModel one, UserModel other) {
                        return ((Boolean) one.isAdmin()).compareTo(other.isAdmin());
                    }
                };
            }
        },
        ENABLED {
            @Override
            public Comparator<UserModel> sorter() {
                return new Comparator<UserModel>() {
                    public int compare(UserModel one, UserModel other) {
                        return ((Boolean) one.isEnabled()).compareTo(other.isEnabled());
                    }
                };
            }
        };

        private static final AlphaAsciiCollectionComparator<String> STRING_COMPARATOR = new AlphaAsciiCollectionComparator<String>();


        public Comparator<UserModel> sorter() {
            return new Comparator<UserModel>() {
                public int compare(UserModel one, UserModel other) {
                    return get(one).compareTo(get(other));
                }
            };
        }

        protected String get(UserModel model) {
            return null;
        }
    }

    public static enum SortDirection {
        ASC {
            @Override
            public Comparator<UserModel> forColumn(final SortableColumn column) {
                return column.sorter();
            }
        },
        DESC {
            @Override
            public Comparator<UserModel> forColumn(final SortableColumn column) {
                return new Comparator<UserModel>() {
                    public int compare(UserModel one, UserModel other) {
                        return column.sorter().compare(other, one);
                    }
                };
            }
        };

        public abstract Comparator<UserModel> forColumn(SortableColumn column);
    }

    public void addUserIfDoesNotExist(Username userName) {
        synchronized (enableUserMutex) {
            User user = new User(CaseInsensitiveString.str(userName.getUsername()));
            if (!(user.isAnonymous() || userExists(user))) {
                assertUnknownUsersAreAllowedToLogin();
                assertLicenseLimitNotExceeded();
                userDao.saveOrUpdate(user);
            }
        }
    }

    private void assertUnknownUsersAreAllowedToLogin() {
        if (goConfigService.isOnlyKnownUserAllowedToLogin()) {
            throw new OnlyKnownUsersAllowedException("Please ask the administrator to add you to Go");
        }
    }

    private void assertLicenseLimitNotExceeded() {
        if (isLicenseUserLimitReached()) {
            throw new UserLicenseLimitExceededException("User license limit exceeded, please contact the administrator");
        }
    }

    public void saveOrUpdate(User user) throws ValidationException {
        validate(user);
        synchronized (enableUserMutex) {
            //TODO: MAY BE WE SHOULD CHECK FOR LICENSE LIMIT
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
        return users.filter(new Filter<User>() {
            public boolean matches(User user) {
                return securityService.hasViewPermissionForPipeline(user.getName(), identifier.getPipelineName());
            }
        });
    }

    public void validate(User user) throws ValidationException {
        user.validateMatcher();
        user.validateEmail();
    }

    public List<UserModel> allUsersForDisplay(SortableColumn column, SortDirection direction) {
        List<UserModel> userModels = allUsersForDisplay();
        Comparator<UserModel> userModelComparator = direction.forColumn(column);

        Collections.sort(userModels, userModelComparator);
        return userModels;
    }

    private List<UserModel> allUsersForDisplay() {
        Collection<User> users = allUsers();
        ArrayList<UserModel> userModels = new ArrayList<UserModel>();
        for (User user : users) {
            String userName = user.getName();

            ArrayList<String> roles = new ArrayList<String>();
            for (Role role : goConfigService.rolesForUser(new CaseInsensitiveString(userName))) {
                roles.add(CaseInsensitiveString.str(role.getName()));
            }

            userModels.add(new UserModel(user, roles, securityService.isUserAdmin(new Username(new CaseInsensitiveString(userName)))));
        }
        return userModels;
    }

    public Collection<User> allUsers() {
        Set<User> result = new HashSet<User>();
        result.addAll(userDao.allUsers());
        return result;
    }

    public void create(List<UserSearchModel> userSearchModels, HttpLocalizedOperationResult result) {
        if (userSearchModels.isEmpty()) {
            result.badRequest(LocalizedMessage.string("NO_USERS_SELECTED"));
            return;
        }
        synchronized (enableUserMutex) {
            for (UserSearchModel userSearchModel : userSearchModels) {
                User user = userSearchModel.getUser();

                if (userExists(user)) {
                    result.conflict(LocalizedMessage.string("USER_ALREADY_EXISTS", user.getName(), user.getDisplayName(), user.getEmail()));
                    return;
                }

                if (user.isAnonymous()) {
                    result.badRequest(LocalizedMessage.string("USERNAME_NOT_PERMITTED", user.getName()));
                    return;
                }

                if (isLicenseUserLimitReached()) {
                    result.badRequest(LocalizedMessage.string("LICENSE_LIMIT_EXCEEDED"));
                    return;
                }
                if (!userSearchModel.getUserSourceType().equals(UserSourceType.PASSWORD_FILE) && validateEmailAndMatcher(result, user)) {
                    return;
                }
                userDao.saveOrUpdate(user);
                result.setMessage(LocalizedMessage.string("USER_SUCCESSFULLY_ADDED", user.getName()));
            }
        }
    }

    public boolean isLicenseUserLimitReached() {
        return licenseService.maximumUsersAllowed() <= userDao.enabledUserCount();
    }

    public boolean isLicenseUserLimitExceeded() {
        return licenseService.maximumUsersAllowed() < userDao.enabledUserCount();
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
        Set<Role> roles = allRoles(securityConfig);
        final List<TriStateSelection> roleSelections = TriStateSelection.forRoles(roles, users);
        final TriStateSelection adminSelection = TriStateSelection.forSystemAdmin(securityConfig.adminsConfig(), roles, new SecurityService.UserRoleMatcherImpl(securityConfig),
                users);
        return new AdminAndRoleSelections(adminSelection, roleSelections);
    }

    private HashSet<Role> allRoles(SecurityConfig security) {
        return new HashSet<Role>(security.getRoles());
    }

    private boolean validateEmailAndMatcher(HttpLocalizedOperationResult result, User user) {
        try {
            validate(user);
        } catch (ValidationException e) {
            result.badRequest(LocalizedMessage.string("USER_FIELD_VALIDATIONS_FAILED", e.getMessage()));
            return true;
        }
        return false;
    }

}
