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
package com.thoughtworks.go.apiv2.compare.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;

public class DependencyMaterialModificationRepresenter {

    public static void toJSONArray(OutputListWriter outputWriter, Modifications modifications) {
        modifications.forEach(modification -> outputWriter.addChild(modificationWriter -> modification(modification, modificationWriter)));
    }

    private static void modification(Modification modification, OutputWriter modificationWriter) {
        modificationWriter
                .add("revision", modification.getRevision())
                .add("pipeline_counter", modification.getPipelineLabel())
                .add("completed_at", modification.getModifiedTime());
    }
}
