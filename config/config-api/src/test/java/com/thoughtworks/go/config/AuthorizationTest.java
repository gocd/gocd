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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.config.Admin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AuthorizationTest {
    @Test
    public void shouldReturnTrueIfViewPermissionDefined() {
        Authorization authorization = new Authorization(new ViewConfig(new AdminUser(new CaseInsensitiveString("baby"))));
        assertThat(authorization.hasViewPermissionDefined(), is(true));
    }

    @Test
    public void shouldReturnFalseIfViewPermissionNotDefined() {
        Authorization authorization = new Authorization(new ViewConfig());
        assertThat(authorization.hasViewPermissionDefined(), is(false));
    }

    @Test
    public void shouldReturnTrueIfOperationPermissionDefined() {
        Authorization authorization = new Authorization(new OperationConfig(new AdminUser(new CaseInsensitiveString("baby"))));
        assertThat(authorization.hasOperationPermissionDefined(), is(true));
    }

    @Test
    public void shouldReturnFalseIfOperationPermissionNotDefined() {
        Authorization authorization = new Authorization(new OperationConfig());
        assertThat(authorization.hasOperationPermissionDefined(), is(false));
    }

    @Test
    public void shouldReturnTrueIfAdminsAreDefined() {
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString("foo"))));
        assertThat(authorization.hasAdminsDefined(), is(true));
    }

    @Test
    public void shouldReturnTrueIfAnUserIsAdmin() {
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(new CaseInsensitiveString("foo"))));
        assertThat(authorization.isUserAnAdmin(new CaseInsensitiveString("foo"), new ArrayList<>()), is(true));
        assertThat(authorization.isUserAnAdmin(new CaseInsensitiveString("bar"), new ArrayList<>()), is(false));
    }

    @Test
    public void shouldReturnTrueIfAnUserBelongsToAnAdminRole() {
        Authorization authorization = new Authorization(new AdminsConfig(new AdminRole(new CaseInsensitiveString("bar1")), new AdminRole(new CaseInsensitiveString("bar2"))));
        assertThat(authorization.isUserAnAdmin(new CaseInsensitiveString("foo1"), Arrays.asList(new RoleConfig(new CaseInsensitiveString("bar1")), new RoleConfig(new CaseInsensitiveString("bar1")
        ))), is(true));
        assertThat(authorization.isUserAnAdmin(new CaseInsensitiveString("foo2"), Arrays.asList(new RoleConfig(new CaseInsensitiveString("bar2")))), is(true));
        assertThat(authorization.isUserAnAdmin(new CaseInsensitiveString("foo3"), Arrays.asList(new RoleConfig(new CaseInsensitiveString("bar1")))), is(true));
        assertThat(authorization.isUserAnAdmin(new CaseInsensitiveString("foo4"), new ArrayList<>()), is(false));
    }

    @Test
    public void shouldSayThatAnAdmin_HasAdminOrViewPermissions() {
        CaseInsensitiveString adminUser = new CaseInsensitiveString("admin");
        Authorization authorization = new Authorization(new AdminsConfig(new AdminUser(adminUser)));
        assertThat(authorization.hasAdminOrViewPermissions(adminUser, null), is(true));
    }

    @Test
    public void shouldSayThatAViewUser_HasAdminOrViewPermissions() {
        CaseInsensitiveString viewUser = new CaseInsensitiveString("view");
        Authorization authorization = new Authorization(new ViewConfig(new AdminUser(viewUser)));
        assertThat(authorization.hasAdminOrViewPermissions(viewUser, null), is(true));
    }

    @Test
    public void shouldSayThatAnAdminWithinARole_HasAdminOrViewPermissions() {
        CaseInsensitiveString adminUser = new CaseInsensitiveString("admin");
        RoleConfig role = new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(adminUser));
        List<Role> roles = new ArrayList<>();
        roles.add(role);
        Authorization authorization = new Authorization(new AdminsConfig(new AdminRole(role)));
        assertThat(authorization.hasAdminOrViewPermissions(adminUser, roles), is(true));
    }

    @Test
    public void shouldSayThatAViewUserWithinARole_HasAdminOrViewPermissions() {
        CaseInsensitiveString viewUser = new CaseInsensitiveString("view");
        RoleConfig role = new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(viewUser));
        List<Role> roles = new ArrayList<>();
        roles.add(role);
        Authorization authorization = new Authorization(new ViewConfig(new AdminRole(role)));
        assertThat(authorization.hasAdminOrViewPermissions(viewUser, roles), is(true));
    }

    @Test
    public void shouldReturnFalseForUserNotInAdminOrViewConfig() {
        CaseInsensitiveString viewUser = new CaseInsensitiveString("view");
        Authorization authorization = new Authorization();
        assertThat(authorization.hasAdminOrViewPermissions(viewUser, null), is(false));
    }

    @Test
    public void shouldReturnFalseForNonAdminNonViewUserWithinARole() {
        CaseInsensitiveString viewUser = new CaseInsensitiveString("view");
        RoleConfig role = new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(viewUser));
        List<Role> roles = new ArrayList<>();
        roles.add(role);
        Authorization authorization = new Authorization(new ViewConfig(new AdminUser(new CaseInsensitiveString("other-user"))));
        assertThat(authorization.hasAdminOrViewPermissions(viewUser, roles), is(false));
    }

    @Test
    public void shouldReturnAuthorizationMapForView() {
        Authorization authorization = new Authorization();
        authorization.getAdminsConfig().add(new AdminRole(new CaseInsensitiveString("group_of_losers")));
        authorization.getOperationConfig().addAll(a(new AdminUser(new CaseInsensitiveString("loser")), new AdminRole(new CaseInsensitiveString("group_of_losers")), new AdminRole(
                new CaseInsensitiveString("gang_of_boozers"))));
        authorization.getViewConfig().addAll(a(new AdminUser(new CaseInsensitiveString("boozer")), new AdminUser(new CaseInsensitiveString("loser"))));

        List<Authorization.PresentationElement> userAuthMap = authorization.getUserAuthorizations();
        assertThat(userAuthMap.size(), is(2));
        assetEntry(userAuthMap.get(0), "boozer", Authorization.PrivilegeState.OFF, Authorization.PrivilegeState.ON, Authorization.PrivilegeState.OFF, Authorization.UserType.USER);
        assetEntry(userAuthMap.get(1), "loser", Authorization.PrivilegeState.OFF, Authorization.PrivilegeState.ON, Authorization.PrivilegeState.ON, Authorization.UserType.USER);

        List<Authorization.PresentationElement> roleAuthMap = authorization.getRoleAuthorizations();
        assertThat(roleAuthMap.size(), is(2));
        assetEntry(roleAuthMap.get(0), "gang_of_boozers", Authorization.PrivilegeState.OFF, Authorization.PrivilegeState.OFF, Authorization.PrivilegeState.ON, Authorization.UserType.ROLE);
        assetEntry(roleAuthMap.get(1), "group_of_losers", Authorization.PrivilegeState.ON, Authorization.PrivilegeState.DISABLED, Authorization.PrivilegeState.DISABLED, Authorization.UserType.ROLE);
    }

    @Test
    public void shouldPopulateErrorsOnPresentationElementWhenAnInvalidUserIsAddedToAdminList() {
        Authorization authorization = new Authorization();
        AdminUser invalidUser = new AdminUser(new CaseInsensitiveString("boo_user"));
        invalidUser.addError(AdminUser.NAME, "some error");
        AdminUser validUser = new AdminUser(new CaseInsensitiveString("valid_user"));
        authorization.getAdminsConfig().add(invalidUser);
        authorization.getAdminsConfig().add(validUser);

        List<Authorization.PresentationElement> userAuthorizations = authorization.getUserAuthorizations();

        assertThat(userAuthorizations.get(0).errors().isEmpty(), is(false));
        assertThat(userAuthorizations.get(0).errors().on(Admin.NAME), is("some error"));

        assertThat(userAuthorizations.get(1).errors().isEmpty(), is(true));
    }

    @Test
    public void shouldPopulateErrorsOnPresentationElementWhenAnInvalidRoleIsAddedToAdminList() {
        Authorization authorization = new Authorization();
        AdminRole invalidRole = new AdminRole(new CaseInsensitiveString("boo_user"));
        invalidRole.addError(AdminUser.NAME, "some error");
        AdminRole validRole = new AdminRole(new CaseInsensitiveString("valid_user"));
        authorization.getAdminsConfig().add(invalidRole);
        authorization.getAdminsConfig().add(validRole);

        List<Authorization.PresentationElement> roleAuthorizations = authorization.getRoleAuthorizations();

        assertThat(roleAuthorizations.get(0).errors().isEmpty(), is(false));
        assertThat(roleAuthorizations.get(0).errors().on(Admin.NAME), is("some error"));

        assertThat(roleAuthorizations.get(1).errors().isEmpty(), is(true));
    }

    private void assetEntry(Authorization.PresentationElement entry, final String name, final Authorization.PrivilegeState adminState, final Authorization.PrivilegeState viewState,
                            final Authorization.PrivilegeState operateState, final Authorization.UserType type) {
        assertThat(entry.getName(), is(name));
        assertThat(entry.getType(), is(type));
        assertThat(entry.getAdmin(), is(adminState));
        assertThat(entry.getView(), is(viewState));
        assertThat(entry.getOperate(), is(operateState));
    }
}
