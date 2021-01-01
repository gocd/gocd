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
package com.thoughtworks.go.apiv2.adminsconfig.representers;

import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv2.adminsconfig.models.BulkUpdateRequest;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class BulkUpdateRequestRepresenter {

    public static BulkUpdateRequest fromJSON(JsonReader jsonReader) {
        JsonReader operations = jsonReader.readJsonObject("operations");
        List<String> usersToAdd = new ArrayList<>();
        List<String> usersToRemove = new ArrayList<>();
        List<String> rolesToAdd = new ArrayList<>();
        List<String> rolesToRemove = new ArrayList<>();

        operations.optJsonObject("users").ifPresent(reader -> {
            usersToAdd.addAll(reader.readStringArrayIfPresent("add").orElse(emptyList()));
            usersToRemove.addAll(reader.readStringArrayIfPresent("remove").orElse(emptyList()));
        });

        operations.optJsonObject("roles").ifPresent(reader -> {
            rolesToAdd.addAll(reader.readStringArrayIfPresent("add").orElse(emptyList()));
            rolesToRemove.addAll(reader.readStringArrayIfPresent("remove").orElse(emptyList()));
        });
        return new BulkUpdateRequest(usersToAdd, usersToRemove, rolesToAdd, rolesToRemove);
    }
}
