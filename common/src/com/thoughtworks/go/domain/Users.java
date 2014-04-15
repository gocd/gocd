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

package com.thoughtworks.go.domain;

import java.util.List;

import com.thoughtworks.go.util.Filter;

public class Users extends BaseCollection<User> {
    public Users() {
    }

    public Users(List<User> users) {
        super(users);
    }

    public void accept(UserHandler handler) {
        for (User setting : this) {
            handler.visit(setting);
        }
    }

    public Users filter(Filter<User> filter) {
        Users result = new Users();
        for (User user : this) {
            if (filter.matches(user)) {
                result.add(user);
            }
        }
        return result;
    }

    public boolean containsUserNamed(String userName) {
        for (User user : this) {
            if(user.getName().equals(userName))
                return true;
        }
        return false;
    }
}
