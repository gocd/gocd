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

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthorizationTest {
    @Test
    public void shouldReturnTrueIfViewPermissionDefined() {
        Authorization authorization = new Authorization(new ViewConfig(new AdminUser(cis("baby"))));
        assertThat(authorization.hasViewPermissionDefined()).isTrue();
    }

    @Test
    public void shouldReturnFalseIfViewPermissionNotDefined() {
        Authorization authorization = new Authorization(new ViewConfig());
        assertThat(authorization.hasViewPermissionDefined()).isFalse();
    }

    @Test
    public void shouldReturnTrueIfOperationPermissionDefined() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(cis("baby"))));
        assertThat(authorization.hasOperationPermissionDefined()).isTrue();
    }

    @Test
    public void shouldReturnFalseIfOperationPermissionNotDefined() {
        Authorization authorization = new Authorization(new OperationConfig());
        assertThat(authorization.hasOperationPermissionDefined()).isFalse();
    }

    @Test
    public void shouldReturnTrueIfAdminsAreDefined() {
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(cis("foo"))));
        assertThat(authorization.hasAdminsDefined()).isTrue();
    }

    @Test
    public void shouldReturnTrueIfAnUserIsAdmin() {
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(cis("foo"))));
        assertThat(authorization.isUserAnAdmin(cis("foo"), new ArrayList<>())).isTrue();
        assertThat(authorization.isUserAnAdmin(cis("bar"), new ArrayList<>())).isFalse();
    }

    @Test
    public void shouldReturnTrueIfAnUserBelongsToAnAdminRole() {
        Authorization authorization = new Authorization(new AdminsConfig(new AdminRole(cis("bar1")), new AdminRole(cis("bar2"))));
        assertThat(authorization.isUserAnAdmin(cis("foo1"), List.of(new RoleConfig(cis("bar1")), new RoleConfig(cis("bar1")
        )))).isTrue();
        assertThat(authorization.isUserAnAdmin(cis("foo2"), List.of(new RoleConfig(cis("bar2"))))).isTrue();
        assertThat(authorization.isUserAnAdmin(cis("foo3"), List.of(new RoleConfig(cis("bar1"))))).isTrue();
        assertThat(authorization.isUserAnAdmin(cis("foo4"), new ArrayList<>())).isFalse();
    }

    @Test
    public void shouldSayThatAnAdmin_HasAdminOrViewPermissions() {
        CaseInsensitiveString adminUser = cis("admin");
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(adminUser)));
        assertThat(authorization.hasAdminOrViewPermissions(adminUser, null)).isTrue();
    }

    @Test
    public void shouldSayThatAViewUser_HasAdminOrViewPermissions() {
        CaseInsensitiveString viewUser = cis("view");
        Authorization authorization = new Authorization(new ViewConfig(new AdminUser(viewUser)));
        assertThat(authorization.hasAdminOrViewPermissions(viewUser, null)).isTrue();
    }

    @Test
    public void shouldSayThatAnAdminWithinARole_HasAdminOrViewPermissions() {
        CaseInsensitiveString adminUser = cis("admin");
        RoleConfig role = new RoleConfig(cis("role1"), new RoleUser(adminUser));
        List<Role> roles = new ArrayList<>();
        roles.add(role);
        Authorization authorization = new Authorization(new AdminsConfig(new AdminRole(role)));
        assertThat(authorization.hasAdminOrViewPermissions(adminUser, roles)).isTrue();
    }

    @Test
    public void shouldSayThatAViewUserWithinARole_HasAdminOrViewPermissions() {
        CaseInsensitiveString viewUser = cis("view");
        RoleConfig role = new RoleConfig(cis("role1"), new RoleUser(viewUser));
        List<Role> roles = new ArrayList<>();
        roles.add(role);
        Authorization authorization = new Authorization(new ViewConfig(new AdminRole(role)));
        assertThat(authorization.hasAdminOrViewPermissions(viewUser, roles)).isTrue();
    }

    @Test
    public void shouldReturnFalseForUserNotInAdminOrViewConfig() {
        CaseInsensitiveString viewUser = cis("view");
        Authorization authorization = new Authorization();
        assertThat(authorization.hasAdminOrViewPermissions(viewUser, null)).isFalse();
    }

    @Test
    public void shouldReturnFalseForNonAdminNonViewUserWithinARole() {
        CaseInsensitiveString viewUser = cis("view");
        RoleConfig role = new RoleConfig(cis("role1"), new RoleUser(viewUser));
        List<Role> roles = new ArrayList<>();
        roles.add(role);
        Authorization authorization = new Authorization(new ViewConfig(new AdminUser(cis("other-user"))));
        assertThat(authorization.hasAdminOrViewPermissions(viewUser, roles)).isFalse();
    }
}
