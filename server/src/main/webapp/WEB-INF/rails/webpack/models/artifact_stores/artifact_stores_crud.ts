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

import {
  ApiRequestBuilder,
  ApiResult,
  ApiVersion,
  ObjectWithEtag
} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {
  ArtifactStore,
  ArtifactStoreJSON,
  ArtifactStores,
  ArtifactStoresJSON
} from "models/artifact_stores/artifact_stores";

export class ArtifactStoresCRUD {
  private static API_VERSION_HEADER = ApiVersion.v1;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.artifactStoresPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              return ArtifactStores.fromJSON(JSON.parse(body) as ArtifactStoresJSON);
                            }));
  }

  static get(id: string) {
    return ApiRequestBuilder.GET(SparkRoutes.artifactStoresPath(id), this.API_VERSION_HEADER)
                            .then(this.extractObjectWithEtag);
  }

  static update(updatedArtifactStore: ArtifactStore, etag: string) {
    return ApiRequestBuilder.PUT(SparkRoutes.artifactStoresPath(updatedArtifactStore.id()),
                                 this.API_VERSION_HEADER,
                                 {payload: updatedArtifactStore, etag})
                            .then(this.extractObjectWithEtag);
  }

  static create(newArtifactStore: ArtifactStore) {
    return ApiRequestBuilder.POST(SparkRoutes.artifactStoresPath(),
                                  this.API_VERSION_HEADER,
                                  {payload: newArtifactStore}).then(this.extractObjectWithEtag);
  }

  static delete(id: string) {
    return ApiRequestBuilder.DELETE(SparkRoutes.artifactStoresPath(id), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  private static extractObjectWithEtag(result: ApiResult<string>) {
    return result.map((body) => {
      return {
        object: ArtifactStore.fromJSON(JSON.parse(body) as ArtifactStoreJSON),
        etag: result.getEtag()
      } as ObjectWithEtag<ArtifactStore>;
    });
  }
}
