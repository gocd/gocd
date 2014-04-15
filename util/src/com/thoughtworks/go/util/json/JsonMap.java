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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.server.web.JsonRenderer;
import org.springframework.context.MessageSourceResolvable;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ObjectUtil.nullSafeEquals;

public class JsonMap extends LinkedHashMap<String, Json> implements Json {

    public void renderTo(JsonRenderer renderer) {
        boolean first = true;
        renderer.append("{ ");
        for (Map.Entry<String, Json> jsonEntry : this.entrySet()) {
            if (!first) {
                renderer.append(",");
            }
            renderer.quote(jsonEntry.getKey());
            renderer.append(": ");
            jsonEntry.getValue().renderTo(renderer);
            first = false;
        }
        renderer.append(" }");
    }

    public boolean contains(Json json) {
        if (json instanceof JsonMap) {
            return containsMap((JsonMap) json);
        }
        return false;
    }

    private boolean containsMap(JsonMap jsonMap) {
        for (String key : jsonMap.keySet()) {
            if (!super.containsKey(key)) {
                return false;
            }
            if (!this.get(key).contains(jsonMap.get(key))) {
                return false;
            }
        }
        return true;
    }

    public Json put(String s, Json json) {
        return super.put(s, json);
    }

    public Json put(String s, String json) {
        return super.put(s, new JsonString(json));
    }


    public Json put(String s, MessageSourceResolvable resolvable) {
        return super.put(s, new JsonI18NResolvable(resolvable));
    }

    public Json put(String s, List<String> strings) {
        JsonList jsonList = new JsonList();
        for (String string : strings) {
            jsonList.add(string);
        }
        return super.put(s, jsonList);
    }

    public JsonString getJsonString(String key) {
        Json json = super.get(key);
        if (json instanceof JsonString) {
            return (JsonString) json;
        }
        throw bomb("Element at key '" + key + "' is not a JsonString");
    }

    public JsonList getJsonList(String key) {
        Json json = super.get(key);
        if (json instanceof JsonList) {
            return (JsonList) json;
        }
        throw bomb("Element at key '" + key + "' is not a JsonList");
    }

    public JsonMap getJsonMap(String key) {
        Json json = super.get(key);
        if (json instanceof JsonMap) {
            return (JsonMap) json;
        }
        throw bomb("Element at key '" + key + "' is not a JsonMap");
    }

    public String toString() {
        return "[JSONMAP:" + super.toString() + "]";
    }

    public void put(String s, long value) {
        put(s, Long.toString(value));
    }

    public boolean hasEntry(String key, String value) {
        return nullSafeEquals(new JsonString(value), getJsonString(key));
    }

    public Json toJson() {
        return this;
    }
}
