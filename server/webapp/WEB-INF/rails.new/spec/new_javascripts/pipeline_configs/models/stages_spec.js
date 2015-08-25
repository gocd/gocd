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

define(['lodash', "pipeline_configs/models/stages", "string-plus"], function (_, Stages, s) {
  describe("Stages Model", function () {
    var stages, stage;
    beforeEach(function () {
      stages = new Stages();
      stage  = stages.createStage({
        name:                  "UnitTest",
        fetchMaterials:        true,
        cleanWorkingDirectory: true,
        neverCleanArtifacts:   true,
        environmentVariables:  ["foo=bar", "boo=baz"]
      });
    });

    it("should initialize stage model with name", function () {
      expect(stage.name()).toBe("UnitTest");
    });

    it("should initialize stage model with fetchMaterials", function () {
      expect(stage.fetchMaterials()).toBe(true);
    });

    it("should initialize stage model with cleanWorkingDirectory", function () {
      expect(stage.cleanWorkingDirectory()).toBe(true);
    });

    it("should initialize stage model with neverCleanArtifacts", function () {
      expect(stage.neverCleanArtifacts()).toBe(true);
    });

    it("should initialize stage model with environmentVariables", function () {
      expect(stage.environmentVariables()).toEqual(['foo=bar', 'boo=baz']);
    });

    describe("validations", function () {
      it("should not allow blank stage names", function () {
        var errors         = stage.validate();
        expect(errors._isEmpty()).toBe(true);

        stage.name("");
        var duplicateStage = stages.createStage({name: stage.name()});

        errors                = stage.validate();
        var errorsOnDuplicate = duplicateStage.validate();

        expect(errors.errors('name')).toEqual(['Name must be present']);
        expect(errorsOnDuplicate.errors('name')).toEqual(['Name must be present']);
      });

      it("should not allow duplicate stage names", function () {
        var errorsOnOriginal = stage.validate();
        expect(errorsOnOriginal._isEmpty()).toBe(true);

        var duplicateStage = stages.createStage({
          name: "UnitTest"
        });

        errorsOnOriginal = stage.validate();
        expect(errorsOnOriginal.errors('name')).toEqual(['Name is a duplicate']);

        var errorsOnDuplicate = duplicateStage.validate();
        expect(errorsOnDuplicate.errors('name')).toEqual(['Name is a duplicate']);
      });
    });

    describe("Serialization/De-serialization to/from JSON", function () {
      beforeEach(function () {
        stage = Stages.Stage.fromJSON(sampleStageJSON());
      });

      it("should de-serialize from JSON", function () {
        expect(stage.name()).toBe("UnitTest");
        expect(stage.fetchMaterials()).toBe(true);
        expect(stage.cleanWorkingDirectory()).toBe(false);
        expect(stage.neverCleanArtifacts()).toBe(true);

        var expectedEnvironmentVarNames = stage.environmentVariables().mapVariables(function (variable) {
          return variable.name();
        });

        expect(expectedEnvironmentVarNames).toEqual(['MULTIPLE_LINES', 'COMPLEX']);

      });

      it("should serialize to JSON", function () {
        expect(JSON.parse(JSON.stringify(stage, s.snakeCaser))).toEqual(sampleStageJSON());
      });

      function sampleStageJSON() {
        return {
          name:                    "UnitTest",
          fetch_materials:         true,
          clean_working_directory: false,
          never_clean_artifacts:   true,
          environment_variables:   [
            {
              name:   "MULTIPLE_LINES",
              value:  "multiplelines",
              secure: true
            },
            {
              name:   "COMPLEX",
              value:  "This has very <complex> data",
              secure: false
            }
          ],
          jobs:                    []
        };
      }
    });
  });

});
