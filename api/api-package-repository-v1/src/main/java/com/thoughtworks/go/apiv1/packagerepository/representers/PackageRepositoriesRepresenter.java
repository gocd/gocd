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
package com.thoughtworks.go.apiv1.packagerepository.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.spark.Routes;

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl;

public class PackageRepositoriesRepresenter {
    public static void toJSON(OutputWriter outputWriter, PackageRepositories packageRepositories) {
        outputWriter.addLinks(outputLinkWriter -> {
            outputLinkWriter.addLink("self", Routes.PackageRepository.BASE);
            outputLinkWriter.addAbsoluteLink("doc", apiDocsUrl("#package-repositories"));
        });
        outputWriter.addEmbedded(embeddedWriter -> {
            embeddedWriter.addChildList("package_repositories", listWriter -> {
                packageRepositories.forEach(packageDefinition -> listWriter.addChild(childWriter -> PackageRepositoryRepresenter.toJSON(childWriter, packageDefinition)));
            });
        });
    }
}
