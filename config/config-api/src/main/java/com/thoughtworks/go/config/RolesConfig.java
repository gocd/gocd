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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Admin;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.go.util.ExceptionUtils.bombIf;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

@ConfigTag("roles")
@ConfigCollection(Role.class)
public class RolesConfig extends BaseCollection<Role> implements Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public RolesConfig() {
    }

    public RolesConfig(Collection<Role> roles) {
        super(roles);
    }

    public RolesConfig(Role... roles) {
        super(roles);
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (new HashSet<>(roleNames()).size() != roleNames().size()) {
            this.configErrors.add("name", "Role names should be unique. Duplicate names found.");
        }
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
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

    public <T extends Role> T findByNameAndType(final CaseInsensitiveString roleName, Class<T> cls) {
        for (Role role : this) {
            if (role.getName().equals(roleName) && (role.getClass().getCanonicalName().equals(cls.getCanonicalName()))) {
                return (T) role;
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

    public void setRoles(List<Role> roles) {
        this.clear();
        this.addAll(roles);
    }

    public List<Role> getRoles() {
        return this;
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

    private static Map<String, Class<? extends Role>> ROLE_FILTER_MAP = new LinkedHashMap<>();

    static {
        ROLE_FILTER_MAP.put("gocd", RoleConfig.class);
        ROLE_FILTER_MAP.put("plugin", PluginRoleConfig.class);
    }

    public RolesConfig ofType(String pluginType) {
        if (isBlank(pluginType)) {
            return this;
        }

        Class<? extends Role> roleClass = ROLE_FILTER_MAP.get(pluginType);

        if (roleClass == null) {
            throw new BadRequestException("Bad role type `" + pluginType + "`. Valid values are " + StringUtils.join(ROLE_FILTER_MAP.keySet(), ", "));
        }

        return this.stream().filter(role -> role.getClass().isAssignableFrom(roleClass)).collect(Collectors.toCollection(RolesConfig::new));

    }
}
