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
package com.thoughtworks.go.plugin.infra.service;

import java.util.List;
import java.util.Map;

import com.thoughtworks.go.plugin.infra.plugininfo.PluginRegistry;
import com.thoughtworks.go.plugin.internal.api.PluginRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultPluginRegistryService implements PluginRegistryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPluginRegistryService.class);

    private final PluginRegistry pluginRegistry;

    public DefaultPluginRegistryService(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    @Override
    public void reportErrorAndInvalidate(String bundleSymbolicName, List<String> messages) {
        try {
            pluginRegistry.markPluginInvalid(bundleSymbolicName, messages);
        } catch (Exception e) {
            LOGGER.warn("[Plugin Health Service] Plugin with id '{}' tried to report health with message '{}' but Go is unaware of this plugin.", bundleSymbolicName, messages, e);
        }
    }

    @Override
    public void reportWarning(String bundleSymbolicName, String message) {
        LOGGER.warn("[Plugin Health Service] Bundle with symbolic '{}' reported health with message '{}' ", bundleSymbolicName, message);
    }

    @Override
    public String getPluginIDOfFirstPluginInBundle(String bundleSymbolicName) {
        return pluginRegistry.getBundleDescriptor(bundleSymbolicName).descriptors().get(0).id();
    }

    @Override
    public String pluginIDFor(String bundleSymbolicName, String extensionClassCannonicalName) {
        return pluginRegistry.pluginIDFor(bundleSymbolicName, extensionClassCannonicalName);
    }

    @Override
    public List<String> extensionClassesInBundle(String bundleSymbolicName) {
        return pluginRegistry.extensionClassesIn(bundleSymbolicName);
    }
}
