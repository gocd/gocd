/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_CONFIGURATION;
import static com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_VIEW;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.PLUGGABLE_TASK_EXTENSION;

@Component
public class PluginSettingsMetadataLoader implements PluginChangeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginSettingsMetadataLoader.class);
    private final List<GoPluginExtension> extensions;
    private PluginSettingsMetadataStore metadataStore = PluginSettingsMetadataStore.getInstance();

    @Autowired
    public PluginSettingsMetadataLoader(List<GoPluginExtension> extensions, PluginManager pluginManager) {
        this.extensions = extensions.stream().filter(goPluginExtension -> !PLUGGABLE_TASK_EXTENSION.equals(goPluginExtension.extensionName())).collect(Collectors.toList());

        pluginManager.addPluginChangeListener(this);
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
        fetchPluginSettingsMetaData(pluginDescriptor);
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        metadataStore.removeMetadataFor(pluginDescriptor.id());
    }

    void fetchPluginSettingsMetaData(GoPluginDescriptor pluginDescriptor) {
        String pluginId = pluginDescriptor.id();
        List<ExtensionSettingsInfo> allMetadata = findSettingsAndViewOfAllExtensionsIn(pluginId);
        List<ExtensionSettingsInfo> validMetadata = allSettingsAndViewPairsWhichAreValid(allMetadata);

        if (validMetadata.size() == 0) {
            LOGGER.warn("Failed to fetch plugin settings metadata for plugin {}. Maybe the plugin does not implement plugin settings and view?", pluginId);
            LOGGER.warn("Plugin: {} - Metadata load info: {}", pluginId, allMetadata);
            LOGGER.warn("Not all plugins are required to implement the request above. This error may be safe to ignore.");
            return;
        }

        if (validMetadata.size() > 1) {
            throw new RuntimeException(String.format("Plugin with ID: %s has more than one extension which supports plugin settings. " +
                    "Only one extension should support it and respond to %s and %s.", pluginId, REQUEST_PLUGIN_SETTINGS_CONFIGURATION, REQUEST_PLUGIN_SETTINGS_VIEW));
        }

        ExtensionSettingsInfo extensionSettingsInfo = validMetadata.get(0);
        metadataStore.addMetadataFor(pluginId, extensionSettingsInfo.extensionName, extensionSettingsInfo.configuration, extensionSettingsInfo.viewTemplate);
    }

    private List<ExtensionSettingsInfo> findSettingsAndViewOfAllExtensionsIn(String pluginId) {
        try {
            return extensions.stream()
                    .filter(extension -> extension.canHandlePlugin(pluginId))
                    .map(extension -> {
                        try {
                            return new ExtensionSettingsInfo(extension.extensionName(), null, extension.getPluginSettingsConfiguration(pluginId), extension.getPluginSettingsView(pluginId));
                        } catch (Exception e) {
                            return new ExtensionSettingsInfo(extension.extensionName(), e.getMessage(), null, null);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<ExtensionSettingsInfo> allSettingsAndViewPairsWhichAreValid(List<ExtensionSettingsInfo> allMetadata) {
        return allMetadata.stream().filter(ExtensionSettingsInfo::settingsAndViewAreValid).collect(Collectors.toList());
    }

    private class ExtensionSettingsInfo {
        private final String extensionName;
        private final String errorMessage;
        private final PluginSettingsConfiguration configuration;
        private final String viewTemplate;

        ExtensionSettingsInfo(String extensionName, String errorMessage, PluginSettingsConfiguration configuration, String viewTemplate) {
            this.extensionName = extensionName;
            this.errorMessage = errorMessage;
            this.configuration = configuration;
            this.viewTemplate = viewTemplate;
        }

        boolean settingsAndViewAreValid() {
            return configuration != null && viewTemplate != null;
        }

        @Override
        public String toString() {
            return String.format("{extension='%s', configuration='%s', view='%s', error='%s'}", extensionName, configuration, viewTemplate, errorMessage);
        }
    }
}
