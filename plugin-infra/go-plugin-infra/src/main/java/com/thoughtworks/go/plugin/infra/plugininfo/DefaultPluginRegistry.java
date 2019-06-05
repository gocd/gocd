/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.infra.plugininfo;

import org.apache.commons.collections4.IterableUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class DefaultPluginRegistry implements PluginRegistry {
    protected ConcurrentMap<String, GoPluginDescriptor> idToDescriptorMap = new ConcurrentHashMap<>();
    protected ConcurrentMap<String, GoPluginBundleDescriptor> idToBundleDescriptorMap = new ConcurrentHashMap<>();

    @Override
    public List<GoPluginDescriptor> plugins() {
        return Collections.unmodifiableList(new ArrayList<>(idToDescriptorMap.values()));
    }

    public void loadPlugin(GoPluginBundleDescriptor bundleDescriptor) {
        if (containsKey(idToDescriptorMap, bundleDescriptor.id())) {
            throw new RuntimeException("Found another plugin with ID: " + bundleDescriptor.id());
        }
        idToDescriptorMap.put(bundleDescriptor.id(), bundleDescriptor.descriptor());
    }

    private boolean containsKey(Map<String, ?> map, String id) {
        for (String key : map.keySet()) {
            if (key.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    public GoPluginBundleDescriptor unloadPlugin(GoPluginBundleDescriptor bundleDescriptor) {
        GoPluginBundleDescriptor existingDescriptor = getPluginBundleByIdOrFileName(bundleDescriptor.id(), bundleDescriptor.fileName());
        if (existingDescriptor == null) {
            throw new RuntimeException("Could not find existing plugin with ID: " + bundleDescriptor.id());
        }
        return new GoPluginBundleDescriptor(idToDescriptorMap.remove(existingDescriptor.descriptor().id()));
    }

    public GoPluginDescriptor getPluginByIdOrFileName(String pluginID, final String fileName) {
        if (pluginID != null) {
            GoPluginDescriptor descriptor = idToDescriptorMap.get(pluginID);
            if (descriptor != null) {
                return descriptor;
            }
        }

        return IterableUtils.find(idToDescriptorMap.values(), object -> object.fileName().equals(fileName));
    }

    private GoPluginBundleDescriptor getPluginBundleByIdOrFileName(String bundleID, final String fileName) {
        final GoPluginDescriptor pluginDescriptor = getPluginByIdOrFileName(bundleID, fileName);
        return pluginDescriptor == null ? null : new GoPluginBundleDescriptor(pluginDescriptor);
    }

    public void markPluginInvalid(String pluginId, List<String> messages) {
        if (pluginId == null || (!containsKey(this.idToDescriptorMap, pluginId))) {
            throw new RuntimeException(String.format("Invalid plugin identifier '%s'", pluginId));
        }
        GoPluginDescriptor goPluginDescriptor = idToDescriptorMap.get(pluginId);
        if (goPluginDescriptor != null) {
            goPluginDescriptor.markAsInvalid(messages, null);
        }
    }

    @Override
    public GoPluginDescriptor getPlugin(String pluginId) {
        return idToDescriptorMap.get(pluginId);
    }

    @Override
    public void clear() {
        idToDescriptorMap.clear();
    }
}
