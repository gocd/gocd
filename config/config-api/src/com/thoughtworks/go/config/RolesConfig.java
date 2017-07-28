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

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bombIf;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

@ConfigTag("roles")
@ConfigCollection(Role.class)
public class RolesConfig extends BaseCollection<Role> implements Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public RolesConfig() {
    }

    public RolesConfig(Role... roles) {
        for (Role role : roles) {
            add(role);
        }
    }

    public void validate(ValidationContext validationContext) {
        if (new HashSet<>(roleNames()).size() != roleNames().size()) {
            this.configErrors.add("name", "Role names should be unique. Duplicate names found.");
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public boolean add(Role role) {
        return super.add(role);
    }

    public boolean add(CaseInsensitiveString roleName) {
        return add(new RoleConfig(roleName));
    }

    public boolean remove(Role role) {
        bombIf(!this.contains(role), "Role '" + CaseInsensitiveString.str(role.getName()) + "' does not exist.");
        return super.remove(role);
    }

    public void removeIfExists(Role role) {
        super.remove(role);
    }


    public List<Role> memberRoles(Admin admin) {
        List<Role> memberRoles = new ArrayList<>();
        for (Role role : this) {
            if (admin.belongsTo(role)) {
                memberRoles.add(role);
            }
        }
        return memberRoles;
    }

    public boolean isRoleExist(final CaseInsensitiveString role) {
        for (Role r : this) {
            if (r.getName().equals(role)) {
                return true;
            }
        }
        return false;
    }

    public Role findByName(final CaseInsensitiveString roleName) {
        for (Role role : this) {
            if (role.getName().equals(roleName)) {
                return role;
            }
        }
        return null;
    }

    public Role findByNameAndType(final CaseInsensitiveString roleName, Class cls) {
        for (Role role : this) {
            if (role.getName().equals(roleName) && (role.getClass().getCanonicalName().equals(cls.getCanonicalName()))) {
                return role;
            }
        }
        return null;
    }

    public boolean isUniqueRoleName(final CaseInsensitiveString roleName) {
        return Collections.frequency(roleNames(), roleName) <= 1;
    }

    public PluginRoleConfig findPluginRoleByName(CaseInsensitiveString pluginRoleName) {
        for (PluginRoleConfig pluginRoleConfig : getPluginRoleConfigs()) {
            if (pluginRoleConfig.getName().equals(pluginRoleName)) {
                return pluginRoleConfig;
            }
        }
        return null;
    }

    public boolean isUserMemberOfRole(final CaseInsensitiveString userName, final CaseInsensitiveString roleName) {
        Role role = findByName(roleName);
        bombIfNull(role, String.format("Role \"%s\" does not exist!", roleName));
        return role.hasMember(userName);
    }

    private List<CaseInsensitiveString> roleNames() {
        List<CaseInsensitiveString> result = new ArrayList<>();
        for (Role role : this) {
            result.add(role.getName());
        }
        return result;
    }

    public List<PluginRoleConfig> getPluginRoleConfigs() {
        return filterRolesBy(PluginRoleConfig.class);
    }

    public List<PluginRoleConfig> pluginRoleConfigsFor(String authConfigId) {
        List<PluginRoleConfig> rolesConfig = new ArrayList<>();
        for (Role role : this) {
            if (role instanceof PluginRoleConfig) {
                if (((PluginRoleConfig) role).getAuthConfigId().equals(authConfigId)) {
                    rolesConfig.add((PluginRoleConfig) role);
                }
            }
        }

        return rolesConfig;
    }

    public List<RoleConfig> getRoleConfigs() {
        return filterRolesBy(RoleConfig.class);
    }

    public RolesConfig allRoles() {
        return new RolesConfig(this.toArray(new Role[0]));
    }

    private <T> List<T> filterRolesBy(Class<T> type) {
        List<T> rolesConfig = new ArrayList<>();
        for (Role role : this) {
            if (role.getClass() == type) {
                rolesConfig.add(type.cast(role));
            }
        }
        return rolesConfig;
    }
}
