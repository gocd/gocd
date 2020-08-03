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

import Stream from 'mithril/stream';
import {ApiRequestBuilder, ApiVersion} from "../../../../helpers/api_request_builder";
import {SparkRoutes} from "../../../../helpers/spark_routes";
import {JobJSON, Result} from "./types";

export class JobsViewModel {
  public readonly checkedState: Map<string, Stream<boolean>> = new Map();
  private readonly jobs: JobJSON[];

  constructor(jobs: JobJSON[]) {
    this.jobs = jobs;
    jobs.forEach(job => this.checkedState.set(job.name, Stream(false)));
  }

  getJobs(): JobJSON[] {
    return this.jobs;
  }

  buildingJobNames(): JobJSON[] {
    return this.getJobNamesByResult(Result.Unknown);
  }

  failedJobNames(): JobJSON[] {
    return this.getJobNamesByResult(Result.Failed).concat(this.getJobNamesByResult(Result.Cancelled));
  }

  passedJobNames(): JobJSON[] {
    return this.getJobNamesByResult(Result.Passed);
  }

  getCheckedJobNames(): string[] {
    const checkedJobNames: string[] = [];
    this.checkedState.forEach((value: Stream<boolean>, key: string) => {
      if (value() === true) {
        checkedJobNames.push(key);
      }
    });

    return checkedJobNames;
  }

  rerunFailedJobs(pipelineName: string, pipelineCounter: string | number, stageName: string, stageCounter: string | number) {
    return ApiRequestBuilder.POST(SparkRoutes.rerunFailedJobs(pipelineName, pipelineCounter, stageName, stageCounter), ApiVersion.latest);
  }

  rerunSelectedJobs(pipelineName: string, pipelineCounter: string | number, stageName: string, stageCounter: string | number) {
    const jobs = this.getCheckedJobNames();
    return ApiRequestBuilder.POST(SparkRoutes.rerunSelectedJobs(pipelineName, pipelineCounter, stageName, stageCounter),
      ApiVersion.latest, {payload: {jobs}});
  }

  // responsible to update the view model
  update(jobs: JobJSON[]) {
    this.jobs = jobs;
  }

  private getJobNamesByResult(result: Result) {
    return this.jobs.filter((j: JobJSON) => j.result === Result[result]);
  }
}
