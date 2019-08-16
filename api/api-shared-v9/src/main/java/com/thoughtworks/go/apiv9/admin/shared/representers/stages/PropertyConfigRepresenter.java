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

package com.thoughtworks.go.apiv9.admin.shared.representers.stages;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.ArtifactPropertiesConfig;
import com.thoughtworks.go.config.ArtifactPropertyConfig;

import java.util.HashMap;

public class PropertyConfigRepresenter {

    public static void toJSONArray(OutputListWriter propertiesWriter, ArtifactPropertiesConfig properties) {
        properties.forEach(artifactPropertyConfig -> {
            propertiesWriter.addChild(artifactPropertyWriter -> toJSON(artifactPropertyWriter, artifactPropertyConfig));
        });
    }

    public static void toJSON(OutputWriter jsonWriter, ArtifactPropertyConfig artifactPropertyConfig) {
        if (!artifactPropertyConfig.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                HashMap<String, String> errorMapping = new HashMap<>();
                errorMapping.put("src", "source");
                new ErrorGetter(errorMapping).toJSON(errorWriter, artifactPropertyConfig);
            });
        }

        jsonWriter.add("name", artifactPropertyConfig.getName());
        jsonWriter.add("source", artifactPropertyConfig.getSrc());
        jsonWriter.add("xpath", artifactPropertyConfig.getXpath());
    }

    public static ArtifactPropertiesConfig fromJSONArray(JsonReader jsonReader) {
        ArtifactPropertiesConfig artifactPropertyConfigs = new ArtifactPropertiesConfig();
        jsonReader.readArrayIfPresent("properties", properties -> {
            properties.forEach(property -> {
                artifactPropertyConfigs.add(PropertyConfigRepresenter.fromJSON(new JsonReader(property.getAsJsonObject())));
            });
        });
        return artifactPropertyConfigs;
    }

    public static ArtifactPropertyConfig fromJSON(JsonReader jsonReader) {
        ArtifactPropertyConfig artifactPropertyConfig = new ArtifactPropertyConfig();
        jsonReader.readStringIfPresent("name", artifactPropertyConfig::setName);
        jsonReader.readStringIfPresent("source", artifactPropertyConfig::setSrc);
        jsonReader.readStringIfPresent("xpath", artifactPropertyConfig::setXpath);

        return artifactPropertyConfig;
    }
}
