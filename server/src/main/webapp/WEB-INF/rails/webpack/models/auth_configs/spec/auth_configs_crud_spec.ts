/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import {AuthConfig, AuthConfigJSON} from "models/auth_configs/auth_configs";
import {AuthConfigsCRUD} from "models/auth_configs/auth_configs_crud";
import {TestData} from "models/auth_configs/spec/test_data";

describe("AuthorizationConfigurationCRUD", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should make get request", () => {
    jasmine.Ajax.stubRequest("/go/api/admin/security/auth_configs").andReturn(listAuthConfigResponse());

    AuthConfigsCRUD.all();

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/admin/security/auth_configs");
    expect(request.method).toEqual("GET");
    expect(request.data()).toEqual(toJSON({} as AuthConfigJSON));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
  });

  it("should create a new auth config", () => {
    jasmine.Ajax.stubRequest("/go/api/admin/security/auth_configs").andReturn(authConfigResponse());

    AuthConfigsCRUD.create(AuthConfig.fromJSON(TestData.ldapAuthConfig()));

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/admin/security/auth_configs");
    expect(request.method).toEqual("POST");
    expect(request.data()).toEqual(toJSON(TestData.ldapAuthConfig()));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  it("should update a auth config", () => {
    const ldapAuthConfig = TestData.ldapAuthConfig();
    jasmine.Ajax.stubRequest(`/go/api/admin/security/auth_configs/${ldapAuthConfig.id}`)
           .andReturn(authConfigResponse());

    AuthConfigsCRUD.update(AuthConfig.fromJSON(ldapAuthConfig), "some-etag");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(`/go/api/admin/security/auth_configs/${ldapAuthConfig.id}`);
    expect(request.method).toEqual("PUT");
    expect(request.data()).toEqual(toJSON(ldapAuthConfig));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
    expect(request.requestHeaders["If-Match"]).toEqual("some-etag");
  });

  it("should delete a auth config", () => {
    const ldapAuthConfig = TestData.ldapAuthConfig();
    jasmine.Ajax.stubRequest(`/go/api/admin/security/auth_configs/${ldapAuthConfig.id}`)
           .andReturn(deleteAuthConfigResponse());

    AuthConfigsCRUD.delete(ldapAuthConfig.id);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(`/go/api/admin/security/auth_configs/${ldapAuthConfig.id}`);
    expect(request.method).toEqual("DELETE");
    expect(request.data()).toEqual(toJSON({} as AuthConfigJSON));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
    expect(request.requestHeaders["Content-Type"]).toEqual(undefined!);
    expect(request.requestHeaders["X-GoCD-Confirm"]).toEqual("true");
  });

});

function toJSON(object: any) {
  return JSON.parse(JSON.stringify(object));
}

function authConfigResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify(TestData.ldapAuthConfig())
  };
}

function listAuthConfigResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8"
    },
    responseText: JSON.stringify(TestData.authConfigList(TestData.ldapAuthConfig()))
  };
}

function deleteAuthConfigResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8"
    },
    responseText: JSON.stringify({message: "The auth config successfully deleted."})
  };
}
