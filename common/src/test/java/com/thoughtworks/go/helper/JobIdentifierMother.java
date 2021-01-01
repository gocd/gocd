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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;

public class JobIdentifierMother {

    public static JobIdentifier anyBuildIdentifier() {
        return new JobIdentifier("pipelineName", "lastgood", "stageName", "LATEST", "buildName", 1L);
    }

    public static JobIdentifier jobIdentifier(String pipelineName, Integer pipelineCounter, String stageName, String stageCounter, String jobName) {
        return new JobIdentifier(new StageIdentifier(pipelineName, pipelineCounter, "LABEL-1", stageName, stageCounter), jobName);
    }

    public static JobIdentifier jobIdentifier(String pipelineName) {
        return new JobIdentifier(pipelineName, "lastgood", "stageName", "LATEST", "buildName", 1L);
    }
}
