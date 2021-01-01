/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import {Role, RoleJSON, RolesJSON} from "models/roles/roles";
import {RolesCRUD} from "models/roles/roles_crud";
import {RolesTestData} from "views/pages/roles/spec/test_data";

describe("RoleCRUD", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should make get request", () => {
    jasmine.Ajax.stubRequest("/go/api/admin/security/roles").andReturn(getAllRoles());

    RolesCRUD.all();

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/admin/security/roles");
    expect(request.method).toEqual("GET");
    expect(request.data()).toEqual(toJSON({} as RolesJSON));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
  });

  it("should create a new gocd role", () => {
    jasmine.Ajax.stubRequest("/go/api/admin/security/roles").andReturn(getGoCDRole());

    RolesCRUD.create(Role.fromJSON(RolesTestData.GoCDRoleJSON()));

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/admin/security/roles");
    expect(request.method).toEqual("POST");
    expect(request.data()).toEqual(toJSON(Role.fromJSON(RolesTestData.GoCDRoleJSON())));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  it("should create a new plugin role", () => {
    jasmine.Ajax.stubRequest("/go/api/admin/security/roles").andReturn(getPluginRole());

    RolesCRUD.create(Role.fromJSON(RolesTestData.LdapPluginRoleJSON()));

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/admin/security/roles");
    expect(request.method).toEqual("POST");
    expect(request.data()).toEqual(toJSON(Role.fromJSON(RolesTestData.LdapPluginRoleJSON())));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  it("should update a gocd role", () => {
    const gocdRole = Role.fromJSON(RolesTestData.GoCDRoleJSON());
    jasmine.Ajax.stubRequest(`/go/api/admin/security/roles/${gocdRole.name()}`)
           .andReturn(getGoCDRole());

    RolesCRUD.update(gocdRole, "some-etag");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(`/go/api/admin/security/roles/${gocdRole.name()}`);
    expect(request.method).toEqual("PUT");
    expect(request.data()).toEqual(toJSON(gocdRole));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
    expect(request.requestHeaders["If-Match"]).toEqual("some-etag");
  });

  it("should delete a gocd role", () => {
    const gocdRole = Role.fromJSON(RolesTestData.GoCDRoleJSON());
    jasmine.Ajax.stubRequest(`/go/api/admin/security/roles/${gocdRole.name()}`)
           .andReturn(deleteRoleResponse());

    RolesCRUD.delete(gocdRole.name());

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(`/go/api/admin/security/roles/${gocdRole.name()}`);
    expect(request.method).toEqual("DELETE");
    expect(request.data()).toEqual(toJSON({} as RoleJSON));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v3+json");
    expect(request.requestHeaders["Content-Type"]).toEqual(undefined!);
    expect(request.requestHeaders["X-GoCD-Confirm"]).toEqual("true");
  });
});

function toJSON(object: any) {
  return JSON.parse(JSON.stringify(object));
}

function getAllRoles() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify(RolesTestData.GetAllRoles())
  };
}

function getGoCDRole() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify(RolesTestData.GoCDRoleJSON())
  };
}

function deleteRoleResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8"
    },
    responseText: JSON.stringify({message: "Role successfully deleted."})
  };
}

function getPluginRole() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify(RolesTestData.LdapPluginRoleJSON())
  };
}
