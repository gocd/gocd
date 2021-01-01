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
package com.thoughtworks.go.domain.config;

import java.util.Arrays;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.UserRoleMatcherMother;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdminsConfigTest {
    @Test
    public void shouldReturnTrueIfHasUser() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(new CaseInsensitiveString("user1")));
        assertThat("shouldReturnTrueIfHasUser", adminsConfig.hasUser(new CaseInsensitiveString("user1"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(true));
    }

    @Test
    public void shouldReturnTrueIfUserMatchRole() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(new CaseInsensitiveString("user1")), new AdminRole(new CaseInsensitiveString("role")));
        assertThat("shouldReturnTrueIfUserMatchRole", adminsConfig.hasUser(new CaseInsensitiveString("roleuser"), UserRoleMatcherMother.ALWAYS_TRUE_MATCHER), is(true));
    }

    @Test
    public void shouldReturnFalseIfUserDoesNotExist() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(new CaseInsensitiveString("user1")));
        assertThat("shouldReturnFalseIfUserDoesNotExist", adminsConfig.hasUser(new CaseInsensitiveString("anyone"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER), is(false));
    }

    @Test
    public void shouldReturnTrueIfAUserBelongsToAnAdminRole() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminRole(new CaseInsensitiveString("Role1")));
        assertThat(adminsConfig.isAdmin(new AdminUser(new CaseInsensitiveString("user1")), Arrays.asList(new RoleConfig(new CaseInsensitiveString("first")
        ), new RoleConfig(new CaseInsensitiveString("role1")))), is(true));
    }

    @Test
    public void shouldReturnTrueIfAUserIsAnAdmin() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(new CaseInsensitiveString("USER1")));
        assertThat(adminsConfig.isAdmin(new AdminUser(new CaseInsensitiveString("user1")), Arrays.asList(new RoleConfig(new CaseInsensitiveString("first")
        ), new RoleConfig(new CaseInsensitiveString("role1")))), is(true));
    }

    @Test
    public void shouldReturnFalseIfAUserBelongsToAnAdminRoleNoRolesGiven() {
        CaseInsensitiveString username = new CaseInsensitiveString("USER1");
        AdminsConfig adminsConfig = new AdminsConfig(new AdminRole(username));
        // this is how isAdmin() is used in TemplatesConfig
        assertThat(adminsConfig.isAdmin(new AdminUser(username), null), is(false));
    }

    @Test
    public void shouldUnderstandIfAUserIsAnAdminThroughRole() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(new CaseInsensitiveString("loser")), new AdminRole(new CaseInsensitiveString("Role1")));
        assertThat(adminsConfig.isAdminRole(Arrays.asList(new RoleConfig(new CaseInsensitiveString("first")), new RoleConfig(new CaseInsensitiveString("role1")))), is(true));
        assertThat(adminsConfig.isAdminRole(Arrays.asList(new RoleConfig(new CaseInsensitiveString("role2")))), is(false));
        assertThat(adminsConfig.isAdminRole(Arrays.asList(new RoleConfig(new CaseInsensitiveString("loser")))), is(false));
    }

    @Test
    public void shouldValidatePresenceOfUserName() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(""));
        ValidationContext validationContext = mock(ValidationContext.class);

        assertFalse(adminsConfig.validateTree(validationContext));

        assertTrue(adminsConfig.hasErrors());
        assertThat(adminsConfig.errors().on("users"), is("User cannot be blank."));
    }

    @Test
    public void shouldValidateIfUserNameIsBlank() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(new CaseInsensitiveString("")));
        ValidationContext validationContext = mock(ValidationContext.class);

        assertFalse(adminsConfig.validateTree(validationContext));

        assertTrue(adminsConfig.hasErrors());
        assertThat(adminsConfig.errors().on("users"), is("User cannot be blank."));
    }

    @Test
    public void shouldValidateIfRoleExists() {
        CaseInsensitiveString roleName = new CaseInsensitiveString("admin_role");
        AdminsConfig adminsConfig = new AdminsConfig(new AdminRole(roleName));
        ValidationContext validationContext = mock(ValidationContext.class);
        SecurityConfig securityConfig = mock(SecurityConfig.class);

        when(validationContext.shouldNotCheckRole()).thenReturn(false);
        when(validationContext.getServerSecurityConfig()).thenReturn(securityConfig);
        when(securityConfig.isRoleExist(roleName)).thenReturn(false);

        assertFalse(adminsConfig.validateTree(validationContext));

        assertTrue(adminsConfig.hasErrors());
        assertThat(adminsConfig.errors().on("roles"), is("Role \"admin_role\" does not exist."));
    }
}
