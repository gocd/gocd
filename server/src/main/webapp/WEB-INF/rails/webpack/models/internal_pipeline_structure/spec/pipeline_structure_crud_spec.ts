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
import {PipelineStructureWithSuggestions, PipelineStructureWithSuggestionsJSON} from "../pipeline_structure";
import {PipelineStructureCRUD} from "../pipeline_structure_crud";

describe('PipelineStructureCRUDSpec', () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should make get all env request", (done) => {
    const apiPath = SparkRoutes.apiAdminInternalPipelinesListPathWithSuggestions("view", "view");
    jasmine.Ajax.stubRequest(apiPath).andReturn(listResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON      = response.unwrap() as SuccessResponse<any>;
      const pipelineStructure = (responseJSON.body as PipelineStructureWithSuggestions);

      const groups = pipelineStructure.pipelineStructure.groups();
      expect(groups).toHaveLength(1);
      expect(groups[0].name()).toEqual('first');
      expect(groups[0].pipelines()).toHaveLength(1);
      expect(pipelineStructure.pipelineStructure.templates()).toEqual([]);
      expect(pipelineStructure.users).toEqual(json.users);
      expect(pipelineStructure.roles).toEqual(json.roles);
      done();
    });

    PipelineStructureCRUD.allPipelines("view", "view").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(apiPath);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });
});

function listResponse() {
  return {
    status:          200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8"
    },
    responseText:    JSON.stringify(json)
  };
}

const json = {
  groups:    [{
    name:      "first",
    pipelines: [{
      name:   "up42",
      origin: {
        type: "gocd"
      },
      stages: [{
        name: "up42_stage",
        jobs: [{
          name:       "up42_job",
          is_elastic: false
        }]
      }]
    }]
  }],
  templates: [],
  users:     ["view", "operate", "admin"],
  roles:     ["xyz"]
} as PipelineStructureWithSuggestionsJSON;
