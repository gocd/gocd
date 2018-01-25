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

import com.google.common.collect.ImmutableMap;
import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.spark.RequestContext;

import java.util.Map;

public class PipelineDependencyModificationRepresenter {

    private static final String VSM_HREF = "/pipelines/value_stream_map/${pipeline_name}/${pipeline_counter}";
    private static final String STAGE_DETAIL_TAB_HREF = "/pipelines/${pipeline_name}/${pipeline_counter}/${stage_name}/${stage_counter}";

    public static Map toJSON(Modification model, RequestContext requestContext, DependencyMaterialRevision latestRevision) {
        return new JsonWriter(requestContext)

                .addLink("vsm", VSM_HREF, ImmutableMap.of(
                        "pipeline_name", latestRevision.getPipelineName(),
                        "pipeline_counter", latestRevision.getPipelineCounter()))
                .addLink("stage_details_url", STAGE_DETAIL_TAB_HREF, ImmutableMap.of(
                        "pipeline_name", latestRevision.getPipelineName(),
                        "pipeline_counter", latestRevision.getPipelineCounter(),
                        "stage_name", latestRevision.getStageName(),
                        "stage_counter", latestRevision.getStageCounter()))

                .addIfNotNull("revision", model.getRevision())
                .addIfNotNull("modified_time", model.getModifiedTime())
                .addIfNotNull("pipeline_label", model.getPipelineLabel()).getAsMap();
    }
}
