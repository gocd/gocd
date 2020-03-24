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

import {ApiResult, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {PackagesCRUD} from "../packages_crud";
import {Packages} from "../package_repositories";
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
});
