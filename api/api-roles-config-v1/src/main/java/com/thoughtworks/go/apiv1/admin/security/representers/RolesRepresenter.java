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


import com.google.common.collect.ImmutableMap;
import com.thoughtworks.go.api.representers.Link;
import com.thoughtworks.go.api.representers.RequestContext;
import com.thoughtworks.go.config.Role;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.thoughtworks.go.api.representers.RepresenterUtils.addLinks;

public class RolesRepresenter {

    public static List<Link> getLinks(RequestContext requestContext) {
        return Arrays.asList(
                requestContext.build("self", "/go/api/admin/security/roles"),
                new Link("doc", "https://api.gocd.org/#roles"),
                requestContext.build("find", "/go/api/admin/security/roles/:role_name"));
    }

    public static Map toJSON(List<Role> roles, RequestContext requestContext) {
        Map<String, Object> jsonObject = new LinkedHashMap<>();
        addLinks(getLinks(requestContext), jsonObject);
        List<Map> rolesArray = roles.stream()
                .map(role -> RoleRepresenter.toJSON(role, requestContext))
                .collect(Collectors.toList());
        jsonObject.put("_embedded", ImmutableMap.of("roles", rolesArray));
        return jsonObject;
    }
}
