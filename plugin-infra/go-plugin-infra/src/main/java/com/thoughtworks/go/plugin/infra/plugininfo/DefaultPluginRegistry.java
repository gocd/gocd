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

    @Override
    public List<GoPluginDescriptor> plugins() {
        return Collections.unmodifiableList(new ArrayList<>(idToDescriptorMap.values()));
    }

    public void loadPlugin(GoPluginBundleDescriptor bundleDescriptor) {
        for (GoPluginDescriptor pluginDescriptor : bundleDescriptor.descriptors()) {
            if (idToDescriptorMap.containsKey(pluginDescriptor.id().toLowerCase())) {
                throw new RuntimeException("Found another plugin with ID: " + pluginDescriptor.id());
            }
        }

        for (GoPluginDescriptor pluginDescriptor : bundleDescriptor.descriptors()) {
            idToDescriptorMap.put(pluginDescriptor.id().toLowerCase(), pluginDescriptor);
        }
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
        final GoPluginDescriptor firstPluginDescriptor = bundleDescriptor.descriptors().get(0);
        final GoPluginDescriptor pluginInBundle = getPluginByIdOrFileName(firstPluginDescriptor.id(), firstPluginDescriptor.fileName());

        if (pluginInBundle == null) {
            throw new RuntimeException("Could not find existing plugin with ID: " + firstPluginDescriptor.id());
        }

        final GoPluginBundleDescriptor bundleToRemove = pluginInBundle.bundleDescriptor();
        for (GoPluginDescriptor pluginDescriptor : bundleToRemove.descriptors()) {
            if (getPluginByIdOrFileName(pluginDescriptor.id(), pluginDescriptor.fileName()) == null) {
                throw new RuntimeException("Could not find existing plugin with ID: " + pluginDescriptor.id());
            }
        }

        for (GoPluginDescriptor pluginDescriptor : bundleToRemove.descriptors()) {
            idToDescriptorMap.remove(pluginDescriptor.id().toLowerCase());
        }

        return bundleToRemove;
    }

    public GoPluginDescriptor getPluginByIdOrFileName(String pluginID, final String fileName) {
        if (pluginID != null) {
            GoPluginDescriptor descriptor = idToDescriptorMap.get(pluginID.toLowerCase());
            if (descriptor != null) {
                return descriptor;
            }
        }

        return IterableUtils.find(idToDescriptorMap.values(), object -> object.fileName().equals(fileName));
    }

    public void markPluginInvalid(String pluginId, List<String> messages) {
        if (pluginId == null || (!idToDescriptorMap.containsKey(pluginId.toLowerCase()))) {
            throw new RuntimeException(String.format("Invalid plugin identifier '%s'", pluginId));
        }

        GoPluginBundleDescriptor goPluginBundleDescriptor = idToDescriptorMap.get(pluginId.toLowerCase()).bundleDescriptor();
        goPluginBundleDescriptor.markAsInvalid(messages, null);
    }

    @Override
    public GoPluginDescriptor getPlugin(String pluginId) {
        return idToDescriptorMap.get(pluginId.toLowerCase());
    }

    @Override
    public void clear() {
        idToDescriptorMap.clear();
    }
}
