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

package com.thoughtworks.go.apiv1.admin.security.representers;

import cd.go.jrepresenter.Link;
import cd.go.jrepresenter.LinksProvider;
import cd.go.jrepresenter.RequestContext;
import cd.go.jrepresenter.annotations.Collection;
import cd.go.jrepresenter.annotations.Property;
import cd.go.jrepresenter.annotations.Represents;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.domain.config.ConfigurationProperty;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Represents(value = PluginRoleConfig.class)
public interface PluginRoleConfigRepresenter {

    @Property(modelAttributeType = String.class)
    String authConfigId();

    @Collection(
            representer = ConfigurationPropertyRepresenter.class,
            getter = ConfigurationPropertyGetter.class,
            setter = ConfigurationPropertySetter.class,
            modelAttributeType = ConfigurationProperty.class)
    List<Map> properties();

    class RoleConfigLinksProvider implements LinksProvider<PluginRoleConfig> {
        private static final Link DOC = new Link("doc", "https://api.gocd.org/#roles");

        @Override
        public List<Link> getLinks(PluginRoleConfig model, RequestContext requestContext) {
            return Arrays.asList(
                    DOC,
                    requestContext.build("self", "/go/api/admin/security/roles/%s", model.getName()),
                    requestContext.build("find", "/go/api/admin/security/roles/:role_name")
            );
        }
    }

    class ConfigurationPropertyGetter implements Function<PluginRoleConfig, List<ConfigurationProperty>> {
        @Override
        public List<ConfigurationProperty> apply(PluginRoleConfig pluginRoleConfig) {
            return pluginRoleConfig;
        }
    }

    class ConfigurationPropertySetter implements BiConsumer<PluginRoleConfig, List<ConfigurationProperty>> {

        @Override
        public void accept(PluginRoleConfig pluginRoleConfig, List<ConfigurationProperty> configurationProperties) {
            pluginRoleConfig.addConfigurations(configurationProperties);
        }
    }

    class ConfigurationPropertySerializer implements Function<ConfigurationProperty, Map> {
        @Override
        public Map apply(ConfigurationProperty configurationProperty) {
            Map<String, String> json = new LinkedHashMap<>();
            json.put("key", configurationProperty.getConfigKeyName());
            json.put("value", configurationProperty.getValue());
            return json;
        }
    }
}
