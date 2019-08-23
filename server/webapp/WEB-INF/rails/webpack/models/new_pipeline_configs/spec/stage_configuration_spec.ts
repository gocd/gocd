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

import {StageConfig} from "models/new_pipeline_configs/stage_configuration";

describe("Pipeline Config - Stages Model", () => {
  describe("Stage model", () => {
    it("should include a name", () => {
      let stage = new StageConfig("stage1");
      expect(stage.isValid()).toBe(true);
      expect(stage.errors().count()).toBe(0);

      stage = new StageConfig("");
      expect(stage.isValid()).toBe(false);
      expect(stage.errors().count()).toBe(1);
      expect(stage.errors().keys()).toEqual(["name"]);
    });

    it("should set approval to success by default", () => {
      const stage = new StageConfig("stage1");
      expect(stage.isValid()).toBe(true);
      expect(stage.errors().count()).toBe(0);

      expect(stage.approval().state()).toBe(true);
      expect(stage.approval().type()).toBe("success");
    });

    it("should toggle approval type when state is toggled", () => {
      const stage = new StageConfig("stage1");
      expect(stage.isValid()).toBe(true);
      expect(stage.errors().count()).toBe(0);

      expect(stage.approval().state()).toBe(true);
      expect(stage.approval().type()).toBe("success");

      stage.approval().state(!stage.approval().state());

      expect(stage.approval().state()).toBe(false);
      expect(stage.approval().type()).toBe("manual");
    });

    it("should set fetchMaterials to false by default", () => {
      const stage = new StageConfig("stage1");

      expect(stage.fetchMaterials()).toBe(false);
    });

    it("should allow toggling fetchMaterials", () => {
      const stage = new StageConfig("stage1");

      expect(stage.fetchMaterials()).toBe(false);
      stage.fetchMaterials(!stage.fetchMaterials());
      expect(stage.fetchMaterials()).toBe(true);
    });

    it("should set neverCleanupArtifacts to false by default", () => {
      const stage = new StageConfig("stage1");

      expect(stage.neverCleanupArtifacts()).toBe(false);
    });

    it("should allow toggling neverCleanupArtifacts", () => {
      const stage = new StageConfig("stage1");

      expect(stage.neverCleanupArtifacts()).toBe(false);
      stage.neverCleanupArtifacts(!stage.neverCleanupArtifacts());
      expect(stage.neverCleanupArtifacts()).toBe(true);
    });

    it("should set cleanWorkingDirectory to false by default", () => {
      const stage = new StageConfig("stage1");

      expect(stage.cleanWorkingDirectory()).toBe(false);
    });

    it("should allow toggling cleanWorkingDirectory", () => {
      const stage = new StageConfig("stage1");

      expect(stage.cleanWorkingDirectory()).toBe(false);
      stage.cleanWorkingDirectory(!stage.cleanWorkingDirectory());
      expect(stage.cleanWorkingDirectory()).toBe(true);
    });

    it("validates name format", () => {
      const expectedError = "Invalid name. This must be alphanumeric and can contain hyphens, underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.";

      const stage = new StageConfig("my awesome stage that has a terrible name");
      expect(stage.isValid()).toBe(false);
      expect(stage.errors().count()).toBe(1);
      expect(stage.errors().keys()).toEqual(["name"]);
      expect(stage.errors().errorsForDisplay("name")).toBe(expectedError);
    });
  });

});
