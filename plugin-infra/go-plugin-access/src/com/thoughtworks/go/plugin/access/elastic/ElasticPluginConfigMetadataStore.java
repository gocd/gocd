/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic;

import com.thoughtworks.go.plugin.access.common.PluginConfigMetadataStore;
import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ElasticPluginConfigMetadataStore extends PluginConfigMetadataStore<ElasticAgentPluginRegistry> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticPluginConfigMetadataStore.class);

    private final Map<PluginDescriptor, Image> icons = new ConcurrentHashMap<>();
    private final Map<PluginDescriptor, PluginProfileMetadataKeys> profileMetadata = new ConcurrentHashMap<>();
    private final Map<PluginDescriptor, String> profileView = new ConcurrentHashMap<>();

    @Override
    public void add(PluginDescriptor plugin, ElasticAgentPluginRegistry extension) {
        try {
            Image icon = extension.getIcon(plugin.id());
            PluginProfileMetadataKeys profileMetadata = extension.getProfileMetadata(plugin.id());
            String profileView = extension.getProfileView(plugin.id());

            this.icons.put(plugin, icon);
            this.profileMetadata.put(plugin, profileMetadata);
            this.profileView.put(plugin, profileView);
        } catch (Exception e) {
            LOGGER.error("Failed to load plugin {}", plugin.id(), e);
            throw e;
        }
    }

    @Override
    public void remove(PluginDescriptor plugin) {
        icons.remove(plugin);
        profileMetadata.remove(plugin);
        profileView.remove(plugin);
    }

    @Override
    public Collection<PluginDescriptor> getPlugins() {
        return new HashSet<>(icons.keySet());
    }

    public Image getIcon(PluginDescriptor plugin) {
        return icons.get(plugin);
    }

    public PluginProfileMetadataKeys getProfileMetadata(PluginDescriptor plugin) {
        return profileMetadata.get(plugin);
    }

    public String getProfileView(PluginDescriptor descriptor) {
        return profileView.get(descriptor);
    }
}
