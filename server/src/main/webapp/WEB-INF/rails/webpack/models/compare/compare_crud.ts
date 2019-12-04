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
import {Comparison} from "./compare";
import {ComparisonJSON} from "./compare_json";

export class ComparisonCRUD {
  private static API_VERSION_HEADER = ApiVersion.v1;

  public static getDifference(pipelineName: string, fromCounter: number, toCounter: number) {
    return ApiRequestBuilder.GET(SparkRoutes.comparePipelines(pipelineName, fromCounter, toCounter), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => {
                                const foo = Comparison.fromJSON(JSON.parse(body) as ComparisonJSON);
                                return foo;
                              });
                            });
  }
}
