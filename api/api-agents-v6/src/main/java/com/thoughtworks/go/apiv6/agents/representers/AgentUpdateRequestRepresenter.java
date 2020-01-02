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
package com.thoughtworks.go.apiv6.agents.representers;

import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv6.agents.model.AgentUpdateRequest;

public class AgentUpdateRequestRepresenter extends UpdateRequestRepresenter {

    public static AgentUpdateRequest fromJSON(String requestBody) {
        final JsonReader reader = GsonTransformer.getInstance().jsonReaderFrom(requestBody);

        return new AgentUpdateRequest(
                reader.optString("hostname").orElse(null),
                toTriState(reader.optString("agent_config_state").orElse(null)),
                getEnvironments(reader),
                getResources(reader)
        );
    }

    private static String getEnvironments(JsonReader reader) {
        return readArrayOrString(reader, "environments");
    }

    private static String getResources(JsonReader reader) {
        return readArrayOrString(reader, "resources");
    }

    private static String readArrayOrString(JsonReader reader, String propertyName) {
        if (reader.hasJsonArray(propertyName)) {
            return toCommaSeparatedString(reader.optJsonArray(propertyName).orElse(null));
        }

        return reader.optString(propertyName).orElse(null);
    }
}
