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
import {SecretConfig, SecretConfigs} from "models/secret_configs/secret_configs";
import {SecretConfigsCRUD} from "models/secret_configs/secret_configs_crud";
import {secretConfigsTestData, secretConfigTestData} from "models/secret_configs/spec/test_data";

describe("SecretConfigsCRUD", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());
  const BASE_PATH = "/go/api/admin/secret_configs";

  it("should get all secret configs", (done) => {
    jasmine.Ajax.stubRequest(BASE_PATH).andReturn(secretConfigsResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect(responseJSON.body as SecretConfigs).toHaveLength(2);
      done();
    });

    SecretConfigsCRUD.all().then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(BASE_PATH);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
  });

  it("should get a secret config", (done) => {
    const secretConfig       = SecretConfig.fromJSON(secretConfigTestData());
    const SECRET_CONFIG_PATH = `${BASE_PATH}/${secretConfig.id()}`;
    jasmine.Ajax.stubRequest(SECRET_CONFIG_PATH).andReturn(secretConfigResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect(responseJSON.body.object.id()).toEqual(secretConfig.id());
      expect(response.getEtag()).toEqual("some-etag");
      done();
    });

    SecretConfigsCRUD.get(secretConfig).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/admin/secret_configs/secrets_id");
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
  });

  it("should create a secret config", () => {
    const secretConfig = SecretConfig.fromJSON(secretConfigTestData());
    jasmine.Ajax.stubRequest(BASE_PATH).andReturn(secretConfigResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<string>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect(responseJSON.body.object.id()).toEqual(secretConfig.id());
      expect(response.getEtag()).toEqual("some-etag");
    });

    SecretConfigsCRUD.create(secretConfig).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(BASE_PATH);
    expect(request.method).toEqual("POST");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
  });

  it("should delete a secret config", () => {
    const secretConfig       = SecretConfig.fromJSON(secretConfigTestData());
    const SECRET_CONFIG_PATH = `${BASE_PATH}/${secretConfig.id()}`;
    jasmine.Ajax.stubRequest(SECRET_CONFIG_PATH).andReturn(deleteSecretConfigResponse(secretConfig.id()));

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<string>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect(responseJSON.body.message).toEqual(`Secret Configuration ${secretConfig.id()} was deleted successfully!`);
      expect(response.getEtag()).toEqual("some-etag");
    });

    SecretConfigsCRUD.delete(secretConfig).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/admin/secret_configs/secrets_id");
    expect(request.method).toEqual("DELETE");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
  });

  it("should update a secret config", () => {
    const secretConfig       = SecretConfig.fromJSON(secretConfigTestData());
    const SECRET_CONFIG_PATH = `${BASE_PATH}/${secretConfig.id()}`;
    jasmine.Ajax.stubRequest(SECRET_CONFIG_PATH).andReturn(secretConfigResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<string>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect(responseJSON.body.object.id()).toEqual(secretConfig.id());
      expect(response.getEtag()).toEqual("some-etag");
    });

    SecretConfigsCRUD.update(secretConfig, "current-etag").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/admin/secret_configs/secrets_id");
    expect(request.method).toEqual("PUT");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
    expect(request.requestHeaders["If-Match"]).toEqual("current-etag");
  });
});

function secretConfigsResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify(secretConfigsTestData())
  };
}

function secretConfigResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify(secretConfigTestData())
  };
}

function deleteSecretConfigResponse(id: string) {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify({
                                   message: `Secret Configuration ${id} was deleted successfully!`
                                 })
  };
}
