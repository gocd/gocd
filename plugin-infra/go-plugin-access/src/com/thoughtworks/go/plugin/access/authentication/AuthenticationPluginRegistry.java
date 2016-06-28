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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Component
public class AuthenticationPluginRegistry {
    private final Map<String, AuthenticationPluginConfiguration> pluginToConfigurationMap = new ConcurrentHashMap<>();
    // all login page renders will require this set & hence a local cache
    private final Set<String> pluginsThatSupportsWebBasedAuthentication = new ConcurrentSkipListSet<>();
    // all API (curl) calls will require this set & hence a local cache
    private final Set<String> pluginsThatSupportsPasswordBasedAuthentication = new ConcurrentSkipListSet<>();

    public void registerPlugin(String pluginId, AuthenticationPluginConfiguration configuration) {
        pluginToConfigurationMap.put(pluginId, configuration);
        if (configuration.supportsWebBasedAuthentication()) {
            pluginsThatSupportsWebBasedAuthentication.add(pluginId);
        }
        if (configuration.supportsPasswordBasedAuthentication()) {
            pluginsThatSupportsPasswordBasedAuthentication.add(pluginId);
        }
    }

    public void deregisterPlugin(String pluginId) {
        pluginToConfigurationMap.remove(pluginId);
        pluginsThatSupportsWebBasedAuthentication.remove(pluginId);
        pluginsThatSupportsPasswordBasedAuthentication.remove(pluginId);
    }

    public Set<String> getAuthenticationPlugins() {
        return new HashSet<>(pluginToConfigurationMap.keySet());
    }

    public Set<String> getPluginsThatSupportsWebBasedAuthentication() {
        return Collections.unmodifiableSet(pluginsThatSupportsWebBasedAuthentication);
    }

    public Set<String> getPluginsThatSupportsPasswordBasedAuthentication() {
        return Collections.unmodifiableSet(pluginsThatSupportsPasswordBasedAuthentication);
    }

    public String getDisplayNameFor(String pluginId) {
        AuthenticationPluginConfiguration configuration = pluginToConfigurationMap.get(pluginId);
        return configuration == null ? null : configuration.getDisplayName();
    }

    public String getDisplayImageURLFor(String pluginId) {
        AuthenticationPluginConfiguration configuration = pluginToConfigurationMap.get(pluginId);
        return configuration == null ? null : configuration.getDisplayImageURL();
    }

    public boolean supportsPasswordBasedAuthentication(String pluginId) {
        AuthenticationPluginConfiguration configuration = pluginToConfigurationMap.get(pluginId);
        return configuration == null ? false : configuration.supportsPasswordBasedAuthentication();
    }

    @Deprecated
    public void clear() {
        pluginToConfigurationMap.clear();
    }
}
