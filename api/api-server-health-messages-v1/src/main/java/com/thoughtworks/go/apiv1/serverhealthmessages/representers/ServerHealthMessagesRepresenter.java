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

package com.thoughtworks.go.apiv1.serverhealthmessages.representers;

import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.spark.RequestContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServerHealthMessagesRepresenter {

    public static List<Map<String, Object>> toJSON(Collection<ServerHealthState> healthStates, RequestContext requestContext) {
        return healthStates.stream().map(healthState -> toJSON(healthState, requestContext)).collect(Collectors.toList());
    }

    public static Map<String, Object> toJSON(ServerHealthState healthState, RequestContext requestContext) {
        return new JsonWriter(null)
                .add("message", healthState.getMessage())
                .add("detail", healthState.getDescription())
                .add("level", healthState.getLogLevel().toString())
                .add("time", healthState.getTimestamp())
                .getAsMap();
    }

}
