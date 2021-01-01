/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {AuthConfig, AuthConfigJSON, AuthConfigs} from "models/auth_configs/auth_configs";

export class AuthConfigsCRUD {
  private static API_VERSION_HEADER = ApiVersion.latest;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.authConfigPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              return AuthConfigs.fromJSON(JSON.parse(body));
                            }));
  }

  static get(id: string) {
    return ApiRequestBuilder.GET(SparkRoutes.authConfigPath(id), this.API_VERSION_HEADER)
                            .then(this.extractObjectWithEtag);
  }

  static update(updatedAuthConfig: AuthConfig, etag: string) {
    return ApiRequestBuilder.PUT(SparkRoutes.authConfigPath(updatedAuthConfig.id()),
                                 this.API_VERSION_HEADER,
                                 {payload: updatedAuthConfig, etag})
                            .then(this.extractObjectWithEtag);
  }

  static create(authConfig: AuthConfig) {
    return ApiRequestBuilder.POST(SparkRoutes.authConfigPath(),
                                  this.API_VERSION_HEADER,
                                  {payload: authConfig}).then(this.extractObjectWithEtag);
  }

  static delete(id: string) {
    return ApiRequestBuilder.DELETE(SparkRoutes.authConfigPath(id), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  static verifyConnection(authConfig: AuthConfig) {
    return ApiRequestBuilder.POST(SparkRoutes.adminInternalVerifyConnectionPath(),
                                  this.API_VERSION_HEADER,
                                  {payload: authConfig})
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => JSON.parse(body));
                            });
  }

  private static extractObjectWithEtag(result: ApiResult<string>) {
    return result.map((body) => {
      const authConfigJSON = JSON.parse(body) as AuthConfigJSON;
      return {
        object: AuthConfig.fromJSON(authConfigJSON),
        etag: result.getEtag()
      } as ObjectWithEtag<AuthConfig>;
    });
  }
}
