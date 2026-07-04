/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class RolesConfigTest {

    @Test
    public void shouldReturnTrueIfUserIsMemberOfRole() {
        RolesConfig rolesConfig = new RolesConfig(new RoleConfig(cis("role1"), new RoleUser(cis("user1"))));
        assertThat(rolesConfig.isUserMemberOfRole(cis("user1"), cis("role1"))).isTrue();
    }

    @Test
    public void shouldReturnFalseIfUserIsNotMemberOfRole() {
        RolesConfig rolesConfig = new RolesConfig(new RoleConfig(cis("role1"), new RoleUser(cis("user1"))));
        assertThat(rolesConfig.isUserMemberOfRole(cis("user2"), cis("role1"))).isFalse();
    }

    @Test
    public void shouldThrowExceptionIfRoleDoesNotExist() {
        RolesConfig rolesConfig = new RolesConfig(new RoleConfig(cis("role1"), new RoleUser(cis("user1"))));

        assertThatThrownBy(() -> rolesConfig.isUserMemberOfRole(cis("user1"), cis("invalid-role-name")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Role \"invalid-role-name\" does not exist!");
    }

    @Test
    public void shouldGiveAListOfAllRolesAUserBelongsTo() {
        Role firstRole = new RoleConfig(cis("role1"), new RoleUser(cis("USER1")), new RoleUser(cis("user2")));
        Role secondRole = new RoleConfig(cis("role2"), new RoleUser(cis("user1")), new RoleUser(cis("user3")));
        Role thirdRole = new RoleConfig(cis("role3"), new RoleUser(cis("user2")), new RoleUser(cis("user3")));
        RolesConfig rolesConfig = new RolesConfig(firstRole, secondRole, thirdRole);
        assertThat(rolesConfig.memberRoles(new AdminUser(cis("user1")))).containsExactly(firstRole, secondRole);
    }

    @Test
    public void shouldListItselfWhenARoleExists() {
        Role firstRole = new RoleConfig(cis("role1"), new RoleUser(cis("USER1")), new RoleUser(cis("user2")));
        Role secondRole = new RoleConfig(cis("ROLE2"), new RoleUser(cis("user1")), new RoleUser(cis("user3")));
        RolesConfig rolesConfig = new RolesConfig(firstRole, secondRole);
        assertThat(rolesConfig.memberRoles(new AdminRole(cis("role1")))).containsExactly(firstRole);
        assertThat(rolesConfig.memberRoles(new AdminRole(cis("role2")))).containsExactly(secondRole);
    }

    @Test
    public void shouldBeInvalidToHaveTwoRolesWithTheSameName() {
        Role role1 = new RoleConfig(cis("role1"));
        Role role2 = new RoleConfig(cis("role1"));
        RolesConfig rolesConfig = new RolesConfig(role1, role2);
        rolesConfig.validate(null);
        assertEquals(1, rolesConfig.errors().getAll().size());
    }


    @Test
    public void getPluginRoleConfigsShouldReturnOnlyPluginRoles() {
        Role admin = new RoleConfig(cis("admin"));
        Role view = new RoleConfig(cis("view"));
        PluginRoleConfig blackbird = new PluginRoleConfig("blackbird", "foo");
        PluginRoleConfig spacetiger = new PluginRoleConfig("spacetiger", "foo");

        RolesConfig rolesConfig = new RolesConfig(admin, blackbird, view, spacetiger);

        List<PluginRoleConfig> roles = rolesConfig.getPluginRoleConfigs();

        assertThat(roles).containsExactly(blackbird, spacetiger);
    }

    @Test
    public void shouldBeAbleToFetchPluginRolesForAAuthConfig() {
        PluginRoleConfig admin = new PluginRoleConfig("admin", "corporate_ldap");
        PluginRoleConfig view = new PluginRoleConfig("view", "corporate_ldap");
        PluginRoleConfig operator = new PluginRoleConfig("operator", "internal_ldap");

        RolesConfig rolesConfig = new RolesConfig(admin, view, operator, new RoleConfig(cis("committer")));

        assertThat(rolesConfig.pluginRoleConfigsFor("corporate_ldap")).containsExactlyInAnyOrder(admin, view);
        assertThat(rolesConfig.pluginRoleConfigsFor("internal_ldap")).containsExactlyInAnyOrder(operator);
    }

    @Test
    public void getRoleConfigsShouldReturnOnlyNonPluginRoles() {
        RoleConfig admin = new RoleConfig(cis("admin"));
        RoleConfig view = new RoleConfig(cis("view"));
        Role blackbird = new PluginRoleConfig("blackbird", "foo");
        Role spacetiger = new PluginRoleConfig("spacetiger", "foo");

        RolesConfig rolesConfig = new RolesConfig(admin, blackbird, view, spacetiger);

        List<RoleConfig> roles = rolesConfig.getRoleConfigs();

        assertThat(roles).containsExactly(admin, view);
    }

    @Test
    public void allRolesShouldReturnAllRoles() {
        Role admin = new RoleConfig(cis("admin"));
        Role view = new RoleConfig(cis("view"));
        Role blackbird = new PluginRoleConfig("blackbird", "foo");
        Role spacetiger = new PluginRoleConfig("spacetiger", "foo");

        RolesConfig rolesConfig = new RolesConfig(admin, blackbird, view, spacetiger);

        List<Role> roles = rolesConfig.allRoles();

        assertThat(roles).containsExactly(admin, blackbird, view, spacetiger);
    }

    @Test
    public void isUniqueRoleName_shouldBeTrueIfRolesAreUnique() {
        RolesConfig rolesConfig = new RolesConfig(new RoleConfig(cis("admin")),
                new RoleConfig(cis("view")));

        assertTrue(rolesConfig.isUniqueRoleName(cis("admin")));
        assertTrue(rolesConfig.isUniqueRoleName(cis("operate")));
    }

    @Test
    public void isUniqueRoleName_shouldBeFalseWithMultipleRolesWithSameName() {
        RolesConfig rolesConfig = new RolesConfig(new RoleConfig(cis("admin")),
                new RoleConfig(cis("view")),
                new RoleConfig(cis("view")));

        assertFalse(rolesConfig.isUniqueRoleName(cis("view")));
    }
}
