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
import {Permission, PermissionJSON, Permissions, PermissionsAPIJSON, SupportedEntity} from "models/shared/permissions";

describe("Entity Permissions", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());
  const BASE_PATH = "/go/api/auth/permissions";

  it("should deserialize fetched permissions", (done) => {
    jasmine.Ajax.stubRequest(BASE_PATH).andReturn(permissionsResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON             = response.unwrap() as SuccessResponse<any>;
      const permissions: Permissions = responseJSON.body;
      expect(permissions.for(SupportedEntity.cluster_profile).permission).toEqual(clusterProfilePermission());
      expect(permissions.for(SupportedEntity.elastic_agent_profile).permission)
        .toEqual(elasticAgentProfilePermission());
      expect(permissions.for(SupportedEntity.config_repo).permission).toEqual(configRepoPermission());
      expect(permissions.for(SupportedEntity.environment).permission).toEqual(environmentPermission());
      done();
    });

    Permissions.all().then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(BASE_PATH);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
  });

  it("should request permissions only for specified entity", () => {
    jasmine.Ajax.stubRequest(BASE_PATH).andReturn(permissionsResponse());

    const onResponse = jasmine.createSpy();
    Permissions.all([SupportedEntity.environment]).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(`${BASE_PATH}?type=environment`);
  });

  it("should request permissions only for specified entities", () => {
    jasmine.Ajax.stubRequest(BASE_PATH).andReturn(permissionsResponse());

    const onResponse = jasmine.createSpy();
    Permissions.all([SupportedEntity.environment, SupportedEntity.config_repo]).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(`${BASE_PATH}?type=environment%2Cconfig_repo`);
  });

  it("should tell whether an entity could be administered", () => {
    const permission = new Permission(SupportedEntity.environment, environmentPermission());

    expect(permission.canAdminister("dev")).toBeTrue();
    expect(permission.canAdminister("prod")).toBeFalse();
    expect(permission.canView("uat")).toBeFalse();
  });

  it("should tell whether an entity could be viewed", () => {
    const permission = new Permission(SupportedEntity.environment, environmentPermission());

    expect(permission.canView("dev")).toBeTrue();
    expect(permission.canView("prod")).toBeTrue();
    expect(permission.canView("uat")).toBeFalse();

  });

  function permissionsResponse() {
    return {
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
        "ETag": "some-etag"
      },
      responseText: JSON.stringify(permissionsJSON())
    };
  }

  function clusterProfilePermission(): PermissionJSON {
    return {
      view: ["dev-cluster", "prod-cluster"],
      administer: ["dev-cluster"]
    };
  }

  function configRepoPermission(): PermissionJSON {
    return {
      view: ["dev-pipelines-repo", "prod-pipelines-repo"],
      administer: ["dev-pipelines-repo"]
    };
  }

  function elasticAgentProfilePermission(): PermissionJSON {
    return {
      view: ["dev-agent", "prod-agent"],
      administer: ["dev-agent"]
    };
  }

  function environmentPermission(): PermissionJSON {
    return {
      view: ["dev", "prod"],
      administer: ["dev"]
    };
  }

  function permissionsJSON(): PermissionsAPIJSON {
    return {
      permissions: {
        cluster_profile: clusterProfilePermission(),
        config_repo: configRepoPermission(),
        elastic_agent_profile: elasticAgentProfilePermission(),
        environment: environmentPermission()
      }
    };
  }

});
