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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.config.Admin;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;

public class SecurityConfigMother {

    public static final Role ROLE1 = new RoleConfig(new CaseInsensitiveString("role1"), new RoleUser(new CaseInsensitiveString("chris")), new RoleUser(new CaseInsensitiveString("jez")));
    public static final Role ROLE2 = new RoleConfig(new CaseInsensitiveString("role2"), new RoleUser(new CaseInsensitiveString("chris")));
    public static final Role[] DEFAULT_ROLES = new Role[]{ROLE1, ROLE2};

    public static SecurityConfig securityConfigWith(String passwordFilePath) {
        final SecurityConfig securityConfig = new SecurityConfig();
        final SecurityAuthConfig passwordFile = new SecurityAuthConfig("file", "cd.go.authentication.passwordfile", create("PasswordFilePath", false, passwordFilePath));
        securityConfig.securityAuthConfigs().add(passwordFile);
        return securityConfig;
    }

    public static SecurityConfig securityConfigWithRole(String roleName, String... users) {
        SecurityConfig securityConfig = securityConfigWith("foo");
        return securityConfigWithRole(securityConfig, roleName, users);
    }

    public static SecurityConfig securityConfigWithRole(SecurityConfig securityConfig, String roleName, String... users) {
        RoleConfig role = new RoleConfig(new CaseInsensitiveString(roleName));
        for (String user : users) {
            role.addUser(new RoleUser(new CaseInsensitiveString(user)));
        }
        securityConfig.addRole(role);
        return securityConfig;
    }

    public static AdminsConfig admins(Admin... admins) {
        return new AdminsConfig(admins);
    }

    public static AdminUser user(String name) {
        return new AdminUser(new CaseInsensitiveString(name));
    }

    public static AdminRole role(String name) {
        return new AdminRole(new CaseInsensitiveString(name));
    }

    public static SecurityAuthConfig passwordFileAuthConfig() {
        return new SecurityAuthConfig("file", "cd.go.authentication.passwordfile");
    }

    public static SecurityConfig security(SecurityAuthConfig securityAuthConfig, AdminsConfig admins) {
        final SecurityConfig security = new SecurityConfig(admins);

        if (securityAuthConfig != null) {
            security.securityAuthConfigs().add(securityAuthConfig);
        }

        for (Role role : DEFAULT_ROLES) {
            security.addRole(role);
        }
        return security;
    }
}
