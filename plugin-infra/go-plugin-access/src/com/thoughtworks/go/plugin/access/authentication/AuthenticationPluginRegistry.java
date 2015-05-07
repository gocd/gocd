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

package com.thoughtworks.go.plugin.access.authentication;

import com.thoughtworks.go.plugin.access.authentication.model.AuthenticationPluginConfiguration;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthenticationPluginRegistry {
    private final Map<String, AuthenticationPluginConfiguration> pluginToConfigurationMap = new ConcurrentHashMap<String, AuthenticationPluginConfiguration>();

    public void registerPlugin(String pluginId, AuthenticationPluginConfiguration configuration) {
        pluginToConfigurationMap.put(pluginId, configuration);
    }

    public void deregisterPlugin(String pluginId) {
        pluginToConfigurationMap.remove(pluginId);
    }

    public Set<String> getAuthenticationPlugins() {
        return pluginToConfigurationMap.keySet();
    }

    public String getDisplayNameFor(String pluginId) {
        return pluginToConfigurationMap.get(pluginId).getDisplayName();
    }

    public boolean supportsUserSearch(String pluginId) {
        return pluginToConfigurationMap.get(pluginId).supportsUserSearch();
    }

    @Deprecated
    public void clear() {
        pluginToConfigurationMap.clear();
    }
}
