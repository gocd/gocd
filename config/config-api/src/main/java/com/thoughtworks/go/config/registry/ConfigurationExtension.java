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
package com.thoughtworks.go.config.registry;

import java.util.Arrays;

/**
* The extension point for Go configuration to be registered by a plugin. Any plugin that wishes to contribute to the configuration needs to register one and only one instance of this class.
*/
public class ConfigurationExtension<T> {
    final PluginNamespace pluginNamespace;
    final ConfigTypeExtension<? extends T>[] implementations;

    public ConfigurationExtension(PluginNamespace pluginNamespace, ConfigTypeExtension<? extends T>... implementations) {
        this.pluginNamespace = pluginNamespace;
        this.implementations = implementations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConfigurationExtension configTag = (ConfigurationExtension) o;

        if (!Arrays.equals(implementations, configTag.implementations)) {
            return false;
        }
        if (pluginNamespace != null ? !pluginNamespace.equals(configTag.pluginNamespace) : configTag.pluginNamespace != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = pluginNamespace != null ? pluginNamespace.hashCode() : 0;
        result = 31 * result + (implementations != null ? Arrays.hashCode(implementations) : 0);
        return result;
    }

    @Override public String toString() {
        return "ConfigTagImpl{" +
                "pluginNamespace=" + pluginNamespace +
                ", implementations=" + (implementations == null ? null : Arrays.asList(implementations)) +
                '}';
    }
}
