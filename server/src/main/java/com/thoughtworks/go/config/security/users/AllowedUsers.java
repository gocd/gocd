/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.RoleUser;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashSet;
import java.util.Set;

/* Understands: The set of users it has. */
public class AllowedUsers implements Users {
    private final Set<PluginRoleConfig> allowedRoles;
    private Set<String> allowedUsers = new HashSet<>();

    public AllowedUsers(Set<String> allowedUsers, Set<PluginRoleConfig> allowedRoles) {
        this.allowedRoles = allowedRoles;
        for (String user : allowedUsers) {
            this.allowedUsers.add(user.toLowerCase());
        }
    }

    @Override
    public boolean contains(String username) {
        return allowedUsers.contains(username.toLowerCase()) || containsInRole(username);
    }

    private boolean containsInRole(String username) {
        for (PluginRoleConfig role : allowedRoles) {
            for (RoleUser r : role.getUsers()) {
                if (r.getName().equals(new CaseInsensitiveString(username)))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AllowedUsers that = (AllowedUsers) o;

        if (allowedRoles != null ? !allowedRoles.equals(that.allowedRoles) : that.allowedRoles != null) return false;
        return allowedUsers != null ? allowedUsers.equals(that.allowedUsers) : that.allowedUsers == null;
    }

    @Override
    public int hashCode() {
        int result = allowedRoles != null ? allowedRoles.hashCode() : 0;
        result = 31 * result + (allowedUsers != null ? allowedUsers.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
