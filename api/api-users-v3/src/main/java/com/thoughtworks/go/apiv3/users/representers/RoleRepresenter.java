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
package com.thoughtworks.go.apiv3.users.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.Role;

public class RoleRepresenter {
    public static void toJSON(OutputWriter writer, Role role) {
        writer.add("name", role.getName().toString()).add("type", getRoleType(role));

        if (isaPluginRole(role)) {
            writer.addChild("attributes", attributeWriter -> attributeWriter.add("auth_config_id", ((PluginRoleConfig) role).getAuthConfigId()));
        }
    }

    private static boolean isaPluginRole(Role role) {
        return role instanceof PluginRoleConfig;
    }

    private static String getRoleType(Role role) {
        return (isaPluginRole(role)) ? "plugin" : "gocd";
    }
}
