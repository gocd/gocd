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
package com.thoughtworks.go.server.service.plugins.processor.console.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.service.plugins.processor.console.ConsoleLogAppendRequest;

public class ConsoleLogAppendRequest1_0 implements ConsoleLogAppendRequest {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose
    private String pipelineName;

    @Expose
    private Integer pipelineCounter;

    @Expose
    private String stageName;

    @Expose
    private String stageCounter;

    @Expose
    private String jobName;

    @Expose
    private String text;

    public static ConsoleLogAppendRequest1_0 fromJSON(String json) {
        return GSON.fromJson(json, ConsoleLogAppendRequest1_0.class);
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
