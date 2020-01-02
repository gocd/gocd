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
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.apiv6.agents.model.AgentBulkUpdateRequest;
import com.thoughtworks.go.apiv6.agents.model.AgentBulkUpdateRequest.Operation;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AgentBulkUpdateRequestRepresenter extends UpdateRequestRepresenter {
    public static AgentBulkUpdateRequest fromJSON(String requestBody) {
        final JsonReader reader = GsonTransformer.getInstance().jsonReaderFrom(requestBody);
        final List<String> uuids = extractToList(reader.optJsonArray("uuids"));
        final String agentConfigState = reader.optString("agent_config_state").orElse(null);

        if (uuids.isEmpty()) {
            HaltApiResponses.haltBecauseOfReason("Must specify agent 'uuids' for bulk update.");
        }

        final AgentBulkUpdateRequest.Operations operations = toOperationsFromJSON(reader.optJsonObject("operations"));

        return new AgentBulkUpdateRequest(uuids, operations, toTriState(agentConfigState));
    }

    public static AgentBulkUpdateRequest.Operations toOperationsFromJSON(Optional<JsonReader> optionalReader) {
        if (optionalReader.isPresent()) {
            final JsonReader reader = optionalReader.get();
            return new AgentBulkUpdateRequest.Operations(
                    toOperationFromJSON(reader.optJsonObject("environments")),
                    toOperationFromJSON(reader.optJsonObject("resources"))
            );
        }
        return new AgentBulkUpdateRequest.Operations();
    }

    public static Operation toOperationFromJSON(Optional<JsonReader> optionalJsonReader) {
        if (optionalJsonReader.isPresent()) {
            JsonReader reader = optionalJsonReader.get();
            return new Operation(extractToList(reader.optJsonArray("add")), extractToList(reader.optJsonArray("remove")));
        }

        return new Operation(Collections.emptyList(), Collections.emptyList());
    }
}
