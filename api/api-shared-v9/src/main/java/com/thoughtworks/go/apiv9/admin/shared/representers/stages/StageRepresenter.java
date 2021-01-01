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
package com.thoughtworks.go.apiv9.admin.shared.representers.stages;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.EnvironmentVariableRepresenter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.JobConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;

public class StageRepresenter {

    public static void toJSONArray(OutputListWriter stagesWriter, Collection<StageConfig> config) {
        config.forEach(stage -> {
            stagesWriter.addChild(stageWriter -> toJSON(stageWriter, stage));
        });
    }

    public static void toJSON(OutputWriter jsonWriter, StageConfig stageConfig) {
        if (!stageConfig.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                new ErrorGetter(new HashMap<>()).toJSON(errorWriter, stageConfig);
            });
        }

        jsonWriter.addIfNotNull("name", stageConfig.name());
        jsonWriter.add("fetch_materials", stageConfig.isFetchMaterials());
        jsonWriter.add("clean_working_directory", stageConfig.isCleanWorkingDir());
        jsonWriter.add("never_cleanup_artifacts", stageConfig.isArtifactCleanupProhibited());
        jsonWriter.addChild("approval", approvalWriter -> ApprovalRepresenter.toJSON(approvalWriter, stageConfig.getApproval()));
        jsonWriter.addChildList("environment_variables", envVarsWriter -> EnvironmentVariableRepresenter.toJSON(envVarsWriter, stageConfig.getVariables()));
        jsonWriter.addChildList("jobs", getJobs(stageConfig));

    }

    private static Consumer<OutputListWriter> getJobs(StageConfig stageConfig) {
        return jobsWriter -> {
            stageConfig.getJobs().forEach(job -> {
                jobsWriter.addChild(jobWriter -> JobRepresenter.toJSON(jobWriter, job));
            });
        };
    }

    public static StageConfig fromJSON(JsonReader jsonReader) {
        StageConfig stageConfig = new StageConfig();
        jsonReader.readCaseInsensitiveStringIfPresent("name", stageConfig::setName);
        jsonReader.optBoolean("fetch_materials").ifPresent(stageConfig::setFetchMaterials);
        jsonReader.optBoolean("clean_working_directory").ifPresent(stageConfig::setCleanWorkingDir);
        jsonReader.optBoolean("never_cleanup_artifacts").ifPresent(stageConfig::setArtifactCleanupProhibited);
        stageConfig.setVariables(EnvironmentVariableRepresenter.fromJSONArray(jsonReader));
        setJobs(jsonReader, stageConfig);
        jsonReader.optJsonObject("approval").ifPresent(approvalReader -> {
            stageConfig.setApproval(ApprovalRepresenter.fromJSON(approvalReader));
        });
        return stageConfig;
    }

    private static void setJobs(JsonReader jsonReader, StageConfig stageConfig) {
        JobConfigs allJobs = new JobConfigs();
        jsonReader.readArrayIfPresent("jobs", jobs -> {
            jobs.forEach(job -> {
                allJobs.add(JobRepresenter.fromJSON(new JsonReader(job.getAsJsonObject())));
            });
        });

        stageConfig.setJobs(allJobs);
    }
}
