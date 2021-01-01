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
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.infra.DefaultPluginManager;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginRoleServiceTest {

    @Mock
    private GoConfigService goConfigService;
    @Mock
    private PluginManager pluginManager;
    private SecurityConfig securityConfig;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        securityConfig = new SecurityConfig();
        when(goConfigService.security()).thenReturn(securityConfig);
    }

    @After
    public void tearDown() throws Exception {
        PluginRoleUsersStore.instance().clearAll();
    }

    @Test
    public void shouldBeAbleToUpdatePluginRolesToUser() throws Exception {
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.authorization.github"));
        securityConfig.addRole(new PluginRoleConfig("blackbird", "github"));
        PluginRoleService pluginRoleService = new PluginRoleService(goConfigService, pluginManager);

        pluginRoleService.updatePluginRoles("cd.go.authorization.github", "bob", CaseInsensitiveString.list("blackbird"));

        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasSize(1));
        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasItem(new RoleUser("bob")));
    }

    @Test
    public void updatePluginRoleShouldIgnoreRolesWhichAreNotMappedToThePlugin() throws Exception {
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.authorization.github"));
        securityConfig.addRole(new PluginRoleConfig("blackbird", "github"));
        securityConfig.addRole(new PluginRoleConfig("spacetiger", "ldap"));
        PluginRoleService pluginRoleService = new PluginRoleService(goConfigService, pluginManager);

        pluginRoleService.updatePluginRoles("cd.go.authorization.github", "bob", CaseInsensitiveString.list("blackbird", "spacetiger"));

        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasSize(1));
        assertThat(pluginRoleService.usersForPluginRole("spacetiger"), hasSize(0));
        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasItem(new RoleUser("bob")));
        assertThat(pluginRoleService.usersForPluginRole("spacetiger"), not(hasItem(new RoleUser("bob"))));
    }

    @Test
    public void updatePluginRolesShouldIgnoreNonExistentRoles() throws Exception {
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.authorization.github"));
        securityConfig.addRole(new PluginRoleConfig("blackbird", "github"));
        PluginRoleService pluginRoleService = new PluginRoleService(goConfigService, pluginManager);

        pluginRoleService.updatePluginRoles("cd.go.authorization.github", "alice", CaseInsensitiveString.list("blackbird", "non_existent_role"));

        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasSize(1));
        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasItem(new RoleUser("alice")));
        assertThat(pluginRoleService.usersForPluginRole("non_existent_role"), hasSize(0));
    }

    @Test
    public void updatePluginRolesShouldNotChangeRoleConfig() throws Exception {
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.authorization.github"));
        securityConfig.addRole(new PluginRoleConfig("blackbird", "github"));
        securityConfig.addRole(new RoleConfig(new CaseInsensitiveString("go_system_admin")));
        PluginRoleService pluginRoleService = new PluginRoleService(goConfigService, pluginManager);

        pluginRoleService.updatePluginRoles("cd.go.authorization.github", "bob", CaseInsensitiveString.list("blackbird", "non_existent_role", "go_system_admin"));

        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasItem(new RoleUser("bob")));
        assertThat(pluginRoleService.usersForPluginRole("non_existent_role"), hasSize(0));
        assertThat(pluginRoleService.usersForPluginRole("go_system_admin"), hasSize(0));
    }

    @Test
    public void updatePluginRolesShouldHandleDeletionOfRoleForAUser() throws Exception {
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.authorization.github"));
        securityConfig.addRole(new PluginRoleConfig("blackbird", "github"));
        securityConfig.addRole(new PluginRoleConfig("spacetiger", "github"));
        PluginRoleService pluginRoleService = new PluginRoleService(goConfigService, pluginManager);

        pluginRoleService.updatePluginRoles("cd.go.authorization.github", "bob", CaseInsensitiveString.list("blackbird", "spacetiger"));

        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasItem(new RoleUser("bob")));
        assertThat(pluginRoleService.usersForPluginRole("spacetiger"), hasItem(new RoleUser("bob")));

        pluginRoleService.updatePluginRoles("cd.go.authorization.github", "bob", CaseInsensitiveString.list("blackbird"));

        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasItem(new RoleUser("bob")));
        assertThat(pluginRoleService.usersForPluginRole("spacetiger"), not(hasItem(new RoleUser("bob"))));
    }

    @Test
    public void updatePluginRolesShouldHandleAdditionOfRoleForUser() throws Exception {
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.authorization.github"));
        securityConfig.addRole(new PluginRoleConfig("blackbird", "github"));
        securityConfig.addRole(new PluginRoleConfig("spacetiger", "github"));
        PluginRoleService pluginRoleService = new PluginRoleService(goConfigService, pluginManager);

        pluginRoleService.updatePluginRoles("cd.go.authorization.github", "bob", CaseInsensitiveString.list("blackbird"));

        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasItem(new RoleUser("bob")));
        assertThat(pluginRoleService.usersForPluginRole("spacetiger"), not(hasItem(new RoleUser("bob"))));

        pluginRoleService.updatePluginRoles("cd.go.authorization.github", "bob", CaseInsensitiveString.list("blackbird", "spacetiger"));

        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasItem(new RoleUser("bob")));
        assertThat(pluginRoleService.usersForPluginRole("spacetiger"), hasItem(new RoleUser("bob")));

    }

    @Test
    public void shouldInvalidateCacheForPluginRolesDeleted_OnConfigChange() throws Exception {
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.authorization.github"));
        securityConfig.addRole(new PluginRoleConfig("blackbird", "github"));
        securityConfig.addRole(new PluginRoleConfig("spacetiger", "github"));
        PluginRoleService pluginRoleService = new PluginRoleService(goConfigService, pluginManager);

        pluginRoleService.updatePluginRoles("cd.go.authorization.github", "bob",
                CaseInsensitiveString.list("blackbird", "spacetiger"));

        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasItem(new RoleUser("bob")));
        assertThat(pluginRoleService.usersForPluginRole("spacetiger"), hasItem(new RoleUser("bob")));

        BasicCruiseConfig newCruiseConfig = GoConfigMother.defaultCruiseConfig();
        newCruiseConfig.server().security().addRole(new PluginRoleConfig("blackbird", "github"));
        pluginRoleService.onConfigChange(newCruiseConfig);

        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasItem(new RoleUser("bob")));
        assertThat(pluginRoleService.usersForPluginRole("spacetiger"), hasSize(0));
    }

    @Test
    public void onPluginUnloadShouldRemoveCorrespondingPluginRolesFromStore() throws Exception {
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.authorization.github"));
        securityConfig.addRole(new PluginRoleConfig("blackbird", "github"));
        GoPluginDescriptor goPluginDescriptor = mock(GoPluginDescriptor.class);
        DefaultPluginManager pluginManager = mock(DefaultPluginManager.class);

        PluginRoleService pluginRoleService = new PluginRoleService(goConfigService, pluginManager);

        pluginRoleService.updatePluginRoles("cd.go.authorization.github", "bob", CaseInsensitiveString.list("blackbird"));

        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasSize(1));
        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasItem(new RoleUser("bob")));

        when(goPluginDescriptor.id()).thenReturn("cd.go.authorization.github");

        pluginRoleService.pluginUnLoaded(goPluginDescriptor);

        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasSize(0));
    }

    @Test
    public void invalidatePluginRolesShouldRemoveRolesCorrespondingToThePluginFromStore() throws Exception {
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.authorization.github"));
        securityConfig.securityAuthConfigs().add(new SecurityAuthConfig("ldap", "cd.go.authorization.ldap"));
        securityConfig.addRole(new PluginRoleConfig("blackbird", "github"));
        securityConfig.addRole(new PluginRoleConfig("spacetiger", "ldap"));

        PluginRoleService pluginRoleService = new PluginRoleService(goConfigService, pluginManager);

        pluginRoleService.updatePluginRoles("cd.go.authorization.github", "bob", CaseInsensitiveString.list("blackbird"));
        pluginRoleService.updatePluginRoles("cd.go.authorization.ldap", "bob", CaseInsensitiveString.list("spacetiger"));
        pluginRoleService.updatePluginRoles("cd.go.authorization.github", "alice", CaseInsensitiveString.list("blackbird"));

        pluginRoleService.invalidateRolesFor("cd.go.authorization.github");

        assertThat(pluginRoleService.usersForPluginRole("blackbird"), hasSize(0));
        assertThat(pluginRoleService.usersForPluginRole("spacetiger"), hasSize(1));
    }
}
