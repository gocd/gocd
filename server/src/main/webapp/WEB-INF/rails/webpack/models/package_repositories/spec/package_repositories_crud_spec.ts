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

import {SparkRoutes} from "helpers/spark_routes";
import {ApiResult, SuccessResponse} from "helpers/api_request_builder";
import {PackageRepositories} from "../package_repositories";
import {PackageRepositoriesCRUD} from "../package_repositories_crud";
import {getPackageRepository} from "./test_data";

describe('PackageRepositoriesCRUDSpec', () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should get all package repositories", (done) => {
    const url = SparkRoutes.packageRepositoryPath();
    jasmine.Ajax.stubRequest(url).andReturn(packageRepositoriesResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON        = response.unwrap() as SuccessResponse<any>;
      const packageRepositories = (responseJSON.body as PackageRepositories);

      expect(packageRepositories).toHaveLength(1);
      expect(packageRepositories[0].repoId()).toBe('pkg-repo-id');
      expect(packageRepositories[0].name()).toBe('pkg-repo-name');
      expect(packageRepositories[0].pluginMetadata().id()).toBe('npm');
      expect(packageRepositories[0].pluginMetadata().version()).toBe('1');

      expect(packageRepositories[0].configuration().count()).toBe(1);
      expect(packageRepositories[0].packages()).toHaveLength(1);

      const pkg = packageRepositories[0].packages()[0];

      expect(pkg.id()).toBe('pkg-id');
      expect(pkg.name()).toBe('pkg-name');
      expect(pkg.autoUpdate()).toBeTrue();
      expect(pkg.configuration().count()).toBe(1);
      done();
    });

    PackageRepositoriesCRUD.all().then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });
});

function packageRepositoriesResponse() {
  const pkgRepos = {
    _embedded: {
      package_repositories: [getPackageRepository()]
    }
  };
  return {
    status:          200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
    },
    responseText:    JSON.stringify(pkgRepos)
  };
}
