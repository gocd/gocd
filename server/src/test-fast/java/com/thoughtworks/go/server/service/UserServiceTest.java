/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.Users;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.exception.UncheckedValidationException;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.presentation.UserModel;
import com.thoughtworks.go.presentation.UserSearchModel;
import com.thoughtworks.go.presentation.UserSourceType;
import com.thoughtworks.go.server.dao.UserDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.UserEnabledException;
import com.thoughtworks.go.server.security.OnlyKnownUsersAllowedException;
import com.thoughtworks.go.server.service.result.BulkUpdateUsersOperationResult;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TestTransactionTemplate;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.thoughtworks.go.helper.PipelineConfigMother.createPipelineConfig;
import static com.thoughtworks.go.helper.SecurityConfigMother.securityConfigWithRole;
import static com.thoughtworks.go.util.SystemEnvironment.ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

public class UserServiceTest {
    private UserDao userDao;
    private GoConfigService goConfigService;
    private SecurityService securityService;
    private UserService userService;
    private TestTransactionTemplate transactionTemplate;
    private TestTransactionSynchronizationManager transactionSynchronizationManager;
    private SystemEnvironment systemEnvironment;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        transactionSynchronizationManager = new TestTransactionSynchronizationManager();
        transactionTemplate = new TestTransactionTemplate(transactionSynchronizationManager);
        systemEnvironment = mock(SystemEnvironment.class);
        userService = new UserService(userDao, securityService, goConfigService, transactionTemplate, systemEnvironment);
    }

    @Test
    void shouldLoadAllUsersOrderedOnUsername() {
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        User bar = new User("bar", Arrays.asList("bAR", "Bar"), "bar@go.com", true);
        User quux = new User("quux", Arrays.asList("qUUX", "Quux"), "quux@cruise.go", false);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(foo, bar, quux)));

        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.USERNAME, UserService.SortDirection.DESC);
        assertThat(models).isEqualTo(Arrays.asList(model(quux), model(foo), model(bar)));

        models = userService.allUsersForDisplay(UserService.SortableColumn.USERNAME, UserService.SortDirection.ASC);
        assertThat(models).isEqualTo(Arrays.asList(model(bar), model(foo), model(quux)));
    }

    @Test
    void shouldLoadAllUsersOrderedOnEmail() {
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        User zoo = new User("bar", Arrays.asList("bAR", "Bar"), "zooboo@go.com", true);
        User quux = new User("quux", Arrays.asList("qUUX", "Quux"), "quux@cruise.go", false);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(foo, zoo, quux)));


        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.EMAIL, UserService.SortDirection.DESC);
        assertThat(models).isEqualTo(Arrays.asList(model(zoo), model(quux), model(foo)));

        models = userService.allUsersForDisplay(UserService.SortableColumn.EMAIL, UserService.SortDirection.ASC);
        assertThat(models).isEqualTo(Arrays.asList(model(foo), model(quux), model(zoo)));
    }

    @Test
    void shouldLoadAllUsersOrderedOnRoles() {
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        User bar = new User("bar", Arrays.asList("bAR", "Bar"), "zooboo@go.com", true);
        User quux = new User("quux", Arrays.asList("qUUX", "Quux"), "quux@cruise.go", false);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(foo, bar, quux)));
        when(goConfigService.rolesForUser(new CaseInsensitiveString("foo"))).thenReturn(Arrays.asList(new RoleConfig(new CaseInsensitiveString("loser")
        ), new RoleConfig(new CaseInsensitiveString("boozer"))));
        when(goConfigService.rolesForUser(new CaseInsensitiveString("bar"))).thenReturn(Arrays.asList(new RoleConfig(new CaseInsensitiveString("user")
        ), new RoleConfig(new CaseInsensitiveString("boozer"))));
        when(goConfigService.rolesForUser(new CaseInsensitiveString("quux"))).thenReturn(Arrays.asList(new RoleConfig(new CaseInsensitiveString("user")
        ), new RoleConfig(new CaseInsensitiveString("loser"))));

        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.ROLES, UserService.SortDirection.DESC);
        UserModel quuxModel = model(quux, Arrays.asList("user", "loser"), false);
        UserModel barModel = model(bar, Arrays.asList("user", "boozer"), false);
        UserModel fooModel = model(foo, Arrays.asList("loser", "boozer"), false);
        assertThat(models).isEqualTo(Arrays.asList(quuxModel, barModel, fooModel));

        models = userService.allUsersForDisplay(UserService.SortableColumn.ROLES, UserService.SortDirection.ASC);
        assertThat(models).isEqualTo(Arrays.asList(fooModel, barModel, quuxModel));
    }

    @Test
    void shouldLoadAllUsersOrderedOnMatchers() {
        User foo = new User("foo", Arrays.asList("abc", "def"), "foo@cruise.com", false);
        User bar = new User("bar", Arrays.asList("ghi", "def"), "zooboo@go.com", true);
        User quux = new User("quux", Arrays.asList("ghi", "jkl"), "quux@cruise.go", false);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(quux, foo, bar)));

        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.MATCHERS, UserService.SortDirection.DESC);

        assertThat(models).isEqualTo(Arrays.asList(model(quux), model(bar), model(foo)));

        models = userService.allUsersForDisplay(UserService.SortableColumn.MATCHERS, UserService.SortDirection.ASC);
        assertThat(models).isEqualTo(Arrays.asList(model(foo), model(bar), model(quux)));
    }

    public static <T> Condition<T> anyOfObject(T... objects) {
        return new Condition<T>() {
            @Override
            public boolean matches(T value) {
                return Arrays.asList(objects).contains(value);
            }
        };
    }

    @Test
    void shouldLoadAllUsersOrderedOnIsAdmin() {
        User foo = new User("foo", new ArrayList<>(), "foo@cruise.com", false);
        User bar = new User("bar", new ArrayList<>(), "zooboo@go.com", false);
        User quux = new User("quux", new ArrayList<>(), "quux@cruise.go", false);
        User baaz = new User("baaz", new ArrayList<>(), "baaz@cruise.go", false);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(quux, foo, bar, baaz)));

        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("foo")))).thenReturn(false);
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("bar")))).thenReturn(true);
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("quux")))).thenReturn(false);
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("baaz")))).thenReturn(true);

        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.IS_ADMIN, UserService.SortDirection.DESC);


        assertThat(models.size()).isEqualTo(4);
        assertThat(models.get(2)).is(anyOfObject(model(quux, false), model(foo, false)));
        assertThat(models.get(3)).is(anyOfObject(model(quux, false), model(foo, false)));
        assertThat(models.get(0)).is(anyOfObject(model(bar, true), model(baaz, true)));
        assertThat(models.get(1)).is(anyOfObject(model(bar, true), model(baaz, true)));
        assertThat(models).contains(model(bar, true), model(baaz, true), model(foo, false), model(quux, false));

        models = userService.allUsersForDisplay(UserService.SortableColumn.IS_ADMIN, UserService.SortDirection.ASC);
        assertThat(models.size()).isEqualTo(4);
        assertThat(models.get(0)).is(anyOfObject(model(quux, false), model(foo, false)));
        assertThat(models.get(1)).is(anyOfObject(model(quux, false), model(foo, false)));
        assertThat(models.get(2)).is(anyOfObject(model(bar, true), model(baaz, true)));
        assertThat(models.get(3)).is(anyOfObject(model(bar, true), model(baaz, true)));

        assertThat(models).contains(model(bar, true), model(baaz, true), model(foo, false), model(quux, false));
    }

    @Test
    void shouldLoadAllUsersOrderedOnEnabled() {
        User foo = new User("foo", new ArrayList<>(), "foo@cruise.com", false);
        User bar = new User("bar", new ArrayList<>(), "zooboo@go.com", false);
        User quux = new User("quux", new ArrayList<>(), "quux@cruise.go", false);
        User baaz = new User("baaz", new ArrayList<>(), "baaz@cruise.go", false);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(quux, foo, bar, baaz)));

        foo.disable();
        quux.disable();

        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.ENABLED, UserService.SortDirection.DESC);

        assertThat(models.size()).isEqualTo(4);
        assertThat(models.get(2)).is(anyOfObject(model(quux), (model(foo))));
        assertThat(models.get(3)).is(anyOfObject(model(quux), (model(foo))));
        assertThat(models.get(0)).is(anyOfObject(model(bar), (model(baaz))));
        assertThat(models.get(1)).is(anyOfObject(model(bar), (model(baaz))));
        assertThat(models).contains(model(bar), model(baaz), model(foo), model(quux));

        models = userService.allUsersForDisplay(UserService.SortableColumn.ENABLED, UserService.SortDirection.ASC);
        assertThat(models.size()).isEqualTo(4);
        assertThat(models.get(0)).is(anyOfObject(model(quux), (model(foo))));
        assertThat(models.get(1)).is(anyOfObject(model(quux), (model(foo))));
        assertThat(models.get(2)).is(anyOfObject(model(bar), (model(baaz))));
        assertThat(models.get(3)).is(anyOfObject(model(bar), (model(baaz))));

        assertThat(models).contains(model(bar), model(baaz), model(foo), model(quux));
    }

    @Test
    void shouldLoadAllUsersWithRolesAndAdminFlag() {
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        User bar = new User("bar", Arrays.asList("bAR", "Bar"), "bar@go.com", true);

        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(foo, bar)));
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("foo")))).thenReturn(true);
        when(goConfigService.rolesForUser(new CaseInsensitiveString("foo"))).thenReturn(Arrays.asList(new RoleConfig(new CaseInsensitiveString("loser")
        ), new RoleConfig(new CaseInsensitiveString("boozer"))));
        when(goConfigService.rolesForUser(new CaseInsensitiveString("bar"))).thenReturn(Arrays.asList(new RoleConfig(new CaseInsensitiveString("user")
        ), new RoleConfig(new CaseInsensitiveString("loser"))));

        List<UserModel> models = userService.allUsersForDisplay(UserService.SortableColumn.USERNAME, UserService.SortDirection.ASC);
        assertThat(models).isEqualTo(Arrays.asList(model(bar, Arrays.asList("user", "loser"), false), model(foo, Arrays.asList("loser", "boozer"), true)));
    }

    @Test
    void shouldCreateNewUsers() throws Exception {
        UserSearchModel foo = new UserSearchModel(new User("fooUser", "Mr Foo", "foo@cruise.com"), UserSourceType.PLUGIN);

        doNothing().when(userDao).saveOrUpdate(foo.getUser());
        when(userDao.findUser("fooUser")).thenReturn(new NullUser());
        when(userDao.enabledUserCount()).thenReturn(10L);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userService.create(Arrays.asList(foo), result);
        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    void shouldReturnConflictWhenUserAlreadyExists() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        User existsingUser = new User("existingUser", "Existing User", "existing@user.com");
        UserSearchModel searchModel = new UserSearchModel(existsingUser, UserSourceType.PLUGIN);
        when(userDao.findUser("existingUser")).thenReturn(existsingUser);
        userService.create(Arrays.asList(searchModel), result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(HttpServletResponse.SC_CONFLICT);
    }

    @Test
    void shouldReturnErrorMessageWhenUserValidationsFail() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        User invalidUser = new User("fooUser", "Foo User", "invalidEmail");
        UserSearchModel searchModel = new UserSearchModel(invalidUser, UserSourceType.PLUGIN);
        when(userDao.findUser("fooUser")).thenReturn(new NullUser());
        when(userDao.enabledUserCount()).thenReturn(1L);

        userService.create(Arrays.asList(searchModel), result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void shouldReturnErrorMessageWhenTheLastAdminIsBeingDisabled() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(userDao.enabledUsers()).thenReturn(Arrays.asList(new User("Jake"), new User("Pavan"), new User("Shilpa")));
        configureAdmin("Jake", true);
        configureAdmin("Pavan", true);
        configureAdmin("Shilpa", false);

        userService.disable(Arrays.asList("Pavan", "Jake"), result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void shouldBeAbleToTurnOffAutoLoginWhenAdminsStillPresent() {
        when(userDao.enabledUsers()).thenReturn(Arrays.asList(new User("Jake"), new User("Pavan"), new User("Shilpa")));
        configureAdmin("Jake", true);

        assertThat(userService.canUserTurnOffAutoLogin()).isTrue();
    }


    @Test
    void shouldNotBeAbleToTurnOffAutoLoginWhenAdminsStillPresent() {
        when(userDao.enabledUsers()).thenReturn(Arrays.asList(new User("Jake"), new User("Pavan"), new User("Shilpa")));

        assertThat(userService.canUserTurnOffAutoLogin()).isFalse();
    }

    @Test
    void shouldNotFailToEnableTheSameUser() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        when(userDao.enabledUsers()).thenReturn(Arrays.asList(new User("Jake")));
        userService.enable(Arrays.asList("Jake"), result);

        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    void shouldReturnUsersInSortedOrderFromPipelineGroupWhoHaveOperatePermissions() {
        CruiseConfig config = new BasicCruiseConfig();
        SecurityConfig securityConfig = new SecurityConfig(null);
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("file", "cd.go.authentication.passwordfile"));
        securityConfig.addRole(new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("user1")), new RoleUser(new CaseInsensitiveString("user2")),
            new RoleUser(new CaseInsensitiveString("user3"))));
        securityConfig.addRole(new RoleConfig(new CaseInsensitiveString("role2"), new RoleUser(new CaseInsensitiveString("user4")), new RoleUser(new CaseInsensitiveString("user5")),
            new RoleUser(new CaseInsensitiveString("user3"))));
        securityConfig.addRole(new RoleConfig(new CaseInsensitiveString("role3"), new RoleUser(new CaseInsensitiveString("user4")), new RoleUser(new CaseInsensitiveString("user5")),
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

        assertThat(allUsers.size()).isEqualTo(7);
        assertThat(allUsers).contains("user1");
        assertThat(allUsers).contains("user2");
        assertThat(allUsers).contains("user3");
        assertThat(allUsers).contains("user4");
        assertThat(allUsers).contains("user5");
        assertThat(allUsers).contains("pavan");
        assertThat(allUsers).contains("admin");
    }

    @Test
    void shouldGetAllUsernamesAsOperatorsOfStage_IfNoSecurityHasBeenDefinedOnTheGroup_AndDefaultPermissionForGroupsWithNoAuthIsToAllowAll() {
        CruiseConfig config = new BasicCruiseConfig();
        SecurityConfig securityConfig = securityConfigWithRole("role1", "user1", "user2");
        securityConfigWithRole(securityConfig, "role2", "user1", "user2", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user2");
        config.setServerConfig(new ServerConfig(null, securityConfig));

        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job")));
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline", stage);
        config.addPipeline("defaultGroup", pipeline);

        when(systemEnvironment.get(ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP)).thenReturn(true);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(new User("user_one"), new User("user_two"))));
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("user_one")))).thenReturn(true);
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("user_two")))).thenReturn(false);

        Set<String> users = userService.usersThatCanOperateOnStage(config, pipeline);

        assertThat(config.findGroup("defaultGroup").hasAuthorizationDefined()).isFalse();
        assertThat(users.size()).isEqualTo(2);
        assertThat(users).contains("user_one");
        assertThat(users).contains("user_two");
    }

    @Test
    void shouldGetOnlySystemAdminsAsOperatorsOfStageIfNoSecurityHasBeenDefinedOnTheGroup_AndDefaultPermissionForGroupsWithNoAuthIsToDeny() {
        CruiseConfig config = new BasicCruiseConfig();
        SecurityConfig securityConfig = securityConfigWithRole("role1", "user1", "user2");
        securityConfigWithRole(securityConfig, "role2", "user1", "user2", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user2");
        config.setServerConfig(new ServerConfig(null, securityConfig));

        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job")));
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline", stage);
        config.addPipeline("defaultGroup", pipeline);

        when(systemEnvironment.get(ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP)).thenReturn(false);
        when(userDao.allUsers()).thenReturn(new Users(Arrays.asList(new User("user_one"), new User("user_two"))));
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("user_one")))).thenReturn(true);
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("user_two")))).thenReturn(false);

        Set<String> users = userService.usersThatCanOperateOnStage(config, pipeline);

        assertThat(config.findGroup("defaultGroup").hasAuthorizationDefined()).isFalse();
        assertThat(users.size()).isEqualTo(1);
        assertThat(users).contains("user_one");
    }

    @Test
    void shouldNotGetAnyUsernamesIfOnlyViewAndAdminPermissionsHaveBeenDefinedOnTheGroup() {
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

        assertThat(group.hasAuthorizationDefined()).isTrue();
        assertThat(group.hasOperationPermissionDefined()).isFalse();
        assertThat(users.size()).isEqualTo(0);
    }

    @Test
    void shouldGetAllRolesWithOperatePermissionFromPipelineGroups() {
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

        assertThat(roles.size()).isEqualTo(2);
        assertThat(roles).contains("role1");
        assertThat(roles).contains("role3");
    }

    @Test
    void shouldNotGetAnyRolesWhenGroupHasOnlyViewAndAdminPermissionDefined() {
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

        assertThat(config.findGroup("defaultGroup").hasAuthorizationDefined()).isTrue();
        assertThat(config.findGroup("defaultGroup").hasOperationPermissionDefined()).isFalse();
        assertThat(roles.size()).isEqualTo(0);
    }

    @Test
    void shouldGetAllRolesFromConfigAsOperators_WhenGroupDoesNotHaveAnyPermissionsDefined_AndDefaultPermissionForGroupsWithNoAuthIsToAllowAll() {
        CruiseConfig config = new BasicCruiseConfig();
        SecurityConfig securityConfig = securityConfigWithRole("role1", "user1", "user2");
        securityConfigWithRole(securityConfig, "role2", "user1", "user2", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user2");
        config.setServerConfig(new ServerConfig(null, securityConfig));

        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job")));
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline", stage);
        config.addPipeline("defaultGroup", pipeline);

        when(systemEnvironment.get(ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP)).thenReturn(true);
        Set<String> roles = userService.rolesThatCanOperateOnStage(config, pipeline);

        assertThat(config.findGroup("defaultGroup").hasAuthorizationDefined()).isFalse();
        assertThat(roles.size()).isEqualTo(3);
        assertThat(roles).contains("role1");
        assertThat(roles).contains("role2");
        assertThat(roles).contains("role3");
    }

    @Test
    void shouldGetNoRolesFromConfigAsOperators_WhenGroupDoesNotHaveAnyPermissionsDefined_AndDefaultPermissionForGroupsWithNoAuthIsToDeny() {
        CruiseConfig config = new BasicCruiseConfig();
        SecurityConfig securityConfig = securityConfigWithRole("role1", "user1", "user2");
        securityConfigWithRole(securityConfig, "role2", "user1", "user2", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user3");
        securityConfigWithRole(securityConfig, "role3", "user4", "user5", "user2");
        config.setServerConfig(new ServerConfig(null, securityConfig));

        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(new JobConfig("job")));
        PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline", stage);
        config.addPipeline("defaultGroup", pipeline);

        when(systemEnvironment.get(ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP)).thenReturn(false);
        Set<String> roles = userService.rolesThatCanOperateOnStage(config, pipeline);

        assertThat(config.findGroup("defaultGroup").hasAuthorizationDefined()).isFalse();
        assertThat(roles.size()).isEqualTo(0);
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
        List<String> usernames = Arrays.asList("john", "joan");

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
        assertThat(result.message()).isEqualTo(EntityType.User.deleteSuccessful(Arrays.asList("john", "joan")));
    }

    @Test
    void shouldFailWithErrorEvenIfOneUserDoesNotExistDuringBulkDelete() {
        List<String> usernames = Arrays.asList("john", "joan");
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
        List<String> usernames = Arrays.asList("john", "joan");
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
        List<String> usernames = null;

        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();
        userService.deleteUsers(usernames, "currentUser", result);

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
        List<String> usernames = Arrays.asList("john", "joan");

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

        List<String> usernames = Arrays.asList("john", "joan");

        User john = new User("john");
        User joan = new User("joan");

        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();

        when(userDao.findUser("john")).thenReturn(john);
        when(userDao.findUser("joan")).thenReturn(joan);
        when(userDao.enabledUsers()).thenReturn(Arrays.asList(joan, joan, admin));
        when(securityService.isUserAdmin(new Username("admin"))).thenReturn(true);

        userService.bulkEnableDisableUsers(usernames, false, result);

        verify(userDao).disableUsers(usernames);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.message()).isEqualTo("Users 'john, joan' were disabled successfully.");
    }

    @Test
    void shouldFailWithErrorIfAllAdminUsersAreTriedToBeDisabled() {
        List<String> usernames = Arrays.asList("john", "joan");

        User john = new User("john");
        User joan = new User("joan");

        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();

        when(userDao.findUser("john")).thenReturn(john);
        when(userDao.findUser("joan")).thenReturn(joan);
        when(userDao.enabledUsers()).thenReturn(Arrays.asList(joan, joan));

        userService.bulkEnableDisableUsers(usernames, false, result);

        verify(userDao, never()).disableUsers(usernames);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).contains("There must be atleast one admin user enabled!");
    }

    @Test
    void shouldFailWithErrorEvenIfOneUserDoesNotExistDuringBulkEnableOrDisableUsers() {
        List<String> usernames = Arrays.asList("john", "joan");
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
        List<String> usernames = null;

        BulkUpdateUsersOperationResult result = new BulkUpdateUsersOperationResult();
        userService.bulkEnableDisableUsers(usernames, true, result);

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
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        foo.addNotificationFilter(new NotificationFilter("p1", "s1", StageEvent.Passes, true));
        User bar = new User("bar", Arrays.asList("bAR", "Bar"), "bar@go.com", true);
        bar.addNotificationFilter(new NotificationFilter("p1", "s1", StageEvent.Passes, true));
        User quux = new User("quux", Arrays.asList("qUUX", "Quux"), "quux@cruise.go", false);
        quux.addNotificationFilter(new NotificationFilter("p2", "s2", StageEvent.Passes, true));

        when(userDao.findNotificationSubscribingUsers()).thenReturn(new Users(Arrays.asList(foo, bar, quux)));
        when(securityService.hasViewPermissionForPipeline(foo.getUsername(), "p1")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(bar.getUsername(), "p1")).thenReturn(false);
        assertThat(userService.findValidSubscribers(new StageConfigIdentifier("p1", "s1"))).containsExactly(foo);
    }

    @Test
    void shouldFindUserSubscribingForAnyPipelineAndThatHasPermission() {
        User foo = new User("foo", Arrays.asList("fOO", "Foo"), "foo@cruise.com", false);
        foo.addNotificationFilter(new NotificationFilter(GoConstants.ANY_PIPELINE, GoConstants.ANY_STAGE, StageEvent.Passes, true));
        User bar = new User("bar", Arrays.asList("bAR", "Bar"), "bar@go.com", true);
        bar.addNotificationFilter(new NotificationFilter(GoConstants.ANY_PIPELINE, GoConstants.ANY_STAGE, StageEvent.Passes, true));

        when(userDao.findNotificationSubscribingUsers()).thenReturn(new Users(Arrays.asList(foo, bar)));
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

    private UserModel model(User user) {
        return model(user, false);
    }

    private UserModel model(User user, boolean admin) {
        return model(user, new ArrayList<>(), admin);
    }

    private UserModel model(User user, List<String> roles, boolean isAdmin) {
        return new UserModel(user, roles, isAdmin);
    }
}
