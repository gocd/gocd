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

package com.thoughtworks.go.plugin.api.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for holding configuration properties for a package or repository
 */
@Deprecated
//Will be moved to internal scope
public class Configuration implements PluginPreference {
    private List<Property> properties = new ArrayList<>();

    /**
     * Adds given property as configuration
     *
     * @param property this is the property to be added to the configuration
     */
    public void add(Property property) {
        properties.add(property);
    }

    /**
     * Gets property for a given property key
     * @param key the key whose associated property to be returned
     * @return the property to which the specified key is mapped, or null if this property with given key not found
     */
    public Property get(String key) {
        for (Property property : properties) {
            if (property.getKey().equals(key)) {
                return property;
            }
        }
        return null;
    }

    /**
     * Returns number of properties in this configuration
     * @return number of properties in this configuration
     */
    public int size() {
        return properties.size();
    }

    /**
     * Returns copy of properties list
     * @return copy of properties list
     */
    public List<? extends  Property> list() {
        return properties;
    }
}

