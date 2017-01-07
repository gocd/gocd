/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class RolesConfigTest {

    @Test
    public void shouldReturnTrueIfUserIsMemberOfRole() {
        RolesConfig rolesConfig = new RolesConfig(new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("user1"))));
        assertThat("shouldReturnTrueIfUserIsMemberOfRole", rolesConfig.isUserMemberOfRole(new CaseInsensitiveString("user1"), new CaseInsensitiveString("role1")), is(true));
    }

    @Test
    public void shouldReturnFalseIfUserIsNotMemberOfRole() {
        RolesConfig rolesConfig = new RolesConfig(new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("user1"))));
        assertThat("shouldReturnFalseIfUserIsNotMemberOfRole", rolesConfig.isUserMemberOfRole(new CaseInsensitiveString("user2"), new CaseInsensitiveString("role1")),
                is(false));
    }

    @Test
    public void shouldThrowExceptionIfRoleDoesNotExist() {
        RolesConfig rolesConfig = new RolesConfig(new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("user1"))));
        try {
            rolesConfig.isUserMemberOfRole(new CaseInsensitiveString("anyone"), new CaseInsensitiveString("invalid-role-name"));
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Role \"invalid-role-name\" does not exist!"));
        }
    }

    @Test
    public void shouldGiveAListOfAllRolesAUserBelongsTo() {
        Role firstRole = new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("USER1")), new RoleUser(new CaseInsensitiveString("user2")));
        Role secondRole = new RoleConfig(new CaseInsensitiveString("role2"), new RoleUser(new CaseInsensitiveString("user1")), new RoleUser(new CaseInsensitiveString("user3")));
        Role thirdRole = new RoleConfig(new CaseInsensitiveString("role3"), new RoleUser(new CaseInsensitiveString("user2")), new RoleUser(new CaseInsensitiveString("user3")));
        RolesConfig rolesConfig = new RolesConfig(firstRole, secondRole, thirdRole);
        assertThat(rolesConfig.memberRoles(new AdminUser(new CaseInsensitiveString("user1"))), is(asList(firstRole, secondRole)));
    }

    @Test
    public void shouldListItselfWhenARoleExists() {
        Role firstRole = new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("USER1")), new RoleUser(new CaseInsensitiveString("user2")));
        Role secondRole = new RoleConfig(new CaseInsensitiveString("ROLE2"), new RoleUser(new CaseInsensitiveString("user1")), new RoleUser(new CaseInsensitiveString("user3")));
        RolesConfig rolesConfig = new RolesConfig(firstRole, secondRole);
        assertThat(rolesConfig.memberRoles(new AdminRole(new CaseInsensitiveString("role1"))), is(asList(firstRole)));
        assertThat(rolesConfig.memberRoles(new AdminRole(new CaseInsensitiveString("role2"))), is(asList(secondRole)));
    }

    @Test
    public void shouldBeInvalidToHaveTwoRolesWithTheSameName() {
        Role role1 = new RoleConfig(new CaseInsensitiveString("role1"));
        Role role2 = new RoleConfig(new CaseInsensitiveString("role1"));
        RolesConfig rolesConfig = new RolesConfig(role1, role2);
        rolesConfig.validate(null);
        assertEquals(1, rolesConfig.errors().getAll().size());
    }
}
