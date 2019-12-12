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

import {PipelineGroup, PipelineGroupJSON, Pipelines, PipelineStructure, PipelineStructureJSON, PipelineWithOrigin} from "models/internal_pipeline_structure/pipeline_structure";
import {Origin, OriginType} from "../../origin";

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

const pipelineGroupJSON: PipelineGroupJSON = {
  name: "group-1",
  pipelines: [{
    name: "some-pipeline",
    origin: {type: OriginType.GoCD},
    stages: []
  }]
};

describe('PipelineGroups', () => {
  describe('containsRemotelyDefinedPipelines', () => {
    it("should return true if the pipelines defined remotely", () => {
      const pipelineGroup = PipelineGroup.fromJSON(pipelineGroupJSON);
      expect(pipelineGroup.containsRemotelyDefinedPipelines()).toBe(false);
    });

    it("should return false if the any of pipelines defined remotely", () => {
      const pipelineGroup      = PipelineGroup.fromJSON(pipelineGroupJSON);
      const pipelineWithOrigin = new PipelineWithOrigin("config-repo-pipeline", undefined, Origin.fromJSON({type: OriginType.ConfigRepo}), []);
      pipelineGroup.pipelines().push(pipelineWithOrigin);
      expect(pipelineGroup.containsRemotelyDefinedPipelines()).toBe(true);
    });

    it("should return false pipelines are not available", () => {
      const pipelineGroup      = PipelineGroup.fromJSON(pipelineGroupJSON);
      pipelineGroup.pipelines(new Pipelines());
      expect(pipelineGroup.containsRemotelyDefinedPipelines()).toBe(false);
    });

  });
});
