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
package com.thoughtworks.go.apiv1.templateauthorization.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class AuthorizationRepresenter {
    public static void toJSON(OutputWriter jsonWriter, Authorization authorization) {
        jsonWriter.add("all_group_admins_are_view_users", authorization.isAllowGroupAdmins());
        AdminsConfig adminsConfig = authorization.getAdminsConfig();
        if (!adminsConfig.isEmpty()) {
            jsonWriter.addChild("admin", adminsWriter -> writeUsersAndRoles(adminsWriter, adminsConfig.getUsers(), adminsConfig.getRoles()));
        }

        ViewConfig viewConfig = authorization.getViewConfig();
        if (!viewConfig.isEmpty()) {
            jsonWriter.addChild("view", viewWriter -> writeUsersAndRoles(viewWriter, viewConfig.getUsers(), viewConfig.getRoles()));
        }
    }

    private static void writeUsersAndRoles(OutputWriter viewWriter, List<AdminUser> users, List<AdminRole> roles) {
        List<String> userErrors = new ArrayList<>();
        users.stream().map(user -> user.errors().getAllOn(AdminUser.ADMIN)).filter(Objects::nonNull).forEach(userErrors::addAll);
        List<String> rolesErrors = new ArrayList<>();
        roles.stream().map(role -> role.errors().getAllOn(AdminRole.ADMIN)).filter(Objects::nonNull).forEach(rolesErrors::addAll);

        if (!rolesErrors.isEmpty() || !userErrors.isEmpty()) {
            viewWriter.addChild("errors", errorsWriter -> {
                errorsWriter.addChildList("roles", rolesErrors);
                errorsWriter.addChildList("users", userErrors);
            });
        }

        if (users.isEmpty()) {
            viewWriter.addChildList("users", emptyList());
        } else {
            viewWriter.addChildList("users", users.stream().map(AdminUser::getName).map(CaseInsensitiveString::toString).collect(toList()));
        }

        if (roles.isEmpty()) {
            viewWriter.addChildList("roles", emptyList());
        } else {
            viewWriter.addChildList("roles", roles.stream().map(AdminRole::getName).map(CaseInsensitiveString::toString).collect(toList()));
        }
    }

    public static Authorization fromJSON(JsonReader jsonReader) {
        Authorization authorization = new Authorization();

        jsonReader.readBooleanIfPresent("all_group_admins_are_view_users", authorization::setAllowGroupAdmins);

        if (jsonReader.hasJsonObject("admin")) {
            AdminsConfig adminsConfig = new AdminsConfig();
            populateConfig(adminsConfig, jsonReader.readJsonObject("admin"));
            authorization.setAdminsConfig(adminsConfig);
        }

        if (jsonReader.hasJsonObject("view")) {
            ViewConfig viewConfig = new ViewConfig();
            populateConfig(viewConfig, jsonReader.readJsonObject("view"));
            authorization.setViewConfig(viewConfig);
        }

        return authorization;
    }

    private static void populateConfig(AdminsConfig config, JsonReader jsonReader) {
        jsonReader.readArrayIfPresent("users", users -> users.forEach(user -> config.add(new AdminUser(new CaseInsensitiveString(user.getAsString())))));
        jsonReader.readArrayIfPresent("roles", roles -> roles.forEach(role -> config.add(new AdminRole(new CaseInsensitiveString(role.getAsString())))));
    }
}
