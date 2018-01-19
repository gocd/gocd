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


import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.representers.Link;
import com.thoughtworks.go.api.representers.RequestContext;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RoleConfig;

import java.util.*;

import static com.thoughtworks.go.api.representers.RepresenterUtils.addLinks;
import static com.thoughtworks.go.api.util.HaltResponses.haltBecauseInvalidJSON;


public class RoleRepresenter {

    private static List<Link> getLinks(Role model, RequestContext requestContext) {
        return Arrays.asList(
                new Link("doc", "https://api.gocd.org/#roles"),
                requestContext.build("self", "/go/api/admin/security/roles/%s", model.getName()),
                requestContext.build("find", "/go/api/admin/security/roles/:role_name")
        );
    }

    public static Map toJSON(Role role, RequestContext requestContext) {
        if (role == null) return null;
        Map<String, Object> jsonObject = new LinkedHashMap<>();
        List<Link> links = getLinks(role, requestContext);
        addLinks(links, jsonObject);

        jsonObject.put("name", role.getName().toString());
        jsonObject.put("type", getRoleType(role));
        if (role.hasErrors()) {
            jsonObject.put("errors", new ErrorGetter(Collections.singletonMap("authConfigId", "auth_config_id"))
                    .apply(role, requestContext));
        }
        if (role instanceof RoleConfig) {
            jsonObject.put("attributes", GoCDRoleConfigRepresenter.toJSON((RoleConfig) role, requestContext));
        } else if (role instanceof PluginRoleConfig) {
            jsonObject.put("attributes", PluginRoleConfigRepresenter.toJSON((PluginRoleConfig) role, requestContext));
        }
        return jsonObject;
    }

    public static Role fromJSON(JsonReader jsonReader) {
        Role model;
        String type = jsonReader.optString("type").orElse("");

        if ("gocd".equals(type)) {
            model = GoCDRoleConfigRepresenter.fromJSON(jsonReader.readJsonObject("attributes"));
        } else if ("plugin".equals(type)) {
            model = PluginRoleConfigRepresenter.fromJSON(jsonReader.readJsonObject("attributes"));
        } else {
            throw haltBecauseInvalidJSON("Invalid role type %s. It has to be one of 'gocd' or 'plugin'");
        }

        model.setName(new CaseInsensitiveString(jsonReader.optString("name").orElse(null)));

        return model;
    }

    private static String getRoleType(Role role) {
        if (role instanceof RoleConfig) {
            return "gocd";
        } else {
            return "plugin";
        }
    }


}
