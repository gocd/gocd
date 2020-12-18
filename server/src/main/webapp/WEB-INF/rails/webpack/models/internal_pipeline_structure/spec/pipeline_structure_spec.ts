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

import {
  PipelineGroup,
  PipelineGroupJSON,
  PipelineJSON,
  Pipelines,
  PipelineStructure,
  PipelineStructureJSON,
  PipelineStructureWithAdditionalInfo,
  PipelineStructureWithAdditionalInfoJSON,
  PipelineWithOrigin
} from "models/internal_pipeline_structure/pipeline_structure";
import {Origin, OriginType} from "models/origin";

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
            environment: null,
            dependant_pipelines: [],
            stages: []
          },
          {
            name: "pipeline-using-template-defined-in-config-repo",
            origin: {
              type: "config_repo",
              id: "config-repo-1"
            },
            environment: null,
            dependant_pipelines: [],
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
    stages: [],
    environment: null,
    dependant_pipelines: [],
  }]
};

describe("PipelineGroups", () => {
  describe("containsRemotelyDefinedPipelines", () => {
    it("should return true if the pipelines defined remotely", () => {
      const pipelineGroup = PipelineGroup.fromJSON(pipelineGroupJSON);
      expect(pipelineGroup.containsRemotelyDefinedPipelines()).toBe(false);
    });

    it("should return false if the any of pipelines defined remotely", () => {
      const pipelineGroup      = PipelineGroup.fromJSON(pipelineGroupJSON);
      const pipelineWithOrigin = new PipelineWithOrigin("config-repo-pipeline",
                                                        undefined,
                                                        Origin.fromJSON({type: OriginType.ConfigRepo}),
                                                        [],
                                                        null,
                                                        []);
      pipelineGroup.pipelines().push(pipelineWithOrigin);
      expect(pipelineGroup.containsRemotelyDefinedPipelines()).toBe(true);
    });

    it("should return false pipelines are not available", () => {
      const pipelineGroup = PipelineGroup.fromJSON(pipelineGroupJSON);
      pipelineGroup.pipelines(new Pipelines());
      expect(pipelineGroup.containsRemotelyDefinedPipelines()).toBe(false);
    });
  });

  describe("canBeDeleted", () => {
    let pipelineJSON: PipelineJSON;

    beforeEach(() => {
      pipelineJSON = {
        name: "some-pipeline",
        origin: {type: OriginType.GoCD},
        stages: [],
        environment: null,
        dependant_pipelines: []
      };
    });

    it("should return true if not defined remotely, not in env and has no dependent pipelines", () => {
      const pipelineWithOrigin = PipelineWithOrigin.fromJSON(pipelineJSON);

      expect(pipelineWithOrigin.canBeDeleted()).toBeTrue();
    });

    it("should return false if defined remotely", () => {
      pipelineJSON.origin.type = OriginType.ConfigRepo;
      pipelineJSON.origin.id   = "config-repo-id";
      const pipelineWithOrigin = PipelineWithOrigin.fromJSON(pipelineJSON);

      expect(pipelineWithOrigin.canBeDeleted()).toBeFalse();
    });

    it("should return false if part of environment", () => {
      pipelineJSON.environment = "test-env";
      const pipelineWithOrigin = PipelineWithOrigin.fromJSON(pipelineJSON);

      expect(pipelineWithOrigin.canBeDeleted()).toBeFalse();
    });

    it("should return false if has dependant pipelines", () => {
      pipelineJSON.dependant_pipelines = [{
        dependent_pipeline_name: "test-pipeline1",
        depends_on_stage: "stage1"
      }];

      const pipelineWithOrigin = PipelineWithOrigin.fromJSON(pipelineJSON);

      expect(pipelineWithOrigin.canBeDeleted()).toBeFalse();
    });
  });

  it('should return true if search string matches name of the grp or pipelines', () => {
    const pipelineGroup = PipelineGroup.fromJSON(pipelineGroupJSON);

    expect(pipelineGroup.matches("group")).toBeTrue();
    expect(pipelineGroup.matches("some-")).toBeTrue();
    expect(pipelineGroup.matches("pipeline")).toBeTrue();
    expect(pipelineGroup.matches("mas")).toBeFalse();
    expect(pipelineGroup.matches("abc")).toBeFalse();
  });
});

describe("PipelineStructureWithAdditionalInfo", () => {
  it("should serialize from json", () => {
    const json = {
      groups: [],
      templates: [],
      additional_info: {
        users: ["user1", "user2"],
        roles: ["role1", "role2"]
      }
    } as PipelineStructureWithAdditionalInfoJSON;

    const pipelineStructure = PipelineStructureWithAdditionalInfo.fromJSON(json);

    expect(pipelineStructure.pipelineStructure.groups()).toEqual([]);
    expect(pipelineStructure.pipelineStructure.templates()).toEqual([]);
    expect(pipelineStructure.additionalInfo.users).toBe(json.additional_info.users);
    expect(pipelineStructure.additionalInfo.roles).toBe(json.additional_info.roles);
  });
});
