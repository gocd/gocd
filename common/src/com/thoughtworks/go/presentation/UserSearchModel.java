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

import com.thoughtworks.go.domain.User;

/**
 * Understands the results coming from LDAP or Password file.
 */
public class UserSearchModel {
    private final User user;
    private final UserSourceType userSourceType;

    public UserSearchModel(User user, UserSourceType userSourceType) {
        this.user = user;
        this.userSourceType = userSourceType;
    }

    public UserSourceType getUserSourceType() {
        return userSourceType;
    }
    
    public User getUser() {
        return user;
    }

    @Override public String toString() {
        return "UserSearchModel{" +
                "user=" + user +
                ", userSourceType=" + userSourceType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserSearchModel that = (UserSearchModel) o;

        if (user != null ? !user.equals(that.user) : that.user != null) {
            return false;
        }
        if (userSourceType != that.userSourceType) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (userSourceType != null ? userSourceType.hashCode() : 0);
        return result;
    }
}
