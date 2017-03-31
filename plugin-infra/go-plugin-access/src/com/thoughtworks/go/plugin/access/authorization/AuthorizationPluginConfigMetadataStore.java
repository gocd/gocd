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

package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.plugin.access.PluginNotFoundException;
import com.thoughtworks.go.plugin.access.authorization.models.Capabilities;
import com.thoughtworks.go.plugin.access.authorization.models.SupportedAuthType;
import com.thoughtworks.go.plugin.access.common.PluginConfigMetadataStore;
import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.access.common.models.PluginProfileMetadataKeys;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConstants.EXTENSION_NAME;
import static java.lang.String.format;

@Component
public class AuthorizationPluginConfigMetadataStore extends PluginConfigMetadataStore<AuthorizationPluginRegistry> {

    private final Map<PluginDescriptor, Image> icons = new ConcurrentHashMap<>();
    private final Map<PluginDescriptor, Capabilities> capabilities = new ConcurrentHashMap<>();

    private final Map<PluginDescriptor, PluginProfileMetadataKeys> authConfigMetadata = new ConcurrentHashMap<>();
    private final Map<PluginDescriptor, String> authConfigView = new ConcurrentHashMap<>();

    private final Map<PluginDescriptor, PluginProfileMetadataKeys> roleMetadata = new ConcurrentHashMap<>();
    private final Map<PluginDescriptor, String> roleViews = new ConcurrentHashMap<>();

    @Override
    public void add(PluginDescriptor plugin, AuthorizationPluginRegistry extension) {
        try {
            Image icon = extension.getIcon(plugin.id());
            Capabilities capabilities = extension.getCapabilities(plugin.id());

            PluginProfileMetadataKeys authConfigMetadata = extension.getPluginConfigurationMetadata(plugin.id());
            String authConfigView = extension.getPluginConfigurationView(plugin.id());

            if (capabilities.canAuthorize()) {
                cacheRoleMetadataAndView(plugin, extension);
            }

            this.icons.put(plugin, icon);
            this.authConfigMetadata.put(plugin, authConfigMetadata);
            this.authConfigView.put(plugin, authConfigView);
            this.capabilities.put(plugin, capabilities);
        } catch (Exception e) {
            LOGGER.error("Failed to load plugin {}", plugin.id(), e);
            throw e;
        }
    }

    private void cacheRoleMetadataAndView(PluginDescriptor plugin, AuthorizationPluginRegistry extension) {
        try {
            PluginProfileMetadataKeys roleMetadata = extension.getRoleConfigurationMetadata(plugin.id());
            this.roleMetadata.put(plugin, roleMetadata);
        } catch (Exception e) {
            LOGGER.error("Failed to load role metadata for plugin {}", plugin.id(), e);
        }

        try {
            String roleConfigurationView = extension.getRoleConfigurationView(plugin.id());
            this.roleViews.put(plugin, roleConfigurationView);
        } catch (Exception e) {
            LOGGER.error("Failed to load role view for plugin {}", plugin.id(), e);
        }
    }

    @Override
    public void remove(PluginDescriptor plugin) {
        icons.remove(plugin);
        authConfigMetadata.remove(plugin);
        authConfigView.remove(plugin);
        roleMetadata.remove(plugin);
        roleViews.remove(plugin);
        capabilities.remove(plugin);
    }

    public Image getIcon(PluginDescriptor plugin) {
        return icons.get(plugin);
    }

    @Override
    public Collection<PluginDescriptor> getPlugins() {
        return new HashSet<>(icons.keySet());
    }

    public PluginProfileMetadataKeys getProfileMetadata(PluginDescriptor plugin) {
        return authConfigMetadata.get(plugin);
    }

    public String getProfileView(PluginDescriptor descriptor) {
        return authConfigView.get(descriptor);
    }

    public String getRoleView(PluginDescriptor descriptor) {
        return roleViews.get(descriptor);
    }

    public PluginProfileMetadataKeys getRoleMetadata(PluginDescriptor descriptor) {
        return roleMetadata.get(descriptor);
    }

    public Set<String> getPluginsThatSupportsPasswordBasedAuthentication() {
        return getPluginsThatSupports(SupportedAuthType.Password);
    }

    private Set<String> getPluginsThatSupports(SupportedAuthType supportedAuthType) {
        Set<String> plugins = new HashSet<>();
        for (Map.Entry<PluginDescriptor, Capabilities> entry : this.capabilities.entrySet()) {
            Capabilities capabilities = entry.getValue();
            if (capabilities.getSupportedAuthType() == supportedAuthType) {
                plugins.add(entry.getKey().id());
            }
        }
        return plugins;
    }

    public Set<String> getPluginsThatSupportsUserSearch() {
        Set<String> plugins = new HashSet<>();
        for (Map.Entry<PluginDescriptor, Capabilities> entry : this.capabilities.entrySet()) {
            Capabilities capabilities = entry.getValue();
            if (capabilities.canSearch()) {
                plugins.add(entry.getKey().id());
            }
        }
        return plugins;
    }

    public Set<String> getPluginsThatSupportsWebBasedAuthentication() {
        return getPluginsThatSupports(SupportedAuthType.Web);
    }

    public boolean canAuthorize(String pluginId) {
        final PluginDescriptor pluginDescriptor = pluginDescriptor(pluginId);
        return this.capabilities.get(pluginDescriptor).canAuthorize();
    }

    private PluginDescriptor pluginDescriptor(final String pluginId) {
        for (PluginDescriptor pluginDescriptor : this.capabilities.keySet()) {
            if (pluginDescriptor.id().equals(pluginId))
                return pluginDescriptor;
        }
        throw new PluginNotFoundException(format("Did not find '%s' plugin with id '%s'. Looks like plugin is missing", EXTENSION_NAME, pluginId));
    }

    public Capabilities getCapabilities(PluginDescriptor descriptor) {
        return this.capabilities.get(descriptor);
    }
}
