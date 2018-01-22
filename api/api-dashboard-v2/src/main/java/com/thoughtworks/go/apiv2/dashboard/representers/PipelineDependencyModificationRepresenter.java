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
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.spark.Link;
import com.thoughtworks.go.spark.RequestContext;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.api.representers.RepresenterUtils.addIfNotNull;
import static com.thoughtworks.go.api.representers.RepresenterUtils.addLinks;

public class PipelineDependencyModificationRepresenter {

    private static final String VSM_HREF = "/pipelines/value_stream_map/${pipeline_name}/${pipeline_counter}";
    private static final String STAGE_DETAIL_TAB_HREF = "/pipelines/${pipeline_name}/${pipeline_counter}/${stage_name}/${stage_counter}";

    private static List<Link> getLinks(RequestContext requestContext, DependencyMaterialRevision latestRevision) {
        return Arrays.asList(
                requestContext.buildWithNamedArgs("vsm", VSM_HREF,
                        ImmutableMap.of(
                                "pipeline_name", latestRevision.getPipelineName(),
                                "pipeline_counter", latestRevision.getPipelineCounter())),
                requestContext.buildWithNamedArgs("stage_details_url", STAGE_DETAIL_TAB_HREF,
                        ImmutableMap.of(
                                "pipeline_name", latestRevision.getPipelineName(),
                                "pipeline_counter", latestRevision.getPipelineCounter(),
                                "stage_name", latestRevision.getStageName(),
                                "stage_counter", latestRevision.getStageCounter()))
        );
    }

    public static Map toJSON(Modification model, RequestContext requestContext, DependencyMaterialRevision latestRevision) {
        Map<String, Object> json = new LinkedHashMap<>();
        addLinks(getLinks(requestContext, latestRevision), json);
        addIfNotNull("revision", model.getRevision(), json);
        addIfNotNull("modified_time", model.getModifiedTime(), json);
        addIfNotNull("pipeline_label", model.getPipelineLabel(), json);
        return json;
    }
}
