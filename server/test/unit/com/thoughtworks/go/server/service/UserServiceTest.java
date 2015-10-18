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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.Users;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.presentation.UserModel;
import com.thoughtworks.go.presentation.UserSearchModel;
import com.thoughtworks.go.presentation.UserSourceType;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.UserEnabledException;
import com.thoughtworks.go.server.exceptions.UserNotFoundException;
import com.thoughtworks.go.server.persistence.OauthRepository;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TestTransactionTemplate;
import com.thoughtworks.go.util.GoConstants;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.thoughtworks.go.helper.SecurityConfigMother.securityConfigWithRole;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class UserServiceTest {
    private UserDao userDao;
    private GoConfigService goConfigService;
    private SecurityService securityService;
    private UserService userService;
    private TestTransactionTemplate transactionTemplate;
    private TestTransactionSynchronizationManager transactionSynchronizationManager;
    private OauthRepository oauthRepo;

    public UserServiceTest() {
        userDao = mock(UserDao.class);
        oauthRepo = mock(OauthRepository.class);
        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        transactionSynchronizationManager = new TestTransactionSynchronizationManager();
        transactionTemplate = new TestTransactionTemplate(transactionSynchronizationManager);
    }

    @Before
    public void setUp() {

        userService = new UserService(userDao, securityService, goConfigService, transactionTemplate, oauthRepo);
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
    public void shouldReturnErrorMessageWhenUserValidationsFail() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        User invalidUser = new User("fooUser", "Foo User", "invalidEmail");
        UserSearchModel searchModel = new UserSearchModel(invalidUser, UserSourceType.LDAP);
        when(userDao.findUser("fooUser")).thenReturn(new NullUser());
        when(userDao.enabledUserCount()).thenReturn(1);

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
    public void shouldNotFailToEnableTheSameUser() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(userDao.enabledUsers()).thenReturn(Arrays.asList(new User("Jake")));
        userService.enable(Arrays.asList("Jake"), result);

        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldReturnUsersInSortedOrderFromPipelineGroupWhoHaveOperatePermissions() {
        CruiseConfig config = new BasicCruiseConfig();
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
        CruiseConfig config = new BasicCruiseConfig();
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
        CruiseConfig config = new BasicCruiseConfig();
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
        CruiseConfig config = new BasicCruiseConfig();
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
        CruiseConfig config = new BasicCruiseConfig();
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
        CruiseConfig config = new BasicCruiseConfig();
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
        when(securityService.hasViewPermissionForPipeline(foo.getUsername(), "p1")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(bar.getUsername(), "p1")).thenReturn(false);
        assertThat(userService.findValidSubscribers(new StageConfigIdentifier("p1", "s1")), contains(foo));
    }

    @Test
    public void shouldFindUserSubscribingForAnyPipelineAndThatHasPermission() {
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        foo.addNotificationFilter(new NotificationFilter(GoConstants.ANY_PIPELINE, GoConstants.ANY_STAGE, StageEvent.Passes, true));
        User bar = new User("bar", Arrays.asList("bAR", "Bar"), "bar@go.com", true);
        bar.addNotificationFilter(new NotificationFilter(GoConstants.ANY_PIPELINE, GoConstants.ANY_STAGE, StageEvent.Passes, true));

        when(userDao.findNotificationSubscribingUsers()).thenReturn(new Users(Arrays.asList(foo, bar)));
        when(securityService.hasViewPermissionForPipeline(foo.getUsername(), "p1")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(bar.getUsername(), "p1")).thenReturn(false);
        assertThat(userService.findValidSubscribers(new StageConfigIdentifier("p1", "s1")), contains(foo));
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

    private void configureAdmin(String username, boolean isAdmin) {
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString(username)))).thenReturn(isAdmin);
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
