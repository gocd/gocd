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

package com.thoughtworks.go.server.ui.plugins;

import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

@Deprecated
public class PackageRepositoryPluginInfo extends NewPluginInfo {
    private final PluggableInstanceSettings packageSettings;
    private final PluggableInstanceSettings repositorySettings;

    public PackageRepositoryPluginInfo(GoPluginDescriptor plugin, PluggableInstanceSettings packageSettings, PluggableInstanceSettings repoSettings) {
        super(plugin, PackageRepositoryExtension.EXTENSION_NAME);
        this.packageSettings = packageSettings;
        this.repositorySettings = repoSettings;
    }

    public PluggableInstanceSettings getPackageSettings() {
        return packageSettings;
    }

    public PluggableInstanceSettings getRepositorySettings() {
        return repositorySettings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PackageRepositoryPluginInfo that = (PackageRepositoryPluginInfo) o;

        if (packageSettings != null ? !packageSettings.equals(that.packageSettings) : that.packageSettings != null)
            return false;
        return repositorySettings != null ? repositorySettings.equals(that.repositorySettings) : that.repositorySettings == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (packageSettings != null ? packageSettings.hashCode() : 0);
        result = 31 * result + (repositorySettings != null ? repositorySettings.hashCode() : 0);
        return result;
    }
}
