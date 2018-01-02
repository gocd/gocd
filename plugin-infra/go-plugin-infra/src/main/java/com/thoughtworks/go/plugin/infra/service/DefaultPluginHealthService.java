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

package com.thoughtworks.go.plugin.infra.service;

import java.util.List;

import com.thoughtworks.go.plugin.infra.plugininfo.PluginRegistry;
import com.thoughtworks.go.plugin.internal.api.PluginHealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultPluginHealthService implements PluginHealthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPluginHealthService.class);

    private final PluginRegistry pluginRegistry;

    public DefaultPluginHealthService(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    @Override
    public void reportErrorAndInvalidate(String pluginId, List<String> message) {
        try {
            pluginRegistry.markPluginInvalid(pluginId, message);
        } catch (Exception e) {
            LOGGER.warn("[Plugin Health Service] Plugin with id '{}' tried to report health with message '{}' but Go is unaware of this plugin.", pluginId, message, e);
        }
    }

    @Override
    public void warning(String pluginId, String message) {
        LOGGER.warn("[Plugin Health Service] Plugin with id '{}' reported health with message '{}' ", pluginId, message);
    }
}
