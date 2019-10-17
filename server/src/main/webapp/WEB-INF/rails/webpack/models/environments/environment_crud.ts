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

import {ApiRequestBuilder, ApiResult, ApiVersion, ObjectWithEtag} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {Environments} from "models/environments/types";
import {EnvironmentJSON, EnvironmentWithOrigin} from "models/new-environments/environments";

export class EnvironmentCRUD {
  private static API_VERSION_HEADER = ApiVersion.v3;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.apiEnvironmentPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => {
                                return Environments.fromJSON(JSON.parse(body));
                              });
                            });

  }

  static create(environment: EnvironmentWithOrigin) {
    return ApiRequestBuilder.POST(SparkRoutes.apiEnvironmentPath(), this.API_VERSION_HEADER, {payload: environment})
                            .then(this.extractObjectWithEtag());
  }

  private static extractObjectWithEtag() {
    return (result: ApiResult<string>) => {
      return result.map((body) => {
        const environmentJSON = JSON.parse(body) as EnvironmentJSON;
        return {
          object: EnvironmentWithOrigin.fromJSON(environmentJSON),
          etag: result.getEtag()
        } as ObjectWithEtag<EnvironmentWithOrigin>;
      });
    };
  }
}
