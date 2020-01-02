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
package com.thoughtworks.go.plugin.infra.plugininfo;

import java.util.List;

public interface PluginRegistry {
    List<GoPluginDescriptor> plugins();

    void markPluginInvalid(String pluginId, List<String> message);

    GoPluginDescriptor getPlugin(String pluginId);

    void clear();

    GoPluginBundleDescriptor getBundleDescriptor(String bundleSymbolicName);

    String pluginIDFor(String bundleSymbolicName, String extensionClassCannonicalName);

    List<String> extensionClassesIn(String bundleSymbolicName);
}
