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
import {MaterialAPIs, MaterialWithFingerprints} from "../materials";
import {GitMaterialAttributes} from "../types";

describe('MaterialsAPISpec', () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should get all materials", (done) => {
    const url = SparkRoutes.getAllMaterials();
    jasmine.Ajax.stubRequest(url).andReturn(materialsResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const materials    = (responseJSON.body as MaterialWithFingerprints);

      expect(materials).toHaveLength(1);

      const material = materials[0];
      expect(material.type()).toBe('git');
      expect(material.name()).toBe('some-name');
      expect(material.fingerprint()).toBe('4879d548d34a4f3ba7ed4a532bc1b02');

      expect(material.attributes()).toBeInstanceOf(GitMaterialAttributes);
      done();
    });

    MaterialAPIs.all().then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  function materialsResponse() {
    const data = {
      _embedded: {
        materials: [{
          type:        "git",
          fingerprint: "4879d548d34a4f3ba7ed4a532bc1b02",
          attributes:  {
            url:              "test-repo",
            destination:      null,
            filter:           null,
            invert_filter:    false,
            name:             'some-name',
            auto_update:      true,
            branch:           "master",
            submodule_folder: null,
            shallow_clone:    false
          }
        }]
      }
    };
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v2+json; charset=utf-8",
      },
      responseText:    JSON.stringify(data)
    };
  }
});
