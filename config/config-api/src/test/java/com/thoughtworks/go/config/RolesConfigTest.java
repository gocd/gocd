/*
 * Copyright 2024 Thoughtworks, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class RolesConfigTest {

    @Test
    public void shouldReturnTrueIfUserIsMemberOfRole() {
        RolesConfig rolesConfig = new RolesConfig(new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("user1"))));
        assertThat(rolesConfig.isUserMemberOfRole(new CaseInsensitiveString("user1"), new CaseInsensitiveString("role1"))).isTrue();
    }

    @Test
    public void shouldReturnFalseIfUserIsNotMemberOfRole() {
        RolesConfig rolesConfig = new RolesConfig(new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("user1"))));
        assertThat(rolesConfig.isUserMemberOfRole(new CaseInsensitiveString("user2"), new CaseInsensitiveString("role1"))).isFalse();
    }

    @Test
    public void shouldThrowExceptionIfRoleDoesNotExist() {
        RolesConfig rolesConfig = new RolesConfig(new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("user1"))));

        assertThatThrownBy(() -> rolesConfig.isUserMemberOfRole(new CaseInsensitiveString("user1"), new CaseInsensitiveString("invalid-role-name")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Role \"invalid-role-name\" does not exist!");
    }

    @Test
    public void shouldGiveAListOfAllRolesAUserBelongsTo() {
        Role firstRole = new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("USER1")), new RoleUser(new CaseInsensitiveString("user2")));
        Role secondRole = new RoleConfig(new CaseInsensitiveString("role2"), new RoleUser(new CaseInsensitiveString("user1")), new RoleUser(new CaseInsensitiveString("user3")));
        Role thirdRole = new RoleConfig(new CaseInsensitiveString("role3"), new RoleUser(new CaseInsensitiveString("user2")), new RoleUser(new CaseInsensitiveString("user3")));
        RolesConfig rolesConfig = new RolesConfig(firstRole, secondRole, thirdRole);
        assertThat(rolesConfig.memberRoles(new AdminUser(new CaseInsensitiveString("user1")))).containsExactly(firstRole, secondRole);
    }

    @Test
    public void shouldListItselfWhenARoleExists() {
        Role firstRole = new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("USER1")), new RoleUser(new CaseInsensitiveString("user2")));
        Role secondRole = new RoleConfig(new CaseInsensitiveString("ROLE2"), new RoleUser(new CaseInsensitiveString("user1")), new RoleUser(new CaseInsensitiveString("user3")));
        RolesConfig rolesConfig = new RolesConfig(firstRole, secondRole);
        assertThat(rolesConfig.memberRoles(new AdminRole(new CaseInsensitiveString("role1")))).containsExactly(firstRole);
        assertThat(rolesConfig.memberRoles(new AdminRole(new CaseInsensitiveString("role2")))).containsExactly(secondRole);
    }

    @Test
    public void shouldBeInvalidToHaveTwoRolesWithTheSameName() {
        Role role1 = new RoleConfig(new CaseInsensitiveString("role1"));
        Role role2 = new RoleConfig(new CaseInsensitiveString("role1"));
        RolesConfig rolesConfig = new RolesConfig(role1, role2);
        rolesConfig.validate(null);
        assertEquals(1, rolesConfig.errors().getAll().size());
    }


    @Test
    public void getPluginRoleConfigsShouldReturnOnlyPluginRoles() {
        Role admin = new RoleConfig(new CaseInsensitiveString("admin"));
        Role view = new RoleConfig(new CaseInsensitiveString("view"));
        PluginRoleConfig blackbird = new PluginRoleConfig("blackbird", "foo");
        PluginRoleConfig spacetiger = new PluginRoleConfig("spacetiger", "foo");

        RolesConfig rolesConfig = new RolesConfig(admin, blackbird, view, spacetiger);

        List<PluginRoleConfig> roles = rolesConfig.getPluginRoleConfigs();

        assertThat(roles).containsExactly(blackbird, spacetiger);
    }

    @Test
    public void shouldBeAbleToFetchPluginRolesForAAuthConfig() throws Exception {
        PluginRoleConfig admin = new PluginRoleConfig("admin", "corporate_ldap");
        PluginRoleConfig view = new PluginRoleConfig("view", "corporate_ldap");
        PluginRoleConfig operator = new PluginRoleConfig("operator", "internal_ldap");

        RolesConfig rolesConfig = new RolesConfig(admin, view, operator, new RoleConfig(new CaseInsensitiveString("committer")));

        assertThat(rolesConfig.pluginRoleConfigsFor("corporate_ldap")).containsExactlyInAnyOrder(admin, view);
        assertThat(rolesConfig.pluginRoleConfigsFor("internal_ldap")).containsExactlyInAnyOrder(operator);
    }

    @Test
    public void getRoleConfigsShouldReturnOnlyNonPluginRoles() {
        RoleConfig admin = new RoleConfig(new CaseInsensitiveString("admin"));
        RoleConfig view = new RoleConfig(new CaseInsensitiveString("view"));
        Role blackbird = new PluginRoleConfig("blackbird", "foo");
        Role spacetiger = new PluginRoleConfig("spacetiger", "foo");

        RolesConfig rolesConfig = new RolesConfig(admin, blackbird, view, spacetiger);

        List<RoleConfig> roles = rolesConfig.getRoleConfigs();

        assertThat(roles).containsExactly(admin, view);
    }

    @Test
    public void allRolesShouldReturnAllRoles() {
        Role admin = new RoleConfig(new CaseInsensitiveString("admin"));
        Role view = new RoleConfig(new CaseInsensitiveString("view"));
        Role blackbird = new PluginRoleConfig("blackbird", "foo");
        Role spacetiger = new PluginRoleConfig("spacetiger", "foo");

        RolesConfig rolesConfig = new RolesConfig(admin, blackbird, view, spacetiger);

        List<Role> roles = rolesConfig.allRoles();

        assertThat(roles).containsExactly(admin, blackbird, view, spacetiger);
    }

    @Test
    public void isUniqueRoleName_shouldBeTrueIfRolesAreUnique() throws Exception {
        RolesConfig rolesConfig = new RolesConfig(new RoleConfig(new CaseInsensitiveString("admin")),
                new RoleConfig(new CaseInsensitiveString("view")));

        assertTrue(rolesConfig.isUniqueRoleName(new CaseInsensitiveString("admin")));
        assertTrue(rolesConfig.isUniqueRoleName(new CaseInsensitiveString("operate")));
    }

    @Test
    public void isUniqueRoleName_shouldBeFalseWithMultipleRolesWithSameName() throws Exception {
        RolesConfig rolesConfig = new RolesConfig(new RoleConfig(new CaseInsensitiveString("admin")),
                new RoleConfig(new CaseInsensitiveString("view")),
                new RoleConfig(new CaseInsensitiveString("view")));

        assertFalse(rolesConfig.isUniqueRoleName(new CaseInsensitiveString("view")));
    }
}
