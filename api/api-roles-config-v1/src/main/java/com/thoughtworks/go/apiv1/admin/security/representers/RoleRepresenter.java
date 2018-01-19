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


import com.google.gson.JsonObject;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.Link;
import com.thoughtworks.go.api.representers.RequestContext;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RoleConfig;

import java.util.*;

import static com.thoughtworks.go.api.representers.RepresenterUtils.addLinks;


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

    public static Role fromJSON(JsonObject jsonObject, RequestContext requestContext) {
        Role model;
        String type = jsonObject.get("type").getAsString();

        if ("gocd".equals(type)) {
            model = GoCDRoleConfigRepresenter.fromJSON(jsonObject.get("attributes").getAsJsonObject());
        }
        else if ("plugin".equals(type)) {
            model = PluginRoleConfigRepresenter.fromJSON(jsonObject.get("attributes").getAsJsonObject());
        }
        else {
            throw new RuntimeException("Could not find any subclass for specified type. Possible values are: gocd,plugin");
        }

        if (jsonObject.has("name")) {
            model.setName(new CaseInsensitiveString(jsonObject.get("name").getAsString()));
        }

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
