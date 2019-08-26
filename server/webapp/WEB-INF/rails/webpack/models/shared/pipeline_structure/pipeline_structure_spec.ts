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

import {OriginType} from "models/shared/origin";
import {
  Job,
  Pipeline, PipelineGroup,
  PipelineStructure,
  Stage
} from "models/shared/pipeline_structure/pipeline_structure";

export function pipelineStructure() {
  return PipelineStructure.fromJSON(
    {
      groups: [{
        name: "g1",
        pipelines: [
          {
            name: "g1p1",
            origin: {
              type: OriginType.GOCD
            },
            stages: [{
              name: "s1",
              jobs: [
                {
                  name: "j1"
                }
              ]
            }
            ]
          },
          {
            name: "g1p2",
            template_name: "template-1",
            origin: {
              type: OriginType.CONFIG_REPO
            },
            stages: []
          }]
      }], templates: [
        {
          name: "template-1",
          stages: [{
            name: "s1",
            jobs: [{name: "j1", is_elastic: true}]
          }]
        }
      ]
    });
}

describe("Pipeline Structure", () => {
  it("should return pipeline proups", () => {
    expect(pipelineStructure().groups).toHaveLength(1);
    expect(pipelineStructure().groups[0].name).toEqual("g1");
    expect(pipelineStructure().groups[0].children).toHaveLength(2);
    expect(pipelineStructure().groups[0].children[0].name).toEqual("g1p1");
    expect(pipelineStructure().groups[0].children[0].origin.type).toEqual("gocd");
    expect(pipelineStructure().groups[0].children[0].children[0].name).toEqual("s1");
    expect(pipelineStructure().groups[0].children[0].children[0].children[0].name).toEqual("j1");

    expect(pipelineStructure().groups[0].children[1].name).toEqual("g1p2");
    expect(pipelineStructure().groups[0].children[1].templateName).toEqual("template-1");
    expect(pipelineStructure().groups[0].children[1].children).toHaveLength(0);
  });

  it("should return templates", () => {
    expect(pipelineStructure().templates).toHaveLength(1);
    expect(pipelineStructure().templates[0].name).toEqual("template-1");
    expect(pipelineStructure().templates[0].children).toHaveLength(1);
    expect(pipelineStructure().templates[0].children[0].name).toEqual("s1");
    expect(pipelineStructure().templates[0].children[0].children[0].name).toEqual("j1");
  });
  it("should return true if pipleine is specified using config repo", () => {
    expect(pipelineStructure().groups[0].children[1].hasConfigRepoOrigin()).toEqual(true);
  });
  describe("Clicking on a job", () => {

    let stageOne: Stage, stageTwo: Stage;
    let pipelineOne: Pipeline;

    beforeEach(() => {
      stageOne = new Stage("s1", [new Job("j1"), new Job("j2"), new Job("j3")]);
      stageTwo = new Stage("s2", [new Job("j1"), new Job("j2"), new Job("j3")]);

      pipelineOne = new Pipeline("p1", undefined, {type: OriginType.GOCD}, [stageOne, stageTwo]);
    });

    it("should change the triState to `indeterminate` when some children are selected", () => {
      expect(pipelineOne.checkboxState()().isUnchecked()).toBe(true);

      stageOne.children[1].checkboxState()().click();
      expect(pipelineOne.checkboxState()().isIndeterminate()).toBe(true);
      expect(stageOne.checkboxState()().isIndeterminate()).toBe(true);

      stageOne.children[0].checkboxState()().click();
      expect(pipelineOne.checkboxState()().isIndeterminate()).toBe(true);
      expect(stageOne.checkboxState()().isIndeterminate()).toBe(true);

      stageOne.children[2].checkboxState()().click();
      expect(pipelineOne.checkboxState()().isIndeterminate()).toBe(true);
      expect(stageOne.checkboxState()().isChecked()).toBe(true);
    });

    it("should change the triState to `on` when all children are selected", () => {
      expect(stageOne.checkboxState()().isChecked()).toBe(false);
      stageOne.children.forEach((child) => child.checkboxState()().click());
      expect(stageOne.checkboxState()().isChecked()).toBe(true);
    });

    it("should change the triState to `off` when all children are unselected", () => {
      stageOne.children[1].checkboxState()().click();
      expect(stageOne.checkboxState()().isIndeterminate()).toBe(true);

      stageOne.children[1].checkboxState()().click();
      expect(stageOne.checkboxState()().isUnchecked()).toBe(true);
    });

    it("should change the triState of all `off` children to `on` when parent is selected", () => {
      expect(stageOne.checkboxState()().isUnchecked()).toBe(true);
      stageOne.wasClicked();

      stageOne.children.forEach((child) => {
        expect(child.checkboxState()().isChecked()).toBe(true);
      });
      expect(stageOne.checkboxState()().isChecked()).toBe(true);
    });

    it("should change the triState of all `on` children to `off` when parent is unselected", () => {
      stageOne.children.forEach((child) => {
        child.checkboxState()().click();
      });

      expect(stageOne.checkboxState()().isChecked()).toBe(true);
      stageOne.wasClicked();

      stageOne.children.forEach((child) => {
        expect(child.checkboxState()().isUnchecked()).toBe(true);
      });
    });

    it("should change the triState of all children to `on` when parent is clicked, and was in `indeterminate` state",
       () => {
         stageOne.children[0].checkboxState()().click();

         expect(stageOne.checkboxState()().isIndeterminate()).toBe(true);
         stageOne.wasClicked();

         stageOne.children.forEach((child) => {
           expect(child.checkboxState()().isChecked()).toBe(true);
         });
       });
  });
  describe("Help Text", () => {
    let configRepoPipeline: Pipeline;
    let stageOne;
    let stageTwo;
    let pipelineGroup: PipelineGroup;
    let pipelineUsingTemplate: Pipeline;
    let gocdPipeline: Pipeline;

    beforeEach(() => {
      stageOne = new Stage("s1", [new Job("j1"), new Job("j2"), new Job("j3")]);
      stageTwo = new Stage("s2", [new Job("j1"), new Job("j2"), new Job("j3")]);

      gocdPipeline          = new Pipeline("p1", undefined, {type: OriginType.GOCD}, [stageOne, stageTwo]);
      configRepoPipeline    = new Pipeline("p1-using-config-repo",
                                           undefined,
                                           {type: OriginType.CONFIG_REPO},
                                           [stageOne, stageTwo]);
      pipelineUsingTemplate = new Pipeline("p1-using-template", "template-1", {type: OriginType.GOCD}, []);
      pipelineGroup         = new PipelineGroup("group-1", [configRepoPipeline]);
    });
    it(
      "pipeline group checkbox should be readonly and show help text if all pipeline in group are specified using config repo",
      () => {
        expect(pipelineGroup.readonly()).toEqual(true);

        expect(pipelineGroup.readonlyReason()).toEqual(`You can't associate agent to pipeline group - ${pipelineGroup.name} as specified in config repo`);
        pipelineGroup.children.forEach((pipeline) => {
          expect(pipeline.readonly()).toEqual(true);
          expect(pipeline.readonlyReason()).toEqual(`You can't associate agent to pipeline - ${pipeline.name} as specified in config repo`);
        });
      });

    it(
      "pipeline group checkbox should be readonly and show help text if all pipelines in group are specified using templates",
      () => {
        const pipelineGroupUsingTemplate = new PipelineGroup("group-1", [pipelineUsingTemplate]);
        expect(pipelineGroupUsingTemplate.readonly()).toEqual(true);

        expect(pipelineGroupUsingTemplate.readonlyReason()).toEqual(`You can't associate agent to pipeline group - ${pipelineGroupUsingTemplate.name} as specified in template`);
        pipelineGroupUsingTemplate.children.forEach((pipeline) => {
          expect(pipeline.readonly()).toEqual(true);
          expect(pipeline.readonlyReason()).toEqual(`You can't associate agent to pipeline - ${pipeline.name} as specified in template`);
        });
      });
    it(
      "pipeline group checkbox should not be readonly and should not have an help text if at least one of the pipeline have gocd origin",
      () => {
        const pipelineGroup1 = new PipelineGroup("group-2", [gocdPipeline, configRepoPipeline, pipelineUsingTemplate]);
        expect(pipelineGroup1.readonly()).toBe(false);
        expect(pipelineGroup1.readonlyReason()).toBe(undefined);
      });
  });
});
