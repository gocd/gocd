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
define(["pipeline_configs/models/artifacts"], function (Artifacts) {
  var artifact;
  beforeEach(function () {
    artifact = new Artifacts.Artifact({
      type:        "test",
      source:      "dist/pkg",
      destination: 'pkg'
    });
  });

  describe("Artifact Model", function () {
    it("should initialize artifact model with type", function () {
      expect(artifact.type()).toBe("test");
    });

    it("should initialize artifact model with source", function () {
      expect(artifact.source()).toBe('dist/pkg');
    });

    it("should initialize artifact model with destination", function () {
      expect(artifact.destination()).toBe('pkg');
    });

    describe("validations", function () {
      it("should add error when source is blank", function () {
        artifact.source("");
        var errors = artifact.validate();
        expect(errors.errors('source')).toEqual(['Source must be present']);
      });

      it("should add error when destination is blank", function () {
        artifact.destination("");
        var errors = artifact.validate();
        expect(errors.errors('destination')).toEqual(['Destination must be present']);
      });

      it("should NOT add error when both source and destination are blank", function () {
        artifact.destination("");
        artifact.source("");
        var errors = artifact.validate();
        expect(errors._isEmpty()).toBe(true);
      });
    });

    describe("Deserialization from JSON", function () {
      beforeEach(function () {
        artifact = Artifacts.Artifact.fromJSON(sampleArtifactJSON());
      });

      it("should initialize from json", function () {
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
});
