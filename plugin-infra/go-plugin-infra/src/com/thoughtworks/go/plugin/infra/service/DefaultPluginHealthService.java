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

package com.thoughtworks.go.plugin.infra.service;

import java.util.List;

import com.thoughtworks.go.plugin.infra.plugininfo.PluginRegistry;
import org.apache.log4j.Logger;

public class DefaultPluginHealthService implements com.thoughtworks.go.plugin.internal.api.PluginHealthService {
    private static final Logger LOGGER = Logger.getLogger(DefaultPluginHealthService.class);

    private final PluginRegistry pluginRegistry;

    public DefaultPluginHealthService(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    @Override
    public void reportErrorAndInvalidate(String pluginId, List<String> message) {
        try {
            pluginRegistry.markPluginInvalid(pluginId, message);
        } catch (Exception e) {
            LOGGER.warn(String.format("[Plugin Health Service] Plugin with id '%s' tried to report health with message '%s' but Go is unaware of this plugin.", pluginId, message), e);
        }
    }

    @Override
    public void warning(String pluginId, String message) {
        LOGGER.warn(String.format("[Plugin Health Service] Plugin with id '%s' reported health with message '%s' ", pluginId, message));
    }
}
