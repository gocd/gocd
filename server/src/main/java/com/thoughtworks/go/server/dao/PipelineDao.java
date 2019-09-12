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
package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;

import java.util.List;

public interface PipelineDao extends PipelineInstanceModelDao {

    Pipeline save(Pipeline pipeline);

    Pipeline loadPipeline(long pipelineId);

    Pipeline mostRecentPipeline(String pipelineName);

    int count(String pipelineName);

    Pipeline pipelineByIdWithMods(long pipelineId);

    Pipeline pipelineWithMaterialsAndModsByBuildId(long buildId);

    void updateComment(String pipelineName, int pipelineCounter, String comment);

    @Deprecated
    // This is only used in test for legacy purpose.
    // Please call pipelineService.save(aPipeline) instead
    Pipeline saveWithStages(Pipeline pipeline);

    PipelineIdentifier mostRecentPipelineIdentifier(String pipelineName);

    Integer getCounterForPipeline(String name);

    void insertOrUpdatePipelineCounter(Pipeline pipeline, Integer lastCount, Integer newCount);

    Pipeline findPipelineByNameAndCounter(String pipelineName, int pipelineCounter);

    Pipeline findPipelineByNameAndLabel(String pipelineName, String pipelineLabel);

    Pipeline loadAssociations(Pipeline pipeline, String pipelineName);


    Pipeline findEarlierPipelineThatPassedForStage(String pipelineName, String stageName, double naturalOrder);

    int getPageNumberForCounter(String pipelineName, int pipelineCounter, int limit);

    BuildCause findBuildCauseOfPipelineByNameAndCounter(String name, int counter);

    StageIdentifier latestPassedStageIdentifier(long pipelineId, String stage);

    List<PipelineIdentifier> getPipelineInstancesTriggeredWithDependencyMaterial(String pipelineName, PipelineIdentifier revision);

    List<PipelineIdentifier> getPipelineInstancesTriggeredWithDependencyMaterial(String pipelineName, MaterialInstance materialInstance, String revision);

    PipelineRunIdInfo getOldestAndLatestPipelineId(String pipelineName);
}
