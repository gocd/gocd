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
package com.thoughtworks.go.apiv7.agents.representers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.util.TriState;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

abstract class UpdateRequestRepresenter {
    static List<String> extractToList(Optional<JsonArray> arrayOptional) {
        final List<String> treeSet = new ArrayList<>();
        if (arrayOptional.isPresent()) {
            for (JsonElement jsonElement : arrayOptional.get()) {
                treeSet.add(jsonElement.getAsString());
            }
        }

        return treeSet;
    }

    static String toCommaSeparatedString(JsonArray jsonArray) {
        if (jsonArray == null) {
            return null;
        }
        final Set<String> list = new TreeSet<>();
        for (JsonElement jsonElement : jsonArray) {
            list.add(jsonElement.getAsString());
        }
        return String.join(",", list);
    }

    static TriState toTriState(String agentConfigState) {
        if (StringUtils.isBlank(agentConfigState)) {
            return TriState.UNSET;
        } else if (StringUtils.equalsIgnoreCase(agentConfigState, "enabled")) {
            return TriState.TRUE;
        } else if (StringUtils.equalsIgnoreCase(agentConfigState, "disabled")) {
            return TriState.FALSE;
        } else {
            throw HaltApiResponses.haltBecauseOfReason("The value of `agent_config_state` can be one of `Enabled`, `Disabled` or null.");
        }
    }
}
