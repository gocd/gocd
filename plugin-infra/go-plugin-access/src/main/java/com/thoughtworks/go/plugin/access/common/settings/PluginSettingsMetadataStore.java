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
package com.thoughtworks.go.plugin.access.common.settings;

import com.thoughtworks.go.plugin.access.config.PluginPreferenceStore;
import com.thoughtworks.go.plugin.api.config.Option;
import com.thoughtworks.go.plugin.api.config.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public final class PluginSettingsMetadataStore extends PluginPreferenceStore<PluginSettingsMetadata> {
    private static PluginSettingsMetadataStore metadataStore = new PluginSettingsMetadataStore();

    private PluginSettingsMetadataStore() {
    }

    public static PluginSettingsMetadataStore getInstance() {
        return metadataStore;
    }

    public void addMetadataFor(String pluginId, String extensionName, PluginSettingsConfiguration configuration, String settingsTemplate) {
        PluginSettingsMetadata pluginSettingsMetadata = new PluginSettingsMetadata(configuration, settingsTemplate, extensionName);
        setPreferenceFor(pluginId, pluginSettingsMetadata);
    }

    public PluginSettingsConfiguration configuration(String pluginId) {
        if (isEmpty(pluginId) || !hasPreferenceFor(pluginId)) {
            return null;
        }
        return preferenceFor(pluginId).getConfiguration();
    }

    public String template(String pluginId) {
        if (isEmpty(pluginId) || !hasPreferenceFor(pluginId)) {
            return null;
        }
        return preferenceFor(pluginId).getTemplate();
    }

    public void removeMetadataFor(String pluginId) {
        if (!isEmpty(pluginId)) {
            removePreferenceFor(pluginId);
        }
    }

    @Override
    public boolean hasOption(String pluginId, String key, Option<Boolean> option) {
        PluginSettingsConfiguration configurations = configuration(pluginId);
        if (configurations != null) {
            Property property = configurations.get(key);
            if (property != null) {
                return property.getOption(option);
            }
        }
        return option.getValue();
    }

    public List<String> getPlugins() {
        return new ArrayList<>(pluginIds());
    }

    public boolean hasPlugin(String pluginId) {
        return hasPreferenceFor(pluginId);
    }

    public String extensionWhichCanHandleSettings(String pluginId) {
        if (isEmpty(pluginId) || !hasPreferenceFor(pluginId)) {
            return null;
        }
        return preferenceFor(pluginId).extensionTypeWhichHandlesThis();
    }

    @Deprecated
    // only for test usage
    public void clear() {
        Set<String> plugins = pluginIds();
        for (String pluginId : plugins) {
            removePreferenceFor(pluginId);
        }
    }
}
