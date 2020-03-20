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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {Scms, ScmsJSON} from "./pluggable_scm";

export class PluggableScmCRUD {
  private static API_VERSION_HEADER = ApiVersion.latest;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.pluggableScmPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              const data = JSON.parse(body) as ScmsJSON;
                              return Scms.fromJSON(data._embedded.scms);
                            }));
  }
}
