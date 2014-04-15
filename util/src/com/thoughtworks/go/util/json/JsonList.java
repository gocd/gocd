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

import java.util.ArrayList;

import com.thoughtworks.go.server.web.JsonRenderer;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class JsonList extends ArrayList<Json> implements Json {
    public static JsonList jsonList(String... json) {
        JsonList jsonList = new JsonList();
        for (String jsonString : json) {
            jsonList.add(jsonString);
        }
        return jsonList;
    }

    public void renderTo(JsonRenderer renderer) {
        boolean first = true;
        renderer.append("[ ");
        for (Json json : this) {
            if (!first) {
                renderer.append(",");
            }
            json.renderTo(renderer);
            first = false;
        }
        renderer.append(" ]");
    }

    public boolean contains(Json json) {
        if (json instanceof JsonList) {
            return containsList((JsonList) json);
        }
        return false;
    }

    private boolean containsList(JsonList jsonList) {
        if (jsonList.size() < this.size()) {
            return containsPartialList(jsonList);
        }
        if (jsonList.size() != this.size()) {
            return false;
        }
        for (Json jsonElement : jsonList) {
            if (!containsElement(jsonElement)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsPartialList(JsonList jsonList) {
        for (Json jsonElement : jsonList) {
            if (!containsElement(jsonElement)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsElement(Json jsonElement) {
        for (Json listElement : this) {
            if (listElement.contains(jsonElement)) {
                return true;
            }
        }
        return false;
    }

    public boolean add(Json json) {
        return super.add(json);
    }

    public boolean add(String json) {
        return add(new JsonString(json));
    }

    public JsonMap getJsonMap(int index) {
        Json json = super.get(index);
        if (json instanceof JsonMap) {
            return (JsonMap) json;
        }
        throw bomb("Element at index " + index + " is not a JsonMap");
    }

    public String toString() {
        return "[JSONLIST:" + super.toString() + "]";
    }

    public Json toJson() {
        return this;
    }
}
