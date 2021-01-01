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
package com.thoughtworks.go.config.security;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.security.users.AllowedUsers;
import com.thoughtworks.go.config.security.users.Users;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static com.thoughtworks.go.util.SystemEnvironment.ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoConfigPipelinePermissionsAuthorityTest {
    private GoConfigMother configMother;
    private GoConfigService configService;
    private GoConfigPipelinePermissionsAuthority service;
    private CruiseConfig config;
    private PluginRoleUsersStore pluginRoleUsersStore;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        configService = mock(GoConfigService.class);
        systemEnvironment = mock(SystemEnvironment.class);
        service = new GoConfigPipelinePermissionsAuthority(configService, systemEnvironment);

        configMother = new GoConfigMother();
        config = GoConfigMother.defaultCruiseConfig();
        pluginRoleUsersStore = PluginRoleUsersStore.instance();
    }

    @After
    public void tearDown() throws Exception {
        pluginRoleUsersStore.clearAll();
    }

    @Test
    public void withSuperAdminsAndGroupLevelAuthorization_shouldConsiderAllSuperAdminUsersAsViewersOperatorsAndAdminsOfPipelines() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");

        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin2");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "superadmin2", "viewer1");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "superadmin2");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "superadmin2");
    }

    @Test
    public void withSuperAdminsThroughRolesAndGroupLevelAuthorization_shouldConsiderUsersOfAllSuperAdminRolesAsViewersOperatorsAndAdminsOfPipelines() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");

        configMother.addRole(config, configMother.createRole("superadminrole1", "superadmin1", "superadmin2"));
        configMother.addRole(config, configMother.createRole("superadminrole2", "superadmin2", "superadmin3"));
        configMother.addRoleAsSuperAdmin(config, "superadminrole1");
        configMother.addRoleAsSuperAdmin(config, "superadminrole2");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "superadmin2", "superadmin3", "viewer1");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "superadmin2", "superadmin3");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "superadmin2", "superadmin3");
    }

    @Test
    public void withSuperAdminsThroughPluginRolesAndGroupLevelAuthorization_shouldConsiderAllUsersHavingSuperAdminPluginRoleAsViewersOperatorsAndAdminsOfPipelineGroups() {
        PluginRoleConfig admin = new PluginRoleConfig("go_admins", "ldap");
        pluginRoleUsersStore.assignRole("admin_user", admin);

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");

        configMother.addRole(config, admin);
        configMother.addRoleAsSuperAdmin(config, "go_admins");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", Collections.singleton(admin), "viewer1");
        assertOperators(permissions, "pipeline1", Collections.singleton(admin));
        assertAdmins(permissions, "pipeline1", Collections.singleton(admin));
    }

    @Test
    public void withSuperAdminsAndNoGroupLevelAuthorization_withDefaultPermissionsSetToAllow_shouldConsiderAllNonAdminUsersAsViewersOperatorsOfPipelines() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        PipelineConfigs group = config.findGroup("group1");
        assertThat(group.getAuthorization(), is(new Authorization()));
        assertFalse(config.server().security().adminsConfig().isEmpty());

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();
        Permissions pipelinePermissions = permissions.get(new CaseInsensitiveString("pipeline1"));

        assertPipelinesInMap(permissions, "pipeline1");
        assertEveryoneIsAPartOf(pipelinePermissions.viewers());
        assertEveryoneIsAPartOf(pipelinePermissions.operators());
        assertEveryoneIsAPartOf(pipelinePermissions.pipelineOperators());
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1");
    }

    @Test
    public void withSuperAdminsThroughRolesAndNoGroupLevelAuthorization_withDefaultPermissionsSetToAllow_shouldConsiderAllNonAdminUsersAsViewersOperatorsOfPipelines() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, configMother.createRole("superadminrole1", "superadmin1", "superadmin2"));
        configMother.addRoleAsSuperAdmin(config, "superadminrole1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();
        Permissions pipelinePermissions = permissions.get(new CaseInsensitiveString("pipeline1"));

        assertPipelinesInMap(permissions, "pipeline1");
        assertEveryoneIsAPartOf(pipelinePermissions.viewers());
        assertEveryoneIsAPartOf(pipelinePermissions.operators());
        assertEveryoneIsAPartOf(pipelinePermissions.pipelineOperators());
    }

    @Test
    public void withSuperAdminsThroughPluginRolesAndNoGroupAuthorization_withDefaultPermissionsSetToAllow_shouldConsiderAllNonAdminUsersAsViewersOperatorsOfPipelines() {
        PluginRoleConfig admin = new PluginRoleConfig("go_admins", "ldap");
        pluginRoleUsersStore.assignRole("admin_user", admin);

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, admin);
        configMother.addRoleAsSuperAdmin(config, "go_admins");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();
        Permissions pipelinePermissions = permissions.get(new CaseInsensitiveString("pipeline1"));

        assertPipelinesInMap(permissions, "pipeline1");
        assertEveryoneIsAPartOf(pipelinePermissions.viewers());
        assertEveryoneIsAPartOf(pipelinePermissions.operators());
        assertEveryoneIsAPartOf(pipelinePermissions.pipelineOperators());
        assertAdmins(permissions, "pipeline1", Collections.singleton(admin));
    }

    @Test
    public void withSuperAdminsAndNoGroupLevelAuthorization_withDefaultPermissionsSetToDeny_shouldConsiderOnlyAdminUsersAsViewersOperatorsOfPipelines() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        PipelineConfigs group = config.findGroup("group1");
        assertThat(group.getAuthorization(), is(new Authorization()));
        assertFalse(config.server().security().adminsConfig().isEmpty());

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissionsWhenDefaultGroupPermissionIsToDeny();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1");
        assertPipelineOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1");
    }

    @Test
    public void withSuperAdminsThroughRolesAndNoGroupLevelAuthorization_withDefaultPermissionsSetToDeny_shouldConsiderOnlyAdminUsersAsViewersOperatorsOfPipelines() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, configMother.createRole("superadminrole1", "superadmin1", "superadmin2"));
        configMother.addRoleAsSuperAdmin(config, "superadminrole1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissionsWhenDefaultGroupPermissionIsToDeny();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "superadmin2");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "superadmin2");
        assertPipelineOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "superadmin2");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "superadmin2");
    }

    @Test
    public void withSuperAdminsThroughPluginRolesAndNoGroupAuthorization_withDefaultPermissionsSetToDeny_shouldConsiderOnlyAdminUsersAsViewersOperatorsOfPipelines() {
        PluginRoleConfig admin = new PluginRoleConfig("go_admins", "ldap");
        pluginRoleUsersStore.assignRole("admin_user", admin);

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, admin);
        configMother.addRoleAsSuperAdmin(config, "go_admins");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissionsWhenDefaultGroupPermissionIsToDeny();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", Collections.singleton(admin));
        assertOperators(permissions, "pipeline1", Collections.singleton(admin));
        assertPipelineOperators(permissions, "pipeline1", Collections.singleton(admin));
        assertAdmins(permissions, "pipeline1", Collections.singleton(admin));
    }

    @Test
    public void withSuperAdminsThroughPluginRolesAndGroupAuthorization_uponRevocationOfARoleForAdminUser_shouldNoLongerBeViewerOperatorOrAdminOfPipelines() {
        PluginRoleConfig admin = new PluginRoleConfig("go_admins", "ldap");
        pluginRoleUsersStore.assignRole("admin_user", admin);

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, admin);
        configMother.addRoleAsSuperAdmin(config, "go_admins");
        configMother.addUserAsViewerOfPipelineGroup(config, "view_user", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", Collections.singleton(admin), "view_user");
        assertOperators(permissions, "pipeline1", Collections.singleton(admin));
        assertAdmins(permissions, "pipeline1", Collections.singleton(admin));

        pluginRoleUsersStore.revokeAllRolesFor("admin_user");

        assertFalse(permissions.get(new CaseInsensitiveString("pipeline1")).admins().contains("admin_user"));
        assertFalse(permissions.get(new CaseInsensitiveString("pipeline1")).operators().contains("admin_user"));
        assertFalse(permissions.get(new CaseInsensitiveString("pipeline1")).viewers().contains("admin_user"));
        assertFalse(permissions.get(new CaseInsensitiveString("pipeline1")).pipelineOperators().contains("admin_user"));
    }

    @Test
    public void withSuperAdminAndPipelineGroupAdminThroughPluginRole_uponRevocationOfTheRoleForGroupAdminUser_shouldNoLongerBeViewerOperatorOrAdminOfPipelines() {
        PluginRoleConfig groupAdmin = new PluginRoleConfig("group_admin", "ldap");
        pluginRoleUsersStore.assignRole("admin_user", groupAdmin);

        configMother.addRoleAsSuperAdmin(config, "super_admin");
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, groupAdmin);
        configMother.addAdminRoleForPipelineGroup(config, "group_admin", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", Collections.singleton(groupAdmin));
        assertOperators(permissions, "pipeline1", Collections.singleton(groupAdmin));
        assertAdmins(permissions, "pipeline1", Collections.singleton(groupAdmin));

        pluginRoleUsersStore.revokeAllRolesFor("admin_user");

        assertFalse(permissions.get(new CaseInsensitiveString("pipeline1")).admins().contains("admin_user"));
        assertFalse(permissions.get(new CaseInsensitiveString("pipeline1")).operators().contains("admin_user"));
        assertFalse(permissions.get(new CaseInsensitiveString("pipeline1")).viewers().contains("admin_user"));
        assertFalse(permissions.get(new CaseInsensitiveString("pipeline1")).pipelineOperators().contains("admin_user"));
    }

    @Test
    public void shouldCreateAUniqueSetOfNamesWhenSameUserIsPartOfBothSuperAdminUsersAndRolesConfigurations() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");

        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addRole(config, configMother.createRole("superadminrole1", "superadmin1", "superadmin2"));
        configMother.addRoleAsSuperAdmin(config, "superadminrole1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "superadmin2", "viewer1");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "superadmin2");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "superadmin2");
    }

    @Test
    public void withSuperAdminsAndPipelineGroupAdminUsers_shouldConsiderPipelineGroupAdminsAsViewersOperatorsAndAdminsOfTheirPipelines() {
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group2");

        configMother.addAdminUserForPipelineGroup(config, "groupadmin1", "group1");
        configMother.addAdminUserForPipelineGroup(config, "groupadmin2", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "groupadmin1", "groupadmin2");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "groupadmin1", "groupadmin2");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "groupadmin1", "groupadmin2");

        assertViewers(permissions, "pipeline2", Collections.emptySet(), "superadmin1", "viewer1");
        assertOperators(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
        assertAdmins(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
    }

    @Test
    public void withSuperAdminsAndPipelineGroupAdminsRole_shouldConsiderAllUsersInPipelineGroupAdminRolesAsViewersOperatorsAndAdminsOfTheirPipelines() {
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group2");

        configMother.addRole(config, configMother.createRole("group1_admin_role1", "groupadmin1", "groupadmin2"));
        configMother.addRole(config, configMother.createRole("group1_admin_role2", "groupadmin2", "groupadmin3"));
        configMother.addAdminRoleForPipelineGroup(config, "group1_admin_role1", "group1");
        configMother.addAdminRoleForPipelineGroup(config, "group1_admin_role2", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "groupadmin1", "groupadmin2", "groupadmin3");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "groupadmin1", "groupadmin2", "groupadmin3");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "groupadmin1", "groupadmin2", "groupadmin3");

        assertViewers(permissions, "pipeline2", Collections.emptySet(), "superadmin1", "viewer1");
        assertOperators(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
        assertAdmins(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
    }

    @Test
    public void withSuperAdminsAndPipelineGroupAdminsPluginRole_shouldConsiderAllUsersInPipelineGroupAdminPluginRolesAsViewersOperatorsAndAdminsOfTheirPipelines() {
        PluginRoleConfig groupAdmin = new PluginRoleConfig("group_admin", "ldap");
        pluginRoleUsersStore.assignRole("admin", groupAdmin);

        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addRole(config, groupAdmin);
        configMother.addAdminRoleForPipelineGroup(config, "group_admin", "group1");

        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group2");


        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", Collections.singleton(groupAdmin), "superadmin1");
        assertOperators(permissions, "pipeline1", Collections.singleton(groupAdmin), "superadmin1");
        assertAdmins(permissions, "pipeline1", Collections.singleton(groupAdmin), "superadmin1");

        assertViewers(permissions, "pipeline2", Collections.emptySet(), "superadmin1", "viewer1");
        assertOperators(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
        assertAdmins(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
    }

    @Test
    public void shouldCreateAUniqueSetOfNamesWhenSameUserIsPartOfBothGroupAdminUsersAndRolesConfigurations() {
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, configMother.createRole("group1_admin_role1", "groupadmin1", "groupadmin2"));
        configMother.addAdminUserForPipelineGroup(config, "groupadmin1", "group1");
        configMother.addAdminRoleForPipelineGroup(config, "group1_admin_role1", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "groupadmin1", "groupadmin2");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "groupadmin1", "groupadmin2");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "groupadmin1", "groupadmin2");
    }

    @Test
    public void withSuperAdminAndGroupAuthorization_shouldConsiderUsersWithViewPermissionsAsOnlyViewersOfTheirPipelines() {
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");

        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer2", "group1");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer3", "group2");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "viewer1", "viewer2");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1");

        assertViewers(permissions, "pipeline2", Collections.emptySet(), "superadmin1", "viewer3");
        assertOperators(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
        assertAdmins(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
    }

    @Test
    public void withSuperAdminAndGroupAuthorizationThroughRole_shouldConsiderUsersOfRolesWithViewPermissionsAsOnlyViewersOfTheirPipelines() {
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group2");

        configMother.addRole(config, configMother.createRole("group1_view_role1", "groupviewer1", "groupviewer2"));
        configMother.addRole(config, configMother.createRole("group1_view_role2", "groupviewer2", "groupviewer3"));
        configMother.addRoleAsViewerOfPipelineGroup(config, "group1_view_role1", "group1");
        configMother.addRoleAsViewerOfPipelineGroup(config, "group1_view_role2", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "groupviewer1", "groupviewer2", "groupviewer3");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1");

        assertViewers(permissions, "pipeline2", Collections.emptySet(), "superadmin1", "viewer1");
        assertOperators(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
        assertAdmins(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
    }

    @Test
    public void withSuperAdminAndGroupAuthorizationThroughPluginRoles_shouldConsiderUsersOfPluginRolesWithViewPermissionsAsOnlyViewersOfTheirPipelines() {
        PluginRoleConfig groupViewer = new PluginRoleConfig("group_viewer", "ldap");
        pluginRoleUsersStore.assignRole("viewer", groupViewer);

        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addRole(config, groupViewer);
        configMother.addRoleAsViewerOfPipelineGroup(config, "group_viewer", "group1");

        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group2");


        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", Collections.singleton(groupViewer), "superadmin1");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1");

        assertViewers(permissions, "pipeline2", Collections.emptySet(), "superadmin1", "viewer1");
        assertOperators(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
        assertAdmins(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
    }

    @Test
    public void withSuperAdminAndGroupAuthorization_shouldConsiderUsersWithOperatePermissionsAsOnlyOperatorsOfTheirPipelines() {
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");

        configMother.addUserAsOperatorOfPipelineGroup(config, "operator1", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "operator2", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "operator3", "group2");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "operator1", "operator2");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1");

        assertViewers(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
        assertOperators(permissions, "pipeline2", Collections.emptySet(), "superadmin1", "operator3");
        assertAdmins(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
    }

    @Test
    public void withSuperAdminAndGroupAuthorizationThroughRole_shouldConsiderUsersOfRolesWithOperatePermissionsAsOnlyOperatorsOfTheirPipelines() {
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsOperatorOfPipelineGroup(config, "operator1", "group2");

        configMother.addRole(config, configMother.createRole("group1_operate_role1", "groupoperator1", "groupoperator2"));
        configMother.addRole(config, configMother.createRole("group2_operate_role2", "groupoperator2", "groupoperator3"));
        configMother.addRoleAsOperatorOfPipelineGroup(config, "group1_operate_role1", "group1");
        configMother.addRoleAsOperatorOfPipelineGroup(config, "group2_operate_role2", "group2");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "groupoperator1", "groupoperator2");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1");

        assertViewers(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
        assertOperators(permissions, "pipeline2", Collections.emptySet(), "superadmin1", "groupoperator2", "groupoperator3", "operator1");
        assertAdmins(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
    }

    @Test
    public void withSuperAdminAndGroupAuthorizationThroughPluginRoles_shouldConsiderUsersOfPluginRolesWithOperatePermissionsAsOnlyOperatorsOfTheirPipelines() {
        PluginRoleConfig groupOperator = new PluginRoleConfig("group_operator", "ldap");
        pluginRoleUsersStore.assignRole("operator", groupOperator);

        GoConfigMother.addUserAsSuperAdmin(config, "super_admin");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addRole(config, groupOperator);
        configMother.addRoleAsOperatorOfPipelineGroup(config, "group_operator", "group1");

        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer2", "group2");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        assertViewers(permissions, "pipeline1", Collections.emptySet(), "super_admin");
        assertOperators(permissions, "pipeline1", Collections.singleton(groupOperator), "super_admin");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "super_admin");

        assertViewers(permissions, "pipeline2", Collections.emptySet(), "super_admin", "viewer2");
        assertOperators(permissions, "pipeline2", Collections.emptySet(), "super_admin");
        assertAdmins(permissions, "pipeline2", Collections.emptySet(), "super_admin");
    }

    @Test
    public void shouldCreateAUniqueSetOfNamesWhenSameUserIsPartOfBothViewUsersAndViewRolesConfigurations() {
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, configMother.createRole("group1_view_role1", "viewer1", "groupviewer2"));
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");
        configMother.addRoleAsViewerOfPipelineGroup(config, "group1_view_role1", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");

        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "viewer1", "groupviewer2");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1");
    }

    @Test
    public void shouldCreateAnEntryForEveryPipelineInTheConfig() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1B", "job1B1", "job1B2");
        configMother.addPipelineWithGroup(config, "group3", "pipeline3", "stage1C", "job1C1", "job1C2");
        configMother.addPipelineWithGroup(config, "group3", "pipeline4", "stage1D", "job1D1", "job1D2");

        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addRole(config, configMother.createRole("group1adminrole", "group1admin1", "group1admin2"));
        configMother.addAdminRoleForPipelineGroup(config, "group1adminrole", "group1");

        configMother.addUserAsViewerOfPipelineGroup(config, "group2viewer1", "group2");

        configMother.addRole(config, configMother.createRole("group3_view_role1", "group3viewer1", "group3viewer2"));
        configMother.addRoleAsViewerOfPipelineGroup(config, "group3_view_role1", "group3");
        configMother.addUserAsOperatorOfPipelineGroup(config, "group3operator1", "group3");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2", "pipeline3", "pipeline4");

        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "group1admin1", "group1admin2");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "group1admin1", "group1admin2");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "group1admin1", "group1admin2");

        assertViewers(permissions, "pipeline2", Collections.emptySet(), "superadmin1", "group2viewer1");
        assertOperators(permissions, "pipeline2", Collections.emptySet(), "superadmin1");
        assertAdmins(permissions, "pipeline2", Collections.emptySet(), "superadmin1");

        assertViewers(permissions, "pipeline3", Collections.emptySet(), "superadmin1", "group3viewer1", "group3viewer2");
        assertOperators(permissions, "pipeline3", Collections.emptySet(), "superadmin1", "group3operator1");
        assertAdmins(permissions, "pipeline3", Collections.emptySet(), "superadmin1");

        assertViewers(permissions, "pipeline4", Collections.emptySet(), "superadmin1", "group3viewer1", "group3viewer2");
        assertOperators(permissions, "pipeline4", Collections.emptySet(), "superadmin1", "group3operator1");
        assertAdmins(permissions, "pipeline4", Collections.emptySet(), "superadmin1");
    }

    @Test
    public void shouldAllowAUserToBePartOfDifferentGroups() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1B", "job1B1", "job1B2");
        configMother.addPipelineWithGroup(config, "group3", "pipeline3", "stage1C", "job1C1", "job1C2");

        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addRole(config, configMother.createRole("group1adminrole", "user1", "user2"));
        configMother.addAdminRoleForPipelineGroup(config, "group1adminrole", "group1");

        configMother.addUserAsViewerOfPipelineGroup(config, "user1", "group2");
        configMother.addUserAsOperatorOfPipelineGroup(config, "user1", "group2");

        configMother.addRole(config, configMother.createRole("group3_view_role1", "user2", "user3"));
        configMother.addRole(config, configMother.createRole("group3_operate_role1", "user3", "user4"));
        configMother.addRoleAsViewerOfPipelineGroup(config, "group3_view_role1", "group3");
        configMother.addRoleAsOperatorOfPipelineGroup(config, "group3_operate_role1", "group3");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2", "pipeline3");

        assertViewers(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "user1", "user2");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "user1", "user2");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "user1", "user2");

        assertViewers(permissions, "pipeline2", Collections.emptySet(), "superadmin1", "user1");
        assertOperators(permissions, "pipeline2", Collections.emptySet(), "superadmin1", "user1");
        assertAdmins(permissions, "pipeline2", Collections.emptySet(), "superadmin1");

        assertViewers(permissions, "pipeline3", Collections.emptySet(), "superadmin1", "user2", "user3");
        assertOperators(permissions, "pipeline3", Collections.emptySet(), "superadmin1", "user3", "user4");
        assertAdmins(permissions, "pipeline3", Collections.emptySet(), "superadmin1");
    }

    @Test
    public void withNoSuperAdminsAndNoGroupAuthorization_shouldConsiderAllUsersAsViewersOperatorsAndAdminsOfAGroup() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        PipelineConfigs group = config.findGroup("group1");
        assertThat(group.getAuthorization(), is(new Authorization()));
        assertTrue(config.server().security().adminsConfig().isEmpty());

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");

        Permissions pipelinePermissions = permissions.get(new CaseInsensitiveString("pipeline1"));
        assertEveryoneIsAPartOf(pipelinePermissions.viewers());
        assertEveryoneIsAPartOf(pipelinePermissions.operators());
        assertEveryoneIsAPartOf(pipelinePermissions.pipelineOperators());
        assertEveryoneIsAPartOf(pipelinePermissions.admins());
    }

    @Test
    public void withNoSuperAdminsAndGroupLevelAuthorization_shouldConsiderAllUsersAsViewersOperatorsAndAdminsOfAGroup_EvenIfExplicitGroupAdminIsSetup() {
        /* No superuser */
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2A", "job2A1", "job2A2");
        configMother.addAdminUserForPipelineGroup(config, "groupadmin1", "group1");
        configMother.addUserAsViewerOfPipelineGroup(config, "some-user", "group2");

        PipelineConfigs group = config.findGroup("group1");
        assertThat(group.getAuthorization(), is(not(new Authorization())));

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1", "pipeline2");

        Permissions pipeline1Permissions = permissions.get(new CaseInsensitiveString("pipeline1"));
        assertEveryoneIsAPartOf(pipeline1Permissions.viewers());
        assertEveryoneIsAPartOf(pipeline1Permissions.operators());
        assertEveryoneIsAPartOf(pipeline1Permissions.pipelineOperators());
        assertEveryoneIsAPartOf(pipeline1Permissions.admins());

        Permissions pipeline2Permissions = permissions.get(new CaseInsensitiveString("pipeline2"));
        assertEveryoneIsAPartOf(pipeline2Permissions.viewers());
        assertEveryoneIsAPartOf(pipeline2Permissions.operators());
        assertEveryoneIsAPartOf(pipeline2Permissions.pipelineOperators());
        assertEveryoneIsAPartOf(pipeline2Permissions.admins());
    }

    @Test
    public void withSuperAdminsThroughEmptyRoleAndGroupAuthorization_shouldApplyAuthorizationBasedOnPipelineGroupAuthorization() {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2A", "job2A1", "job2A2");

        configMother.addRoleAsSuperAdmin(config, "empty_role");

        configMother.addUserAsViewerOfPipelineGroup(config, "group1_user", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "group1_user", "group1");
        configMother.addAdminUserForPipelineGroup(config, "group1_user", "group1");

        configMother.addUserAsViewerOfPipelineGroup(config, "group2_user", "group2");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertViewers(permissions, "pipeline1", Collections.emptySet(), "group1_user");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "group1_user");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "group1_user");
    }

    @Test
    public void shouldHandleRoleNamesCaseInsensitively() {
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addRole(config, configMother.createRole("roleWithDifferentCase", "user1", "user2"));

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addAdminRoleForPipelineGroup(config, "ROLEWithDIFFERENTCase", "group1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");
        assertViewers(permissions, "pipeline1", Collections.emptySet(), "user1", "user2", "superadmin1");
        assertOperators(permissions, "pipeline1", Collections.emptySet(), "user1", "user2", "superadmin1");
        assertAdmins(permissions, "pipeline1", Collections.emptySet(), "user1", "user2", "superadmin1");
    }

    @Test
    public void withSuperAdminsAndGroupLevelAuthorization_shouldAllowStageToOverrideOperators() {
        PipelineConfig pipelineConfig = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addUserAsOperatorOfPipelineGroup(config, "user1", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "user2", "group1");

        configMother.addRole(config, configMother.createRole("role1", "user3", "user4"));
        configMother.addRole(config, configMother.createRole("role2", "user5", "user6"));
        configMother.addRoleAsOperatorOfPipelineGroup(config, "role1", "group1");
        configMother.addRoleAsOperatorOfPipelineGroup(config, "role2", "group1");

        StageConfigMother.addApprovalWithUsers(pipelineConfig.first(), "user1");
        StageConfigMother.addApprovalWithRoles(pipelineConfig.first(), "role1");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");

        assertGroupOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "user1", "user2", "user3", "user4", "user5", "user6");
        assertPipelineOperators(permissions, "pipeline1", Collections.emptySet(), "superadmin1", "user1", "user3", "user4");
    }

    @Test
    public void withSuperAdminsAndGroupLevelAuthorization_shouldAllowStageToOverrideOperatorsThroughPluginRole() {
        PipelineConfig pipelineConfig = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        PluginRoleConfig groupOperator = new PluginRoleConfig("group_operator", "ldap");
        pluginRoleUsersStore.assignRole("operator", groupOperator);

        GoConfigMother.addUserAsSuperAdmin(config, "super_admin");

        configMother.addRole(config, groupOperator);
        configMother.addRoleAsOperatorOfPipelineGroup(config, "group_operator", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "user1", "group1");

        StageConfigMother.addApprovalWithRoles(pipelineConfig.first(), "group_operator");

        Map<CaseInsensitiveString, Permissions> permissions = getPipelinesAndTheirPermissions();

        assertPipelinesInMap(permissions, "pipeline1");

        assertGroupOperators(permissions, "pipeline1", Collections.singleton(groupOperator), "user1", "super_admin");
        assertPipelineOperators(permissions, "pipeline1", Collections.singleton(groupOperator), "super_admin");
    }

    @Test
    public void shouldAllowRetrievingPermissionsOfASinglePipelineByName() {
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        PipelineConfig p1Config = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        PipelineConfig p2Config = configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1A", "job1A1", "job1A2");

        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "operator1", "group1");
        configMother.addAdminUserForPipelineGroup(config, "groupadmin1", "group2");

        when(configService.security()).thenReturn(config.server().security());
        when(configService.findGroupByPipeline(p1Config.name())).thenReturn(config.findGroup("group1"));
        when(configService.findGroupByPipeline(p2Config.name())).thenReturn(config.findGroup("group2"));
        when(systemEnvironment.get(ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP)).thenReturn(true);

        Permissions p1Permissions = service.permissionsForPipeline(p1Config.name());
        assertThat(p1Permissions.viewers(), is(new AllowedUsers(s("superadmin1", "viewer1"), emptySet())));
        assertThat(p1Permissions.operators(), is(new AllowedUsers(s("superadmin1", "operator1"), emptySet())));
        assertThat(p1Permissions.admins(), is(new AllowedUsers(s("superadmin1"), emptySet())));
        assertThat(p1Permissions.pipelineOperators(), is(new AllowedUsers(s("superadmin1", "operator1"), emptySet())));

        Permissions p2Permission = service.permissionsForPipeline(p2Config.name());
        assertThat(p2Permission.viewers(), is(new AllowedUsers(s("superadmin1", "groupadmin1"), emptySet())));
        assertThat(p2Permission.operators(), is(new AllowedUsers(s("superadmin1", "groupadmin1"), emptySet())));
        assertThat(p2Permission.admins(), is(new AllowedUsers(s("superadmin1", "groupadmin1"), emptySet())));
        assertThat(p2Permission.pipelineOperators(), is(new AllowedUsers(s("superadmin1", "groupadmin1"), emptySet())));
    }

    @Test
    public void shouldAllowRetrievingPermissionsOfAStage() {
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        PipelineConfig p1Config = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "operator1", "group1");

        when(configService.security()).thenReturn(config.server().security());
        when(configService.findGroupByPipeline(p1Config.name())).thenReturn(config.findGroup("group1"));
        when(systemEnvironment.get(ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP)).thenReturn(true);

        Permissions p1Permissions = service.permissionsForPipeline(p1Config.name());
        assertThat(p1Permissions.viewers(), is(new AllowedUsers(s("superadmin1", "viewer1"), emptySet())));
        assertThat(p1Permissions.operators(), is(new AllowedUsers(s("superadmin1", "operator1"), emptySet())));
        assertThat(p1Permissions.admins(), is(new AllowedUsers(s("superadmin1"), emptySet())));
        assertThat(p1Permissions.pipelineOperators(), is(new AllowedUsers(s("superadmin1", "operator1"), emptySet())));

        assertThat(p1Permissions.stageOperators("stage1A"), is(new AllowedUsers(s("superadmin1", "operator1"), emptySet())));
    }

    @Test
    public void shouldAllowRetrievingPermissionsOfAStage_whenPermissionsAreOverridenAtStageLevel() {
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        PipelineConfig p1Config = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        StageConfig stageWithOverriddenPermissions = StageConfigMother.stageConfig("stage2A");
        stageWithOverriddenPermissions.getApproval().addAdmin(new AdminUser("operator2"));
        p1Config.add(stageWithOverriddenPermissions);

        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "operator1", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "operator2", "group1");

        when(configService.security()).thenReturn(config.server().security());
        when(configService.findGroupByPipeline(p1Config.name())).thenReturn(config.findGroup("group1"));
        when(systemEnvironment.get(ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP)).thenReturn(true);

        Permissions p1Permissions = service.permissionsForPipeline(p1Config.name());
        assertThat(p1Permissions.viewers(), is(new AllowedUsers(s("superadmin1", "viewer1"), emptySet())));
        assertThat(p1Permissions.operators(), is(new AllowedUsers(s("superadmin1", "operator1", "operator2"), emptySet())));
        assertThat(p1Permissions.admins(), is(new AllowedUsers(s("superadmin1"), emptySet())));
        assertThat(p1Permissions.pipelineOperators(), is(new AllowedUsers(s("superadmin1", "operator1", "operator2"), emptySet())));

        assertThat(p1Permissions.stageOperators("stage1A"), is(new AllowedUsers(s("superadmin1", "operator1", "operator2"), emptySet())));
        assertThat(p1Permissions.stageOperators("stage2A"), is(new AllowedUsers(s("superadmin1", "operator2"), emptySet())));
    }

    @Test
    public void shouldReturnPipelinePermissionsWhenSpecifiedStageNameDoesNotExists() {
        GoConfigMother.addUserAsSuperAdmin(config, "superadmin1");

        PipelineConfig p1Config = configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        StageConfig stageWithOverriddenPermissions = StageConfigMother.stageConfig("stage2A");
        stageWithOverriddenPermissions.getApproval().addAdmin(new AdminUser("operator2"));
        p1Config.add(stageWithOverriddenPermissions);

        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "operator1", "group1");
        configMother.addUserAsOperatorOfPipelineGroup(config, "operator2", "group1");

        when(configService.security()).thenReturn(config.server().security());
        when(configService.findGroupByPipeline(p1Config.name())).thenReturn(config.findGroup("group1"));
        when(systemEnvironment.get(ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP)).thenReturn(true);

        Permissions p1Permissions = service.permissionsForPipeline(p1Config.name());
        assertThat(p1Permissions.viewers(), is(new AllowedUsers(s("superadmin1", "viewer1"), emptySet())));
        assertThat(p1Permissions.operators(), is(new AllowedUsers(s("superadmin1", "operator1", "operator2"), emptySet())));
        assertThat(p1Permissions.admins(), is(new AllowedUsers(s("superadmin1"), emptySet())));
        assertThat(p1Permissions.pipelineOperators(), is(new AllowedUsers(s("superadmin1", "operator1", "operator2"), emptySet())));

        assertThat(p1Permissions.stageOperators("stage1A"), is(new AllowedUsers(s("superadmin1", "operator1", "operator2"), emptySet())));
        assertThat(p1Permissions.stageOperators("stage2A"), is(new AllowedUsers(s("superadmin1", "operator2"), emptySet())));
        assertThat(p1Permissions.stageOperators("stage-non-existing-stage-A"), is(new AllowedUsers(s("superadmin1", "operator1", "operator2"), emptySet())));
    }

    private Map<CaseInsensitiveString, Permissions> getPipelinesAndTheirPermissionsWhenDefaultGroupPermissionIsToDeny() {
        return getPipelinesAndTheirPermissions(false);
    }

    private Map<CaseInsensitiveString, Permissions> getPipelinesAndTheirPermissions() {
        return getPipelinesAndTheirPermissions(true);
    }

    private Map<CaseInsensitiveString, Permissions> getPipelinesAndTheirPermissions(boolean defaultPermissionForGroupsWithNoAuth) {
        when(configService.security()).thenReturn(config.server().security());
        when(configService.groups()).thenReturn(config.getGroups());
        when(systemEnvironment.get(ALLOW_EVERYONE_TO_VIEW_OPERATE_GROUPS_WITH_NO_GROUP_AUTHORIZATION_SETUP)).thenReturn(defaultPermissionForGroupsWithNoAuth);

        return service.pipelinesAndTheirPermissions();
    }

    private void assertViewers(Map<CaseInsensitiveString, Permissions> permissionsForAllPipelines, String pipelineToCheckFor, Set<PluginRoleConfig> pluginRoleConfigs, String... expectedViewers) {
        Permissions permissions = permissionsForAllPipelines.get(new CaseInsensitiveString(pipelineToCheckFor));
        assertThat(permissions.viewers(), is(new AllowedUsers(s(expectedViewers), pluginRoleConfigs)));
    }

    private void assertOperators(Map<CaseInsensitiveString, Permissions> permissionsForAllPipelines, String pipelineToCheckFor, Set<PluginRoleConfig> pluginRoleConfigs, String... expectedOperators) {
        assertGroupOperators(permissionsForAllPipelines, pipelineToCheckFor, pluginRoleConfigs, expectedOperators);
        assertPipelineOperators(permissionsForAllPipelines, pipelineToCheckFor, pluginRoleConfigs, expectedOperators);
    }

    private void assertPipelineOperators(Map<CaseInsensitiveString, Permissions> permissionsForAllPipelines, String pipelineToCheckFor, Set<PluginRoleConfig> pluginRoleConfigs, String... expectedOperators) {
        Permissions permissions = permissionsForAllPipelines.get(new CaseInsensitiveString(pipelineToCheckFor));
        assertThat(permissions.pipelineOperators(), is(new AllowedUsers(s(expectedOperators), pluginRoleConfigs)));
    }

    private void assertGroupOperators(Map<CaseInsensitiveString, Permissions> permissionsForAllPipelines, String pipelineToCheckFor, Set<PluginRoleConfig> pluginRoleConfigs, String... expectedOperators) {
        Permissions permissions = permissionsForAllPipelines.get(new CaseInsensitiveString(pipelineToCheckFor));
        assertThat(permissions.operators(), is(new AllowedUsers(s(expectedOperators), pluginRoleConfigs)));
    }

    private void assertAdmins(Map<CaseInsensitiveString, Permissions> permissionsForAllPipelines, String pipelineToCheckFor, Set<PluginRoleConfig> pluginRoleConfigs, String... expectedAdmins) {
        Permissions permissions = permissionsForAllPipelines.get(new CaseInsensitiveString(pipelineToCheckFor));
        assertThat(permissions.admins(), is(new AllowedUsers(s(expectedAdmins), pluginRoleConfigs)));
    }

    private void assertPipelinesInMap(Map<CaseInsensitiveString, Permissions> pipelinesToPermissions, String... expectedPipelines) {
        Set<CaseInsensitiveString> expectedCaseInsensitivePipelineNames = new HashSet<>();
        for (String expectedPipeline : expectedPipelines) {
            expectedCaseInsensitivePipelineNames.add(new CaseInsensitiveString(expectedPipeline));
        }
        assertThat(pipelinesToPermissions.keySet(), is(expectedCaseInsensitivePipelineNames));
    }

    private void assertEveryoneIsAPartOf(Users users) {
        assertThat(users.contains("some-user"), is(true));
        assertThat(users.contains("some-other-user"), is(true));
        assertThat(users.contains("any-random-user"), is(true));
    }
}
