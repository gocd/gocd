/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class ElasticAgentExtensionConverterV1 implements ElasticAgentMessageConverter {

    public static final String VERSION = "1.0";

    @Override
    public String canHandlePluginRequestBody(List<String> resources, String environment) {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("resources", gson.toJsonTree(resources));
        jsonObject.addProperty("environment", environment);
        return gson.toJson(jsonObject);
    }

    @Override
    public String createAgentRequestBody(List<String> resources, String environment) {
        return canHandlePluginRequestBody(resources, environment);
    }

    @Override
    public String shouldAssignWorkRequestBody(String elasticAgentId, List<String> resources, String environment) {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("resources", gson.toJsonTree(resources));
        jsonObject.addProperty("environment", environment);
        jsonObject.addProperty("elastic_agent_id", elasticAgentId);
        return gson.toJson(jsonObject);
    }

    @Override
    public String notifyAgentBusyRequestBody(String elasticAgentId) {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("elastic_agent_id", elasticAgentId);
        return gson.toJson(jsonObject);
    }

    @Override
    public String notifyAgentIdleRequestBody(String elasticAgentId) {
        return notifyAgentBusyRequestBody(elasticAgentId);
    }

    @Override
    public String serverPingRequestBody(List<AgentMetadata> metadata) {
        Gson gson = new Gson();
        JsonArray array = new JsonArray();
        for (AgentMetadata agentMetadata : metadata) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("agent_id", agentMetadata.elasticAgentId());
            jsonObject.addProperty("agent_state", agentMetadata.agentState());
            jsonObject.addProperty("config_state", agentMetadata.configState());
            jsonObject.addProperty("build_state", agentMetadata.buildState());
            array.add(jsonObject);
        }
        return gson.toJson(array);
    }

    @Override
    public Boolean canHandlePluginResponseFromBody(String responseBody) {
        return new Gson().fromJson(responseBody, Boolean.class);
    }

    @Override
    public Boolean shouldAssignWorkResponseFromBody(String responseBody) {
        return canHandlePluginResponseFromBody(responseBody);
    }

}

