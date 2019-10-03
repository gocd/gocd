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
package com.thoughtworks.go.apiv4.shared.representers.stages.artifacts;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;

import java.util.HashMap;

public class ArtifactRepresenter {

    public static void toJSON(OutputWriter jsonWriter, ArtifactTypeConfig artifactTypeConfig) {
        if (!artifactTypeConfig.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                HashMap<String, String> errorMapping = new HashMap<>();
                errorMapping.put("src", "source");
                errorMapping.put("dest", "destination");
                errorMapping.put("id", "artifact_id");
                errorMapping.put("storeId", "store_id");
                errorMapping.put("pluginId", "plugin_id");

                new ErrorGetter(errorMapping).toJSON(errorWriter, artifactTypeConfig);
            });
        }

        jsonWriter.add("type", artifactTypeConfig.getArtifactType().name());
        switch (artifactTypeConfig.getArtifactType()) {
            case test:
            case build:
                BuiltinArtifactConfigRepresenter.toJSON(jsonWriter, (BuiltinArtifactConfig) artifactTypeConfig);
                break;
            case external:
                ExternalArtifactConfigRepresenter.toJSON(jsonWriter, (PluggableArtifactConfig) artifactTypeConfig);
        }

    }

    public static ArtifactTypeConfig fromJSON(JsonReader jsonReader) {
        String type = jsonReader.getString("type");
        ArtifactTypeConfig artifactTypeConfig;
        switch (type) {
            case "build":
                artifactTypeConfig = BuiltinArtifactConfigRepresenter.fromJSON(jsonReader, new BuildArtifactConfig());
                break;
            case "test":
                artifactTypeConfig = BuiltinArtifactConfigRepresenter.fromJSON(jsonReader, new TestArtifactConfig());
                break;
            case "external":
                artifactTypeConfig = ExternalArtifactConfigRepresenter.fromJSON(jsonReader, new PluggableArtifactConfig());
                break;
            default:
                throw new UnprocessableEntityException(String.format("Invalid Artifact type: '%s'. It has to be one of %s.", type, String.join(",", "build", "test", "external")));
        }
        return artifactTypeConfig;
    }
}
