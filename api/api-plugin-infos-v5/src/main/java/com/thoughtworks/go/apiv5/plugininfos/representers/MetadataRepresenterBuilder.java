/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv5.plugininfos.representers;

import com.thoughtworks.go.apiv5.plugininfos.representers.metadata.MetadataRepresenter;
import com.thoughtworks.go.apiv5.plugininfos.representers.metadata.MetadataWithPartOfIdentityRepresenter;
import com.thoughtworks.go.apiv5.plugininfos.representers.metadata.PackageMaterialMetadataRepresenter;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.MetadataWithPartOfIdentity;
import com.thoughtworks.go.plugin.domain.common.PackageMaterialMetadata;

public class MetadataRepresenterBuilder {
    public static MetadataRepresenter create(Metadata metadata) {
        if (metadata.getClass() == PackageMaterialMetadata.class) {
            return new PackageMaterialMetadataRepresenter();
        }

        if (metadata.getClass() == MetadataWithPartOfIdentity.class) {
            return new MetadataWithPartOfIdentityRepresenter();
        }

        return new MetadataRepresenter();
    }
}
