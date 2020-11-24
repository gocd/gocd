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

import {ApiRequestBuilder, ApiResult, ApiVersion, ObjectWithEtag} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {SecretConfig, SecretConfigs, SecretConfigsWithSuggestions} from "models/secret_configs/secret_configs";
import {SecretConfigsJSON, SecretConfigsWithSuggestionsJSON} from "models/secret_configs/secret_configs_json";

export class SecretConfigsCRUD {
  private static API_VERSION_HEADER = ApiVersion.latest;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.apiSecretConfigsPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => {
                                return SecretConfigs.fromJSON(JSON.parse(body) as SecretConfigsJSON);
                              });
                            });
  }

  static allWithAutocompleteSuggestions() {
    return ApiRequestBuilder.GET(SparkRoutes.apiSecretConfigsWithAutocompleteSuggestionsPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => {
                                return SecretConfigsWithSuggestions.fromJSON(JSON.parse(body) as SecretConfigsWithSuggestionsJSON);
                              });
                            });
  }

  static get(secretConfig: SecretConfig) {
    return ApiRequestBuilder.GET(SparkRoutes.apiSecretConfigsPath(secretConfig.id()), this.API_VERSION_HEADER)
                            .then(this.extractObjectWithEtag);
  }

  static create(secretConfig: SecretConfig) {
    return ApiRequestBuilder.POST(SparkRoutes.apiSecretConfigsPath(), this.API_VERSION_HEADER,
                                  {payload: secretConfig})
                            .then(this.extractObjectWithEtag);
  }

  static delete(secretConfig: SecretConfig) {
    return ApiRequestBuilder.DELETE(SparkRoutes.apiSecretConfigsPath(secretConfig.id()), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  static update(secretConfig: SecretConfig, etag: string) {
    return ApiRequestBuilder.PUT(SparkRoutes.apiSecretConfigsPath(secretConfig.id()),
                                 this.API_VERSION_HEADER,
                                 {payload: secretConfig, etag})
                            .then(this.extractObjectWithEtag);
  }

  private static extractObjectWithEtag(response: ApiResult<string>) {
    return response.map((body) => {
      const parsedSecretConfig = JSON.parse(body);
      return {
        object: SecretConfig.fromJSON(parsedSecretConfig),
        etag:   response.getEtag()
      } as ObjectWithEtag<SecretConfig>;
    });
  }
}
