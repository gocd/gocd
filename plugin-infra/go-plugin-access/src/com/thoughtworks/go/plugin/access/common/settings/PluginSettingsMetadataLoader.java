/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.String.format;

@Component
public class PluginSettingsMetadataLoader implements PluginChangeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginSettingsMetadataLoader.class);
    private final List<GoPluginExtension> extensions;
    private PluginSettingsMetadataStore metadataStore = PluginSettingsMetadataStore.getInstance();

    @Autowired
    public PluginSettingsMetadataLoader(List<GoPluginExtension> extensions, PluginManager pluginManager) {
        this.extensions = extensions;
        pluginManager.addPluginChangeListener(this, GoPlugin.class);
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
        try {
            PluginSettingsConfiguration configuration = null;
            String view = null;
            Boolean isTaskPlugin = false;

            for (GoPluginExtension extension : extensions) {
                if (extension.canHandlePlugin(pluginId)) {
                    if (extension.extensionName().equals(TaskExtension.TASK_EXTENSION)) {
                        isTaskPlugin = true;
                    } else {
                        configuration = extension.getPluginSettingsConfiguration(pluginId);
                        view = extension.getPluginSettingsView(pluginId);
                    }
                }
            }
            if ((configuration == null || view == null) && !isTaskPlugin) {
                throw new RuntimeException("Plugin Settings - Configuration or View cannot be null");
            }
            metadataStore.addMetadataFor(pluginId, configuration, view);
        } catch (Exception e) {
            LOGGER.error(format("Failed to fetch Plugin Settings metadata for plugin : %s", pluginId), e);
        }
    }
}