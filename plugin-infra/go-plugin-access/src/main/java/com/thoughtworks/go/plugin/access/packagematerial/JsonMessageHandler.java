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
package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;

public interface JsonMessageHandler {

    // repository related
    RepositoryConfiguration responseMessageForRepositoryConfiguration(String responseBody);

    String requestMessageForIsRepositoryConfigurationValid(RepositoryConfiguration repositoryConfiguration);

    ValidationResult responseMessageForIsRepositoryConfigurationValid(String responseBody);

    String requestMessageForCheckConnectionToRepository(RepositoryConfiguration repositoryConfiguration);

    Result responseMessageForCheckConnectionToRepository(String responseBody);


    //package related
    com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration responseMessageForPackageConfiguration(String responseBody);

    String requestMessageForIsPackageConfigurationValid(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration);

    ValidationResult responseMessageForIsPackageConfigurationValid(String responseBody);

    String requestMessageForCheckConnectionToPackage(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration);

    Result responseMessageForCheckConnectionToPackage(String responseBody);


    //revision related
    String requestMessageForLatestRevision(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration);

    PackageRevision responseMessageForLatestRevision(String responseBody);

    String requestMessageForLatestRevisionSince(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration, PackageRevision previousRevision);

    PackageRevision responseMessageForLatestRevisionSince(String responseBody);

}
