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
package com.thoughtworks.go.domain.packagerepository;

import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.config.Configuration;

public class PackageRepositoryMother {
    public static PackageRepository create(String id) {
        PackageRepository packageRepository = create(id, "repo-" + id, "plugin", "1.0", new Configuration());
        packageRepository.setPackages(new Packages(PackageDefinitionMother.create("package-name", packageRepository)));
        return packageRepository;
    }

    public static PackageRepository create(String id, String name, String pluginId, String pluginVersion, Configuration configuration) {
        PackageRepository repository = new PackageRepository();
        repository.setId(id);
        repository.setName(name);
        repository.setPluginConfiguration(new PluginConfiguration(pluginId, pluginVersion));
        repository.setConfiguration(configuration);
        return repository;
    }
}
