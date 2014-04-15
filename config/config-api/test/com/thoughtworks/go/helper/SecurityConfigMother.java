/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PasswordFileConfig;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RoleUser;
import com.thoughtworks.go.config.SecurityConfig;

public class SecurityConfigMother {

    public static SecurityConfig securityConfigWith(String passwordFile) {
        return new SecurityConfig(null, new PasswordFileConfig(passwordFile), true);
    }

    public static SecurityConfig securityConfigWithRole(String roleName, String...users) {
        SecurityConfig securityConfig = securityConfigWith("foo");
        return securityConfigWithRole(securityConfig, roleName, users);
    }

    public static SecurityConfig securityConfigWithRole(SecurityConfig securityConfig, String roleName, String... users) {
        Role role = new Role(new CaseInsensitiveString(roleName));
        for (String user : users) {
            role.addUser(new RoleUser(new CaseInsensitiveString(user)));
        }
        securityConfig.addRole(role);
        return securityConfig;
    }
}
