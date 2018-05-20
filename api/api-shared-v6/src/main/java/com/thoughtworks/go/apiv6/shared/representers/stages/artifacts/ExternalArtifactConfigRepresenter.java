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

package com.thoughtworks.go.apiv6.shared.representers.stages.artifacts;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ConfigurationPropertyRepresenter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv6.shared.representers.stages.ConfigHelperOptions;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PluggableArtifactConfig;
import com.thoughtworks.go.domain.config.ConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

public class ExternalArtifactConfigRepresenter {

    public static void toJSON(OutputWriter jsonWriter, PluggableArtifactConfig pluggableArtifactConfig) {
        jsonWriter.add("artifact_id", pluggableArtifactConfig.getId());
        jsonWriter.add("store_id", pluggableArtifactConfig.getStoreId());
        jsonWriter.addChildList("configuration", configurationWriter -> ConfigurationPropertyRepresenter.toJSON(configurationWriter, pluggableArtifactConfig.getConfiguration()));
    }

    public static PluggableArtifactConfig fromJSON(JsonReader jsonReader, PluggableArtifactConfig pluggableArtifactConfig, ConfigHelperOptions options) {
        jsonReader.readStringIfPresent("artifact_id", pluggableArtifactConfig::setId);
        jsonReader.readStringIfPresent("store_id", pluggableArtifactConfig::setStoreId);
        CruiseConfig cruiseConfig = options.getCruiseConfig();
        jsonReader.readArrayIfPresent("configuration", properties -> {
            List<ConfigurationProperty> configurationProperties = new ArrayList<>();
            properties.forEach(property -> {
                JsonReader configPropertyReader = new JsonReader(property.getAsJsonObject());
                ConfigurationProperty configurationProperty = ConfigurationPropertyRepresenter.fromJSON(configPropertyReader);
                configurationProperties.add(configurationProperty);
            });
            pluggableArtifactConfig.addConfigurations(configurationProperties, cruiseConfig.getArtifactStores().find(pluggableArtifactConfig.getStoreId()));
        });
        return pluggableArtifactConfig;
    }
}
