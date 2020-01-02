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
package com.thoughtworks.go.server.service.result;

import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BulkUpdateAdminsResult extends HttpLocalizedOperationResult {
    private List<CaseInsensitiveString> nonExistentUsers;
    private List<CaseInsensitiveString> nonExistentRoles;
    private AdminsConfig adminsConfig;

    public BulkUpdateAdminsResult() {
        this.nonExistentUsers = new ArrayList<>();
        this.nonExistentRoles = new ArrayList<>();
    }

    public void setNonExistentUsers(Collection<CaseInsensitiveString> nonExistentUsers) {
        this.nonExistentUsers = new ArrayList<>(nonExistentUsers);
    }

    public void setNonExistentRoles(Collection<CaseInsensitiveString> nonExistentRoles) {
        this.nonExistentRoles = new ArrayList<>(nonExistentRoles);
    }

    public void setAdminsConfig(AdminsConfig adminsConfig) {
        this.adminsConfig = adminsConfig;
    }

    public List<CaseInsensitiveString> getNonExistentUsers() {
        return nonExistentUsers;
    }

    public List<CaseInsensitiveString> getNonExistentRoles() {
        return nonExistentRoles;
    }

    public AdminsConfig getAdminsConfig() {
        return adminsConfig;
    }
}
