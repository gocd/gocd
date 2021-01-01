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
import {PackageRepositories, PackageRepository} from "./package_repositories";
import {PackageRepositoriesJSON, PackageRepositoryJSON} from "./package_repositories_json";

export class PackageRepositoriesCRUD {
  private static API_VERSION_HEADER = ApiVersion.latest;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.packageRepositoryPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              const data = JSON.parse(body) as PackageRepositoriesJSON;
                              return PackageRepositories.fromJSON(data._embedded.package_repositories);
                            }));
  }

  static get(repoId: string) {
    return ApiRequestBuilder.GET(SparkRoutes.packageRepositoryPath(repoId), this.API_VERSION_HEADER)
                            .then(this.extractObjectWithEtag);
  }

  static create(packageRepository: PackageRepository) {
    return ApiRequestBuilder.POST(SparkRoutes.packageRepositoryPath(), this.API_VERSION_HEADER,
                                  {payload: packageRepository})
                            .then(this.extractObjectWithEtag);
  }

  static update(packageRepository: PackageRepository, etag: string) {
    return ApiRequestBuilder.PUT(SparkRoutes.packageRepositoryPath(packageRepository.repoId()),
                                 this.API_VERSION_HEADER,
                                 {payload: packageRepository, etag})
                            .then(this.extractObjectWithEtag);
  }

  static delete(repoId: string) {
    return ApiRequestBuilder.DELETE(SparkRoutes.packageRepositoryPath(repoId), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  static verifyConnection(packageRepository: PackageRepository) {
    return ApiRequestBuilder.POST(SparkRoutes.adminInternalPackageRepositoriesVerifyConnectionPath(),
                                  ApiVersion.v1, {payload: packageRepository})
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  private static extractObjectWithEtag(result: ApiResult<string>) {
    return result.map((body) => {
      const packageRepositoryJSON = JSON.parse(body) as PackageRepositoryJSON;
      return {
        object: PackageRepository.fromJSON(packageRepositoryJSON),
        etag: result.getEtag()
      } as ObjectWithEtag<PackageRepository>;
    });
  }
}
