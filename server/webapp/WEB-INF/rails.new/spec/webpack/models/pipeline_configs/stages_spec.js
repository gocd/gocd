/*
 * Copyright 2017 ThoughtWorks, Inc.
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
describe("Stages Model", () => {

  const s = require("string-plus");

  const Stages   = require("models/pipeline_configs/stages");
  const Approval = require('models/pipeline_configs/approval');

  let stages, stage;
  beforeEach(() => {
    stages = new Stages();
    stage  = stages.createStage({
      name:                  "UnitTest",
      fetchMaterials:        true,
      cleanWorkingDirectory: true,
      neverCleanupArtifacts: true,
      environmentVariables:  ["foo=bar", "boo=baz"],
      approval:              new Approval({type: 'manual'})
    });
  });

  it("should initialize stage model with name", () => {
    expect(stage.name()).toBe("UnitTest");
  });

  it("should initialize stage model with fetchMaterials", () => {
    expect(stage.fetchMaterials()).toBe(true);
  });

  it("should initialize stage model with cleanWorkingDirectory", () => {
    expect(stage.cleanWorkingDirectory()).toBe(true);
  });

  it("should initialize stage model with neverCleanupArtifacts", () => {
    expect(stage.neverCleanupArtifacts()).toBe(true);
  });

  it("should initialize stage model with environmentVariables", () => {
    expect(stage.environmentVariables()).toEqual(['foo=bar', 'boo=baz']);
  });

  it("should initialize stage model with approval", () => {
    expect(stage.approval().type()).toEqual('manual');
  });

  describe("validations", () => {
    it("should not allow blank stage names", () => {
      let errors = stage.validate();
      expect(errors._isEmpty()).toBe(true);

      stage.name("");
      const duplicateStage = stages.createStage({name: stage.name()});

      errors                = stage.validate();
      const errorsOnDuplicate = duplicateStage.validate();

      expect(errors.errors('name')).toEqual(['Name must be present']);
      expect(errorsOnDuplicate.errors('name')).toEqual(['Name must be present']);
    });

    it("should not allow duplicate stage names", () => {
      let errorsOnOriginal = stage.validate();
      expect(errorsOnOriginal._isEmpty()).toBe(true);

      const duplicateStage = stages.createStage({
        name: "UnitTest"
      });

      errorsOnOriginal = stage.validate();
      expect(errorsOnOriginal.errors('name')).toEqual(['Name is a duplicate']);

      const errorsOnDuplicate = duplicateStage.validate();
      expect(errorsOnDuplicate.errors('name')).toEqual(['Name is a duplicate']);
    });

    describe('validate associations', () => {
      it('should validate environmental variables', () => {
        const stage = Stages.Stage.fromJSON(sampleStageJSON());

        expect(stage.isValid()).toBe(true);

        stage.environmentVariables().firstVariable().name('');

        expect(stage.isValid()).toBe(false);
        expect(stage.environmentVariables().firstVariable().errors().errors('name')).toEqual(['Name must be present']);
      });

      it('should validate jobs', () => {
        const stage = Stages.Stage.fromJSON({
          name: 'stage1',
          jobs: [
            {name: 'job1'}
          ]
        });

        expect(stage.isValid()).toBe(true);

        stage.jobs().firstJob().name('');

        expect(stage.isValid()).toBe(false);
        expect(stage.jobs().firstJob().errors().errors('name')).toEqual(['Name must be present']);
      });
    });
  });

  describe("Serialization/De-serialization to/from JSON", () => {
    beforeEach(() => {
      stage = Stages.Stage.fromJSON(sampleStageJSON());
    });

    it("should de-serialize from JSON", () => {
      expect(stage.name()).toBe("UnitTest");
      expect(stage.fetchMaterials()).toBe(true);
      expect(stage.cleanWorkingDirectory()).toBe(false);
      expect(stage.neverCleanupArtifacts()).toBe(true);

      const expectedEnvironmentVarNames = stage.environmentVariables().mapVariables((variable) => variable.name());

      expect(expectedEnvironmentVarNames).toEqual(['MULTIPLE_LINES', 'COMPLEX']);
    });

    it("should serialize to JSON", () => {
      expect(JSON.parse(JSON.stringify(stage, s.snakeCaser))).toEqual(sampleStageJSON());
    });
  });

  function sampleStageJSON() {
    /* eslint-disable camelcase */
    return {
      name:                    "UnitTest",
      fetch_materials:         true,
      clean_working_directory: false,
      never_cleanup_artifacts: true,
      environment_variables:   [
        {
          name:            "MULTIPLE_LINES",
          encrypted_value: "multiplelines",
          secure:          true
        },
        {
          name:   "COMPLEX",
          value:  "This has very <complex> data",
          secure: false
        }
      ],
      jobs:                    [],
      approval:                {
        type:          'manual',
        authorization: {
          users: [],
          roles: []
        }
      }
    };
    /* eslint-enable camelcase */
  }
});
