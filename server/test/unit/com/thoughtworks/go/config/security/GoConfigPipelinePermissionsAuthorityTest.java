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

package com.thoughtworks.go.config.security;

import com.thoughtworks.go.config.Authorization;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.security.users.AllowedViewers;
import com.thoughtworks.go.config.security.users.Viewers;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoConfigPipelinePermissionsAuthorityTest {
    private GoConfigMother configMother;
    private GoConfigService configService;
    private GoConfigPipelinePermissionsAuthority service;
    private CruiseConfig config;
    private PluginRoleUsersStore pluginRoleUsersStore;

    @Before
    public void setUp() throws Exception {
        configService = mock(GoConfigService.class);
        service = new GoConfigPipelinePermissionsAuthority(configService);

        configMother = new GoConfigMother();
        config = GoConfigMother.defaultCruiseConfig();
        pluginRoleUsersStore = PluginRoleUsersStore.instance();
    }

    @After
    public void tearDown() throws Exception {
        pluginRoleUsersStore.clearAll();
    }

    @Test
    public void shouldConsiderAllSuperAdminUsersAsViewersOfPipelineGroups() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");

        configMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addUserAsSuperAdmin(config, "superadmin2");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(1));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(viewers(Collections.emptySet(), "superadmin1", "superadmin2", "viewer1")));
    }

    @Test
    public void shouldConsiderUsersOfAllSuperAdminRolesAsViewersOfPipelineGroups() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");

        configMother.addRole(config, configMother.createRole("superadminrole1", "superadmin1", "superadmin2"));
        configMother.addRole(config, configMother.createRole("superadminrole2", "superadmin2", "superadmin3"));
        configMother.addRoleAsSuperAdmin(config, "superadminrole1");
        configMother.addRoleAsSuperAdmin(config, "superadminrole2");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(1));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(viewers(Collections.emptySet(), "superadmin1", "superadmin2", "superadmin3", "viewer1")));
    }

    @Test
    public void shouldConsiderAllUsersHavingSuperAdminPluginRoleAsViewersOfPipelineGroups() throws Exception {
        PluginRoleConfig admin = new PluginRoleConfig("go_admins", "ldap");
        pluginRoleUsersStore.assignRole("admin_user", admin);

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, admin);
        configMother.addRoleAsSuperAdmin(config, "go_admins");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(1));
        assertTrue(pipelinesAndTheirViewers.get("group1").contains("admin_user"));
    }

    @Test
    public void usersBelongingToAdminPluginRole_shouldNotBeViewers_uponRevocationOfTheRole() throws Exception {
        PluginRoleConfig admin = new PluginRoleConfig("go_admins", "ldap");
        pluginRoleUsersStore.assignRole("admin_user", admin);

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, admin);
        configMother.addRoleAsSuperAdmin(config, "go_admins");
        configMother.addUserAsViewerOfPipelineGroup(config, "view_user", "group1");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(1));
        assertTrue(pipelinesAndTheirViewers.get("group1").contains("admin_user"));

        pluginRoleUsersStore.revokeAllRolesFor("admin_user");

        assertFalse(pipelinesAndTheirViewers.get("group1").contains("admin_user"));
    }

    @Test
    public void pipelineGroupViewersThroughPluginRole_shouldNotBeViewers_uponRevocationOfTheRole() throws Exception {
        PluginRoleConfig pipelineViewers = new PluginRoleConfig("pipeline_viewers", "ldap");
        pluginRoleUsersStore.assignRole("viewer", pipelineViewers);

        configMother.addRoleAsSuperAdmin(config, "super_admin");
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, pipelineViewers);
        configMother.addRoleAsViewerOfPipelineGroup(config, "pipeline_viewers", "group1");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(1));
        assertTrue(pipelinesAndTheirViewers.get("group1").contains("viewer"));

        pluginRoleUsersStore.revokeAllRolesFor("viewer");

        assertFalse(pipelinesAndTheirViewers.get("group1").contains("viewer"));
    }

    @Test
    public void shouldCreateAUniqueSetOfNamesWhenSameUserIsPartOfBothSuperAdminUsersAndRolesConfigurations() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");

        configMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addRole(config, configMother.createRole("superadminrole1", "superadmin1", "superadmin2"));
        configMother.addRoleAsSuperAdmin(config, "superadminrole1");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(1));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(viewers(Collections.emptySet(), "superadmin1", "superadmin2", "viewer1")));
    }

    @Test
    public void shouldConsiderPipelineGroupAdminsAsViewersOfTheirPipelineGroup() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group2");

        configMother.addAdminUserForPipelineGroup(config, "groupadmin1", "group1");
        configMother.addAdminUserForPipelineGroup(config, "groupadmin2", "group1");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(2));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(viewers(Collections.emptySet(), "superadmin1", "groupadmin1", "groupadmin2")));
        assertThat(pipelinesAndTheirViewers.get("group2"), is(viewers(Collections.emptySet(), "superadmin1", "viewer1")));
    }

    @Test
    public void shouldConsiderAllUsersInPipelineGroupAdminRolesAsViewersOfTheirPipelineGroup() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group2");

        configMother.addRole(config, configMother.createRole("group1_admin_role1", "groupadmin1", "groupadmin2"));
        configMother.addRole(config, configMother.createRole("group1_admin_role2", "groupadmin2", "groupadmin3"));
        configMother.addAdminRoleForPipelineGroup(config, "group1_admin_role1", "group1");
        configMother.addAdminRoleForPipelineGroup(config, "group1_admin_role2", "group1");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(2));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(viewers(Collections.emptySet(), "superadmin1", "groupadmin1", "groupadmin2", "groupadmin3")));
        assertThat(pipelinesAndTheirViewers.get("group2"), is(viewers(Collections.emptySet(), "superadmin1", "viewer1")));
    }

    @Test
    public void shouldCreateAUniqueSetOfNamesWhenSameUserIsPartOfBothGroupAdminUsersAndRolesConfigurations() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, configMother.createRole("group1_admin_role1", "groupadmin1", "groupadmin2"));
        configMother.addAdminUserForPipelineGroup(config, "groupadmin1", "group1");
        configMother.addAdminRoleForPipelineGroup(config, "group1_admin_role1", "group1");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(1));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(viewers(Collections.emptySet(), "superadmin1", "groupadmin1", "groupadmin2")));
    }

    @Test
    public void shouldConsiderUsersWithViewPermissionsAsViewersOfTheirPipelineGroup() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer2", "group1");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer3", "group2");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(2));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(viewers(Collections.emptySet(), "superadmin1", "viewer1", "viewer2")));
        assertThat(pipelinesAndTheirViewers.get("group2"), is(viewers(Collections.emptySet(), "superadmin1", "viewer3")));
    }

    @Test
    public void shouldConsiderUsersOfRolesWithViewPermissionsAsViewersOfTheirPipelineGroup() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group2");

        configMother.addRole(config, configMother.createRole("group1_view_role1", "groupviewer1", "groupviewer2"));
        configMother.addRole(config, configMother.createRole("group1_view_role2", "groupviewer2", "groupviewer3"));
        configMother.addRoleAsViewerOfPipelineGroup(config, "group1_view_role1", "group1");
        configMother.addRoleAsViewerOfPipelineGroup(config, "group1_view_role2", "group1");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(2));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(viewers(Collections.emptySet(), "superadmin1", "groupviewer1", "groupviewer2", "groupviewer3")));
        assertThat(pipelinesAndTheirViewers.get("group2"), is(viewers(Collections.emptySet(), "superadmin1", "viewer1")));
    }

    @Test
    public void shouldCreateAUniqueSetOfNamesWhenSameUserIsPartOfBothViewUsersAndViewRolesConfigurations() throws Exception {
        configMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, configMother.createRole("group1_view_role1", "viewer1", "groupviewer2"));
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");
        configMother.addRoleAsViewerOfPipelineGroup(config, "group1_view_role1", "group1");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(1));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(viewers(Collections.emptySet(), "superadmin1", "viewer1", "groupviewer2")));
    }

    @Test
    public void shouldCreateAnEntryForEveryPipelineGroupInTheConfig() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1B", "job1B1", "job1B2");
        configMother.addPipelineWithGroup(config, "group3", "pipeline3", "stage1C", "job1C1", "job1B2");

        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addRole(config, configMother.createRole("group1adminrole", "group1admin1", "group1admin2"));
        configMother.addAdminRoleForPipelineGroup(config, "group1adminrole", "group1");

        configMother.addUserAsViewerOfPipelineGroup(config, "group2viewer1", "group2");

        configMother.addRole(config, configMother.createRole("group3_view_role1", "group3viewer1", "group3viewer2"));
        configMother.addRoleAsViewerOfPipelineGroup(config, "group3_view_role1", "group3");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(3));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(viewers(Collections.emptySet(), "superadmin1", "group1admin1", "group1admin2")));
        assertThat(pipelinesAndTheirViewers.get("group2"), is(viewers(Collections.emptySet(), "superadmin1", "group2viewer1")));
        assertThat(pipelinesAndTheirViewers.get("group3"), is(viewers(Collections.emptySet(), "superadmin1", "group3viewer1", "group3viewer2")));
    }

    @Test
    public void shouldAllowAUserToBePartOfDifferentGroups() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage1B", "job1B1", "job1B2");
        configMother.addPipelineWithGroup(config, "group3", "pipeline2", "stage1B", "job1B1", "job1B2");

        configMother.addUserAsSuperAdmin(config, "superadmin1");

        configMother.addRole(config, configMother.createRole("group1adminrole", "user1", "user2"));
        configMother.addAdminRoleForPipelineGroup(config, "group1adminrole", "group1");

        configMother.addUserAsViewerOfPipelineGroup(config, "user1", "group2");

        configMother.addRole(config, configMother.createRole("group3_view_role1", "user2", "user3"));
        configMother.addRoleAsViewerOfPipelineGroup(config, "group3_view_role1", "group3");

        Map<String, Viewers> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(3));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(viewers(Collections.emptySet(), "superadmin1", "user1", "user2")));
        assertThat(pipelinesAndTheirViewers.get("group2"), is(viewers(Collections.emptySet(), "superadmin1", "user1")));
        assertThat(pipelinesAndTheirViewers.get("group3"), is(viewers(Collections.emptySet(), "superadmin1", "user2", "user3")));
    }

    @Test
    public void shouldConsiderAllUsersAsViewersOfAGroupWithNoAuthorizationConfigurationSetup() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        PipelineConfigs group = config.findGroup("group1");
        assertThat(group.getAuthorization(), is(new Authorization()));

        Viewers viewersOfGroup1 = getGroupsAndTheirViewers().get("group1");

        assertThat(viewersOfGroup1.contains("some-user"), is(true));
        assertThat(viewersOfGroup1.contains("some-other-user"), is(true));
        assertThat(viewersOfGroup1.contains("any-random-user"), is(true));
    }

    @Test
    public void shouldConsiderAllUsersAsViewersOfAGroupWithNoAuthorizationConfigurationSetup_EvenWhenExplicitSuperAdminsAreSetup() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addUserAsSuperAdmin(config, "superadmin1");

        PipelineConfigs group = config.findGroup("group1");
        assertThat(group.getAuthorization(), is(new Authorization()));

        Viewers viewersOfGroup1 = getGroupsAndTheirViewers().get("group1");

        assertThat(viewersOfGroup1.contains("superadmin1"), is(true));
        assertThat(viewersOfGroup1.contains("some-user"), is(true));
        assertThat(viewersOfGroup1.contains("some-other-user"), is(true));
        assertThat(viewersOfGroup1.contains("any-random-user"), is(true));
    }

    @Test
    public void shouldConsiderAllUsersAsViewersOfAGroup_EvenIfExplicitGroupAdminIsSetup_AND_NoGlobalSuperAdminsExist() throws Exception {
        /* No superuser */
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addAdminUserForPipelineGroup(config, "groupadmin1", "group1");

        PipelineConfigs group = config.findGroup("group1");
        assertThat(group.getAuthorization(), is(not(new Authorization())));

        Viewers viewersOfGroup1 = getGroupsAndTheirViewers().get("group1");

        assertThat(viewersOfGroup1.contains("groupadmin1"), is(true));
        assertThat(viewersOfGroup1.contains("some-user"), is(true));
        assertThat(viewersOfGroup1.contains("some-other-user"), is(true));
    }

    @Test
    public void shouldApplyAuthorizationBasedOnPipelineGroupAuthorization_IfSuperAdminsAreConfiguredUsingEmptyRoles() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline2", "stage2A", "job2A1", "job2A2");

        configMother.addRoleAsSuperAdmin(config, "empty_role");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer2", "group2");

        Viewers viewersOfGroup1 = getGroupsAndTheirViewers().get("group1");

        assertTrue(viewersOfGroup1.contains("viewer1"));
        assertFalse(viewersOfGroup1.contains("viewer2"));
    }

    @Test
    public void shouldHandleRoleNamesCaseInsensitively() throws Exception {
        configMother.addRole(config, configMother.createRole("roleWithDifferentCase", "user1", "user2"));

        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addAdminRoleForPipelineGroup(config, "ROLEWithDIFFERENTCase", "group1");

        Viewers viewersOfGroup = getGroupsAndTheirViewers().get("group1");

        assertThat(viewersOfGroup.contains("user1"), is(true));
        assertThat(viewersOfGroup.contains("user2"), is(true));
    }

    @Test
    public void shouldConsiderAUserAsViewerOfGroup_IfUserBelongsToTheConfiguredPluginRole_AndInPresenceOfSuperAdmin() throws Exception {
        PluginRoleConfig admin = configMother.createPluginRole("go_admin", "ldap");

        configMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addRole(config, admin);
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addRoleAsViewerOfPipelineGroup(config, "go_admin", "group1");
        pluginRoleUsersStore.assignRole("user", admin);

        Viewers viewersOfGroup = getGroupsAndTheirViewers().get("group1");

        assertTrue(viewersOfGroup.contains("user"));
    }

    private Map<String, Viewers> getGroupsAndTheirViewers() {
        when(configService.security()).thenReturn(config.server().security());
        when(configService.groups()).thenReturn(config.getGroups());

        return service.groupsAndTheirViewers();
    }

    private Viewers viewers(Set<PluginRoleConfig> allowedRoles, String... users) {
        return new AllowedViewers(s(users), allowedRoles);
    }
}
