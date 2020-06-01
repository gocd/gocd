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

import {PipelineGroupsJSON} from "models/internal_pipeline_structure/pipeline_structure";
import {EnvironmentsJSON, EnvironmentWithOrigin} from "models/new-environments/environments";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import data from "./test_data";

describe("EnvironmentsApiSpec", () => {

  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should make get all env request", () => {
    jasmine.Ajax.stubRequest("/go/api/admin/internal/environments/merged").andReturn(listEnvironmentResponse());
    EnvironmentsAPIs.all();
    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/admin/internal/environments/merged");
    expect(request.method).toEqual("GET");
    expect(request.data()).toEqual(toJSON({} as EnvironmentsJSON));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it("should make create request", () => {
    jasmine.Ajax.stubRequest("/go/api/admin/environments").andReturn(environmentWithEtag());

    const environment = EnvironmentWithOrigin.fromJSON(data.environment_json());
    EnvironmentsAPIs.create(environment);

    const request = jasmine.Ajax.requests.mostRecent();

    expect(request.url).toEqual("/go/api/admin/environments");
    expect(request.method).toEqual("POST");
    expect(request.data()).toEqual(toJSON(environment));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it("should make request to get all pipelines", () => {
    jasmine.Ajax.stubRequest("/go/api/internal/pipeline_structure").andReturn(pipelines());

    EnvironmentsAPIs.allPipelines("view", "view");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/internal/pipeline_structure?pipeline_group_authorization=view&template_authorization=view");
    expect(request.method).toEqual("GET");
    expect(request.data()).toEqual(toJSON({} as PipelineGroupsJSON));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it("should make request to get all pipeline groups", () => {
    jasmine.Ajax.stubRequest("/go/api/internal/pipeline_structure").andReturn(pipelines());

    EnvironmentsAPIs.allPipelineGroups("view");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/internal/pipeline_groups?pipeline_group_authorization=view");
    expect(request.method).toEqual("GET");
    expect(request.data()).toEqual(toJSON({} as PipelineGroupsJSON));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it("should make request to delete env", () => {
    jasmine.Ajax.stubRequest("/go/api/admin/environments/env").andReturn(deleteEnvResponse("env"));

    EnvironmentsAPIs.delete("env");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/admin/environments/env");
    expect(request.method).toEqual("DELETE");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual(undefined!);
    expect(request.requestHeaders["X-GoCD-Confirm"]).toEqual("true");
  });

  it("should make request for patch", () => {
    jasmine.Ajax.stubRequest("/go/api/admin/environments/env").andReturn(environmentWithEtag());

    const payload = {
      environment_variables: {
        add: [],
        remove: ["pipeline1"]
      }
    };
    EnvironmentsAPIs.patch("env", payload);

    const request = jasmine.Ajax.requests.mostRecent();

    expect(request.url).toEqual("/go/api/admin/environments/env");
    expect(request.method).toEqual("PATCH");
    expect(request.data()).toEqual(toJSON(payload));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it('should make request to update agent association', () => {
    jasmine.Ajax.stubRequest("/go/api/admin/internal/environments/env").andReturn(environmentWithEtag());

    const agentsToAssociate = ["agent1"];
    const agentsToRemove    = ["agent2"];
    const payload           = {
      agents: {
        add: agentsToAssociate,
        remove: agentsToRemove
      }
    };
    EnvironmentsAPIs.updateAgentAssociation("env", agentsToAssociate, agentsToRemove);

    const request = jasmine.Ajax.requests.mostRecent();

    expect(request.url).toEqual("/go/api/admin/internal/environments/env");
    expect(request.method).toEqual("PATCH");
    expect(request.data()).toEqual(toJSON(payload));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");

  });

  function toJSON(object: any) {
    return JSON.parse(JSON.stringify(object));
  }

  function environmentWithEtag() {
    return {
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
        "ETag": "some-etag"
      },
      responseText: JSON.stringify(data.environment_json())
    };
  }

  function listEnvironmentResponse() {
    return {
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8"
      },
      responseText: JSON.stringify(environments(data.environment_json()))
    };
  }

  function environments(...objects: any[]) {
    return {
      _links: {
        self: {
          href: "https://ci.example.com/go/api/admin/environments"
        },
        doc: {
          href: "https://api.gocd.org/#environment-config"
        },
        find: {
          href: "https://ci.example.com/go/api/admin/environments/:environment_name"
        }
      },
      _embedded: {
        environments: objects
      }
    } as EnvironmentsJSON;
  }

  function pipelines() {
    return {
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8"
      },
      responseText: JSON.stringify(data.pipeline_groups_json())
    };
  }

  function deleteEnvResponse(id: string) {
    return {
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd+json; charset=utf-8",
        "ETag": "some-etag"
      },
      responseText: JSON.stringify({
                                     message: `Environment ${id} was deleted successfully!`
                                   })
    };
  }

});
