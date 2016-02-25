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

import com.thoughtworks.go.plugin.api.response.Result;

/**
 * PackageMaterialPoller encapsulates all methods which are required to communicate with package repository.
 */
@Deprecated
public interface PackageMaterialPoller {

    /**
     * Gets the latest modification for the given package and repository configuration
     * @param packageConfiguration package configuration for which latest modification should be fetched
     * @param repositoryConfiguration repository configuration which contains package configuration
     * @return PackageRevision for the latest modification
     */
    PackageRevision getLatestRevision(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration);

    /**
     * Gets the latest modification for the given package and repository configuration since later than the last known revision by GO.
     * @param packageConfiguration package configuration for which latest modification should be fetched
     * @param repositoryConfiguration repository configuration which contains package configuration
     * @param previouslyKnownRevision last known revision by GO
     * @return PackageRevision for the modification since last known revision by GO
     */
    PackageRevision latestModificationSince(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration, PackageRevision previouslyKnownRevision);

    /**
     * Checks if the repository with the given configuration is accessible.
     * @param repositoryConfiguration  repository configuration for which connectivity should be checked
     * @return Result of connection check
     */
    Result checkConnectionToRepository(RepositoryConfiguration repositoryConfiguration);

    /**
     * Checks if the package with the given configuration is available in the repository.
     * @param packageConfiguration package configuration for which connectivity should be checked
     * @param repositoryConfiguration repository configuration which contains package configuration
     * @return Result of connection check
     */
    Result checkConnectionToPackage(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration);
}
