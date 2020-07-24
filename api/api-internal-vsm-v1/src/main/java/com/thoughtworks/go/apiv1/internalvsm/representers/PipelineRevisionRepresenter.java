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

package com.thoughtworks.go.apiv1.internalvsm.representers;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.domain.valuestreammap.PipelineRevision;
import com.thoughtworks.go.domain.valuestreammap.Revision;
import com.thoughtworks.go.spark.Routes;

import java.util.List;

public class PipelineRevisionRepresenter {
    public static void toJSON(OutputListWriter outputListWriter, String pipelineName, List<Revision> revisions) {
        if (revisions == null) {
            return;
        }
        revisions.forEach((rev) -> {
            PipelineRevision pipelineRevision = (PipelineRevision) rev;
            outputListWriter.addChild(childWriter -> childWriter.add("label", pipelineRevision.getLabel())
                    .add("counter", pipelineRevision.getCounter())
                    .add("locator", Routes.InternalVsm.pipelineLocator(pipelineName, pipelineRevision.getCounter()))
                    .addChildList("stages", stagesWriter -> StageRepresenter.toJSON(stagesWriter, pipelineRevision.getStages(), pipelineName, pipelineRevision.getCounter())));
        });
    }
}
