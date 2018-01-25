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

package com.thoughtworks.go.apiv2.dashboard.representers;

import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.spark.RequestContext;

import java.util.Map;

import static java.util.stream.Collectors.toList;

public class MaterialRevisionRepresenter {

    public static Map toJSON(MaterialRevision model, RequestContext requestContext) {
        return new JsonWriter(requestContext)
                .add("material_type", model.getMaterialType())
                .add("material_name", model.getMaterialName())
                .add("changed", model.isChanged())
                .add("modifications", model.getModifications().stream().map(modification -> {
                    if ("Pipeline".equals(model.getMaterial().getTypeForDisplay())) {
                        //copied from ruby: not typesafe, can be improved
                        return PipelineDependencyModificationRepresenter.toJSON(modification, requestContext, (DependencyMaterialRevision) model.getRevision());
                    }
                    return ModificationRepresenter.toJSON(modification, requestContext, model.getMaterial());
                }).collect(toList())).getAsMap();
    }

}
