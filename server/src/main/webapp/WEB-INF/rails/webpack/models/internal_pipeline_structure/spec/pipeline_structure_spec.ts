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

import {PipelineStructure, PipelineStructureJSON} from "models/internal_pipeline_structure/pipeline_structure";

describe("Pipeline Structure", () => {
  let json: PipelineStructureJSON, pipelineStructure: PipelineStructure;
  beforeEach(() => {
    json              = pipelineStructureJSON;
    pipelineStructure = PipelineStructure.fromJSON(json);
  });

  it("should find a pipeline", () => {
    expect(pipelineStructure.findPipeline(json.groups[0].pipelines[0].name)).not.toBeUndefined();
    expect(pipelineStructure.findPipeline(json.groups[0].pipelines[1].name)).not.toBeUndefined();
  });

  it("should return undefined for a non existing pipeline", () => {
    expect(pipelineStructure.findPipeline("blah")).toBeUndefined();
  });

  it("should return pipelines defined in config xml which are not using templates", () => {
    const pipelinesNotUsingTemplates = pipelineStructure.getAllConfigPipelinesNotUsingTemplates();

    expect(pipelinesNotUsingTemplates.length).toBe(1);
    expect(pipelinesNotUsingTemplates).toEqual(["pipeline-using-template-defined-in-xml"]);
  });

  const pipelineStructureJSON = {
    groups: [
      {
        name: "group1",
        pipelines: [
          {
            name: "pipeline-using-template-defined-in-xml",
            origin: {
              type: "gocd"
            },
            stages: []
          },
          {
            name: "pipeline-using-template-defined-in-config-repo",
            origin: {
              type: "config_repo",
              id: "config-repo-1"
            },
            stages: []
          }
        ]
      }
    ],
    templates: []
  } as PipelineStructureJSON;
});
