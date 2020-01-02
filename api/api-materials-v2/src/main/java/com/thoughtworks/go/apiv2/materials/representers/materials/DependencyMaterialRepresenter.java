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
package com.thoughtworks.go.apiv2.materials.representers.materials;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;

import java.util.function.Consumer;

public class DependencyMaterialRepresenter implements MaterialRepresenter<DependencyMaterialConfig> {

    @Override
    public Consumer<OutputWriter> toJSON(DependencyMaterialConfig dependencyMaterialConfig) {
        return jsonWriter -> {
            jsonWriter.add("pipeline", dependencyMaterialConfig.getPipelineName());
            jsonWriter.add("stage", dependencyMaterialConfig.getStageName());
            jsonWriter.add("name", dependencyMaterialConfig.getName());
            jsonWriter.add("auto_update", dependencyMaterialConfig.isAutoUpdate());
            jsonWriter.add("ignore_for_scheduling", dependencyMaterialConfig.ignoreForScheduling());
        };
    }

}
