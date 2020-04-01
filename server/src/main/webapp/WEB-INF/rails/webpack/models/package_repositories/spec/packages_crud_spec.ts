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
import {PackagesCRUD} from "../packages_crud";
import {Package, Packages, PackageUsages} from "../package_repositories";
import {getPackage} from "./test_data";

describe('PackagesCRUDSpec', () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should get all packages", (done) => {
    const url = SparkRoutes.packagePath();
    jasmine.Ajax.stubRequest(url).andReturn(packagesResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const packages     = (responseJSON.body as Packages);

      expect(packages[0].id()).toBe('pkg-id');
      expect(packages[0].name()).toBe('pkg-name');
      expect(packages[0].autoUpdate()).toBeTrue();
      expect(packages[0].configuration().count()).toBe(1);

      expect(packages[0].packageRepo().id()).toBe('pkg-repo-id');
      expect(packages[0].packageRepo().name()).toBe('pkg-repo-name');
      done();
    });

    PackagesCRUD.all().then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it('should get the specified package', (done) => {
    const url = SparkRoutes.packagePath("pkg-id");
    jasmine.Ajax.stubRequest(url).andReturn(packageResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const pkg          = (responseJSON.body as ObjectWithEtag<Package>);

      expect(pkg.object.id()).toBe('pkg-id');
      expect(pkg.object.name()).toBe('pkg-name');
      expect(pkg.object.autoUpdate()).toBeTrue();
      expect(pkg.object.configuration().count()).toBe(1);
      done();
    });

    PackagesCRUD.get("pkg-id").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it("should create a new package", () => {
    const url = SparkRoutes.packagePath();
    jasmine.Ajax.stubRequest(url).andReturn(packageResponse());

    const pkg = Package.fromJSON(getPackage());
    PackagesCRUD.create(pkg);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("POST");
    expect(request.data()).toEqual(toJSON(pkg.toJSON()));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  it("should update a package", () => {
    const url = SparkRoutes.packagePath("pkg-id");
    jasmine.Ajax.stubRequest(url).andReturn(packageResponse());

    const pkg = Package.fromJSON(getPackage());
    PackagesCRUD.update(pkg, "old-etag");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("PUT");
    expect(request.data()).toEqual(toJSON(pkg.toJSON()));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  it("should delete a package", () => {
    const url = SparkRoutes.packagePath("pkg-id");
    jasmine.Ajax.stubRequest(url).andReturn(deletePackageResponse());

    PackagesCRUD.delete("pkg-id");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("DELETE");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual(undefined!);
    expect(request.requestHeaders["X-GoCD-Confirm"]).toEqual("true");
  });

  it('should get the list of pipelines where this package was used', (done) => {
    const url = SparkRoutes.packagesUsagePath("pkg-id");
    jasmine.Ajax.stubRequest(url).andReturn(packageUsageResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const pkg          = (responseJSON.body as PackageUsages);

      expect(pkg.length).toBe(1);
      expect(pkg[0].group).toBe('group');
      expect(pkg[0].pipeline).toBe('pipeline');
      done();
    });

    PackagesCRUD.usages("pkg-id").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  function toJSON(object: any) {
    return JSON.parse(JSON.stringify(object));
  }

  function packagesResponse() {
    const packageJSON = {
      _embedded: {
        packages: [getPackage()]
      }
    };
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
        "ETag":         "some-etag"
      },
      responseText:    JSON.stringify(packageJSON)
    };
  }

  function packageResponse() {
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
        "ETag":         "some-etag"
      },
      responseText:    JSON.stringify(getPackage())
    };
  }

  function deletePackageResponse() {
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8"
      },
      responseText:    JSON.stringify({message: "The package was successfully deleted."})
    };
  }

  function packageUsageResponse() {
    const usages = {
      usages: [
        {
          group:    "group",
          pipeline: "pipeline"
        }
      ]
    };
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8"
      },
      responseText:    JSON.stringify(usages)
    };
  }
});
