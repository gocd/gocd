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
package com.thoughtworks.go.apiv1.admin.pipelinegroups.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AuthorizationRepresenter {
    public static void toJSON(OutputWriter jsonWriter, Authorization authorization) {
        ViewConfig viewConfig = authorization.getViewConfig();
        if (!viewConfig.isEmpty()) {
            jsonWriter.addChild("view", viewWriter -> writeUsersAndRoles(viewWriter, viewConfig.getUsers(), viewConfig.getRoles()));
        }
        AdminsConfig operationConfig = authorization.getOperationConfig();
        if (!operationConfig.isEmpty()) {
            jsonWriter.addChild("operate", operateWriter -> writeUsersAndRoles(operateWriter, operationConfig.getUsers(), operationConfig.getRoles()));
        }
        AdminsConfig adminsConfig = authorization.getAdminsConfig();
        if (!adminsConfig.isEmpty()) {
            jsonWriter.addChild("admins", adminsWriter -> writeUsersAndRoles(adminsWriter, adminsConfig.getUsers(), adminsConfig.getRoles()));
        }
    }

    private static void writeUsersAndRoles(OutputWriter viewWriter, List<AdminUser> users, List<AdminRole> roles) {
        if (users.isEmpty()) {
            viewWriter.addChildList("users", Collections.emptyList());
        } else {
            viewWriter.addChildList("users", users.stream().map(AdminUser::getName).map(CaseInsensitiveString::toString).collect(Collectors.toList()));
        }
        if (roles.isEmpty()) {
            viewWriter.addChildList("roles", Collections.emptyList());
        } else {
            viewWriter.addChildList("roles", roles.stream().map(AdminRole::getName).map(CaseInsensitiveString::toString).collect(Collectors.toList()));
        }
    }

    public static Authorization fromJSON(JsonReader jsonReader) {
        Authorization authorization = new Authorization();
        if (jsonReader.hasJsonObject("view")) {
            ViewConfig viewConfig = new ViewConfig();
            populateConfig(viewConfig, jsonReader.readJsonObject("view"));
            authorization.setViewConfig(viewConfig);
        }
        if (jsonReader.hasJsonObject("operate")) {
            OperationConfig operationConfig = new OperationConfig();
            populateConfig(operationConfig, jsonReader.readJsonObject("operate"));
            authorization.setOperationConfig(operationConfig);
        }
        if (jsonReader.hasJsonObject("admins")) {
            AdminsConfig adminsConfig = new AdminsConfig();
            populateConfig(adminsConfig, jsonReader.readJsonObject("admins"));
            authorization.setAdminsConfig(adminsConfig);
        }
        return authorization;
    }

    private static void populateConfig(AdminsConfig config, JsonReader jsonReader) {
        jsonReader.readArrayIfPresent("users", users -> users.forEach(user -> config.add(new AdminUser(new CaseInsensitiveString(user.getAsString())))));
        jsonReader.readArrayIfPresent("roles", roles -> roles.forEach(role -> config.add(new AdminRole(new CaseInsensitiveString(role.getAsString())))));
    }
}
