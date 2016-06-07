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

package com.thoughtworks.go.server.service;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.domain.NullPlugin;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsMetadataStore;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.DefaultPluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.PluginDao;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.server.ui.PluginViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PluginService {
    private final List<GoPluginExtension> extensions;
    private final PluginDao pluginDao;
    private DefaultPluginManager pluginManager;

    @Autowired
    public PluginService(List<GoPluginExtension> extensions, PluginDao pluginDao, DefaultPluginManager pluginManager) {
        this.extensions = extensions;
        this.pluginDao = pluginDao;
        this.pluginManager = pluginManager;
    }

    public PluginSettings getPluginSettingsFor(String pluginId) {
        PluginSettings pluginSettings = new PluginSettings(pluginId);
        Plugin plugin = pluginDao.findPlugin(pluginId);
        if (plugin instanceof NullPlugin) {
            pluginSettings.populateSettingsMap(PluginSettingsMetadataStore.getInstance().configuration(pluginId));
        } else {
            pluginSettings.populateSettingsMap(plugin);
        }
        return pluginSettings;
    }

    public PluginSettings getPluginSettingsFor(String pluginId, Map<String, String> parameterMap) {
        PluginSettings pluginSettings = new PluginSettings(pluginId);
        pluginSettings.populateSettingsMap(parameterMap);
        return pluginSettings;
    }

    public void validatePluginSettingsFor(PluginSettings pluginSettings) {
        String pluginId = pluginSettings.getPluginId();
        PluginSettingsConfiguration configuration = pluginSettings.toPluginSettingsConfiguration();
        ValidationResult result = null;

        boolean anyExtensionSupportsPluginId = false;
        for (GoPluginExtension extension : extensions) {
            if (extension.canHandlePlugin(pluginId)) {
                result = extension.validatePluginSettings(pluginId, configuration);
                anyExtensionSupportsPluginId = true;
            }
        }
        if(!anyExtensionSupportsPluginId)
            throw new IllegalArgumentException(String.format(
                    "Plugin '%s' is not supported by any extension point",pluginId));

        if (!result.isSuccessful()) {
            for (ValidationError error : result.getErrors()) {
                pluginSettings.populateErrorMessageFor(error.getKey(), error.getMessage());
            }
        }
    }

    public void savePluginSettingsFor(PluginSettings pluginSettings) {
        Plugin plugin = pluginDao.findPlugin(pluginSettings.getPluginId());
        if (plugin instanceof NullPlugin) {
            plugin = new Plugin(pluginSettings.getPluginId(), null);
        }
        Map<String, String> settingsMap = pluginSettings.getSettingsAsKeyValuePair();
        plugin.setConfiguration(toJSON(settingsMap));
        pluginDao.saveOrUpdate(plugin);
    }

    public ArrayList<PluginViewModel> plugins(String type) {
        ArrayList<PluginViewModel> plugins = new ArrayList<>();

        List<GoPluginDescriptor> goPluginDescriptors = type == null ? this.pluginManager.plugins() : descriptorsFor(type);

        for(GoPluginDescriptor descriptor: goPluginDescriptors) {
            GoPluginIdentifier identifier = this.pluginManager.pluginFor(descriptor.id());
            if(identifier != null) {
                plugins.add(PluginBuilder.getByExtension(identifier.getExtension()).build(descriptor.id(), descriptor.about(), false));
            }
        }
        return plugins;
    }

    public PluginViewModel plugin(String pluginId) {
        GoPluginDescriptor descriptor = this.pluginManager.getPluginDescriptorFor(pluginId);
        GoPluginIdentifier identifier = this.pluginManager.pluginFor(pluginId);

        if(descriptor != null && identifier != null) {
            return PluginBuilder.getByExtension(identifier.getExtension()).build(descriptor.id(), descriptor.about(), true);
        }

        return null;
    }

    private List<GoPluginDescriptor> descriptorsFor(String type) {
        List<GoPluginDescriptor> descriptors = new ArrayList<>();

        for(GoPluginDescriptor descriptor: this.pluginManager.plugins()) {
            if (this.pluginManager.isPluginOfType(type, descriptor.id())) {
                descriptors.add(descriptor);
            }
        }
        return descriptors;
    }

    private String toJSON(Map<String, String> settingsMap) {
        return new GsonBuilder().serializeNulls().create().toJson(settingsMap);
    }
}
