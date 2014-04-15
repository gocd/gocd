/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.util.json;

import java.util.Map;

import com.thoughtworks.go.server.web.JsonRenderer;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * ModelAndView object requires a Map. This will fake a map when all we care about is a list
 */
public class JsonFakeMap extends JsonMap {
    private final Json json;

    public JsonFakeMap(Json json) {
        this.json = json;
        super.put("json", json);
    }

    public Json put(String s, Json json) {
        throw bomb("This is a fake map with a single list element");
    }

    public Json put(String s, String json) {
        throw bomb("This is a fake map with a single list element");
    }

    public void putAll(Map<? extends String, ? extends Json> map) {
        throw bomb("This is a fake map with a single list element");
    }

    public Json get(Object o) {
        return json;
    }

    public void renderTo(JsonRenderer renderer) {
        json.renderTo(renderer);
    }

//    public String asJsonString(GoRequestContext context) {
//        return json.asJsonString(context);
//    }
//
    public boolean contains(Json json) {
        return this.json.contains(json);
    }

    public String toString() {
        return json.toString();
    }

    public Json toJson() {
        return json;
    }
}
