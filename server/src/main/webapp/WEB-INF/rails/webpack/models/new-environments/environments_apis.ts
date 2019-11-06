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
import {EnvironmentVariableJSON} from "models/environment_variables/types";
import {PipelineStructure} from "models/internal_pipeline_structure/pipeline_structure";
import {Environments} from "models/new-environments/environments";

export class EnvironmentsAPIs {
  private static LATEST_API_VERSION_HEADER = ApiVersion.latest;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.apiAdminInternalMergedEnvironmentsPath(), this.LATEST_API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => Environments.fromJSON(JSON.parse(body)));
                            });
  }

  static allPipelines() {
    return ApiRequestBuilder.GET(SparkRoutes.apiAdminInternalPipelinesListPath(), this.LATEST_API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => PipelineStructure.fromJSON(JSON.parse(body)));
                            });
  }

  static delete(name: string) {
    return ApiRequestBuilder.DELETE(SparkRoutes.apiAdminEnvironmentsPath(name), this.LATEST_API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  static patch(name: string, payload: EnvironmentPatchJson) {
    return ApiRequestBuilder.PATCH(SparkRoutes.apiAdminEnvironmentsPath(name), this.LATEST_API_VERSION_HEADER, {payload});
  }
}

export interface EnvironmentPatchJson {
  environment_variables?: {
    add: EnvironmentVariableJSON[],
    remove: string[]
  };
}
