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

package com.thoughtworks.go.server.dao;

import java.util.List;
import java.util.Set;

import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.Users;

public interface UserDao {
    void saveOrUpdate(User user);

    User findUser(String name);

    Users findNotificationSubscribingUsers();

    Users allUsers();

    Integer enabledUserCount();

    void deleteAll();

    void disableUsers(List<String> usernames);

    void enableUsers(List<String> usernames);

    List<User> enabledUsers();

    Set<String> findUsernamesForIds(Set<Long> userIds);

    User load(long id);

    boolean deleteUser(String username);
}
