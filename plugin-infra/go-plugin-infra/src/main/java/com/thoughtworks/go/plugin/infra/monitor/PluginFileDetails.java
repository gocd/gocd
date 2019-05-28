/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.infra.monitor;

import java.io.File;

public class PluginFileDetails {
    private String pluginFileName;
    private File pluginFile;
    private boolean bundledPlugin;
    private long lastModified;

    public PluginFileDetails(File plugin, boolean bundledPlugin) {
        this.pluginFile = plugin;
        this.bundledPlugin = bundledPlugin;
        pluginFileName = plugin.getName();
        lastModified = plugin.lastModified();
    }

    public File file() {
        return pluginFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PluginFileDetails that = (PluginFileDetails) o;

        if (!pluginFileName.equals(that.pluginFileName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return pluginFileName.hashCode();
    }

    public boolean doesTimeStampDiffer(PluginFileDetails other) {
        return lastModified != other.lastModified;
    }

    public boolean isBundledPlugin() {
        return bundledPlugin;
    }
}
