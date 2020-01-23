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
import {GitMaterialAttributes, Material} from "models/materials/types";
import {Job} from "models/pipeline_configs/job";
import {PipelineParameter} from "models/pipeline_configs/parameter";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {ExecTask} from "models/pipeline_configs/task";
import {EnvironmentVariable, EnvironmentVariables} from "models/environment_variables/types";

describe("PipelineConfig model", () => {
  const defaultMaterials = [new Material("git", new GitMaterialAttributes(undefined, true, "https://github.com/gocd/gocd"))];
  const defaultStages    = [new Stage("stage1", [new Job("job1", [new ExecTask("echo", [])])])];

  it("should include a name", () => {
    let pip = new PipelineConfig("name", defaultMaterials, defaultStages).withGroup("foo");
    expect(pip.isValid()).toBe(true);
    expect(pip.errors().count()).toBe(0);

    pip = new PipelineConfig("", defaultMaterials, defaultStages).withGroup("foo");
    expect(pip.isValid()).toBe(false);
    expect(pip.errors().count()).toBe(1);
  });

  it("validate name format", () => {
    const pip = new PipelineConfig("my awesome pipeline that has a terrible name", defaultMaterials, defaultStages).withGroup("foo");
    expect(pip.isValid()).toBe(false);
    expect(pip.errors().count()).toBe(1);
    expect(pip.errors().keys()).toEqual(["name"]);
    expect(pip.errors().errorsForDisplay("name")).toBe("Invalid name. This must be alphanumeric and can contain hyphens, underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
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

  it("should validate parameters don't have duplicate names", () => {
    const pip    = new PipelineConfig("name", defaultMaterials, defaultStages).withGroup("foo");
    const param1 = new PipelineParameter("same_name", "foo");
    pip.parameters([param1, new PipelineParameter("same_name", "bar")]);

    expect(pip.isValid()).toBe(false);
    expect(pip.errors().keys()).toContain("parameters");
    expect(param1.errors().errorsForDisplay("name")).toBe("Parameter names must be unique.");
  });

  it("should include a material", () => {
    let pip = new PipelineConfig("name", defaultMaterials, defaultStages).withGroup("foo");
    expect(pip.isValid()).toBe(true);
    expect(pip.errors().count()).toBe(0);

    pip = new PipelineConfig("name", [], defaultStages).withGroup("foo");
    expect(pip.isValid()).toBe(false);
    expect(pip.errors().count()).toBe(1);
  });

  it("adopts errors in server response", () => {
    const pip = new PipelineConfig("meh", [
      new Material("git", new GitMaterialAttributes("one", true, "uh...")),
      new Material("git", new GitMaterialAttributes("two", true, "")),
    ], [
      new Stage("meow", [
        new Job("scooby", [], new EnvironmentVariables(
          new EnvironmentVariable("FOO", "OOF"),
          new EnvironmentVariable("BAR", "RAB")
        )), new Job("doo", [
          new ExecTask("whoami", []),
          new ExecTask("id", ["apache"])
        ])
      ]), new Stage("oink", [])
    ]).withGroup("foo");

    const unmatched = pip.consumeErrorsResponse({
      errors: {name: ["this name is fugly"]},
      materials: [{}, {
        errors: {
          url: ["you dolt! you can't have a blank url"],
          not_exist: ["well, ain't that a doozy"]
        }
      }],
      stages: [{
        errors: {name: ["yay"]}, jobs: [
          {
            errors: {name: ["ruh-roh!"]},
            tasks: [],
            environment_variables: [{}, {errors: {name: ["BAR? yes please!"]}}]
          },
          {tasks: [{errors: {command: ["who are you?"]}}, {}]}
        ]
      }, {errors: {name: ["boo"], jobs: ["all them other stages are taking our jobs"]}, jobs: []}]
    }, "pipeline");

    expect(unmatched.hasErrors()).toBe(true);
    expect(unmatched.errorsForDisplay("pipeline.materials[1].notExist")).toBe("well, ain't that a doozy.");

    expect(pip.errors().errorsForDisplay("name")).toBe("this name is fugly.");

    const materials = Array.from(pip.materials());
    expect(materials[0].attributes()!.errors().hasErrors()).toBe(false);
    expect(materials[1].attributes()!.errors().errorsForDisplay("url")).toBe("you dolt! you can't have a blank url.");

    const stages = Array.from(pip.stages());
    expect(stages[0].errors().errorsForDisplay("name")).toBe("yay.");
    expect(stages[1].errors().errorsForDisplay("name")).toBe("boo.");
    expect(stages[1].errors().errorsForDisplay("jobs")).toBe("all them other stages are taking our jobs.");

    const s1jobs = Array.from(stages[0].jobs());
    expect(s1jobs[0].errors().errorsForDisplay("name")).toBe("ruh-roh!.");
    expect(s1jobs[1].errors().hasErrors()).toBe(false);

    const s1j2tasks = s1jobs[1].tasks();
    expect(s1j2tasks[0].attributes().errors().errorsForDisplay("command")).toBe("who are you?.");
    expect(s1j2tasks[1].attributes().errors().hasErrors()).toBe(false);

    const s1j1envs = s1jobs[0].environmentVariables();
    expect(s1j1envs[0].errors().hasErrors()).toBe(false);
    expect(s1j1envs[1].errors().errorsForDisplay("name")).toBe("BAR? yes please!.");
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
});

function stubPipelineCreateSuccess(config: PipelineConfig) {
  jasmine.Ajax.stubRequest(SparkRoutes.pipelineConfigCreatePath(), undefined, "POST").andReturn({
    responseText: JSON.stringify(config.toApiPayload()),
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v7+json"
    }
  });
}

function stubPipelineTrigger(name: string) {
  jasmine.Ajax.stubRequest(SparkRoutes.pipelineTriggerPath(name), undefined, "POST").andReturn({
    responseText: JSON.stringify({message: `Request to schedule pipeline '${name}' accepted successfully.`}),
    status: 202,
    responseHeaders: {
      'Content-Type': 'application/vnd.go.cd.v1+json'
    }
  });
}
