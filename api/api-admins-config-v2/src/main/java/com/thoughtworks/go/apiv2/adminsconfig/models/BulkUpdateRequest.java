/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv2.adminsconfig.models;

import java.util.List;

public class BulkUpdateRequest {

    private List<String> users;
    private List<String> roles;
    private boolean isAdmin;

    public BulkUpdateRequest(List<String> users, List<String> roles, boolean isAdmin) {
        this.users = users;
        this.roles = roles;
        this.isAdmin = isAdmin;
    }

    public List<String> getUsers() {
        return users;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean isAdmin() {
        return isAdmin;
    }
}
