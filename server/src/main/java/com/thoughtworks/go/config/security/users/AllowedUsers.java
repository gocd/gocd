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
package com.thoughtworks.go.config.security.users;

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.RoleUser;

import java.util.Set;
import java.util.stream.Collectors;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;

/* Understands: The set of users it has. */
public record AllowedUsers(Set<String> allowedUsers, Set<PluginRoleConfig> allowedRoles) implements Users {

    public AllowedUsers(Set<String> allowedUsers, Set<PluginRoleConfig> allowedRoles) {
        this.allowedUsers = allowedUsers.stream().map(String::toLowerCase).collect(Collectors.toSet());
        this.allowedRoles = allowedRoles;
    }

    @Override
    public boolean contains(String username) {
        return allowedUsers.contains(username.toLowerCase()) || containsInRole(username);
    }

    private boolean containsInRole(String username) {
        for (PluginRoleConfig role : allowedRoles) {
            for (RoleUser r : role.getUsers()) {
                if (r.getName().equals(cis(username))) {
                    return true;
                }
            }
        }
        return false;
    }
}
