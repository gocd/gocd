/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.admin.representers;

import cd.go.jrepresenter.Link;
import cd.go.jrepresenter.LinksProvider;
import cd.go.jrepresenter.RequestContext;
import cd.go.jrepresenter.annotations.Collection;
import cd.go.jrepresenter.annotations.Represents;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Represents(value = ArtifactStores.class, linksProvider = ArtifactStoresRepresenter.ArtifactStoresProvider.class)
public interface ArtifactStoresRepresenter {
    @Collection(modelAttributeType = ArtifactStore.class, embedded = true, representer = ArtifactStoreRepresenter.class)
    List<Map> artifactStores();

    class ArtifactStoresProvider implements LinksProvider<ArtifactStores> {
        @Override
        public List<Link> getLinks(ArtifactStores model, RequestContext requestContext) {
            return Arrays.asList(
                    requestContext.build("self", "/go/api/admin/artifact_stores"),
                    new Link("doc", "https://api.gocd.org/#artifact-stores"),
                    requestContext.build("find", "/go/api/admin/artifact_stores/:storeId"));
        }

    }
}
