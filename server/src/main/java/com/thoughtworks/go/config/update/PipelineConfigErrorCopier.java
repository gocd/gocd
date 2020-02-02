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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.Task;

import java.util.List;

public class PipelineConfigErrorCopier {
    private static void copy(Validatable from, Validatable to) {
        if (from == null || to == null) return;
        to.errors().addAll(from.errors());
    }

    private static void copyCollectionErrors(BaseCollection from, BaseCollection to) {
        if (from == null || to == null) return;
        copy((Validatable) from, (Validatable) to);
        for (int i = 0; i < to.size(); i++) {
            copy((Validatable) from.get(i), (Validatable) to.get(i));
        }
    }

    static void copyErrors(PipelineConfig from, PipelineConfig to) {
        copy(from, to);
        copyCollectionErrors(from.materialConfigs(), to.materialConfigs());
        copyCollectionErrors(from.getVariables(), to.getVariables());
        copyCollectionErrors(from.getParams(), to.getParams());
        copy(from.getTrackingTool(), to.getTrackingTool());
        copy(from.getTimer(), to.getTimer());
        for (StageConfig toStage : to.getStages()) {
            StageConfig fromStage = from.findBy(toStage.name());
            copy(fromStage, toStage);
            copyCollectionErrors(fromStage.getVariables(), toStage.getVariables());
            copy(fromStage.getApproval(), toStage.getApproval());
            copyCollectionErrors(fromStage.getApproval().getAuthConfig(), toStage.getApproval().getAuthConfig());

            for (JobConfig toJob : toStage.getJobs()) {
                JobConfig fromJob = fromStage.jobConfigByConfigName(toJob.name());
                copy(fromJob, toJob);
                copyCollectionErrors(fromJob.getTasks(), toJob.getTasks());
                copyCollectionErrors(fromJob.artifactTypeConfigs(), toJob.artifactTypeConfigs());
                copyCollectionErrors(fromJob.getTabs(), toJob.getTabs());
                copyCollectionErrors(fromJob.getVariables(), toJob.getVariables());
                copyCollectionErrors(fromJob.resourceConfigs(), toJob.resourceConfigs());
                Tasks toTasks = toJob.getTasks();
                Tasks fromTasks = fromJob.getTasks();
                copyCollectionErrors(fromTasks, toTasks);
                for (int i = 0; i < toTasks.size(); i++) {
                    Task fromTask = fromTasks.get(i);
                    Task toTask = toTasks.get(i);
                    copy(fromTask, toTask);
                    copy(fromTask.cancelTask(), toTask.cancelTask());
                    copyCollectionErrors(fromTask.getConditions(), toTask.getConditions());
                    if (toTask instanceof ExecTask) {
                        copyCollectionErrors(((ExecTask) fromTask).getArgList(), ((ExecTask) toTask).getArgList());
                    }
                }
                List<PluggableArtifactConfig> toPluggableArtifactConfigs = toJob.artifactTypeConfigs().getPluggableArtifactConfigs();
                List<PluggableArtifactConfig> fromPluggableArtifactConfigs = fromJob.artifactTypeConfigs().getPluggableArtifactConfigs();
                for (int i = 0; i < toPluggableArtifactConfigs.size(); i++) {
                    PluggableArtifactConfig fromPluggableArtifactConfig = fromPluggableArtifactConfigs.get(i);
                    PluggableArtifactConfig toPluggableArtifactConfig = toPluggableArtifactConfigs.get(i);
                    copyCollectionErrors(fromPluggableArtifactConfig.getConfiguration(), toPluggableArtifactConfig.getConfiguration());
                }
            }
        }
    }
}
