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
import {ElasticAgentProfile, ElasticAgentProfiles, ElasticProfileJSON, ProfileUsage, ProfileUsageJSON} from "./types";

export class ElasticAgentProfilesCRUD {
  private static USAGES_API_VERSION_HEADER = ApiVersion.v1;
  private static API_VERSION_HEADER        = ApiVersion.v2;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.elasticProfileListPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              const data = JSON.parse(body)._embedded.profiles as ElasticProfileJSON[];
                              return ElasticAgentProfiles.fromJSON(data);
                            }));
  }

  static get(id: string) {
    return ApiRequestBuilder.GET(SparkRoutes.elasticProfilePath(id), this.API_VERSION_HEADER)
                            .then(this.extractObjectWithEtag());
  }

  static usage(id: string) {
    return ApiRequestBuilder.GET(SparkRoutes.elasticProfileUsagePath(id), this.USAGES_API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((response) => {
                              const usages = JSON.parse(response) as ProfileUsageJSON[];
                              return usages.map((usage: ProfileUsageJSON) => ProfileUsage.fromJSON(usage));
                            }));
  }

  static update(updatedProfile: ElasticAgentProfile, etag: string) {
    return ApiRequestBuilder.PUT(SparkRoutes.elasticProfilePath(updatedProfile.id()!),
                                 this.API_VERSION_HEADER,
                                 {payload: updatedProfile, etag})
                            .then(this.extractObjectWithEtag());
  }

  static create(profile: ElasticAgentProfile) {
    return ApiRequestBuilder.POST(SparkRoutes.elasticProfileListPath(),
                                  this.API_VERSION_HEADER,
                                  {payload: profile}).then(this.extractObjectWithEtag());
  }

  static delete(id: string) {
    return ApiRequestBuilder.DELETE(SparkRoutes.elasticProfilePath(id), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  private static extractObjectWithEtag() {
    return (result: ApiResult<string>) => {
      return result.map((body) => {
        const profileJSON = JSON.parse(body) as ElasticProfileJSON;
        return {
          object: ElasticAgentProfile.fromJSON(profileJSON),
          etag: result.getEtag()
        } as ObjectWithEtag<ElasticAgentProfile>;
      });
    };
  }
}
