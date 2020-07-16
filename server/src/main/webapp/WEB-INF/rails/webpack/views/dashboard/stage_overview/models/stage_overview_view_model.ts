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

import Stream from "mithril/stream";
import {StageInstance} from "./stage_instance";
import {ApiRequestBuilder, ApiResult, ApiVersion} from "../../../../helpers/api_request_builder";
import {SparkRoutes} from "../../../../helpers/spark_routes";
import {Result, StageInstanceJSON, StageState} from "./types";
import {JobsViewModel} from "./jobs_view_model";

export class StageOverviewViewModel {
  private static STAGES_API_VERSION_HEADER = ApiVersion.latest;
  readonly stageInstance: Stream<StageInstance>;
  readonly jobsVM: Stream<JobsViewModel>;
  readonly lastPassedStageInstance: Stream<StageInstance | undefined>;

  constructor(stageInstance: StageInstance, lastPassedStageInstance?: StageInstance) {
    this.stageInstance = Stream(stageInstance);
    this.lastPassedStageInstance = Stream(lastPassedStageInstance);
    this.jobsVM = Stream(new JobsViewModel(stageInstance.jobs()))
  }


  /*
    ** Responsible to fetch all the required data to show stage overview. **
    *
    * Initializes:
    * - StageInstance: The current stage instance
    * - JobViewModel: The view model for all the jobs of the current stage
    *
    * Fetches:
    * - Current StageInstance - to show current stage and belonging job information
    * - Stage History (latest 10) - to get the information of the last passed stage. The last passed stage information
    *                               is required to compute ETA of individual jobs.
    * - Last Passed StageInstance - Unfortunately, the stage history API does not include the job state information of
    *                               each job, hence, after the last passed stage is identified using stage history api,
    *                               the stage instance is fetched to get the detailed information of all the jobs.
    *
    * Implementation Details:
    *                           Current Stage Status == COMPLETED
    *                             /                        \
    *                       NO  /                            \ YES
    *                         /                                \
    *                FETCH STAGE HISTORY             FETCH CURRENT STAGE INSTANCE
    *                       |                                    |
    *                       |                                    |
    *                FIND LAST PASSED STAGE                     STOP
    *                       |
    *                       |
    *           FETCH LAST PASSED STAGE INSTANCE
    *                       |
    *                       |
    *             FETCH CURRENT STAGE INSTANCE
    *                       |
    *                       |
    *                      STOP
    *
   */
  static initialize(pipelineName: string,
                    pipelineCounter: string | number,
                    stageName: string,
                    stageCounter: string | number,
                    stageStatus: StageState) {
    const isCompleted = stageStatus === StageState[StageState.Cancelled]
      || stageStatus === StageState[StageState.Passed]
      || stageStatus === StageState[StageState.Failed];

    if (isCompleted) {
      return this.initializeCurrentStageInstance(pipelineName, pipelineCounter, stageName, stageCounter);
    }

    return this.getLastPastStageHistoryInstance(pipelineName, stageName)
      .then((result) => {
        return result.do((successResponse) => {
          if (successResponse.body) {
            const passedStagePipelineName = successResponse.body.pipeline_name;
            const passedStagePipelineCounter = successResponse.body.pipeline_counter;
            const passedStageName = successResponse.body.name;
            const passedStageCounter = successResponse.body.counter;

            return this.fetchStageInstance(passedStagePipelineName, passedStagePipelineCounter, passedStageName, passedStageCounter)
              .then((result) => {
                return result.do((successResponse) => {
                  return this.initializeCurrentStageInstance(pipelineName, pipelineCounter, stageName, stageCounter, successResponse.body);
                });
              });
          }

          return this.initializeCurrentStageInstance(pipelineName, pipelineCounter, stageName, stageCounter)
        });
      });
  }

  private static initializeCurrentStageInstance(pipelineName: string, pipelineCounter: string | number,
                                                stageName: string, stageCounter: string | number,
                                                latestStageInstance?: StageInstance) {
    return this.fetchStageInstance(pipelineName, pipelineCounter, stageName, stageCounter)
      .then((result) => {
        return result.do((successResponse) => {
          return new StageOverviewViewModel(successResponse.body, latestStageInstance);
        });
      });
  }

  private static fetchStageInstance(pipelineName: string, pipelineCounter: string | number, stageName: string, stageCounter: string | number) {
    return ApiRequestBuilder.GET(SparkRoutes.getStageInstance(pipelineName, pipelineCounter, stageName, stageCounter), this.STAGES_API_VERSION_HEADER)
      .then((result: ApiResult<string>) => {
        return result.map((body) => StageInstance.fromJSON(JSON.parse(body) as StageInstanceJSON));
      });
  }

  private static getLastPastStageHistoryInstance(pipelineName: string, stageName: string) {
    return ApiRequestBuilder.GET(SparkRoutes.getStageHistory(pipelineName, stageName), this.STAGES_API_VERSION_HEADER)
      .then((result: ApiResult<string>) => {
        return result.map((body) => {
          const stageHistoryJSON = JSON.parse(body) as any;
          const passedInstances = (stageHistoryJSON.stages as any[]).filter(instance => instance.result === Result[Result.Passed]);
          return passedInstances.length > 0 ? passedInstances[0] : undefined;
        });
      });
  }
}
