/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import java.util.ArrayList;
import java.util.List;
public class BulkUpdateUsersOperationResult extends HttpLocalizedOperationResult {
    private List<String> nonExistentUsers;
    private List<String> enabledUsers;

    public BulkUpdateUsersOperationResult() {
        nonExistentUsers = new ArrayList<>();
        enabledUsers = new ArrayList<>();
    }

    public BulkUpdateUsersOperationResult(List<String> nonExistentUsers, List<String> enabledUsers) {
        this.nonExistentUsers = new ArrayList<>();
        this.nonExistentUsers = nonExistentUsers;
        this.enabledUsers = new ArrayList<>();
        this.enabledUsers = enabledUsers;
    }

    public List<String> getNonExistentUsers(){
        return nonExistentUsers;
    }

    public List<String> getEnabledUsers(){
        return enabledUsers;
    }

    public void addNonExistentUserName(String userName) {
        nonExistentUsers.add(userName);
    }

    public void addEnabledUserName(String userName) {
        enabledUsers.add(userName);
    }

    public boolean isEmpty() {
        return nonExistentUsers.isEmpty() && enabledUsers.isEmpty();
    }
}
