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
import {Package, Packages, PackageUsages} from "./package_repositories";
import {PackageJSON, PackagesJSON, PackageUsagesJSON} from "./package_repositories_json";

export class PackagesCRUD {
  private static API_VERSION_HEADER = ApiVersion.latest;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.packagePath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              const data = JSON.parse(body) as PackagesJSON;
                              return Packages.fromJSON(data._embedded.packages);
                            }));
  }

  static get(repoId: string) {
    return ApiRequestBuilder.GET(SparkRoutes.packagePath(repoId), this.API_VERSION_HEADER)
                            .then(this.extractObjectWithEtag);
  }

  static create(pkg: Package) {
    return ApiRequestBuilder.POST(SparkRoutes.packagePath(), this.API_VERSION_HEADER,
                                  {payload: pkg})
                            .then(this.extractObjectWithEtag);
  }

  static update(pkg: Package, etag: string) {
    return ApiRequestBuilder.PUT(SparkRoutes.packagePath(pkg.id()), this.API_VERSION_HEADER,
                                 {payload: pkg, etag})
                            .then(this.extractObjectWithEtag);
  }

  static delete(id: string) {
    return ApiRequestBuilder.DELETE(SparkRoutes.packagePath(id), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  static usages(id: string) {
    return ApiRequestBuilder.GET(SparkRoutes.packagesUsagePath(id), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((response) => {
                              const usages = JSON.parse(response) as PackageUsagesJSON;
                              return PackageUsages.fromJSON(usages);
                            }));
  }

  private static extractObjectWithEtag(result: ApiResult<string>) {
    return result.map((body) => {
      const packageJSON = JSON.parse(body) as PackageJSON;
      return {
        object: Package.fromJSON(packageJSON),
        etag:   result.getEtag()
      } as ObjectWithEtag<Package>;
    });
  }
}
