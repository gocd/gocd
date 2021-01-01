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
package com.thoughtworks.go.apiv3.rolesconfig.representers;


import com.google.gson.JsonParseException;
import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RoleConfig;
import com.thoughtworks.go.config.policy.Policy;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;
import java.util.function.Consumer;


public class RoleRepresenter {
    public static void toJSON(OutputWriter jsonWriter, Role role) {
        jsonWriter.addLinks(
                outputLinkWriter -> outputLinkWriter.addAbsoluteLink("doc", Routes.Roles.DOC)
                        .addLink("self", Routes.Roles.name(role.getName().toString()))
                        .addLink("find", Routes.Roles.find()))
                .add("name", role.getName().toString())
                .add("type", getRoleType(role))
                .addChildList("policy", policyToJSON(role.getPolicy()));

        if (role.hasErrors()) {
            jsonWriter.addChild("errors", errorWriter -> {
                new ErrorGetter(Collections.singletonMap("authConfigId", "auth_config_id")).toJSON(errorWriter, role);
            });
        }
        if (role instanceof RoleConfig) {
            jsonWriter.addChild("attributes", attributeWriter -> GoCDRoleConfigRepresenter.toJSON(attributeWriter, (RoleConfig) role));
        } else if (role instanceof PluginRoleConfig) {
            jsonWriter.addChild("attributes", attributeWriter -> PluginRoleConfigRepresenter.toJSON(attributeWriter, (PluginRoleConfig) role));
        }
    }

    private static Consumer<OutputListWriter> policyToJSON(Policy policy) {
        return listWriter -> {
            policy.stream().forEach(permission -> {
                listWriter.addChild(childItemWriter -> {
                    DirectiveRepresenter.toJSON(childItemWriter, permission);
                });
            });
        };
    }

    public static Role fromJSON(JsonReader jsonReader) {
        Role model;
        String type = jsonReader.optString("type").orElse("");

        if ("gocd".equals(type)) {
            model = GoCDRoleConfigRepresenter.fromJSON(jsonReader.readJsonObject("attributes"));
        } else if ("plugin".equals(type)) {
            model = PluginRoleConfigRepresenter.fromJSON(jsonReader.readJsonObject("attributes"));
        } else {
            throw new JsonParseException("Invalid role type '%s'. It has to be one of 'gocd' or 'plugin'");
        }

        model.setName(new CaseInsensitiveString(jsonReader.optString("name").orElse(null)));

        Policy directives = new Policy();
        jsonReader.readArrayIfPresent("policy", policy -> {
            policy.forEach(directive -> directives.add(DirectiveRepresenter.fromJSON(new JsonReader(directive.getAsJsonObject()))));
        });
        model.setPolicy(directives);

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
