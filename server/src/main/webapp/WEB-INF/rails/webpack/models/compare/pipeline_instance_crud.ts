/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {PipelineHistory, PipelineInstance} from "./pipeline_instance";
import {PipelineHistoryJSON, PipelineInstanceJSON} from "./pipeline_instance_json";

export class PipelineInstanceCRUD {
  private static API_VERSION_HEADER = ApiVersion.latest;

  public static get(pipelineName: string, pipelineCounter: number) {
    return ApiRequestBuilder.GET(SparkRoutes.getPipelineInstance(pipelineName, pipelineCounter), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => {
                                return PipelineInstance.fromJSON(JSON.parse(body) as PipelineInstanceJSON);
                              });
                            });
  }

  /*
  *  Link is the href provided in the first response which can be used to get the next/previous list of records
  */
  public static history(pipelineName: string, link?: string) {
    const path = link ? link : SparkRoutes.getPipelineHistory(pipelineName);
    return ApiRequestBuilder.GET(path, this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              return PipelineHistory.fromJSON(pipelineName, JSON.parse(body) as PipelineHistoryJSON);
                            }));
  }
}
