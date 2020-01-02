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
package com.thoughtworks.go.apiv1.pipelineoperations.representers;

import com.thoughtworks.go.api.representers.EnvironmentVariableRepresenter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.server.domain.MaterialForScheduling;
import com.thoughtworks.go.server.domain.PipelineScheduleOptions;

import java.util.ArrayList;
import java.util.List;

public class PipelineScheduleOptionsRepresenter {
    public static PipelineScheduleOptions fromJSON(JsonReader jsonReader) {
        PipelineScheduleOptions model = new PipelineScheduleOptions();
        model.shouldPerformMDUBeforeScheduling(jsonReader.optBoolean("update_materials_before_scheduling").orElse(Boolean.TRUE));
        jsonReader.readArrayIfPresent("materials", materials -> {
            List<MaterialForScheduling> materialsForScheduling = new ArrayList<>();
            materials.forEach(material -> materialsForScheduling.add(MaterialRevisionRepresenter.fromJSON(new JsonReader(material.getAsJsonObject()))));
            model.setMaterials(materialsForScheduling);
        });
        model.setEnvironmentVariables(EnvironmentVariableRepresenter.fromJSONArray(jsonReader));
        return model;
    }
}
