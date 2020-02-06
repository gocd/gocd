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

package com.thoughtworks.go.apiv1.permissions.representers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.spark.Routes;

import java.util.Map;

public class PermissionsRepresenter {
    public static void toJSON(OutputWriter outputWriter, Map<String, Object> permissions) {
        outputWriter.addLinks(linkWriter -> {
            linkWriter.addLink("self", Routes.Permissions.BASE);
            linkWriter.addAbsoluteLink("doc", Routes.Permissions.DOC);
        });

        outputWriter.add("permissions", new ObjectMapper().convertValue(permissions, JsonNode.class));
    }
}
