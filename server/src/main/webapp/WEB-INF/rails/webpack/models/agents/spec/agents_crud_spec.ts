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

import {AgentsCRUD} from "../agents_crud";
import {AgentsJSON} from "../agents_json";
import {AgentsTestData} from "./agents_test_data";

describe('AgentsCRUD', () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should make get request", () => {
    jasmine.Ajax.stubRequest("/go/api/agents").andReturn(listAgentsResponse());

    AgentsCRUD.all();

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/agents");
    expect(request.method).toEqual("GET");
    expect(request.data()).toEqual(toJSON({} as AgentsJSON));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it("should make a delete request", () => {
    jasmine.Ajax.stubRequest("/go/api/agents").andReturn(listAgentsResponse());

    const agentUuids = ['agent1', 'agent2'];
    AgentsCRUD.delete(agentUuids);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/agents");
    expect(request.method).toEqual("DELETE");
    expect(request.data()).toEqual({ uuids: agentUuids });
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

});

function toJSON(object: any) {
  return JSON.parse(JSON.stringify(object));
}

function listAgentsResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v6+json; charset=utf-8"
    },
    responseText: JSON.stringify(AgentsTestData.list())
  };
}
