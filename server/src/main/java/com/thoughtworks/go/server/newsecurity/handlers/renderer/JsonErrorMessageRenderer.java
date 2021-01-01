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
package com.thoughtworks.go.server.newsecurity.handlers.renderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.thoughtworks.go.server.newsecurity.models.ContentTypeAwareResponse;
import org.springframework.http.MediaType;

class JsonErrorMessageRenderer extends ContentTypeAwareResponse {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    JsonErrorMessageRenderer(MediaType type) {
        super(type);
    }

    @Override
    public String getFormattedMessage(String message) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", message);
        return GSON.toJson(jsonObject);
    }
}

