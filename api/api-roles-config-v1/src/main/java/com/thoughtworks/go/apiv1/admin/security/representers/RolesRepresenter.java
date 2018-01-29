/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.admin.security.representers;


import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.spark.RequestContext;
import com.thoughtworks.go.spark.Routes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RolesRepresenter {

    private static JsonWriter addLinks(JsonWriter jsonWriter) {
        return jsonWriter.addLink("self", Routes.Roles.BASE)
                .addDocLink(Routes.Roles.DOC)
                .addLink("find", Routes.Roles.find());
    }

    public static Map toJSON(List<Role> roles, RequestContext requestContext) {
        List<Map> rolesArray = roles.stream()
                .map(role -> RoleRepresenter.toJSON(role, requestContext))
                .collect(Collectors.toList());

        return addLinks(new JsonWriter(requestContext))
                .addEmbedded("roles", rolesArray)
                .getAsMap();
    }
}
