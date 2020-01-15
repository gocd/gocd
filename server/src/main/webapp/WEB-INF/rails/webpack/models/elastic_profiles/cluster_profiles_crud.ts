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
import {ClusterProfile, ClusterProfileJSON, ClusterProfiles} from "./types";

export class ClusterProfilesCRUD {
  private static API_VERSION_HEADER = ApiVersion.v2;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.clusterProfilesListPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              const data = JSON.parse(body)._embedded.cluster_profiles as ClusterProfileJSON[];
                              return ClusterProfiles.fromJSON(data);
                            }));
  }

  static get(id: string) {
    return ApiRequestBuilder.GET(SparkRoutes.apiAdminAccessClusterProfilesPath(id), this.API_VERSION_HEADER)
                            .then(this.extractObjectWithEtag());
  }

  static update(updatedProfile: ClusterProfile, etag: string) {
    return ApiRequestBuilder.PUT(SparkRoutes.apiAdminAccessClusterProfilesPath(updatedProfile.id()),
                                 this.API_VERSION_HEADER,
                                 {payload: updatedProfile, etag})
                            .then(this.extractObjectWithEtag());
  }

  static create(clusterProfile: ClusterProfile) {
    return ApiRequestBuilder.POST(SparkRoutes.apiAdminAccessClusterProfilesPath(),
                                  this.API_VERSION_HEADER,
                                  {payload: clusterProfile.toJSON()}).then(this.extractObjectWithEtag());
  }

  static delete(id: string) {
    return ApiRequestBuilder.DELETE(SparkRoutes.apiAdminAccessClusterProfilesPath(id), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  private static extractObjectWithEtag() {
    return (result: ApiResult<string>) => {
      return result.map((body) => {
        const profileJSON = JSON.parse(body) as ClusterProfileJSON;
        return {
          object: ClusterProfile.fromJSON(profileJSON),
          etag: result.getEtag()
        } as ObjectWithEtag<ClusterProfile>;
      });
    };
  }
}
