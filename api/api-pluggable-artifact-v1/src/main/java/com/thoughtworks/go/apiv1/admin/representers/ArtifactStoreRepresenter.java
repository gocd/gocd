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
import cd.go.jrepresenter.annotations.Errors;
import cd.go.jrepresenter.annotations.Property;
import cd.go.jrepresenter.annotations.Represents;
import cd.go.jrepresenter.util.TriConsumer;
import com.thoughtworks.go.api.ErrorGetter;
import com.thoughtworks.go.api.IfNoErrors;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.domain.config.ConfigurationProperty;

import java.util.*;
import java.util.function.BiFunction;

@Represents(value = ArtifactStore.class, linksProvider = ArtifactStoreRepresenter.ArtifactStoreLinksProvider.class)
public interface ArtifactStoreRepresenter {
    @Errors(getter = ArtifactStoreErrorGetter.class, skipRender = IfNoErrors.class)
    Map errors();

    @Property(modelAttributeType = String.class)
    String id();

    @Property(modelAttributeType = String.class)
    String pluginId();

    @Collection(
            representer = ConfigurationPropertyRepresenter.class,
            getter = ConfigurationPropertyGetter.class,
            setter = ConfigurationPropertySetter.class,
            modelAttributeType = ConfigurationProperty.class)
    List<Map> properties();

    class ArtifactStoreLinksProvider implements LinksProvider<ArtifactStore> {
        private static final Link DOC = new Link("doc", "https://api.gocd.org/#artifact-stores");

        @Override
        public List<Link> getLinks(ArtifactStore model, RequestContext requestContext) {
            return Arrays.asList(
                    DOC,
                    requestContext.build("self", "/go/api/admin/artifact_stores/%s", model.getId()),
                    requestContext.build("find", "/go/api/admin/artifact_stores/:storeId")
            );
        }
    }

    class ArtifactStoreErrorGetter extends ErrorGetter {

        public ArtifactStoreErrorGetter() {
            super(Collections.singletonMap("pluginId", "plugin_id"));
        }
    }

    class ConfigurationPropertyGetter implements BiFunction<ArtifactStore, RequestContext, List<ConfigurationProperty>> {
        @Override
        public List<ConfigurationProperty> apply(ArtifactStore artifactStore, RequestContext requestContext) {
            return artifactStore;
        }
    }

    class ConfigurationPropertySetter implements TriConsumer<ArtifactStore, List<ConfigurationProperty>, RequestContext> {

        @Override
        public void accept(ArtifactStore artifactStore, List<ConfigurationProperty> configurationProperties, RequestContext requestContext) {
            artifactStore.addConfigurations(configurationProperties);
        }
    }

    class ConfigurationPropertySerializer implements BiFunction<ConfigurationProperty, RequestContext, Map> {
        @Override
        public Map apply(ConfigurationProperty configurationProperty, RequestContext requestContext) {
            Map<String, String> json = new LinkedHashMap<>();
            json.put("key", configurationProperty.getConfigKeyName());
            json.put("value", configurationProperty.getValue());
            return json;
        }
    }
}
