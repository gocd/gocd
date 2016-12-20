/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.access.packagematerial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.thoughtworks.go.plugin.api.config.PluginPreference;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;

public class PackageConfigurations implements PluginPreference {
    private List<PackageConfiguration> packageConfigurations = new ArrayList<>();
    private RepositoryConfiguration repositoryConfiguration;
    private com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration;

    public PackageConfigurations() {
    }

    public PackageConfigurations(RepositoryConfiguration repositoryConfiguration) {
        this.repositoryConfiguration = repositoryConfiguration;
        for (Property property : repositoryConfiguration.list()) {
            packageConfigurations.add(new PackageConfiguration(property));
        }
    }

    public PackageConfigurations(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration) {
        this.packageConfiguration = packageConfiguration;
        for (Property property : packageConfiguration.list()) {
            packageConfigurations.add(new PackageConfiguration(property));
        }
    }

    public void add(PackageConfiguration packageConfiguration) {
        packageConfigurations.add(packageConfiguration);
    }

    public PackageConfiguration get(String key) {
        for (PackageConfiguration packageConfiguration : packageConfigurations) {
            if (packageConfiguration.getKey().equals(key)) {
                return packageConfiguration;
            }
        }
        return null;
    }

    public void addConfiguration(PackageConfiguration packageConfiguration) {
        packageConfigurations.add(packageConfiguration);
    }

    public int size() {
        return packageConfigurations.size();
    }

    public List<PackageConfiguration> list() {
        Collections.sort(packageConfigurations);
        return packageConfigurations;
    }

    public RepositoryConfiguration getRepositoryConfiguration() {
        return repositoryConfiguration;
    }

    public com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration getPackageConfiguration() {
        return packageConfiguration;
    }
}
