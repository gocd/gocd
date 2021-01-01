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
package com.thoughtworks.go.server.service.plugins.processor.serverhealth.v1;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.server.service.plugins.processor.serverhealth.MessageHandlerForServerHealthRequestProcessor;
import com.thoughtworks.go.server.service.plugins.processor.serverhealth.PluginHealthMessage;

import java.util.List;

public class MessageHandlerForServerHealthRequestProcessorV1 implements MessageHandlerForServerHealthRequestProcessor {

    private final Gson gson;

    public MessageHandlerForServerHealthRequestProcessorV1() {
        gson = new Gson();
    }

    @Override
    public List<PluginHealthMessage> deserializeServerHealthMessages(String requestBody) {
        try {
            return gson.fromJson(requestBody, new TypeToken<List<PluginHealthMessage>>() {}.getType());
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize message from plugin: " + requestBody);
        }
    }
}
