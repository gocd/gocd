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
import {SecretConfig, SecretConfigs, SecretConfigsWithSuggestions} from "models/secret_configs/secret_configs";
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
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it("should get all secret configs along with autocomplete suggestions", (done) => {
    const api = SparkRoutes.apiSecretConfigsWithAutocompleteSuggestionsPath();
    jasmine.Ajax.stubRequest(api).andReturn(secretConfigsWithAutocompleteSuggestionsResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const body         = responseJSON.body as SecretConfigsWithSuggestions;
      expect(body.secretConfigs).toHaveLength(2);
      expect(body.autoCompletion).toHaveLength(1);
      expect(body.autoCompletion[0].key).toBe('key1');
      expect(body.autoCompletion[0].value).toEqual(['value1', 'value2']);
      done();
    });

    SecretConfigsCRUD.allWithAutocompleteSuggestions().then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(api);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
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
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
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
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
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
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
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
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["If-Match"]).toEqual("current-etag");
  });
});

function secretConfigsResponse() {
  return {
    status:          200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag":         "some-etag"
    },
    responseText:    JSON.stringify(secretConfigsTestData())
  };
}

function secretConfigsWithAutocompleteSuggestionsResponse() {
  const data           = secretConfigsTestData();
  // @ts-ignore
  data.auto_completion = [
    {
      key:   "key1",
      value: ["value1", "value2"]
    }
  ];
  return {
    status:          200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag":         "some-etag"
    },
    responseText:    JSON.stringify(data)
  };
}

function secretConfigResponse() {
  return {
    status:          200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag":         "some-etag"
    },
    responseText:    JSON.stringify(secretConfigTestData())
  };
}

function deleteSecretConfigResponse(id: string) {
  return {
    status:          200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag":         "some-etag"
    },
    responseText:    JSON.stringify({
                                      message: `Secret Configuration ${id} was deleted successfully!`
                                    })
  };
}
