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

package com.thoughtworks.go.apiv2.adminsconfig.representers;

import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv2.adminsconfig.models.BulkUpdateRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BulkUpdateRequestRepresenter {

    public static BulkUpdateRequest fromJSON(JsonReader jsonReader) {
        Optional<List<String>> users = jsonReader.readStringArrayIfPresent("users");
        Optional<List<String>> roles = jsonReader.readStringArrayIfPresent("roles");
        boolean isAdmin = jsonReader.readJsonObject("operations").getBoolean("isAdmin");
        return new BulkUpdateRequest(users.orElse(Collections.emptyList()), roles.orElse(Collections.emptyList()), isAdmin);
    }
}
