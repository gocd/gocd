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
package com.thoughtworks.go.apiv1.compare.representers;

import com.thoughtworks.go.api.base.OutputLinkWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.spark.Routes;

import java.util.List;
import java.util.function.Consumer;

public class ComparisonRepresenter {
    public static void toJSON(OutputWriter outputWriter, String pipelineName, Integer fromCounter, Integer toCounter, boolean isBisect, List<MaterialRevision> materialRevisions) {
        outputWriter.addLinks(addLinks())
                .add("pipeline_name", pipelineName)
                .add("from_counter", fromCounter)
                .add("to_counter", toCounter)
                .add("is_bisect", isBisect)
                .addChildList("changes", revisionsWriter -> MaterialRevisionsRepresenter.toJSONArray(revisionsWriter, materialRevisions));
    }

    private static Consumer<OutputLinkWriter> addLinks() {
        return outputLinkWriter -> outputLinkWriter
                .addLink("self", Routes.CompareAPI.BASE)
                .addAbsoluteLink("doc", Routes.CompareAPI.DOC);
    }
}
