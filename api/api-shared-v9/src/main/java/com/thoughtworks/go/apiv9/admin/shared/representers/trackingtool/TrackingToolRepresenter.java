/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv9.admin.shared.representers.trackingtool;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;

import java.util.HashMap;

public class TrackingToolRepresenter {

    public static void toJSON(OutputWriter jsonWriter, PipelineConfig pipelineConfig) {
        HashMap<String, String> mapping = new HashMap<>();
        mapping.put("projectIdentifier", "project_identifier");
        mapping.put("baseUrl", "base_url");
        mapping.put("link", "url_pattern");
        if (pipelineConfig.getTrackingTool() != null) {
            TrackingTool trackingTool = pipelineConfig.getTrackingTool();
            if (!trackingTool.errors().isEmpty()) {
                jsonWriter.addChild("errors", errorWriter -> {
                    new ErrorGetter(mapping).toJSON(errorWriter, trackingTool);
                });
            }
            jsonWriter.add("type", "generic");
            jsonWriter.addChild("attributes", attributeWriter -> ExternalTrackingToolRepresenter.toJSON(attributeWriter, trackingTool));
        }

    }

    public static Object fromJSON(JsonReader jsonReader) {
        String type = jsonReader.getString("type");
        JsonReader attributes = jsonReader.readJsonObject("attributes");
        if ("generic".equals(type)) {
            return ExternalTrackingToolRepresenter.fromJSON(attributes);
        }
        throw new UnprocessableEntityException(String.format("Invalid Tracking tool type '%s'. It has to be one of '%s'.", type, String.join(", ", "generic")));
    }
}
