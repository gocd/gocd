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
package com.thoughtworks.go.server.service.plugins.processor.serverinfo.v1;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.server.service.plugins.processor.serverinfo.MessageHandlerForServerInfoRequestProcessor;

public class MessageHandlerForServerInfoRequestProcessor1_0 implements MessageHandlerForServerInfoRequestProcessor {
    private final Gson gson;

    public MessageHandlerForServerInfoRequestProcessor1_0() {
        gson = new Gson();
    }

    @Override
    public String serverInfoToJSON(ServerConfig serverConfig) {
        JsonObject object = new JsonObject();

        object.addProperty("server_id", serverConfig.getServerId());
        object.addProperty("site_url", serverConfig.getSiteUrl().getUrl());
        object.addProperty("secure_site_url", serverConfig.getSecureSiteUrl().getUrl());

        return gson.toJson(object);
    }
}
