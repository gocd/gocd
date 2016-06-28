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

package com.thoughtworks.go.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Admin;

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
        if (new HashSet<>(roleNames()).size() != roleNames().size()){
            this.configErrors.add("role", "Role names should be unique. Duplicate names found.");
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
        return add(new Role(roleName));
    }

    public boolean remove(Role role) {
        bombIf(!this.contains(role), "Role '" + CaseInsensitiveString.str(role.getName()) + "' does not exist.");
        return super.remove(role);
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

    public boolean isUserMemberOfRole(final CaseInsensitiveString userName, final CaseInsensitiveString roleName) {
        Role role = findByName(roleName);
        bombIfNull(role, String.format("Role \"%s\" does not exist!", roleName));
        return role.hasMember(userName);
    }

    private List<CaseInsensitiveString> roleNames() {
        List<CaseInsensitiveString> result = new ArrayList<>();
        for(Role role : this){
            result.add(role.getName());
        }
        return result;
    }
}
