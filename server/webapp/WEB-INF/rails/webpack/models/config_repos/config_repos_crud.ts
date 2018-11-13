/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import {ApiRequestBuilder, ApiVersion, HttpResponseWithEtag} from "helpers/api_request_builder";
import SparkRoutes from "helpers/spark_routes";
import {ConfigRepo, ConfigRepos} from "./types";

export class ConfigReposCRUD {
  private static API_VERSION_HEADER = ApiVersion.v1;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.ApiConfigReposListPath(), this.API_VERSION_HEADER)
      .then((xhr: XMLHttpRequest) => JSON.parse(xhr.responseText) as ConfigRepos);
  }

  static get(id: string) {
    return ApiRequestBuilder.GET(SparkRoutes.ApiConfigReposPath(id), this.API_VERSION_HEADER)
      .then(this.extractResponseWithEtag());
  }

  static update(response: HttpResponseWithEtag<ConfigRepo>) {
    return ApiRequestBuilder.PUT(SparkRoutes.ApiConfigReposPath(response.object.id), this.API_VERSION_HEADER, response.object, response.etag)
      .then(this.extractResponseWithEtag());
  }

  static delete(repo: ConfigRepo) {
    return ApiRequestBuilder.DELETE(Routes.apiv1AdminConfigRepoPath(repo.id), this.API_VERSION_HEADER)
      .then((xhr: XMLHttpRequest) => JSON.parse(xhr.responseText));
  }

  static create(repo: ConfigRepo) {
    return ApiRequestBuilder.POST(Routes.apiv1AdminConfigReposPath(), this.API_VERSION_HEADER, repo).then(this.extractResponseWithEtag());
  }

  private static extractResponseWithEtag() {
    return (xhr: XMLHttpRequest) => {
      return {
        object: JSON.parse(xhr.responseText) as ConfigRepo,
        etag: xhr.getResponseHeader("etag")
      } as HttpResponseWithEtag<ConfigRepo>;
    };
  }
}
