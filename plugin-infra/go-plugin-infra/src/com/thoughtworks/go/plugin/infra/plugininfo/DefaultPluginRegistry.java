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

package com.thoughtworks.go.plugin.infra.plugininfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.springframework.stereotype.Component;

@Component
public class DefaultPluginRegistry implements PluginRegistry {
    private static final String PLUGIN_XML = "plugin.xml";
    protected ConcurrentMap<String, GoPluginDescriptor> idToDescriptorMap = new ConcurrentHashMap<>();

    @Override
    public List<GoPluginDescriptor> plugins() {
        return Collections.unmodifiableList(new ArrayList<>(idToDescriptorMap.values()));
    }

    public void loadPlugin(GoPluginDescriptor descriptor) {
        if (containsKey(descriptor.id())) {
            throw new RuntimeException("Found another plugin with ID: " + descriptor.id());
        }
        idToDescriptorMap.put(descriptor.id(), descriptor);
    }

    private boolean containsKey(String id) {
        for (String key : idToDescriptorMap.keySet()) {
            if (key.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    public GoPluginDescriptor unloadPlugin(GoPluginDescriptor descriptor) {
        GoPluginDescriptor existingDescriptor = getPluginByIdOrFileName(descriptor.id(), descriptor.fileName());
        if (existingDescriptor == null) {
            throw new RuntimeException("Could not find existing plugin with ID: " + descriptor.id());
        }
        return idToDescriptorMap.remove(existingDescriptor.id());
    }

    public void unloadAll() {
        idToDescriptorMap.clear();
    }

    public GoPluginDescriptor getPluginByIdOrFileName(String pluginID, final String fileName) {
        if (pluginID != null) {
            GoPluginDescriptor descriptor = idToDescriptorMap.get(pluginID);
            if (descriptor != null) {
                return descriptor;
            }
        }

        return (GoPluginDescriptor) CollectionUtils.find(idToDescriptorMap.values(), new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                GoPluginDescriptor descriptor = (GoPluginDescriptor) object;
                return descriptor.fileName().equals(fileName);
            }
        });
    }

    public void markPluginInvalid(String pluginId, List<String> messages) {
        if (pluginId == null || (!containsKey(pluginId))) {
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
