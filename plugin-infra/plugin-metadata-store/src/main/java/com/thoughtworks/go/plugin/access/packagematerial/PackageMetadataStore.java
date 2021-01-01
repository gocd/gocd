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

public class PackageMetadataStore extends AbstractMetaDataStore {

    private static PackageMetadataStore packageMetadataStore = new PackageMetadataStore();

    private PackageMetadataStore() {
    }

    public static PackageMetadataStore getInstance() {
        return packageMetadataStore;
    }

    public com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration getPackageMetadata(String pluginId) {

        PackageConfigurations metadata = packageMetadataStore.getMetadata(pluginId);
        if (metadata != null) {
            return metadata.getPackageConfiguration();
        }
        return null;
    }
}
