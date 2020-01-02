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
package com.thoughtworks.go.apiv1.packages.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.spark.Routes;

import java.util.HashMap;
import java.util.List;

public class PackageDefinitionRepresenter {
    public static void toJSON(OutputWriter outputWriter, PackageDefinition packageDefinition) {

        outputWriter.addLinks(linksWriter -> {
            linksWriter.addLink("self", Routes.Packages.self(packageDefinition.getId()));
            linksWriter.addAbsoluteLink("doc", Routes.Packages.DOC);
            linksWriter.addLink("find", Routes.Packages.FIND);
        });
        outputWriter.add("name", packageDefinition.getName());
        outputWriter.add("id", packageDefinition.getId());
        outputWriter.add("auto_update", packageDefinition.isAutoUpdate());
        outputWriter.addChild("package_repo", childWriter -> PackageRepositoryRepresenter.toJSON(childWriter, packageDefinition.getRepository()));
        outputWriter.addChildList("configuration", configWriter ->
                ConfigurationPropertyRepresenter.toJSON(configWriter, packageDefinition.getConfiguration()));
        if (!packageDefinition.errors().isEmpty()) {
            outputWriter.addChild("errors", errorWriter -> {
                HashMap<String, String> errorMapping = new HashMap<>();
                new ErrorGetter(errorMapping).toJSON(errorWriter, packageDefinition);
            });
        }
    }

    public static PackageDefinition fromJSON(JsonReader jsonReader) {
        PackageDefinition packageDefinition = new PackageDefinition();
        jsonReader.readStringIfPresent("id", packageDefinition::setId);
        jsonReader.readStringIfPresent("name", packageDefinition::setName);
        List<ConfigurationProperty> configuration = ConfigurationPropertyRepresenter.fromJSONArray(jsonReader, "configuration");
        packageDefinition.addConfigurations(configuration);
        packageDefinition.setAutoUpdate(jsonReader.getBoolean("auto_update"));
        PackageRepository packageRepo = PackageRepositoryRepresenter.fromJSON(jsonReader.readJsonObject("package_repo"));
        packageDefinition.setRepository(packageRepo);
        return packageDefinition;
    }
}
