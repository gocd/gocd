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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.config.AdminRole;
import com.thoughtworks.go.config.AdminUser;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.JobConfigs;
import com.thoughtworks.go.config.PasswordFileConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RoleUser;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.domain.StageEvent;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.Users;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.presentation.UserModel;
import com.thoughtworks.go.presentation.UserSearchModel;
import com.thoughtworks.go.presentation.UserSourceType;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.UserEnabledException;
import com.thoughtworks.go.server.exceptions.UserNotFoundException;
import com.thoughtworks.go.server.persistence.OauthRepository;
import com.thoughtworks.go.server.security.LdapUserSearch;
import com.thoughtworks.go.server.security.OnlyKnownUsersAllowedException;
import com.thoughtworks.go.server.security.PasswordFileUserSearch;
import com.thoughtworks.go.server.security.UserLicenseLimitExceededException;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TestTransactionTemplate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.helper.SecurityConfigMother.securityConfigWithRole;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UserServiceTest {
    private UserDao userDao;
    private GoConfigService goConfigService;
    private SecurityService securityService;
    private UserService userService;
    private LdapUserSearch ldapUserSearch;
    private GoLicenseService licenseService;
    private PasswordFileUserSearch passwordFileUserSearch;
    private TestTransactionTemplate transactionTemplate;
    private TestTransactionSynchronizationManager transactionSynchronizationManager;
    private OauthRepository oauthRepo;

    public UserServiceTest() {
        userDao = mock(UserDao.class);
        oauthRepo = mock(OauthRepository.class);
        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        licenseService = mock(GoLicenseService.class);
        transactionSynchronizationManager = new TestTransactionSynchronizationManager();
        transactionTemplate = new TestTransactionTemplate(transactionSynchronizationManager);
    }

    @Before
    public void setUp() {

        userService = new UserService(userDao, securityService, goConfigService, licenseService, transactionTemplate, transactionSynchronizationManager, oauthRepo);
    }

    @Test
    public void shouldLoadAllUsersOrderedOnUsername() {
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        User bar = new User("bar", Arrays.asList("bAR", "Bar"), "bar@go.com", true);
        User quux = new User("quux", Arrays.asList("qUUX", "Quux"), "quux@cruise.go", false);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(foo, bar, quux)));

        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.USERNAME, UserService.SortDirection.DESC);
        assertThat(models, is(Arrays.asList(model(quux), model(foo), model(bar))));

        models = userService.allUsersForDisplay(UserService.SortableColumn.USERNAME, UserService.SortDirection.ASC);
        assertThat(models, is(Arrays.asList(model(bar), model(foo), model(quux))));
    }

    @Test
    public void shouldLoadAllUsersOrderedOnEmail() {
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        User zoo = new User("bar", Arrays.asList("bAR", "Bar"), "zooboo@go.com", true);
        User quux = new User("quux", Arrays.asList("qUUX", "Quux"), "quux@cruise.go", false);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(foo, zoo, quux)));


        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.EMAIL, UserService.SortDirection.DESC);
        assertThat(models, is(Arrays.asList(model(zoo), model(quux), model(foo))));

        models = userService.allUsersForDisplay(UserService.SortableColumn.EMAIL, UserService.SortDirection.ASC);
        assertThat(models, is(Arrays.asList(model(foo), model(quux), model(zoo))));
    }

    @Test
    public void shouldLoadAllUsersOrderedOnRoles() {
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        User bar = new User("bar", Arrays.asList("bAR", "Bar"), "zooboo@go.com", true);
        User quux = new User("quux", Arrays.asList("qUUX", "Quux"), "quux@cruise.go", false);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(foo, bar, quux)));
        when(goConfigService.rolesForUser(new CaseInsensitiveString("foo"))).thenReturn(Arrays.asList(new Role(new CaseInsensitiveString("loser")
        ), new Role(new CaseInsensitiveString("boozer"))));
        when(goConfigService.rolesForUser(new CaseInsensitiveString("bar"))).thenReturn(Arrays.asList(new Role(new CaseInsensitiveString("user")
        ), new Role(new CaseInsensitiveString("boozer"))));
        when(goConfigService.rolesForUser(new CaseInsensitiveString("quux"))).thenReturn(Arrays.asList(new Role(new CaseInsensitiveString("user")
        ), new Role(new CaseInsensitiveString("loser"))));

        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.ROLES, UserService.SortDirection.DESC);
        UserModel quuxModel = model(quux, Arrays.asList("user", "loser"), false);
        UserModel barModel = model(bar, Arrays.asList("user", "boozer"), false);
        UserModel fooModel = model(foo, Arrays.asList("loser", "boozer"), false);
        assertThat(models, is(Arrays.asList(quuxModel, barModel, fooModel)));

        models = userService.allUsersForDisplay(UserService.SortableColumn.ROLES, UserService.SortDirection.ASC);
        assertThat(models, is(Arrays.asList(fooModel, barModel, quuxModel)));
    }

    @Test
    public void shouldLoadAllUsersOrderedOnMatchers() {
        User foo = new User("foo", Arrays.asList("abc", "def"), "foo@cruise.com", false);
        User bar = new User("bar", Arrays.asList("ghi", "def"), "zooboo@go.com", true);
        User quux = new User("quux", Arrays.asList("ghi", "jkl"), "quux@cruise.go", false);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(quux, foo, bar)));

        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.MATCHERS, UserService.SortDirection.DESC);

        assertThat(models, is(Arrays.asList(model(quux), model(bar), model(foo))));

        models = userService.allUsersForDisplay(UserService.SortableColumn.MATCHERS, UserService.SortDirection.ASC);
        assertThat(models, is(Arrays.asList(model(foo), model(bar), model(quux))));
    }

    @Test
    public void shouldLoadAllUsersOrderedOnIsAdmin() {
        User foo = new User("foo", new ArrayList<String>(), "foo@cruise.com", false);
        User bar = new User("bar", new ArrayList<String>(), "zooboo@go.com", false);
        User quux = new User("quux", new ArrayList<String>(), "quux@cruise.go", false);
        User baaz = new User("baaz", new ArrayList<String>(), "baaz@cruise.go", false);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(quux, foo, bar, baaz)));

        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("foo")))).thenReturn(false);
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("bar")))).thenReturn(true);
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("quux")))).thenReturn(false);
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("baaz")))).thenReturn(true);

        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.IS_ADMIN, UserService.SortDirection.DESC);

        assertThat(models.size(), is(4));
        assertThat(models.get(2), anyOf(is(model(quux, false)), is(model(foo, false))));
        assertThat(models.get(3), anyOf(is(model(quux, false)), is(model(foo, false))));
        assertThat(models.get(0), anyOf(is(model(bar, true)), is(model(baaz, true))));
        assertThat(models.get(1), anyOf(is(model(bar, true)), is(model(baaz, true))));
        assertThat(models, hasItems(model(bar, true), model(baaz, true), model(foo, false), model(quux, false)));

        models = userService.allUsersForDisplay(UserService.SortableColumn.IS_ADMIN, UserService.SortDirection.ASC);
        assertThat(models.size(), is(4));
        assertThat(models.get(0), anyOf(is(model(quux, false)), is(model(foo, false))));
        assertThat(models.get(1), anyOf(is(model(quux, false)), is(model(foo, false))));
        assertThat(models.get(2), anyOf(is(model(bar, true)), is(model(baaz, true))));
        assertThat(models.get(3), anyOf(is(model(bar, true)), is(model(baaz, true))));

        assertThat(models, hasItems(model(bar, true), model(baaz, true), model(foo, false), model(quux, false)));
    }

    @Test
    public void shouldLoadAllUsersOrderedOnEnabled() {
        User foo = new User("foo", new ArrayList<String>(), "foo@cruise.com", false);
        User bar = new User("bar", new ArrayList<String>(), "zooboo@go.com", false);
        User quux = new User("quux", new ArrayList<String>(), "quux@cruise.go", false);
        User baaz = new User("baaz", new ArrayList<String>(), "baaz@cruise.go", false);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(quux, foo, bar, baaz)));

        foo.disable();
        quux.disable();

        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.ENABLED, UserService.SortDirection.DESC);

        assertThat(models.size(), is(4));
        assertThat(models.get(2), anyOf(is(model(quux)), is(model(foo))));
        assertThat(models.get(3), anyOf(is(model(quux)), is(model(foo))));
        assertThat(models.get(0), anyOf(is(model(bar)), is(model(baaz))));
        assertThat(models.get(1), anyOf(is(model(bar)), is(model(baaz))));
        assertThat(models, hasItems(model(bar), model(baaz), model(foo), model(quux)));

        models = userService.allUsersForDisplay(UserService.SortableColumn.ENABLED, UserService.SortDirection.ASC);
        assertThat(models.size(), is(4));
        assertThat(models.get(0), anyOf(is(model(quux)), is(model(foo))));
        assertThat(models.get(1), anyOf(is(model(quux)), is(model(foo))));
        assertThat(models.get(2), anyOf(is(model(bar)), is(model(baaz))));
        assertThat(models.get(3), anyOf(is(model(bar)), is(model(baaz))));

        assertThat(models, hasItems(model(bar), model(baaz), model(foo), model(quux)));
    }

    @Test
    public void shouldLoadAllUsersWithRolesAndAdminFlag() {
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        User bar = new User("bar", Arrays.asList("bAR", "Bar"), "bar@go.com", true);

        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(foo, bar)));
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("foo")))).thenReturn(true);
        when(goConfigService.rolesForUser(new CaseInsensitiveString("foo"))).thenReturn(Arrays.asList(new Role(new CaseInsensitiveString("loser")
        ), new Role(new CaseInsensitiveString("boozer"))));
        when(goConfigService.rolesForUser(new CaseInsensitiveString("bar"))).thenReturn(Arrays.asList(new Role(new CaseInsensitiveString("user")
        ), new Role(new CaseInsensitiveString("loser"))));

        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.USERNAME, UserService.SortDirection.ASC);
        assertThat(models, is(Arrays.asList(model(bar, Arrays.asList("user", "loser"), false), model(foo, Arrays.asList("loser", "boozer"), true))));
    }

    @Test
    public void shouldCreateNewUsers() throws Exception {
        UserSearchModel foo = new UserSearchModel(new User("fooUser", "Mr Foo", "foo@cruise.com"), UserSourceType.LDAP);

        doNothing().when(userDao).saveOrUpdate(foo.getUser());
        when(userDao.findUser("fooUser")).thenReturn(new NullUser());
        when(userDao.enabledUserCount()).thenReturn(10);
        when(licenseService.maximumUsersAllowed()).thenReturn(100);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.create(Arrays.asList(foo), result);
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldAllowCreateNewUsersFromPasswordFileWithoutEmailAddress() throws Exception {
        UserSearchModel passwordUser = new UserSearchModel(new User("passwordUser"), UserSourceType.PASSWORD_FILE);

        doNothing().when(userDao).saveOrUpdate(passwordUser.getUser());
        when(userDao.findUser("passwordUser")).thenReturn(new NullUser());
        when(userDao.enabledUserCount()).thenReturn(10);
        when(licenseService.maximumUsersAllowed()).thenReturn(100);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.create(Arrays.asList(passwordUser), result);
        assertThat(result.isSuccessful(), is(true));

    }

    @Test
    public void shouldReturnConflictWhenUserAlreadyExists() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        User existsingUser = new User("existingUser", "Existing User", "existing@user.com");
        UserSearchModel searchModel = new UserSearchModel(existsingUser, UserSourceType.LDAP);
        when(userDao.findUser("existingUser")).thenReturn(existsingUser);
        userService.create(Arrays.asList(searchModel), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(HttpServletResponse.SC_CONFLICT));
    }

    @Test
    public void shouldReturnErrorMessageWhenLicenseLimitExceeds() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        User foo = new User("fooUser", "Foo User", "foo@user.com");
        UserSearchModel searchModel = new UserSearchModel(foo, UserSourceType.LDAP);
        when(userDao.findUser("fooUser")).thenReturn(new NullUser());
        when(userDao.enabledUserCount()).thenReturn(10);
        when(licenseService.maximumUsersAllowed()).thenReturn(10);

        userService.create(Arrays.asList(searchModel), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(HttpServletResponse.SC_BAD_REQUEST));
        assertThat(result.localizable(), is((Localizable) LocalizedMessage.string("LICENSE_LIMIT_EXCEEDED")));
    }

    @Test
    public void enforceLicenseLimit_shouldThrowExceptionIfUserDowsNotExistAndOnlyKnownUsersAreAllowedToLogin() throws Exception {
        when(licenseService.maximumUsersAllowed()).thenReturn(100);
        when(userDao.enabledUserCount()).thenReturn(10);
        when(userDao.findUser("new_user")).thenReturn(new NullUser());
        when(goConfigService.isOnlyKnownUserAllowedToLogin()).thenReturn(true);
        try {
            userService.addUserIfDoesNotExist(new Username(new CaseInsensitiveString("new_user")));
            fail("should have thrown OnlyKnownUsersAllowedException");
        } catch (OnlyKnownUsersAllowedException e) {
            assertThat(e.getMessage(), is("Please ask the administrator to add you to Go"));
        }
    }

    @Test
    public void shouldReturnErrorMessageWhenUserValidationsFail() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        User invalidUser = new User("fooUser", "Foo User", "invalidEmail");
        UserSearchModel searchModel = new UserSearchModel(invalidUser, UserSourceType.LDAP);
        when(userDao.findUser("fooUser")).thenReturn(new NullUser());
        when(userDao.enabledUserCount()).thenReturn(1);
        when(licenseService.maximumUsersAllowed()).thenReturn(10);

        userService.create(Arrays.asList(searchModel), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(HttpServletResponse.SC_BAD_REQUEST));

    }

    @Test
    public void shouldReturnErrorMessageWhenTheLastAdminIsBeingDisabled() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(userDao.enabledUsers()).thenReturn(Arrays.asList(new User("Jake"), new User("Pavan"), new User("Shilpa")));
        configureAdmin("Jake", true);
        configureAdmin("Pavan", true);
        configureAdmin("Shilpa", false);

        userService.disable(Arrays.asList("Pavan", "Jake"), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(HttpServletResponse.SC_BAD_REQUEST));
    }

    @Test
    public void shouldBeAbleToTurnOffAutoLoginWhenAdminsStillPresent() throws Exception {
        when(userDao.enabledUsers()).thenReturn(Arrays.asList(new User("Jake"), new User("Pavan"), new User("Shilpa")));
        configureAdmin("Jake", true);

        assertThat(userService.canUserTurnOffAutoLogin(), is(true));
    }


    @Test
    public void shouldNotBeAbleToTurnOffAutoLoginWhenAdminsStillPresent() throws Exception {
        when(userDao.enabledUsers()).thenReturn(Arrays.asList(new User("Jake"), new User("Pavan"), new User("Shilpa")));

        assertThat(userService.canUserTurnOffAutoLogin(), is(false));
    }

    @Test
    public void shouldNotBeAbleToEnableMoreUserThanWeHaveLicenceFor() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(licenseService.maximumUsersAllowed()).thenReturn(1);
        when(userDao.enabledUsers()).thenReturn(Arrays.asList(new User("Jake")));
        userService.enable(Arrays.asList("Pavan"), result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(HttpServletResponse.SC_BAD_REQUEST));
        assertThat(result.localizable(), is((Localizable) LocalizedMessage.string("DID_NOT_ENABLE_SELECTED_USERS")));

        userService.enable(Arrays.asList("user_three"), result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(HttpServletResponse.SC_BAD_REQUEST));
        assertThat(result.localizable(), is((Localizable) LocalizedMessage.string("DID_NOT_ENABLE_SELECTED_USERS")));
    }

    @Test
    public void shouldBeAbleToReachFullLicenseLimit() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(licenseService.maximumUsersAllowed()).thenReturn(2);
        when(userDao.enabledUsers()).thenReturn(Arrays.asList(new User("Jake")));
        userService.enable(Arrays.asList("Pavan"), result);

        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldNotFailToEnableTheSameUser() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(licenseService.maximumUsersAllowed()).thenReturn(1);
        when(userDao.enabledUsers()).thenReturn(Arrays.asList(new User("Jake")));
        userService.enable(Arrays.asList("Jake"), result);

        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldReturnUsersInSortedOrderFromPipelineGroupWhoHaveOperatePermissions() {
        CruiseConfig config = new CruiseConfig();
        SecurityConfig securityConfig = new SecurityConfig(null, new PasswordFileConfig("path"), true, null);
        securityConfig.addRole(new Role(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("user1")), new RoleUser(new CaseInsensitiveString("user2")),
                new RoleUser(new CaseInsensitiveString("user3"))));
        securityConfig.addRole(new Role(new CaseInsensitiveString("role2"), new RoleUser(new CaseInsensitiveString("user4")), new RoleUser(new CaseInsensitiveString("user5")),
                new RoleUser(new CaseInsensitiveString("user3"))));
        securityConfig.addRole(new Role(new CaseInsensitiveString("role3"), new RoleUser(new CaseInsensitiveString("user4")), new RoleUser(new CaseInsensitiveString("user5")),
                new RoleUser(new CaseInsensitiveString("user2"))));
        config.setServerConfig(new ServerConfig(null, securityConfig));

        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job")));
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline", stage);
        config.addPipeline("defaultGroup", pipeline);
        PipelineConfigs group = config.findGroup("defaultGroup");
        group.getAuthorization().getOperationConfig().add(new AdminRole(new CaseInsensitiveString("role1")));
        group.getAuthorization().getOperationConfig().add(new AdminRole(new CaseInsensitiveString("role3")));
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString("pavan")));
        group.getAuthorization().getOperationConfig().add(new AdminUser(new CaseInsensitiveString("admin")));

        Set<String> allUsers = userService.usersThatCanOperateOnStage(config, pipeline);

        assertThat(allUsers.size(), is(7));
        assertThat(allUsers, hasItem("user1"));
        assertThat(allUsers, hasItem("user2"));
        assertThat(allUsers, hasItem("user3"));
        assertThat(allUsers, hasItem("user4"));
        assertThat(allUsers, hasItem("user5"));
        assertThat(allUsers, hasItem("pavan"));
        assertThat(allUsers, hasItem("admin"));
    }

    @Test
    public void shouldGetAllUsernamesIfNoSecurityHasBeenDefinedOnTheGroup() throws Exception {
        CruiseConfig config = new CruiseConfig();
        SecurityConfig securityConfig = securityConfigWithRole("role1", "user1", "user2");
        securityConfigWithRole(securityConfig, "role2", "user1", "user2", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user2");
        config.setServerConfig(new ServerConfig(null, securityConfig));

        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job")));
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline", stage);
        config.addPipeline("defaultGroup", pipeline);

        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(new User("user_one"), new User("user_two"))));
        Set<String> users = userService.usersThatCanOperateOnStage(config, pipeline);

        assertThat(config.findGroup("defaultGroup").hasAuthorizationDefined(), is(false));
        assertThat(users.size(), is(2));
        assertThat(users, hasItem("user_one"));
        assertThat(users, hasItem("user_two"));
    }

    @Test
    public void shouldNotGetAnyUsernamesIfOnlyViewAndAdminPermissionsHaveBeenDefinedOnTheGroup() throws Exception {
        CruiseConfig config = new CruiseConfig();
        SecurityConfig securityConfig = securityConfigWithRole("role1", "user1", "user2");
        securityConfigWithRole(securityConfig, "role2", "user1", "user2", "user3");
        config.setServerConfig(new ServerConfig(null, securityConfig));

        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job")));
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline", stage);
        config.addPipeline("defaultGroup", pipeline);
        PipelineConfigs group = config.findGroup("defaultGroup");
        group.getAuthorization().getAdminsConfig().add(new AdminUser(new CaseInsensitiveString("admin")));
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("pavan")));
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(new User("user_one"), new User("user_two"))));
        Set<String> users = userService.usersThatCanOperateOnStage(config, pipeline);

        assertThat(group.hasAuthorizationDefined(), is(true));
        assertThat(group.hasOperationPermissionDefined(), is(false));
        assertThat(users.size(), is(0));
    }

    @Test
    public void shouldGetAllRolesWithOperatePermissionFromPipelineGroups() throws Exception {
        CruiseConfig config = new CruiseConfig();
        SecurityConfig securityConfig = securityConfigWithRole("role1", "user1", "user2");
        securityConfigWithRole(securityConfig, "role2", "user1", "user2", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user2");
        config.setServerConfig(new ServerConfig(null, securityConfig));

        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job")));
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline", stage);
        config.addPipeline("defaultGroup", pipeline);
        PipelineConfigs group = config.findGroup("defaultGroup");
        group.getAuthorization().getOperationConfig().add(new AdminRole(new CaseInsensitiveString("role1")));
        group.getAuthorization().getOperationConfig().add(new AdminRole(new CaseInsensitiveString("role3")));

        Set<String> roles = userService.rolesThatCanOperateOnStage(config, pipeline);

        assertThat(roles.size(), is(2));
        assertThat(roles, hasItem("role1"));
        assertThat(roles, hasItem("role3"));
    }

    @Test
    public void shouldNotGetAnyRolesWhenGroupHasOnlyViewAndAdminPermissionDefined() throws Exception {
        CruiseConfig config = new CruiseConfig();
        SecurityConfig securityConfig = securityConfigWithRole("role1", "user1", "user2");
        securityConfigWithRole(securityConfig, "role2", "user1", "user2", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user2");
        config.setServerConfig(new ServerConfig(null, securityConfig));

        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job")));
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline", stage);
        config.addPipeline("defaultGroup", pipeline);
        PipelineConfigs group = config.findGroup("defaultGroup");
        group.getAuthorization().getViewConfig().add(new AdminRole(new CaseInsensitiveString("role1")));
        group.getAuthorization().getViewConfig().add(new AdminUser(new CaseInsensitiveString("shilpa")));
        group.getAuthorization().getAdminsConfig().add(new AdminRole(new CaseInsensitiveString("role3")));

        Set<String> roles = userService.rolesThatCanOperateOnStage(config, pipeline);

        assertThat(config.findGroup("defaultGroup").hasAuthorizationDefined(), is(true));
        assertThat(config.findGroup("defaultGroup").hasOperationPermissionDefined(), is(false));
        assertThat(roles.size(), is(0));
    }

    @Test
    public void shouldGetAllRolesFromConfigWhenGroupDoesNotHaveAnyPermissionsDefined() throws Exception {
        CruiseConfig config = new CruiseConfig();
        SecurityConfig securityConfig = securityConfigWithRole("role1", "user1", "user2");
        securityConfigWithRole(securityConfig, "role2", "user1", "user2", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user2");
        config.setServerConfig(new ServerConfig(null, securityConfig));

        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job")));
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline", stage);
        config.addPipeline("defaultGroup", pipeline);

        Set<String> roles = userService.rolesThatCanOperateOnStage(config, pipeline);

        assertThat(config.findGroup("defaultGroup").hasAuthorizationDefined(), is(false));
        assertThat(roles.size(), is(3));
        assertThat(roles, hasItem("role1"));
        assertThat(roles, hasItem("role2"));
        assertThat(roles, hasItem("role3"));
    }

    @Test
    public void shouldDismissLicenseExpiryWarningForUser() {
        Username username = new Username(new CaseInsensitiveString("loser"));

        User loser = new User("loser");
        loser.setId(1);
        when(userDao.load(loser.getId())).thenReturn(loser);

        userService.disableLicenseExpiryWarning(loser.getId());

        assertThat(loser.hasDisabledLicenseExpiryWarning(), is(true));
        verify(userDao).saveOrUpdate(loser);
        verify(userDao).load(loser.getId());
        verifyNoMoreInteractions(userDao);
    }

    @Test
    public void shouldDeleteUserSuccessfully() {
        String username = "username";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(userDao.deleteUser(username)).thenReturn(true);
        userService.deleteUser(username, result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.hasMessage(), is(true));
    }

    @Test
    public void shouldFailWithErrorWhenDeletingAUserFails() {
        String username = "username";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(userDao.deleteUser(username)).thenThrow(new UserNotFoundException());
        userService.deleteUser(username, result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.hasMessage(), is(true));
    }

    @Test
    public void shouldFindUserHavingSubscriptionAndPermissionForPipeline() {
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        foo.addNotificationFilter(new NotificationFilter("p1", "s1", StageEvent.Passes, true));
        User bar = new User("bar", Arrays.asList("bAR", "Bar"), "bar@go.com", true);
        bar.addNotificationFilter(new NotificationFilter("p1", "s1", StageEvent.Passes, true));
        User quux = new User("quux", Arrays.asList("qUUX", "Quux"), "quux@cruise.go", false);
        quux.addNotificationFilter(new NotificationFilter("p2", "s2", StageEvent.Passes, true));

        when(userDao.findNotificationSubscribingUsers()).thenReturn(new Users(Arrays.asList(foo, bar, quux)));
        when(securityService.hasViewPermissionForPipeline(foo.getName(), "p1")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(bar.getName(), "p1")).thenReturn(false);
        assertThat(userService.findValidSubscribers(new StageConfigIdentifier("p1", "s1")), contains(foo));
    }
    
    @Test
    public void shouldFindUserByEmailSuccess() {
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        foo.addNotificationFilter(new NotificationFilter("p1", "s1", StageEvent.Passes, true));
        User bar = new User("bar", Arrays.asList("bAR", "Bar"), "bar@go.com", true);
        bar.addNotificationFilter(new NotificationFilter("p1", "s1", StageEvent.Passes, true));
        User quux = new User("quux", Arrays.asList("qUUX", "Quux"), "quux@cruise.go", false);
        quux.addNotificationFilter(new NotificationFilter("p2", "s2", StageEvent.Passes, true));

        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(foo, bar, quux)));
        assertThat(userService.findUserByEmail(foo.getEmail()), is(foo));
    }
    
    @Test
    public void shouldFindUserByEmailFailure() {
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        foo.addNotificationFilter(new NotificationFilter("p1", "s1", StageEvent.Passes, true));
        User bar = new User("bar", Arrays.asList("bAR", "Bar"), "bar@go.com", true);
        bar.addNotificationFilter(new NotificationFilter("p1", "s1", StageEvent.Passes, true));
        User quux = new User("quux", Arrays.asList("qUUX", "Quux"), "quux@cruise.go", false);
        quux.addNotificationFilter(new NotificationFilter("p2", "s2", StageEvent.Passes, true));

        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(foo, bar, quux)));
        assertThat((userService.findUserByEmail("notreal@fake.com") instanceof NullUser), is(true));
    }
    

    @Test
    public void shouldFailWithErrorWhenDeletingAnEnabledUserFails() {
        String username = "username";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(userDao.deleteUser(username)).thenThrow(new UserEnabledException());
        userService.deleteUser(username, result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.hasMessage(), is(true));
    }

    @Test
    public void addUserIfDoesNotExist_shouldThrowExceptionIfUserLimitExceededForNewUser() throws Exception {
        Username user = new Username(new CaseInsensitiveString("user"));
        when(userDao.findUser(user.getUsername().toString())).thenReturn(new NullUser());
        when(goConfigService.isOnlyKnownUserAllowedToLogin()).thenReturn(false);
        when(licenseService.maximumUsersAllowed()).thenReturn(2);
        when(userDao.enabledUserCount()).thenReturn(2);

        try {
            userService.addUserIfDoesNotExist(user);
            Assert.fail();
        } catch (UserLicenseLimitExceededException e) {
            assertThat(e.getMessage(), is("User license limit exceeded, please contact the administrator"));
        }
    }

    private void configureAdmin(String username, boolean isAdmin) {
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString(username)))).thenReturn(isAdmin);
    }

    private List<UserSearchModel> users(String... usernames) {
        List<UserSearchModel> models = new ArrayList<UserSearchModel>();
        for (String username : usernames) {
            models.add(new UserSearchModel(new User(username, username, "foo@cruise.com")));
        }
        return models;
    }


    private UserModel model(User user) {
        return model(user, false);
    }

    private UserModel model(User user, boolean admin) {
        return model(user, new ArrayList<String>(), admin);
    }

    private UserModel model(User user, List<String> roles, boolean isAdmin) {
        return new UserModel(user, roles, isAdmin);
    }
}
