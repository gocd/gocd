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

package com.thoughtworks.go.apiv6.shared.representers.stages;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.ArtifactConfig;
import com.thoughtworks.go.config.BuildArtifactConfig;
import com.thoughtworks.go.config.BuiltinArtifactConfig;
import com.thoughtworks.go.config.TestArtifactConfig;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;

import java.util.HashMap;

public class ArtifactRepresenter {
    public static void toJSON(OutputWriter jsonWriter, ArtifactConfig artifactConfig) {
        if (!artifactConfig.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                HashMap<String, String> errorMapping = new HashMap<>();
                errorMapping.put("src", "source");
                errorMapping.put("dest", "destination");

                new ErrorGetter(errorMapping).toJSON(errorWriter, artifactConfig);
            });
        }
        //TODO: Change this to accommodate external artifacts in pipeline config v6
        BuiltinArtifactConfig builtinArtifactConfig = (BuiltinArtifactConfig) artifactConfig;
        jsonWriter.add("source", builtinArtifactConfig.getSource());
        jsonWriter.add("destination", builtinArtifactConfig.getDestination());
        jsonWriter.add("type", builtinArtifactConfig.getArtifactType().name());
    }

    public static ArtifactConfig fromJSON(JsonReader jsonReader) {
        if (jsonReader == null) {
            return null;
        }
        String type = jsonReader.getString("type");
        BuiltinArtifactConfig artifactConfig;
        switch (type) {
            case "build":
                artifactConfig = new BuildArtifactConfig();
                break;
            case "test":
                artifactConfig = new TestArtifactConfig();
                break;
            default:
                //TODO: change this when support for external artifacts is introduced
                throw new UnprocessableEntityException(String.format("Invalid Artifact type: '%s'. It has to be one of %s.", type, String.join(",", "build", "test")));
        }
        jsonReader.readStringIfPresent("source", artifactConfig::setSource);
        jsonReader.readStringIfPresent("destination", artifactConfig::setDestination);
        return artifactConfig;
    }
}
