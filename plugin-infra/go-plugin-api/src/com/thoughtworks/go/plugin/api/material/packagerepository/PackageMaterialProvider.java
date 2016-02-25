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

import com.thoughtworks.go.plugin.api.GoPluginApiMarker;

/**
 * PackageMaterialProvider interface is the starting point for package repository as material plugin.
 * It  provides
 * {@link com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialConfiguration} and
 * {@link com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialPoller}
 * which are two essential parts of package repository as material plugin
 */
@GoPluginApiMarker
@Deprecated
public interface PackageMaterialProvider {
    /**
     * Gets an instance of PackageMaterialConfiguration which provides configuration for package repository as material.
     * @return  instance of PackageMaterialConfiguration
     */
    PackageMaterialConfiguration getConfig();

    /**
     * Gets an instance of PackageMaterialPoller which communicates with package repository.
     * @return instance of PackageMaterialPoller
     */
    PackageMaterialPoller getPoller();
}
