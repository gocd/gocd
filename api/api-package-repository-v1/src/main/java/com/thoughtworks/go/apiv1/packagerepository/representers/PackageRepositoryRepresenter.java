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
package com.thoughtworks.go.apiv1.packagerepository.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.representers.PluginConfigurationRepresenter;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.spark.Routes;

import java.util.HashMap;
import java.util.List;

public class PackageRepositoryRepresenter {
    public static void toJSON(OutputWriter outputWriter, PackageRepository packageRepository) {
        outputWriter.addLinks(linkWriter -> {
            linkWriter.addLink("self", Routes.PackageRepository.self(packageRepository.getId()));
            linkWriter.addAbsoluteLink("doc", Routes.PackageRepository.DOC);
            linkWriter.addLink("find", Routes.PackageRepository.FIND);
        });
        outputWriter.add("repo_id", packageRepository.getId());
        outputWriter.add("name", packageRepository.getName());
        outputWriter.addChild("plugin_metadata", childWriter -> PluginConfigurationRepresenter.toJSON(childWriter, packageRepository.getPluginConfiguration()));
        outputWriter.addChildList("configuration", configWriter -> ConfigurationPropertyRepresenter.toJSON(configWriter, packageRepository.getConfiguration()));
        outputWriter.addChild("_embedded", embeddedWriter -> embeddedWriter.addChildList("packages", packageWriter -> PackagesRepresenter.toJSON(packageWriter, packageRepository.getPackages())));

        if (!packageRepository.errors().isEmpty()) {
            outputWriter.addChild("errors", errorWriter -> {
                HashMap<String, String> errorMapping = new HashMap<>();
                new ErrorGetter(errorMapping).toJSON(errorWriter, packageRepository);
            });
        }
    }

    public static PackageRepository fromJSON(JsonReader jsonReader) {
        PackageRepository packageRepository = new PackageRepository();
        jsonReader.readStringIfPresent("repo_id", packageRepository::setId);
        jsonReader.readStringIfPresent("name", packageRepository::setName);

        PluginConfiguration pluginConfiguration = PluginConfigurationRepresenter.fromJSON(jsonReader.readJsonObject("plugin_metadata"));
        packageRepository.setPluginConfiguration(pluginConfiguration);

        List<ConfigurationProperty> configuration = ConfigurationPropertyRepresenter.fromJSONArray(jsonReader, "configuration");
        packageRepository.addConfigurations(configuration);
        return packageRepository;
    }
}
