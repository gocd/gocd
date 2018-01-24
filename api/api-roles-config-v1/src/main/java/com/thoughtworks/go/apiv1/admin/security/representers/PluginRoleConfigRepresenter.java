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


import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.spark.RequestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PluginRoleConfigRepresenter {

    public static Map toJSON(PluginRoleConfig pluginRoleConfig, RequestContext requestContext) {
        if (pluginRoleConfig == null) {
            return null;
        }

        return new JsonWriter(requestContext)
                .add("auth_config_id", pluginRoleConfig.getAuthConfigId())
                .add("properties", ConfigurationPropertyRepresenter.toJSON(pluginRoleConfig, requestContext))
                .getAsMap();
    }

    public static PluginRoleConfig fromJSON(JsonReader jsonReader) {
        PluginRoleConfig model = new PluginRoleConfig();
        if (jsonReader == null) {
            return model;
        }
        jsonReader.readStringIfPresent("auth_config_id", model::setAuthConfigId);
        jsonReader.readArrayIfPresent("properties", properties -> {
            List<ConfigurationProperty> configurationProperties = new ArrayList<>();
            properties.forEach(property -> {
                JsonReader configPropertyReader = new JsonReader(property.getAsJsonObject());
                ConfigurationProperty configurationProperty = ConfigurationPropertyRepresenter.fromJSON(configPropertyReader);
                configurationProperties.add(configurationProperty);
            });
            model.addConfigurations(configurationProperties);
        });
        return model;
    }

}
