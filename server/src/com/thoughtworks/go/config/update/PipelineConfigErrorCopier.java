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
    private static void copy(Validatable from, Validatable to){
        to.errors().addAll(from.errors());
    }

    private static void copyCollectionErrors(BaseCollection from, BaseCollection to){
        copy((Validatable) from, (Validatable) to);
        for (int i = 0; i < from.size(); i++) {
            copy((Validatable) from.get(i), (Validatable) to.get(i));
        }
    }

   static void copyErrors(PipelineConfig from, PipelineConfig to) {
        copy(from, to);
        copyCollectionErrors(from.materialConfigs(), to.materialConfigs());
        for (StageConfig fromStage : from.getStages()) {
            StageConfig toStage = to.findBy(fromStage.name());
            copy(fromStage, toStage);
            for (JobConfig fromJob : fromStage.getJobs()) {
                JobConfig toJob = toStage.jobConfigByConfigName(fromJob.name());
                copy(fromJob, toJob);
                copyCollectionErrors(fromJob.getTasks(), toJob.getTasks());
            }
        }
    }
}
