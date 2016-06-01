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

package com.thoughtworks.go.config.parser;

import java.util.HashMap;
import java.util.Map;

public class ConfigReferenceElements {
    private Map<String, Map<String, Object>> collectionRegistry;

    public ConfigReferenceElements() {
        collectionRegistry = new HashMap<>();
    }

    public void add(String collectionName, String referenceElementId, Object referenceElement) {
        if (!collectionRegistry.containsKey(collectionName)) {
            collectionRegistry.put(collectionName, new HashMap<String, Object>());
        }
        collectionRegistry.get(collectionName).put(referenceElementId, referenceElement);
    }

    public Object get(String collection, String referenceElementId) {
        if (collectionRegistry.containsKey(collection)) {
            return collectionRegistry.get(collection).get(referenceElementId);
        }
        return null;
    }
}
