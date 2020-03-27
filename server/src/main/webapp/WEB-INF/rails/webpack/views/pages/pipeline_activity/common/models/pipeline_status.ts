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

import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import Stream from "mithril/stream";

export interface PipelineStatusJSON {
  paused: boolean;
  paused_cause: string;
  paused_by: string;
  locked: boolean;
  schedulable: boolean;
}

export class PipelineStatus {
  private static API_VERSION_HEADER = ApiVersion.v1;

  readonly pipelineName: Stream<string>;
  readonly isPaused: Stream<boolean>;
  readonly pausedBy: Stream<string | undefined>;
  readonly pausedCause: Stream<string | undefined>;

  constructor(pipelineName: string, isPaused: boolean, pausedBy?: string, pausedCause?: string) {
    this.isPaused     = Stream(isPaused);
    this.pausedBy     = Stream(pausedBy);
    this.pausedCause  = Stream(pausedCause);
    this.pipelineName = Stream(pipelineName);
  }

  static fromJSON(pipelineName: string, json: PipelineStatusJSON) {
    return new PipelineStatus(pipelineName, json.paused, json.paused_by, json.paused_cause);
  }

  static fetch(pipelineName: string) {
    return ApiRequestBuilder
      .GET(SparkRoutes.pipelineStatusApiPath(pipelineName), PipelineStatus.API_VERSION_HEADER)
      .then((result) => {
        return result.do((successResponse) => {
          return PipelineStatus.fromJSON(pipelineName, JSON.parse(successResponse.body) as PipelineStatusJSON);
        });
      });
  }

  unpause() {
    return ApiRequestBuilder.POST(SparkRoutes.pipelineUnpausePath(this.pipelineName()),
                                  PipelineStatus.API_VERSION_HEADER);
  }

  pause() {
    return ApiRequestBuilder.POST(SparkRoutes.pipelinePausePath(this.pipelineName()),
                                  PipelineStatus.API_VERSION_HEADER,
                                  {payload: {pause_cause: this.pausedCause()}});
  }
}
