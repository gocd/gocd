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

import {GitMaterialAttributes, Material} from "models/materials/types";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";

describe("PipelineConfig model", () => {
  const defaultMaterials = [new Material("git", new GitMaterialAttributes(undefined, true, "https://github.com/gocd/gocd"))];

  it("should include a name", () => {
    let pip = new PipelineConfig("name", defaultMaterials, [new Stage("stage1")]);
    expect(pip.isValid()).toBe(true);
    expect(pip.errors().count()).toBe(0);

    pip = new PipelineConfig("", defaultMaterials, [new Stage("stage1")]);
    expect(pip.isValid()).toBe(false);
    expect(pip.errors().count()).toBe(1);
  });

  it("should include a material", () => {
    let pip = new PipelineConfig("name", defaultMaterials, [new Stage("stage1")]);
    expect(pip.isValid()).toBe(true);
    expect(pip.errors().count()).toBe(0);

    pip = new PipelineConfig("name", [], [new Stage("stage1")]);
    expect(pip.isValid()).toBe(false);
    expect(pip.errors().count()).toBe(1);
  });

  it("should include a stage", () => {
    let pip = new PipelineConfig("name", defaultMaterials, [new Stage("stage1")]);
    expect(pip.isValid()).toBe(true);
    expect(pip.errors().count()).toBe(0);

    pip = new PipelineConfig("name", defaultMaterials, []);
    expect(pip.isValid()).toBe(false);
    expect(pip.errors().count()).toBe(1);
  });
});
