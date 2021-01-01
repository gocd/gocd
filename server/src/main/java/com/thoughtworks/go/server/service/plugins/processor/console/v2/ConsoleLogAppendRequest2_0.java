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
package com.thoughtworks.go.server.service.plugins.processor.console.v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.service.plugins.processor.console.ConsoleLogAppendRequest;

public class ConsoleLogAppendRequest2_0 implements ConsoleLogAppendRequest {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose
    @SerializedName("pipeline_name")
    private String pipelineName;

    @Expose
    @SerializedName("pipeline_counter")
    private Integer pipelineCounter;

    @Expose
    @SerializedName("stage_name")
    private String stageName;

    @Expose
    @SerializedName("stage_counter")
    private String stageCounter;

    @Expose
    @SerializedName("job_name")
    private String jobName;

    @Expose
    @SerializedName("text")
    private String text;

    public static ConsoleLogAppendRequest2_0 fromJSON(String json) {
        return GSON.fromJson(json, ConsoleLogAppendRequest2_0.class);
    }

    @Override
    public JobIdentifier jobIdentifier() {
        return new JobIdentifier(pipelineName, pipelineCounter, null, stageName, stageCounter, jobName);
    }

    @Override
    public String text() {
        return text;
    }
}
