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

import SparkRoutes from "helpers/spark_routes";
import {GitMaterialAttributes, Material} from "models/materials/types";
import {Job} from "models/pipeline_configs/job";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {ExecTask} from "models/pipeline_configs/task";

describe("PipelineConfig model", () => {
  const defaultMaterials = [new Material("git", new GitMaterialAttributes(undefined, true, "https://github.com/gocd/gocd"))];
  const defaultStages = [new Stage("stage1", [new Job("job1", [new ExecTask("echo", [])])])];

  it("should include a name", () => {
    let pip = new PipelineConfig("name", defaultMaterials, defaultStages);
    expect(pip.isValid()).toBe(true);
    expect(pip.errors().count()).toBe(0);

    pip = new PipelineConfig("", defaultMaterials, defaultStages);
    expect(pip.isValid()).toBe(false);
    expect(pip.errors().count()).toBe(1);
  });

  it("validate name format", () => {
    const pip = new PipelineConfig("my awesome pipeline that has a terrible name", defaultMaterials, defaultStages);
    expect(pip.isValid()).toBe(false);
    expect(pip.errors().count()).toBe(1);
    expect(pip.errors().keys()).toEqual(["name"]);
    expect(pip.errors().errorsForDisplay("name")).toBe("Invalid name. This must be alphanumeric and can contain hyphens, underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
  });

  it("should include a material", () => {
    let pip = new PipelineConfig("name", defaultMaterials, defaultStages);
    expect(pip.isValid()).toBe(true);
    expect(pip.errors().count()).toBe(0);

    pip = new PipelineConfig("name", [], defaultStages);
    expect(pip.isValid()).toBe(false);
    expect(pip.errors().count()).toBe(1);
  });

  it("should include a stage", () => {
    let pip = new PipelineConfig("name", defaultMaterials, defaultStages);
    expect(pip.isValid()).toBe(true);
    expect(pip.errors().count()).toBe(0);

    pip = new PipelineConfig("name", defaultMaterials, []);
    expect(pip.isValid()).toBe(false);
    expect(pip.errors().count()).toBe(1);
  });

  it("create()", (done) => {
    jasmine.Ajax.withMock(() => {
      const config = new PipelineConfig("name", defaultMaterials, defaultStages);
      stubPipelineCreateSuccess(config);

      config.create().then((response) => {
        expect(response.getStatusCode()).toBe(200);
        expect(() => response.getOrThrow()).not.toThrow();
        expect(JSON.parse(response.getOrThrow())).toEqual(config.toApiPayload());
        done();
      });
    });
  });

  it("run()", (done) => {
    jasmine.Ajax.withMock(() => {
      const config = new PipelineConfig("name", defaultMaterials, defaultStages);
      stubPipelineTrigger(config.name());

      config.run().then((response) => {
        expect(response.getStatusCode()).toBe(202);
        expect(() => response.getOrThrow()).not.toThrow();
        expect(JSON.parse(response.getOrThrow())).toEqual({message: `Request to schedule pipeline '${config.name()}' accepted successfully.`});
        done();
      });
    });
  });

  it("pause()", (done) => {
    jasmine.Ajax.withMock(() => {
      const config = new PipelineConfig("name", defaultMaterials, defaultStages);
      stubPipelinePause(config.name());

      config.pause().then((response) => {
        expect(response.getStatusCode()).toBe(202);
        expect(() => response.getOrThrow()).not.toThrow();
        expect(JSON.parse(response.getOrThrow())).toEqual({message: `Pipeline '${config.name()}' paused successfully.`});
        done();
      });
    });
  });
});

function stubPipelineCreateSuccess(config: PipelineConfig) {
  jasmine.Ajax.stubRequest(SparkRoutes.pipelineConfigCreatePath(), undefined, "POST").
  andReturn({
    responseText:    JSON.stringify(config.toApiPayload()),
    status:          200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v7+json"
    }
  });
}

function stubPipelineTrigger(name: string) {
  jasmine.Ajax.stubRequest(SparkRoutes.pipelineTriggerPath(name), undefined, "POST").andReturn({
    responseText:    JSON.stringify({message: `Request to schedule pipeline '${name}' accepted successfully.`}),
    status:          202,
    responseHeaders: {
      'Content-Type': 'application/vnd.go.cd.v1+json'
    }
  });
}

function stubPipelinePause(name: string) {
  jasmine.Ajax.stubRequest(SparkRoutes.pipelinePausePath(name), undefined, "POST").andReturn({
    responseText:    JSON.stringify({message: `Pipeline '${name}' paused successfully.`}),
    status:          202,
    responseHeaders: {
      'Content-Type': 'application/vnd.go.cd.v1+json'
    }
  });
}
