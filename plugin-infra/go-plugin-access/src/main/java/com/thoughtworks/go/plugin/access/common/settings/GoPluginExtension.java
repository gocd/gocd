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

import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;

import java.util.List;
import java.util.Map;

public interface GoPluginExtension {

    boolean canHandlePlugin(String pluginId);

    String extensionName();

    PluginSettingsConfiguration getPluginSettingsConfiguration(String pluginId);

    String getPluginSettingsView(String pluginId);

    ValidationResult validatePluginSettings(String pluginId, PluginSettingsConfiguration configuration);

    void notifyPluginSettingsChange(String pluginId, Map<String, String> pluginSettings);

    List<String> goSupportedVersions();
}
