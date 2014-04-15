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

package com.thoughtworks.go.config;

import java.util.Map;

import com.thoughtworks.go.config.registry.ConfigElementRegistry;

/**
 * @understands some extra context about the config in progress e.g. plugin info
 */
public class ConfigContext {
    private ConfigElementRegistry registry;
    private final Map pluginAttributeMap;

    public ConfigContext(ConfigElementRegistry registry, Object pluginAttributeMap) {
        this.registry = registry;
        this.pluginAttributeMap = (Map) pluginAttributeMap;
    }

    public ConfigElementRegistry getRegistry() {
        return registry;
    }

    public Map getPluginAttributeMap() {
        return pluginAttributeMap;
    }

    public Map getTaskPluginAttributeMap() {
        return (Map) pluginAttributeMap.get("task");
    }
}
