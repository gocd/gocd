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
package com.thoughtworks.go.apiv2.adminsconfig.models;

import java.util.List;

public class BulkUpdateRequest {

    private List<String> usersToAdd;
    private List<String> usersToRemove;
    private List<String> rolesToAdd;
    private List<String> rolesToRemove;

    public BulkUpdateRequest(List<String> usersToAdd, List<String> usersToRemove, List<String> rolesToAdd, List<String> rolesToRemove) {
        this.usersToAdd = usersToAdd;
        this.usersToRemove = usersToRemove;
        this.rolesToAdd = rolesToAdd;
        this.rolesToRemove = rolesToRemove;
    }

    public List<String> getUsersToAdd() {
        return usersToAdd;
    }

    public List<String> getUsersToRemove() {
        return usersToRemove;
    }

    public List<String> getRolesToAdd() {
        return rolesToAdd;
    }

    public List<String> getRolesToRemove() {
        return rolesToRemove;
    }
}
