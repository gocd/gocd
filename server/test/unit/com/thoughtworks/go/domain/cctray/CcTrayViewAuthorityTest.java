package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CcTrayViewAuthorityTest {
    private GoConfigMother configMother;
    private GoConfigService configService;
    private CcTrayViewAuthority service;
    private CruiseConfig config;

    @Before
    public void setUp() throws Exception {
        configService = mock(GoConfigService.class);
        service = new CcTrayViewAuthority(configService);

        configMother = new GoConfigMother();
        config = GoConfigMother.defaultCruiseConfig();
    }

    @Test
    public void shouldConsiderAllSuperAdminUsersAsViewersOfPipelineGroups() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addUserAsSuperAdmin(config, "superadmin2");

        Map<String, Set<String>> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(1));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(s("superadmin1", "superadmin2")));
    }

    @Test
    public void shouldConsiderUsersOfAllSuperAdminRolesAsViewersOfPipelineGroups() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, configMother.createRole("superadminrole1", "superadmin1", "superadmin2"));
        configMother.addRole(config, configMother.createRole("superadminrole2", "superadmin2", "superadmin3"));
        configMother.addRoleAsSuperAdmin(config, "superadminrole1");
        configMother.addRoleAsSuperAdmin(config, "superadminrole2");

        Map<String, Set<String>> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(1));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(s("superadmin1", "superadmin2", "superadmin3")));
    }

    @Test
    public void shouldCreateAUniqueSetOfNamesWhenSameUserIsPartOfBothSuperAdminUsersAndRolesConfigurations() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addUserAsSuperAdmin(config, "superadmin1");
        configMother.addRole(config, configMother.createRole("superadminrole1", "superadmin1", "superadmin2"));
        configMother.addRoleAsSuperAdmin(config, "superadminrole1");

        Map<String, Set<String>> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(1));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(s("superadmin1", "superadmin2")));
    }

    @Test
    public void shouldConsiderPipelineGroupAdminsAsViewersOfTheirPipelineGroup() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addAdminUserForPipelineGroup(config, "groupadmin1", "group1");
        configMother.addAdminUserForPipelineGroup(config, "groupadmin2", "group1");

        Map<String, Set<String>> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(2));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(s("groupadmin1", "groupadmin2")));
        assertThat(pipelinesAndTheirViewers.get("group2"), is(Collections.<String>emptySet()));
    }

    @Test
    public void shouldConsiderAllUsersInPipelineGroupAdminRolesAsViewersOfTheirPipelineGroup() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, configMother.createRole("group1_admin_role1", "groupadmin1", "groupadmin2"));
        configMother.addRole(config, configMother.createRole("group1_admin_role2", "groupadmin2", "groupadmin3"));
        configMother.addAdminRoleForPipelineGroup(config, "group1_admin_role1", "group1");
        configMother.addAdminRoleForPipelineGroup(config, "group1_admin_role2", "group1");

        Map<String, Set<String>> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(2));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(s("groupadmin1", "groupadmin2", "groupadmin3")));
        assertThat(pipelinesAndTheirViewers.get("group2"), is(Collections.<String>emptySet()));
    }

    @Test
    public void shouldCreateAUniqueSetOfNamesWhenSameUserIsPartOfBothGroupAdminUsersAndRolesConfigurations() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, configMother.createRole("group1_admin_role1", "groupadmin1", "groupadmin2"));
        configMother.addAdminUserForPipelineGroup(config, "groupadmin1", "group1");
        configMother.addAdminRoleForPipelineGroup(config, "group1_admin_role1", "group1");

        Map<String, Set<String>> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(1));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(s("groupadmin1", "groupadmin2")));
    }

    @Test
    public void shouldConsiderUsersWithViewPermissionsAsViewersOfTheirPipelineGroup() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer2", "group1");

        Map<String, Set<String>> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(2));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(s("viewer1", "viewer2")));
        assertThat(pipelinesAndTheirViewers.get("group2"), is(Collections.<String>emptySet()));
    }

    @Test
    public void shouldConsiderUsersOfRolesWithViewPermissionsAsViewersOfTheirPipelineGroup() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");
        configMother.addPipelineWithGroup(config, "group2", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, configMother.createRole("group1_view_role1", "groupviewer1", "groupviewer2"));
        configMother.addRole(config, configMother.createRole("group1_view_role2", "groupviewer2", "groupviewer3"));
        configMother.addRoleAsViewerOfPipelineGroup(config, "group1_view_role1", "group1");
        configMother.addRoleAsViewerOfPipelineGroup(config, "group1_view_role2", "group1");

        Map<String, Set<String>> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(2));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(s("groupviewer1", "groupviewer2", "groupviewer3")));
        assertThat(pipelinesAndTheirViewers.get("group2"), is(Collections.<String>emptySet()));
    }

    @Test
    public void shouldCreateAUniqueSetOfNamesWhenSameUserIsPartOfBothViewUsersAndViewRolesConfigurations() throws Exception {
        configMother.addPipelineWithGroup(config, "group1", "pipeline1", "stage1A", "job1A1", "job1A2");

        configMother.addRole(config, configMother.createRole("group1_view_role1", "viewer1", "groupviewer2"));
        configMother.addUserAsViewerOfPipelineGroup(config, "viewer1", "group1");
        configMother.addRoleAsViewerOfPipelineGroup(config, "group1_view_role1", "group1");

        Map<String, Set<String>> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(1));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(s("viewer1", "groupviewer2")));
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

        Map<String, Set<String>> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(3));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(s("superadmin1", "group1admin1", "group1admin2")));
        assertThat(pipelinesAndTheirViewers.get("group2"), is(s("superadmin1", "group2viewer1")));
        assertThat(pipelinesAndTheirViewers.get("group3"), is(s("superadmin1", "group3viewer1", "group3viewer2")));
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

        Map<String, Set<String>> pipelinesAndTheirViewers = getGroupsAndTheirViewers();

        assertThat(pipelinesAndTheirViewers.size(), is(3));
        assertThat(pipelinesAndTheirViewers.get("group1"), is(s("superadmin1", "user1", "user2")));
        assertThat(pipelinesAndTheirViewers.get("group2"), is(s("superadmin1", "user1")));
        assertThat(pipelinesAndTheirViewers.get("group3"), is(s("superadmin1", "user2", "user3")));
    }

    private Map<String, Set<String>> getGroupsAndTheirViewers() {
        when(configService.security()).thenReturn(config.server().security());
        when(configService.groups()).thenReturn(config.getGroups());

        return service.groupsAndTheirViewers();
    }
}
