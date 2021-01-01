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

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.AdminRole;
import com.thoughtworks.go.config.AdminUser;
import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AdminsConfigRepresenter {
    public static void toJSON(OutputWriter jsonWriter, AdminsConfig admin) {
        jsonWriter.addLinks(
                outputLinkWriter -> outputLinkWriter.addAbsoluteLink("doc", Routes.SystemAdmins.DOC)
                        .addLink("self", Routes.SystemAdmins.BASE));
        toJSONWithoutLinks(jsonWriter, admin);
    }

    public static void toJSONWithoutLinks(OutputWriter jsonWriter, AdminsConfig admin) {
        jsonWriter.addChildList("roles", rolesAsString(admin.getRoles()));
        jsonWriter.addChildList("users", userAsString(admin.getUsers()));
        if (admin.hasErrors()) {
            jsonWriter.addChild("errors", errorWriter -> new ErrorGetter(Collections.singletonMap("SystemAdmin", "system_admin"))
                    .toJSON(errorWriter, admin));
        }
    }

    public static AdminsConfig fromJSON(JsonReader jsonReader) {
        AdminsConfig adminsConfig = new AdminsConfig();

        jsonReader.readArrayIfPresent("users", users -> {
            users.forEach(user -> adminsConfig.add(new AdminUser(new CaseInsensitiveString(user.getAsString()))));
        });

        jsonReader.readArrayIfPresent("roles", roles -> {
            roles.forEach(role -> adminsConfig.add(new AdminRole(new CaseInsensitiveString(role.getAsString()))));
        });

        return adminsConfig;
    }

    private static List<String> rolesAsString(List<AdminRole> roles) {
        return roles.stream().map(role -> role.getName().toString()).collect(Collectors.toList());
    }

    private static List<String> userAsString(List<AdminUser> users) {
        return users.stream().map(user -> user.getName().toString()).collect(Collectors.toList());
    }
}
