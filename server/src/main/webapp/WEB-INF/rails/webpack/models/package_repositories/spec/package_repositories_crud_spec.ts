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

import {ApiResult, ObjectWithEtag, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {PackageRepositories, PackageRepository} from "../package_repositories";
import {PackageRepositoriesCRUD} from "../package_repositories_crud";
import {PackageRepositoryJSON} from "../package_repositories_json";
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

  it('should get the specified package repository', (done) => {
    const url = SparkRoutes.packageRepositoryPath("repo-id");
    jasmine.Ajax.stubRequest(url).andReturn(packageRepositoryResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON      = response.unwrap() as SuccessResponse<any>;
      const packageRepository = (responseJSON.body as ObjectWithEtag<PackageRepository>).object;

      expect(packageRepository.repoId()).toBe('pkg-repo-id');
      expect(packageRepository.name()).toBe('pkg-repo-name');
      expect(packageRepository.pluginMetadata().id()).toBe('npm');
      expect(packageRepository.pluginMetadata().version()).toBe('1');

      expect(packageRepository.configuration().count()).toBe(1);
      expect(packageRepository.packages()).toHaveLength(1);

      const pkg = packageRepository.packages()[0];

      expect(pkg.id()).toBe('pkg-id');
      expect(pkg.name()).toBe('pkg-name');
      expect(pkg.autoUpdate()).toBeTrue();
      expect(pkg.configuration().count()).toBe(1);
      done();
    });

    PackageRepositoriesCRUD.get("repo-id").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it("should create a new package repository", () => {
    const url = SparkRoutes.packageRepositoryPath();
    jasmine.Ajax.stubRequest(url).andReturn(packageRepositoryResponse());

    const packageRepository = PackageRepository.fromJSON(getPackageRepository());
    PackageRepositoriesCRUD.create(packageRepository);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("POST");
    expect(request.data()).toEqual(toJSON(packageRepository.toJSON()));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  it("should update a package repository", () => {
    const url = SparkRoutes.packageRepositoryPath("pkg-repo-id");
    jasmine.Ajax.stubRequest(url).andReturn(packageRepositoryResponse());

    const packageRepository = PackageRepository.fromJSON(getPackageRepository());
    PackageRepositoriesCRUD.update(packageRepository, "old-etag");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("PUT");
    expect(request.data()).toEqual(toJSON(packageRepository.toJSON()));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  it("should delete a package repository", () => {
    const url = SparkRoutes.packageRepositoryPath("pkg-repo-id");
    jasmine.Ajax.stubRequest(url).andReturn(deletePackageRepoResponse());

    PackageRepositoriesCRUD.delete("pkg-repo-id");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("DELETE");
    expect(request.data()).toEqual(toJSON({} as PackageRepositoryJSON));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual(undefined!);
    expect(request.requestHeaders["X-GoCD-Confirm"]).toEqual("true");
  });

  function toJSON(object: any) {
    return JSON.parse(JSON.stringify(object));
  }

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

  function packageRepositoryResponse() {
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
        "ETag":         "some-etag"
      },
      responseText:    JSON.stringify(getPackageRepository())
    };
  }

  function deletePackageRepoResponse() {
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8"
      },
      responseText:    JSON.stringify({message: "The package repository was successfully deleted."})
    };
  }
});
