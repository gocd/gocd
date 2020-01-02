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
package com.thoughtworks.go.apiv2.rolesconfig.representers;

import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.update.GoCDRolesBulkUpdateRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoCDRolesBulkUpdateRequestRepresenter {

    public static GoCDRolesBulkUpdateRequest fromJSON(JsonReader jsonReader) {
        List<GoCDRolesBulkUpdateRequest.Operation> operations = new ArrayList<>();
        jsonReader.readArrayIfPresent("operations", operationsJSON -> operationsJSON.forEach(operationJSON -> {
            JsonReader operationReader = new JsonReader(operationJSON.getAsJsonObject());
            operations.add(parseOperation(operationReader));
        }));
        return new GoCDRolesBulkUpdateRequest(operations);
    }

    private static GoCDRolesBulkUpdateRequest.Operation parseOperation(JsonReader operationReader) {
        JsonReader users = operationReader.readJsonObject("users");
        List<String> usersToAdd = users.readStringArrayIfPresent("add").orElse(Collections.emptyList());
        List<String> usersToRemove = users.readStringArrayIfPresent("remove").orElse(Collections.emptyList());
        return new GoCDRolesBulkUpdateRequest.Operation(operationReader.getString("role"), usersToAdd, usersToRemove);
    }

}
