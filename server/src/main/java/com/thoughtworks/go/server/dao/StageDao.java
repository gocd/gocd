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

import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.presentation.pipelinehistory.StageHistoryPage;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.server.domain.JobDurationStrategy;
import com.thoughtworks.go.server.domain.StageIdentity;
import com.thoughtworks.go.server.util.Pagination;

import java.util.List;

public interface StageDao extends JobDurationStrategy {

    Stage mostRecentWithBuilds(String pipelineName, StageConfig stageConfig);

    Stage save(Pipeline pipeline, Stage stage);
    @Deprecated
    // This is only used in test for legacy purpose.
    // Please call pipelineService.save(aPipeline) instead
    Stage saveWithJobs(Pipeline pipeline, Stage stage);

    int getCount(String pipelineName, String stageName);

    Stages getStagesByPipelineId(long pipelineId);

    Stage stageById(long stageId);

    Stage getStageByBuild(long buildInstanceId);

    Stage mostRecentPassed(String pipelineName, String stageName);

    boolean isStageActive(String pipelineName, String stageName);

    Long getDurationOfLastSuccessfulOnAgent(String pipelineName, String stageName, JobInstance job);

    int getMaxStageOrder(long pipelineId);

    Integer getStageOrderInPipeline(long pipelineId, String stageName);

    void updateResult(Stage stage, StageResult result, String username);

    int getMaxStageCounter(long pipelineId, String stageName);

    int findLatestStageCounter(PipelineIdentifier pipelineIdentifier, String stageName);

    Stage findStageWithIdentifier(StageIdentifier stageIdentifier);

    Stage mostRecentCompleted(StageConfigIdentifier identifier);

    Stage mostRecentStage(StageConfigIdentifier identifier);

    List<JobInstance> mostRecentJobsForStage(String pipelineName, String stageName);

    List<StageFeedEntry> findAllCompletedStages(FeedModifier feedModifier, long id, int pageSize);

    List<StageFeedEntry> findCompletedStagesFor(String pipelineName, FeedModifier feedModifier, long transitionId, long pageSize);

    Stages getPassedStagesByName(String pipelineName, String stageName, int limit, int offset);

    List<StageAsDMR> getPassedStagesAfter(StageIdentifier stageIdentifier, int limit, int offset);

    Stages getAllRunsOfStageForPipelineInstance(String pipelineName, Integer pipelineCounter, String stageName);

    List<Stage> findStageHistoryForChart(String pipelineName, String stageName, int pageSize, int offset);

    StageHistoryPage findStageHistoryPage(Stage stageIdentifier, int pageSize);

    StageHistoryPage findStageHistoryPageByNumber(String pipelineName, String stageName, int pageNumber, int pageSize);

	StageInstanceModels findDetailedStageHistoryByOffset(String pipelineName, String stageName, Pagination pagination);

    List<StageIdentifier> findFailedStagesBetween(String pipelineName, String stageName, double fromNaturalOrder, double toNaturalOrder);

    void clearCachedAllStages(String pipelineName, int pipelineCounter, String stageName);

    Stages findAllStagesFor(String pipelineName, int counter);

    List<Stage> oldestStagesHavingArtifacts();

    void markArtifactsDeletedFor(Stage stage);

    void clearCachedStage(StageIdentifier stageIdentifier);

    int getTotalStageCountForChart(String pipelineName, String stageName);

    List<StageIdentity> findLatestStageInstances();
}
