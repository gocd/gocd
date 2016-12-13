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

import java.util.*;

import static com.thoughtworks.go.util.ExceptionUtils.bombIf;

@ConfigInterface
public interface Role extends Validatable {
    CaseInsensitiveString getName();

    void setName(CaseInsensitiveString name);

    Collection<RoleUser> doGetUsers();

    void doSetUsers(Collection<RoleUser> users);

    default boolean hasMember(CaseInsensitiveString user) {
        if (user == null) {
            return false;
        }
        for (RoleUser roleUser : doGetUsers()) {
            if (roleUser.getName().equals(user)) {
                return true;
            }
        }
        return false;
    }

    default boolean addUser(RoleUser user) {
        bombIf(doGetUsers().contains(user), "User '" + CaseInsensitiveString.str(user.getName()) + "' already exists in '" + getName() + "'.");
        return doGetUsers().add(user);
    }

    default void removeUser(RoleUser roleUser) {
        getUsers().remove(roleUser);
    }

    default Set<String> usersOfRole() {
        Set<String> users = new HashSet<>();
        for (RoleUser roleUser : doGetUsers()) {
            users.add(CaseInsensitiveString.str(roleUser.getName()));
        }
        return users;
    }

    default List<RoleUser> getUsers() {
        return new ArrayList<>(doGetUsers());
    }

}
