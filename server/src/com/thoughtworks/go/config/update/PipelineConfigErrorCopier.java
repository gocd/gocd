/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.BaseCollection;

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

            for (JobConfig toJob : toStage.getJobs()) {
                JobConfig fromJob = fromStage.jobConfigByConfigName(toJob.name());
                copy(toJob, fromJob);
                copyCollectionErrors(fromJob.getTasks(), toJob.getTasks());
                copyCollectionErrors(fromJob.artifactPlans(), toJob.artifactPlans());
                copyCollectionErrors(fromJob.getTabs(), toJob.getTabs());
                copyCollectionErrors(fromJob.getProperties(), toJob.getProperties());
                copyCollectionErrors(fromJob.getVariables(), toJob.getVariables());
            }
        }
    }
}
