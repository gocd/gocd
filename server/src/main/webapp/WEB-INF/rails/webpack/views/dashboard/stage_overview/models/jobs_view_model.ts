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

import {JobJSON, Result} from "./stage_instance";

export class JobsViewModel {
  private readonly __jobs: JobJSON[];

  constructor(jobs: JobJSON[]) {
    this.__jobs = jobs;
  }

  buildingJobNames(): string[] {
    return this.getJobNamesByResult(Result.Unknown);
  }

  failedJobNames(): string[] {
    return this.getJobNamesByResult(Result.Failed);
  }

  cancelledJobNames(): string[] {
    return this.getJobNamesByResult(Result.Cancelled);
  }

  passedJobNames(): string[] {
    return this.getJobNamesByResult(Result.Passed);
  }

  private getJobNamesByResult(result: Result) {
    return this.__jobs.filter((j: JobJSON) => j.result === Result[result]).map(j => j.name);
  }
}
