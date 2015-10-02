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

package com.thoughtworks.go.plugin.access.config;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.thoughtworks.go.plugin.api.config.PluginPreference;

public abstract class PluginPreferenceStore<T extends PluginPreference> {
    private Map<String, T> pluginIdPreferenceMap = new ConcurrentHashMap<String, T>();

    public T preferenceFor(String pluginId) {
        return pluginIdPreferenceMap.get(pluginId);
    }

    public void setPreferenceFor(String pluginId, T preference) {
        validatePluginId(pluginId);
        validatePreference(preference);
        pluginIdPreferenceMap.put(pluginId, preference);
    }

    private void validatePreference(T preference) {
        if (preference == null) {
            throw new IllegalArgumentException("Invalid Preference. It cannot be null.");
        }

    }

    private void validatePluginId(String pluginId) {
        if (pluginId == null || pluginId.trim().length() == 0) {
            throw new IllegalArgumentException("Invalid Plugin Id. Its null or empty.");
        }
    }

    public T removePreferenceFor(String pluginId) {
        return pluginIdPreferenceMap.remove(pluginId);
    }

    public boolean hasPreferenceFor(String pluginId) {
        return pluginIdPreferenceMap.containsKey(pluginId);
    }

    public Set<String> pluginIds(){
        return pluginIdPreferenceMap.keySet();
    }

}
