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
package com.thoughtworks.go.apiv2.compare.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.MaterialRevision;

import java.util.List;
import java.util.stream.Collectors;

public class MaterialRevisionsRepresenter {

    public static void toJSONArray(OutputListWriter outputWriter, List<MaterialRevision> revisions) {
        List<MaterialRevision> materialRevisions = revisions.stream().filter(revision -> !revision.isDependencyMaterialRevision()).collect(Collectors.toList());
        List<MaterialRevision> dependencyMaterialRevisions = revisions.stream().filter(MaterialRevision::isDependencyMaterialRevision).collect(Collectors.toList());

        materialRevisions.addAll(dependencyMaterialRevisions);
        materialRevisions.forEach(revision -> outputWriter.addChild(revisionWriter -> toJSON(revisionWriter, revision)));
    }

    private static void toJSON(OutputWriter outputWriter, MaterialRevision revision) {
        outputWriter.addChild("material", materialWriter -> MaterialRepresenter.toJSON(outputWriter, revision.getMaterial().config()));

        if (revision.isDependencyMaterialRevision()) {
            outputWriter.addChildList("revision", revisionWriter -> DependencyMaterialModificationRepresenter.toJSONArray(revisionWriter, revision.getModifications()));
        } else {
            outputWriter.addChildList("revision", revisionWriter -> MaterialModificationRepresenter.toJSONArray(revisionWriter, revision.getModifications()));
        }
    }
}
