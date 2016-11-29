/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.presentation.pipelinehistory.MatchedPipelineRevision;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;

import java.util.List;


public interface PipelineDao {

    Pipeline save(Pipeline pipeline);

    void updatePauseInfo(Pipeline pipeline);

    Pipeline loadPipeline(long pipelineId);

    Pipeline mostRecentPipeline(String pipelineName);

    Pipeline fullPipelineByBuildId(long buildId);

    Pipeline pipelineWithModsByStageId(String pipelineName, long stageId);

    PipelineInstanceModel loadHistory(long id);

    PipelineInstanceModels loadHistory(String pipelineName, int resultsPerPage, int start);

    int count(String pipelineName);

    Pipeline pipelineByBuildIdWithMods(long buildId);

    Pipeline pipelineByIdWithMods(long pipelineId);

    Pipeline pipelineWithMaterialsAndModsByBuildId(long buildId);

    void updateComment(String pipelineName, int pipelineCounter, String comment);

    @Deprecated
    // This is only used in test for legacy purpose.
    // Please call pipelineService.save(aPipeline) instead
    Pipeline saveWithStages(Pipeline pipeline);

    String mostRecentLabel(String pipelineName);

    PipelineInstanceModels loadHistory(String name, int count, String startingLabel);

    Integer getCounterForPipeline(String name);

    void insertOrUpdatePipelineCounter(Pipeline pipeline, Integer lastCount, Integer newCount);

    Pipeline findPipelineByNameAndCounter(String pipelineName, int pipelineCounter);

    Pipeline findPipelineByNameAndLabel(String pipelineName, String pipelineLabel);

    Pipeline loadAssociations(Pipeline pipeline, String pipelineName);

    PipelineInstanceModels loadHistory(String pipelineName);

    PipelineInstanceModel findPipelineHistoryByNameAndCounter(String pipelineName, int pipelineCounter);

    StageIdentifier findLastSuccessfulStageIdentifier(String pipelineName, String stageName);

    PipelineInstanceModels loadActivePipelines();

    Pipeline findPipelineByCounterOrLabel(String pipelineName, String counterOrLabel);

    PipelineDependencyGraphOld pipelineGraphByNameAndCounter(String pipelineName, int pipelineCounter);

    Pipeline findEarlierPipelineThatPassedForStage(String pipelineName, String stageName, double naturalOrder);

    PipelineInstanceModel loadHistoryByIdWithBuildCause(Long id);

    int getPageNumberForCounter(String pipelineName, int pipelineCounter, int limit);

    PipelineInstanceModels findMatchingPipelineInstances(String pipelineName, String pattern, int limit);

    BuildCause findBuildCauseOfPipelineByNameAndCounter(String name, int counter);

    StageIdentifier latestPassedStageIdentifier(long pipelineId, String stage);

    List<PipelineIdentifier> getPipelineInstancesTriggeredWithDependencyMaterial(String pipelineName, PipelineIdentifier revision);

    List<PipelineIdentifier> getPipelineInstancesTriggeredWithDependencyMaterial(String pipelineName, MaterialInstance materialInstance, String revision);

    List<MatchedPipelineRevision> findPipelineVSMByRevision(String revision, int limit);
}
