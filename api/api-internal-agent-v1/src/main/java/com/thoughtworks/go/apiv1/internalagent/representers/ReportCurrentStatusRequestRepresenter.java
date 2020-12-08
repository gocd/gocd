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

package com.thoughtworks.go.apiv1.internalagent.representers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.apiv1.internalagent.ReportCurrentStatusRequest;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;

import java.lang.reflect.Type;
import java.util.Map;

public class ReportCurrentStatusRequestRepresenter {
    private static final Gson gson = new Gson();

    public static ReportCurrentStatusRequest fromJSON(String request) {
        Type empMapType = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> map = gson.fromJson(request, empMapType);

        return new ReportCurrentStatusRequest(agentRuntimeInfo(map), jobIdentifier(map), jobState(map));
    }

    private static AgentRuntimeInfo agentRuntimeInfo(Map<String, Object> request) {
        return (AgentRuntimeInfo) request.get("agent_runtime_info");
    }

    private static JobIdentifier jobIdentifier(Map<String, Object> request) {
        return (JobIdentifier) request.get("job_identifier");
    }

    private static JobState jobState(Map<String, Object> request) {
        return (JobState) request.get("job_state");
    }
}
