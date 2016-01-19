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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

public class ElasticAgentExtensionConverterV1 implements ElasticAgentMessageConverter {

    public static final String VERSION = "1.0";

    @Override
    public String canHandlePluginRequestBody(Collection<String> resources, String environment) {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("resources", gson.toJsonTree(resources));
        jsonObject.addProperty("environment", environment);
        return gson.toJson(jsonObject);
    }

    @Override
    public String createAgentRequestBody(String autoRegisterKey, Collection<String> resources, String environment) {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("auto_register_key", autoRegisterKey);
        jsonObject.add("resources", gson.toJsonTree(resources));
        jsonObject.addProperty("environment", environment);
        return gson.toJson(jsonObject);

    }

    @Override
    public String shouldAssignWorkRequestBody(AgentMetadata elasticAgent, Collection<String> resources, String environment) {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("resources", gson.toJsonTree(resources));
        jsonObject.addProperty("environment", environment);
        jsonObject.add("agent", elasticAgent.toJSON());
        return gson.toJson(jsonObject);
    }

    @Override
    public String notifyAgentBusyRequestBody(AgentMetadata elasticAgent) {
        return new Gson().toJson(elasticAgent.toJSON());
    }

    @Override
    public String notifyAgentIdleRequestBody(AgentMetadata elasticAgent) {
        return notifyAgentBusyRequestBody(elasticAgent);
    }

    @Override
    public String serverPingRequestBody(Collection<AgentMetadata> metadata) {
        Gson gson = new Gson();
        JsonArray array = new JsonArray();
        for (AgentMetadata agentMetadata : metadata) {
            array.add(agentMetadata.toJSON());
        }
        return gson.toJson(array);
    }

    @Override
    public Collection<AgentMetadata> deleteAgentRequestBody(String requestBody) {
        Type AGENT_METADATA_LIST_TYPE = new TypeToken<ArrayList<AgentMetadata>>() {
        }.getType();
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        return gson.fromJson(requestBody, AGENT_METADATA_LIST_TYPE);
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

