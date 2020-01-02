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
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.spark.Routes;

import java.util.List;

public class RolesRepresenter {

    public static void toJSON(OutputWriter writer, List<Role> roles) {
        writer.addLinks(
            outputLinkWriter -> outputLinkWriter.addAbsoluteLink("doc", Routes.Roles.DOC)
                .addLink("find", Routes.Roles.find())
                .addLink("self", Routes.Roles.BASE))
            .addChild("_embedded", embeddedWriter -> embeddedWriter.addChildList("roles", rolesWriter -> roles.forEach(role -> rolesWriter.addChild(roleWriter -> RoleRepresenter.toJSON(roleWriter, role)))));
    }
}
