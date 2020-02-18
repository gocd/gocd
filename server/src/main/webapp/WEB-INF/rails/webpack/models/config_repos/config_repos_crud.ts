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
import {JsonUtils} from "helpers/json_utils";
import {SparkRoutes} from "helpers/spark_routes";
import {ConfigRepoJSON, ConfigReposJSON} from "models/config_repos/serialization";
import {ConfigRepo, ConfigRepos} from "models/config_repos/types";

export function configRepoToSnakeCaseJSON(o: ConfigRepo) {
  const configurations = o.createConfigurationsFromText();
  const json           = JsonUtils.toSnakeCasedObject(o);
  json.configuration   = configurations.map((config) => config.toJSON());
  return json;
}

export class ConfigReposCRUD {
  private static API_VERSION_HEADER = ApiVersion.v3;

  static all(etag?: string) {
    return ApiRequestBuilder.GET(SparkRoutes.apiConfigReposInternalPath(), this.API_VERSION_HEADER, {etag})
    .then((result: ApiResult<string>) => {
      return result.map((body) => {
        return ConfigRepos.fromJSON(JSON.parse(body) as ConfigReposJSON);
      });
    });
  }

  static get(id: string) {
    return ApiRequestBuilder.GET(SparkRoutes.ApiConfigRepoPath(id), this.API_VERSION_HEADER)
    .then(this.extractObjectWithEtag());
  }

  static update(response: ObjectWithEtag<ConfigRepo>) {
    return ApiRequestBuilder.PUT(SparkRoutes.ApiConfigRepoPath(response.object.id()!), this.API_VERSION_HEADER,
                                 {payload: configRepoToSnakeCaseJSON(response.object), etag: response.etag})
    .then(this.extractObjectWithEtag());

  }

  static delete(repo: ConfigRepo) {
    return ApiRequestBuilder.DELETE(SparkRoutes.ApiConfigRepoPath(repo.id()!), this.API_VERSION_HEADER)
    .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  static create(repo: ConfigRepo) {
    return ApiRequestBuilder.POST(SparkRoutes.ApiConfigReposListPath(),
                                  this.API_VERSION_HEADER,
                                  {payload: configRepoToSnakeCaseJSON(repo)})
    .then(this.extractObjectWithEtag());
  }

  static triggerUpdate(id: string) {
    return ApiRequestBuilder.POST(SparkRoutes.configRepoTriggerUpdatePath(id), this.API_VERSION_HEADER);
  }

  private static extractObjectWithEtag() {
    return (result: ApiResult<string>) => {
      return result.map((body) => {
        const configRepo = JSON.parse(body) as ConfigRepoJSON;
        return {object: ConfigRepo.fromJSON(configRepo), etag: result.getEtag()} as ObjectWithEtag<ConfigRepo>;
      });
    };
  }
}
