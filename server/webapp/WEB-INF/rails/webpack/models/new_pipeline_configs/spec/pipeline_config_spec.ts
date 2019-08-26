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

import {SparkRoutes} from "helpers/spark_routes";
import {GitMaterialAttributes, Material} from "models/new_pipeline_configs/materials";
import {PipelineConfig} from "models/new_pipeline_configs/pipeline_config";
import {StageConfig} from "models/new_pipeline_configs/stage_configuration";

describe("PipelineConfig model", () => {
  const defaultMaterials = [new Material("git", new GitMaterialAttributes("https://github.com/gocd/gocd", "", true))];
  const defaultStages    = [new StageConfig("stage1")];

  it("should include a name", () => {
    let pip = new PipelineConfig("name", defaultMaterials, defaultStages).withGroup("foo");
    expect(pip.isValid()).toBe(true);
    expect(pip.errors().count()).toBe(0);

    pip = new PipelineConfig("", defaultMaterials, defaultStages).withGroup("foo");
    expect(pip.isValid()).toBe(false);
    expect(pip.errors().count()).toBe(1);
  });

  it("validate name format", () => {
    const pip              = new PipelineConfig("my awesome pipeline that has a terrible name", defaultMaterials, defaultStages).withGroup("foo");
    const expectedErrorMsg = "Invalid name. This must be alphanumeric and can contain hyphens, underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.";

    expect(pip.isValid()).toBe(false);
    expect(pip.errors().count()).toBe(1);
    expect(pip.errors().keys()).toEqual(["name"]);
    expect(pip.errors().errorsForDisplay("name")).toBe(expectedErrorMsg);
  });

  it("should validate mutual exclusivity of template and stages", () => {
    const pip = new PipelineConfig("name", defaultMaterials, defaultStages).withGroup("foo");
    expect(pip.isValid()).toBe(true);

    pip.template("wubba_lubba_dub_dub");
    expect(pip.isValid()).toBe(false);
    expect(pip.errors().errorsForDisplay("template")).toBe("Pipeline stages must not be defined when using a pipeline template.");

    pip.stages().clear();
    expect(pip.isValid()).toBe(true);
  });

  it("should include a material", () => {
    let pip = new PipelineConfig("name", defaultMaterials, defaultStages).withGroup("foo");

    expect(pip.isValid()).toBe(true);
    expect(pip.errors().count()).toBe(0);

    pip = new PipelineConfig("name", [], defaultStages).withGroup("foo");
    expect(pip.isValid()).toBe(false);
    expect(pip.errors().count()).toBe(1);
  });

  it("create()", (done) => {
    jasmine.Ajax.withMock(() => {
      const config = new PipelineConfig("name", defaultMaterials, defaultStages).withGroup("foo");
      stubPipelineCreateSuccess(config);

      config.create(false).then((response) => {
        expect(response.getStatusCode()).toBe(200);
        expect(() => response.getOrThrow()).not.toThrow();
        expect(JSON.parse(response.getOrThrow())).toEqual(config.toApiPayload());
        done();
      });
    });
  });

  it("run()", (done) => {
    jasmine.Ajax.withMock(() => {
      const config = new PipelineConfig("name", defaultMaterials, defaultStages).withGroup("foo");
      stubPipelineTrigger(config.name());

      config.run().then((response) => {
        expect(response.getStatusCode()).toBe(202);
        expect(() => response.getOrThrow()).not.toThrow();
        expect(JSON.parse(response.getOrThrow())).toEqual({message: `Request to schedule pipeline '${config.name()}' accepted successfully.`});
        done();
      });
    });
  });

  it("adopts errors in server response", () => {
    const pip = new PipelineConfig("New pipeline", [
      new Material("git", new GitMaterialAttributes("one", "uh...", true)),
      new Material("git", new GitMaterialAttributes("two", "", true)),
    ], [new StageConfig("meow"), new StageConfig("oink")]).withGroup("foo");

    const unmatched = pip.consumeErrorsResponse({
                                                  errors: {name: ["Invalid name"]},
                                                  materials: [{}, {errors: {url: ["Invalid Url"], not_exist: ["Url cannot be blank"]}}],
                                                  stages: [{errors: {name: ["Invalid name"]}}, {errors: {name: ["Invalid name"]}}]
                                                }, "pipeline");

    expect(unmatched.hasErrors()).toBe(true);
    expect(unmatched.errorsForDisplay("pipeline.materials[1].notExist")).toBe("Url cannot be blank.");

    expect(pip.errors().errorsForDisplay("name")).toBe("Invalid name.");

    const materials = Array.from(pip.materials());
    expect(materials[0].attributes().errors().hasErrors()).toBe(false);
    expect(materials[1].attributes().errors().errorsForDisplay("url")).toBe("Invalid Url.");

    const stages = Array.from(pip.stages());
    expect(stages[0].errors().errorsForDisplay("name")).toBe("Invalid name.");
    expect(stages[1].errors().errorsForDisplay("name")).toBe("Invalid name.");

  });
});

function stubPipelineCreateSuccess(config: PipelineConfig) {
  jasmine.Ajax.stubRequest(SparkRoutes.pipelineConfigCreatePath(), undefined, "POST")
         .andReturn({
                      responseText: JSON.stringify(
                        config.toApiPayload()),
                      status: 200,
                      responseHeaders: {
                        "Content-Type": "application/vnd.go.cd.v7+json"
                      }
                    });
}

function stubPipelineTrigger(name: string) {
  jasmine.Ajax.stubRequest(SparkRoutes.pipelineTriggerPath(name), undefined, "POST")
         .andReturn({
                      responseText: JSON.stringify(
                        {message: `Request to schedule pipeline '${name}' accepted successfully.`}),
                      status: 202,
                      responseHeaders: {
                        "Content-Type": "application/vnd.go.cd.v1+json"
                      }
                    });
}
