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

import {EnvironmentCRUD} from "models/environments/environment_crud";
import {TestData} from "models/environments/spec/test_data";
import {EnvironmentsJSON} from "models/environments/types";

describe("EnvironmentCRUD", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should make get request", () => {
    jasmine.Ajax.stubRequest("/go/api/admin/environments").andReturn(listEnvironmentResponse());

    EnvironmentCRUD.all();

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/admin/environments");
    expect(request.method).toEqual("GET");
    expect(request.data()).toEqual(toJSON({} as EnvironmentsJSON));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
  });
});

function toJSON(object: any) {
  return JSON.parse(JSON.stringify(object));
}

function listEnvironmentResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8"
    },
    responseText: JSON.stringify(TestData.environmentList(TestData.newEnvironment()))
  };
}
