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

public interface PluginSettingsConstants {
    public static final String REQUEST_PLUGIN_SETTINGS_CONFIGURATION = "go.plugin-settings.get-configuration";
    public static final String REQUEST_PLUGIN_SETTINGS_VIEW = "go.plugin-settings.get-view";
    public static final String REQUEST_VALIDATE_PLUGIN_SETTINGS = "go.plugin-settings.validate-configuration";
    public static final String REQUEST_NOTIFY_PLUGIN_SETTINGS_CHANGE = "go.plugin-settings.plugin-settings-changed";
}
