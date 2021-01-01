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
package com.thoughtworks.go.plugin.domain.packagematerial;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

public class PackageMaterialPluginInfo extends PluginInfo {

    private final PluggableInstanceSettings repositorySettings;
    private final PluggableInstanceSettings packageSettings;

    public PackageMaterialPluginInfo(PluginDescriptor descriptor, PluggableInstanceSettings repositorySettings, PluggableInstanceSettings packageSettings, PluggableInstanceSettings pluginSettings) {
        super(descriptor, PluginConstants.PACKAGE_MATERIAL_EXTENSION, pluginSettings, null);
        this.repositorySettings = repositorySettings;
        this.packageSettings = packageSettings;
    }

    public PluggableInstanceSettings getRepositorySettings() {
        return repositorySettings;
    }

    public PluggableInstanceSettings getPackageSettings() {
        return packageSettings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PackageMaterialPluginInfo that = (PackageMaterialPluginInfo) o;

        if (repositorySettings != null ? !repositorySettings.equals(that.repositorySettings) : that.repositorySettings != null)
            return false;
        return packageSettings != null ? packageSettings.equals(that.packageSettings) : that.packageSettings == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (repositorySettings != null ? repositorySettings.hashCode() : 0);
        result = 31 * result + (packageSettings != null ? packageSettings.hashCode() : 0);
        return result;
    }
}
