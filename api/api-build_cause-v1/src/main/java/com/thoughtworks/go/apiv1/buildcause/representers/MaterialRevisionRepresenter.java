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
package com.thoughtworks.go.apiv1.buildcause.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;

public class MaterialRevisionRepresenter {

    public static void toJSON(OutputWriter jsonOutputWriter, MaterialRevision model) {
        jsonOutputWriter
            .add("material_type", model.getMaterialType())
            .add("material_name", model.getMaterialName())
            .add("changed", model.isChanged())
            .addChildList("modifications", listWriter -> model.getModifications().forEach(modification -> listWriter.addChild(childWriter -> {
                if (model.getMaterial() instanceof DependencyMaterial) {
                    PipelineDependencyModificationRepresenter.toJSON(jsonOutputWriter, modification, (DependencyMaterialRevision) model.getRevision());
                } else {
                    ModificationRepresenter.toJSON(jsonOutputWriter, modification, model.getMaterial());
                }
            })));
    }
}
