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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CaseInsensitiveString;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GoCDRolesBulkUpdateRequest {

    private List<Operation> operations;

    public GoCDRolesBulkUpdateRequest(List<Operation> operations) {
        this.operations = operations;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public List<CaseInsensitiveString> getRolesToUpdate() {
        return operations.stream()
                .map(operation -> new CaseInsensitiveString(operation.roleName))
                .collect(Collectors.toList());
    }

    public String getRolesToUpdateAsString() {
        return operations.stream()
                .map(operation -> operation.roleName)
                .collect(Collectors.joining(","));
    }

    public static class Operation {
        private String roleName;
        private List<String> usersToAdd;
        private List<String> usersToRemove;

        public Operation(String roleName, List<String> usersToAdd, List<String> usersToRemove) {
            this.roleName = roleName;
            this.usersToAdd = usersToAdd;
            this.usersToRemove = usersToRemove;
        }

        public String getRoleName() {
            return roleName;
        }

        public List<String> getUsersToAdd() {
            return usersToAdd;
        }

        public List<String> getUsersToRemove() {
            return usersToRemove;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Operation operation = (Operation) o;
            return Objects.equals(roleName, operation.roleName) &&
                    Objects.equals(usersToAdd, operation.usersToAdd) &&
                    Objects.equals(usersToRemove, operation.usersToRemove);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roleName, usersToAdd, usersToRemove);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoCDRolesBulkUpdateRequest request = (GoCDRolesBulkUpdateRequest) o;
        return Objects.equals(operations, request.operations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operations);
    }
}
