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
package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

import java.util.List;

public interface PluginManager {
    List<GoPluginDescriptor> plugins();

    boolean isPluginLoaded(String pluginId);

    GoPluginDescriptor getPluginDescriptorFor(String pluginId);

    void startInfrastructure(boolean shouldPoll);

    void stopInfrastructure();

    void addPluginChangeListener(PluginChangeListener pluginChangeListener);

    PluginPostLoadHook addPluginPostLoadHook(PluginPostLoadHook pluginPostLoadHook);

    GoPluginApiResponse submitTo(String pluginId, String extensionType, GoPluginApiRequest apiRequest);

    boolean isPluginOfType(String extension, String pluginId);

    String resolveExtensionVersion(String pluginId, String extensionType, List<String> goSupportedExtensionVersions);

    List<String> getRequiredExtensionVersionsByPlugin(String pluginId, String extensionType);

}
