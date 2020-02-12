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
import {AccessTokenCRUD} from "models/access_tokens/access_token_crud";
import {AccessTokenTestData} from "models/access_tokens/spec/access_token_test_data";
import {AccessToken, AccessTokens} from "../types";

describe("AccessTokenCRUD", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());
  const BASE_PATH             = "/go/api/current_user/access_tokens";
  const ALL_ACCESS_TOKENS_PATH = `${BASE_PATH}?filter=all`;

  it("should get all access tokens", (done) => {
    jasmine.Ajax.stubRequest(ALL_ACCESS_TOKENS_PATH).andReturn(accessTokensResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect((responseJSON.body as AccessTokens)).toHaveLength(2);
      done();
    });

    AccessTokenCRUD.all().then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(ALL_ACCESS_TOKENS_PATH);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
  });

  it("should create access token", (done) => {
    const accessToken = AccessToken.fromJSON(AccessTokenTestData.validAccessToken());
    jasmine.Ajax.stubRequest(BASE_PATH).andReturn(accessTokenResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect(responseJSON.body.object.description()).toEqual(accessToken.description());
      expect(responseJSON.body.etag).toEqual("some-etag");
      done();
    });

    AccessTokenCRUD.create(accessToken).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(BASE_PATH);
    expect(request.method).toEqual("POST");
    const requestData = toAccessTokenJSON(request.data());
    expect(requestData.description).toEqual(accessToken.description());
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  it("should revoke access token", (done) => {
    const accessToken = AccessToken.fromJSON(AccessTokenTestData.validAccessToken());
    jasmine.Ajax.stubRequest(`${BASE_PATH}/${accessToken.id()}/revoke`).andReturn(accessTokenResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      expect(responseJSON.body.object.description()).toEqual(accessToken.description());
      expect(responseJSON.body.etag).toEqual("some-etag");
      done();
    });

    AccessTokenCRUD.revoke(accessToken, "Some cause to revoke the token").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(`${BASE_PATH}/${accessToken.id()}/revoke`);
    expect(request.method).toEqual("POST");
    const data = toAccessTokenJSON(request.data());
    expect(data.revoke_cause).toEqual("Some cause to revoke the token");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

});

function accessTokensResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify(AccessTokenTestData.all())
  };
}

function accessTokenResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify(AccessTokenTestData.validAccessToken())
  };
}

function toAccessTokenJSON(object: any) {
  return JSON.parse(JSON.stringify(object));
}
