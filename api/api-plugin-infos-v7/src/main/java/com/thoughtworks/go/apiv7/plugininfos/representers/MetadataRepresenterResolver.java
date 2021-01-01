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
package com.thoughtworks.go.apiv7.plugininfos.representers;

import com.thoughtworks.go.apiv7.plugininfos.representers.metadata.MetadataRepresenter;
import com.thoughtworks.go.apiv7.plugininfos.representers.metadata.MetadataWithPartOfIdentityRepresenter;
import com.thoughtworks.go.apiv7.plugininfos.representers.metadata.PackageMaterialMetadataRepresenter;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.MetadataWithPartOfIdentity;
import com.thoughtworks.go.plugin.domain.common.PackageMaterialMetadata;

import java.util.HashMap;

public class MetadataRepresenterResolver {
    private static final HashMap<Class<? extends Metadata>, MetadataRepresenter> representerMap;

    static {
        representerMap = new HashMap<>();
        representerMap.put(PackageMaterialMetadata.class, new PackageMaterialMetadataRepresenter());
        representerMap.put(MetadataWithPartOfIdentity.class, new MetadataWithPartOfIdentityRepresenter());
    }

    public static MetadataRepresenter resolve(Metadata metadata) {
        return representerMap.getOrDefault(metadata.getClass(), new MetadataRepresenter());
    }
}
