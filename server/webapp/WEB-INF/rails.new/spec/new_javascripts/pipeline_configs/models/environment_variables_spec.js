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

define(["pipeline_configs/models/environment_variables"], function (EnvironmentVariables) {
  var variables, variable;
  beforeEach(function () {
    variables = new EnvironmentVariables();

    variable = variables.createVariable({
      name:   "WORKING_DIR",
      value:  "/var/foo",
      secure: false
    });
  });

  describe("EnvironmentVariable Model", function () {
    it("should initialize variable model with name", function () {
      expect(variable.name()).toBe("WORKING_DIR");
    });

    it("should initialize variable model with value", function () {
      expect(variable.value()).toBe('/var/foo');
    });

    it("should initialize variable model with secure flag", function () {
      expect(variable.secure()).toBe(false);
    });

    describe("validations", function () {
      it("should add error when name is blank but value is not", function () {
        variable.name("");
        variable.value('foo');
        var errors = variable.validate();
        expect(errors.errors('name')).toEqual(['Name must be present']);
      });

      it("should NOT add error when both name and value are blank", function () {
        variable.name("");
        variable.value("");

        var errors = variable.validate();
        expect(errors._isEmpty()).toBe(true);
      });

      it("should not allow variables with duplicate names", function () {
        var errorsOnOriginal = variable.validate();
        expect(errorsOnOriginal._isEmpty()).toBe(true);

        var duplicateVariable = variables.createVariable({
          name: "WORKING_DIR"
        });

        var errorsOnOriginal = variable.validate();
        expect(errorsOnOriginal.errors('name')).toEqual(['Name is a duplicate']);

        var errorsOnDuplicate = duplicateVariable.validate();
        expect(errorsOnDuplicate.errors('name')).toEqual(['Name is a duplicate']);
      });
    });

    describe("Deserialization from JSON", function () {
      beforeEach(function () {
        variable = EnvironmentVariables.Variable.fromJSON(sampleJSON());
      });

      it("should initialize from json", function () {
        expect(variable.name()).toBe("WORKING_DIR");
        expect(variable.value()).toBe('/var/foo');
        expect(variable.secure()).toBe(false);
      });

      function sampleJSON() {
        return {
          name:   "WORKING_DIR",
          value:  "/var/foo",
          secure: false
        };
      }
    });
  });
});
