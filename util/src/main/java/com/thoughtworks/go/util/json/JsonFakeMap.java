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

package com.thoughtworks.go.util.json;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * ModelAndView object requires a Map. This will fake a map when all we care about is a list
 */
public class JsonFakeMap extends LinkedHashMap<String, Object> {
    private final Object json;

    public JsonFakeMap(Object json) {
        this.json = json;
        super.put("json", json);
    }

    public Object put(String s, Object json) {
        throw bomb("This is a fake map with a single list element");
    }

    public void putAll(Map map) {
        throw bomb("This is a fake map with a single list element");
    }

    public Object get(Object o) {
        if (json instanceof JsonAware){
            return ((JsonAware) json).toJson();
        }
        return json;
    }
}
