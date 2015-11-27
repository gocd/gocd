/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util;

import com.sdicons.json.model.JSONValue;
import com.sdicons.json.parser.JSONParser;
import com.thoughtworks.go.server.web.JsonRenderer;

import java.io.StringReader;

public class JsonUtils {
    public static JSONValue parseJsonValue(String json) {
        JSONParser parser = new JSONParser(new StringReader(json));
        try {
            return parser.nextValue();
        } catch (Exception e) {
            throw new RuntimeException("Error parsing json: [" + json + "]", e);
        }
    }

    public static JSONValue parseJsonValue(Object json) {
        return parseJsonValue(JsonRenderer.render(json));
    }

    public static JsonValue from(Object json) {
        return new JsonValue(JsonRenderer.render(json));
    }
}
