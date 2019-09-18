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
    private final File pluginFile;
    private final boolean bundledPlugin;
    private final long lastModified;

    public PluginFileDetails(File pluginFile, boolean bundledPlugin) {
        this.pluginFile = pluginFile;
        this.bundledPlugin = bundledPlugin;
        this.lastModified = pluginFile.lastModified();
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

        if (!pluginFile.getName().equals(that.pluginFile.getName())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return pluginFile.getName().hashCode();
    }

    public boolean doesTimeStampDiffer(PluginFileDetails other) {
        return lastModified != other.lastModified;
    }

    public boolean isBundledPlugin() {
        return bundledPlugin;
    }
}
