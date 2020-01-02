/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.domain.common;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Since a single plugin can have multiple extensions, this combines multiple
 * plugin info objects into one, representing a full plugin.
 */
public class CombinedPluginInfo extends LinkedHashSet<PluginInfo> {
    public CombinedPluginInfo() {
    }

    public CombinedPluginInfo(PluginInfo pluginInfo) {
        add(pluginInfo);
    }

    public CombinedPluginInfo(Collection<? extends PluginInfo> c) {
        super(c);
    }

    public List<String> extensionNames() {
        return stream().map(PluginInfo::getExtensionName).collect(toList());
    }

    public PluginDescriptor getDescriptor() {
        Iterator<PluginInfo> iterator = this.iterator();
        if (!iterator.hasNext()) {
            throw new RuntimeException("Cannot get descriptor. Could not find any plugin information.");
        }
        return iterator.next().getDescriptor();
    }

    public List<PluginInfo> getExtensionInfos() {
        return new ArrayList<>(this);
    }

    public PluginInfo extensionFor(String extensionName) {
        return stream().filter(pluginInfo -> extensionName.equals(pluginInfo.getExtensionName())).findFirst().orElse(null);
    }

    public Image getImage() {
        for (PluginInfo extensionInfo : this) {
            Image image = extensionInfo.getImage();
            if (image != null) {
                return image;
            }
        }

        return null;
    }
}
