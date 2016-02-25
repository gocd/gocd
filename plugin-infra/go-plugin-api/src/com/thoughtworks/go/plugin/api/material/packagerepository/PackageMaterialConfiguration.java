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

package com.thoughtworks.go.plugin.api.material.packagerepository;

import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;

/**
 * PackageMaterialConfiguration encapsulates all the methods which will be used to capture the configurations related to the package and the repository.
 */
@Deprecated
public interface PackageMaterialConfiguration {

    /**
     * Gets the repository configuration.
     * @return  repository configuration
     */
    RepositoryConfiguration getRepositoryConfiguration();

    /**
     * Gets the package configuration.
     * @return  package configuration
     */
    PackageConfiguration getPackageConfiguration();

    /**
     * Checks if given repository configuration is valid.
     * @param repositoryConfiguration repository configuration on which validation should be performed
     * @return validation result
     */
    ValidationResult isRepositoryConfigurationValid(RepositoryConfiguration repositoryConfiguration);

    /**
     * Checks if given package and repository configuration is valid as a unit.
     * @param packageConfiguration package configuration on which validation should be performed
     * @param repositoryConfiguration repository configuration which contains the package configuration
     * @return validation result
     */
    ValidationResult isPackageConfigurationValid(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration);
}
