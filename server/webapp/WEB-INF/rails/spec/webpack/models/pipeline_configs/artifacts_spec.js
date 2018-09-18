/*
 * Copyright 2015 ThoughtWorks, Inc.
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
describe("Artifact Model", () => {
  const Artifacts = require("models/pipeline_configs/artifacts");
  let artifact;
  beforeEach(() => {
    artifact = new Artifacts.Artifact({
      type:        "test",
      source:      "dist/pkg",
      destination: 'pkg'
    });
  });


  it("should initialize artifact model with type", () => {
    expect(artifact.type()).toBe("test");
  });

  it("should initialize artifact model with source", () => {
    expect(artifact.source()).toBe('dist/pkg');
  });

  it("should initialize artifact model with destination", () => {
    expect(artifact.destination()).toBe('pkg');
  });

  describe("validations", () => {
    it("should add error when source is blank", () => {
      artifact.source("");
      const errors = artifact.validate();
      expect(errors.errors('source')).toEqual(['Source must be present']);
    });

    it("should NOT add error when both source and destination are blank", () => {
      artifact.destination("");
      artifact.source("");
      const errors = artifact.validate();
      expect(errors._isEmpty()).toBe(true);
    });
  });

  describe("Deserialization from JSON", () => {
    beforeEach(() => {
      artifact = Artifacts.Artifact.fromJSON(sampleArtifactJSON());
    });

    it("should initialize from json", () => {
      expect(artifact.type()).toBe("test");
      expect(artifact.source()).toBe('dist/pkg');
      expect(artifact.destination()).toBe("pkg");
    });

    function sampleArtifactJSON() {
      return {
        type:        "test",
        source:      'dist/pkg',
        destination: 'pkg'
      };
    }
  });

});
