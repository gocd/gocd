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
package com.thoughtworks.go.domain.config;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.UserRoleMatcherMother;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdminsConfigTest {
    @Test
    public void shouldReturnTrueIfHasUser() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(cis("user1")));
        assertThat(adminsConfig.hasUser(cis("user1"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isTrue();
    }

    @Test
    public void shouldReturnTrueIfUserMatchRole() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(cis("user1")), new AdminRole(cis("role")));
        assertThat(adminsConfig.hasUser(cis("roleuser"), UserRoleMatcherMother.ALWAYS_TRUE_MATCHER)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfUserDoesNotExist() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(cis("user1")));
        assertThat(adminsConfig.hasUser(cis("anyone"), UserRoleMatcherMother.ALWAYS_FALSE_MATCHER)).isFalse();
    }

    @Test
    public void shouldReturnTrueIfAUserBelongsToAnAdminRole() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminRole(cis("Role1")));
        assertThat(adminsConfig.isAdmin(new AdminUser(cis("user1")), List.of(new RoleConfig(cis("first")
        ), new RoleConfig(cis("role1"))))).isTrue();
    }

    @Test
    public void shouldReturnTrueIfAUserIsAnAdmin() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(cis("USER1")));
        assertThat(adminsConfig.isAdmin(new AdminUser(cis("user1")), List.of(new RoleConfig(cis("first")
        ), new RoleConfig(cis("role1"))))).isTrue();
    }

    @Test
    public void shouldReturnFalseIfAUserBelongsToAnAdminRoleNoRolesGiven() {
        CaseInsensitiveString username = cis("USER1");
        AdminsConfig adminsConfig = new AdminsConfig(new AdminRole(username));
        // this is how isAdmin() is used in TemplatesConfig
        assertThat(adminsConfig.isAdmin(new AdminUser(username), null)).isFalse();
    }

    @Test
    public void shouldUnderstandIfAUserIsAnAdminThroughRole() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(cis("loser")), new AdminRole(cis("Role1")));
        assertThat(adminsConfig.isAdminRole(List.of(new RoleConfig(cis("first")), new RoleConfig(cis("role1"))))).isTrue();
        assertThat(adminsConfig.isAdminRole(List.of(new RoleConfig(cis("role2"))))).isFalse();
        assertThat(adminsConfig.isAdminRole(List.of(new RoleConfig(cis("loser"))))).isFalse();
    }

    @Test
    public void shouldValidatePresenceOfUserName() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(""));
        ValidationContext validationContext = mock(ValidationContext.class);

        assertFalse(adminsConfig.validateTree(validationContext));

        assertTrue(adminsConfig.hasErrors());
        assertThat(adminsConfig.errors().firstErrorOn("users")).isEqualTo("User cannot be blank.");
    }

    @Test
    public void shouldValidateIfUserNameIsBlank() {
        AdminsConfig adminsConfig = new AdminsConfig(new AdminUser(cis("")));
        ValidationContext validationContext = mock(ValidationContext.class);

        assertFalse(adminsConfig.validateTree(validationContext));

        assertTrue(adminsConfig.hasErrors());
        assertThat(adminsConfig.errors().firstErrorOn("users")).isEqualTo("User cannot be blank.");
    }

    @Test
    public void shouldValidateIfRoleExists() {
        CaseInsensitiveString roleName = cis("admin_role");
        AdminsConfig adminsConfig = new AdminsConfig(new AdminRole(roleName));
        ValidationContext validationContext = mock(ValidationContext.class);
        SecurityConfig securityConfig = mock(SecurityConfig.class);

        when(validationContext.shouldNotCheckRole()).thenReturn(false);
        when(validationContext.getServerSecurityConfig()).thenReturn(securityConfig);
        when(securityConfig.isRoleExist(roleName)).thenReturn(false);

        assertFalse(adminsConfig.validateTree(validationContext));

        assertTrue(adminsConfig.hasErrors());
        assertThat(adminsConfig.errors().firstErrorOn("roles")).isEqualTo("Role \"admin_role\" does not exist.");
    }
}
