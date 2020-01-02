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
package com.thoughtworks.go.plugin.infra;

public final class PluginManagerReference {
    private static final PluginManagerReference PLUGIN_MANAGER_PROVIDER = new PluginManagerReference();
    private PluginManager pluginManager;

    private PluginManagerReference() {
    }

    public static PluginManagerReference reference() {
        return PLUGIN_MANAGER_PROVIDER;
    }

    public PluginManager getPluginManager() {
        if (pluginManager != null) {
            return pluginManager;
        }
        throw new IllegalStateException("PluginManager reference is not set");
    }

    public void setPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }
}
