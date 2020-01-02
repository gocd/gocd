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
package com.thoughtworks.go.apiv3.rolesconfig.representers;


import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.RoleConfig;
import com.thoughtworks.go.config.RoleUser;

import java.util.List;
import java.util.stream.Collectors;

public class GoCDRoleConfigRepresenter {

    public static void toJSON(OutputWriter attributeWriter, RoleConfig role) {
        attributeWriter.addChildList("users", usersAsString(role));
    }

    public static RoleConfig fromJSON(JsonReader jsonReader) {
        RoleConfig model = new RoleConfig();
        if (jsonReader == null) {
            return model;
        }
        jsonReader.readArrayIfPresent("users", users -> {
            users.forEach(user -> model.addUser(new RoleUser(user.getAsString())));
        });
        return model;
    }

    private static List<String> usersAsString(RoleConfig roleConfig) {
        return roleConfig.getUsers().stream().map(user -> user.getName().toString()).collect(Collectors.toList());
    }

}
