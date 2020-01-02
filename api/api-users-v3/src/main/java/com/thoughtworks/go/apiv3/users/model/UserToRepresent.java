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
package com.thoughtworks.go.apiv3.users.model;

import com.thoughtworks.go.config.RolesConfig;
import com.thoughtworks.go.domain.User;

/**
 * UserToRepresent Class extends com.thoughtworks.go.domain.User class,
 * hence it has all the data fields of a User defined in DB.
 * <p>
 * Additionally, UserToRepresent also knows following fields:
 *
 * @field isAdmin Boolean
 * Tells whether the current user is admin or not.
 * </p>
 * <br/>
 * @since Users API v3
 */

public class UserToRepresent extends User {
    private final boolean isAdmin;
    private final RolesConfig rolesConfig;

    private UserToRepresent(User user, boolean isAdmin, RolesConfig rolesConfig) {
        super(user);
        this.isAdmin = isAdmin;
        this.rolesConfig = (rolesConfig == null) ? new RolesConfig() : rolesConfig;
    }

    public static UserToRepresent from(User user, boolean isAdmin, RolesConfig rolesConfig) {
        return new UserToRepresent(user, isAdmin, rolesConfig);
    }

    public boolean isAdmin() {
        return this.isAdmin;
    }

    public RolesConfig getRoles() {
        return this.rolesConfig;
    }
}
