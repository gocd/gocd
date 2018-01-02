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

package com.thoughtworks.go.presentation;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import com.thoughtworks.go.domain.User;

public class UserModel {
    private final User user;
    private final List<String> roles;
    private final boolean admin;

    public UserModel(User user, List<String> roles, boolean admin) {
        this.user = user;
        this.roles = roles;
        this.admin = admin;
    }

    public User getUser() {
        return user;
    }

    public boolean isAdmin() {
        return admin;
    }

    public List<String> getRoles() {
        ArrayList<String> sortedRoles = new ArrayList<>(roles);
        Collections.sort(sortedRoles);
        return sortedRoles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserModel userModel = (UserModel) o;

        if (admin != userModel.admin) {
            return false;
        }
        if (roles != null ? !roles.equals(userModel.roles) : userModel.roles != null) {
            return false;
        }
        if (user != null ? !user.equals(userModel.user) : userModel.user != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (roles != null ? roles.hashCode() : 0);
        result = 31 * result + (admin ? 1 : 0);
        return result;
    }

    @Override public String toString() {
        return "UserModel{" +
                "user=" + user +
                ", roles=" + roles +
                ", admin=" + admin +
                '}';
    }

    public boolean isEnabled() {
        return user.isEnabled();
    }
}
