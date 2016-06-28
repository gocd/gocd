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
import java.util.Set;

import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import org.apache.log4j.Logger;

import static com.thoughtworks.go.util.ExceptionUtils.bombIf;

@ConfigTag("role")
public class Role implements Validatable {
    private static final Logger LOGGER = Logger.getLogger(Role.class);

    @ConfigAttribute(value = "name", optional = false)
    private CaseInsensitiveString name;

    @ConfigSubtag
    private Users users = new Users();

    private final ConfigErrors configErrors = new ConfigErrors();

    public Role() {
    }

    public Role(final CaseInsensitiveString name, RoleUser... users) {
        this(name, Users.users(users));
    }

    public Role(final CaseInsensitiveString name, Users users) {
        this.name = name;
        this.users = users;
        checkForDuplicateUsers(new DuplicateHandler() {
            public void invoke(RoleUser roleUser) {
                throw new RuntimeException(userExistsError(roleUser));
            }
        });
    }

    public void validate(ValidationContext validationContext) {
        if (this.name.isBlank() || !new NameTypeValidator().isNameValid(name)) {
            configErrors.add("name", NameTypeValidator.errorMessage("role name", name));
        }
        checkForDuplicateUsers(new ErrorMarkingDuplicateHandler(this));
    }

    private void checkForDuplicateUsers(DuplicateHandler dupHandler) {
        Set<RoleUser> roleUsers = new HashSet<>();
        for (RoleUser user : users) {
            if (roleUsers.contains(user)) {
                dupHandler.invoke(user);
            } else {
                roleUsers.add(user);
            }
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public CaseInsensitiveString getName() {
        return name;
    }

    public boolean hasMember(CaseInsensitiveString user) {
        if (user == null) {
            return false;
        }
        for (RoleUser roleUser : this.users) {
            if (roleUser.getName().equals(user)) {
                return true;
            }
        }
        return false;
    }

    public boolean addUser(RoleUser user) {
        bombIf(this.users.contains(user), userExistsError(user));
        return this.users.add(user);
    }

    private String userExistsError(RoleUser user) {
        return "User '" + CaseInsensitiveString.str(user.getName()) + "' already exists in '" + name + "'.";
    }

    public Set<String> usersOfRole() {
        Set<String> users = new HashSet<>();
        for (RoleUser roleUser : this.users) {
            users.add(CaseInsensitiveString.str(roleUser.getName()));
        }
        return users;
    }

    public List<RoleUser> getUsers() {
        List<RoleUser> users = new ArrayList<>(this.users);
        return users;
    }

    public void removeUser(RoleUser roleUser) {
        this.users.remove(roleUser);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Role role = (Role) o;

        if (name != null ? !name.equals(role.name) : role.name != null) {
            return false;
        }
        if (users != null ? !users.equals(role.users) : role.users != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (users != null ? users.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Role{" +
                "name=" + name +
                ", users=" + users +
                '}';
    }

    private static class ErrorMarkingDuplicateHandler implements DuplicateHandler {

        private final Role role;

        public ErrorMarkingDuplicateHandler(Role role) {
            this.role = role;
        }

        public void invoke(final RoleUser roleUser) {
            roleUser.addDuplicateError(CaseInsensitiveString.str(role.name));
        }
    }

    public static interface DuplicateHandler {
        void invoke(RoleUser roleUser);
    }
}
