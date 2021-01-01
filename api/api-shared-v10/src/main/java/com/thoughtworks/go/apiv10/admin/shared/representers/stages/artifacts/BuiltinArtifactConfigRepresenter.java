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
package com.thoughtworks.go.apiv10.admin.shared.representers.stages.artifacts;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.BuiltinArtifactConfig;

public class BuiltinArtifactConfigRepresenter {

    public static void toJSON(OutputWriter jsonWriter, BuiltinArtifactConfig builtinArtifactConfig) {
        jsonWriter.add("source", builtinArtifactConfig.getSource());
        jsonWriter.add("destination", builtinArtifactConfig.getDestination());
    }

    public static BuiltinArtifactConfig fromJSON(JsonReader jsonReader, BuiltinArtifactConfig builtinArtifactConfig) {
        jsonReader.readStringIfPresent("source", builtinArtifactConfig::setSource);
        jsonReader.readStringIfPresent("destination", builtinArtifactConfig::setDestination);
        return builtinArtifactConfig;
    }
}
