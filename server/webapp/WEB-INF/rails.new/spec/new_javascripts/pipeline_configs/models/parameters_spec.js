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

define(["pipeline_configs/models/parameters"], function (Parameters) {
  var parameters, parameter;
  beforeEach(function () {
    parameters = new Parameters();

    parameter = parameters.createParameter({
      name:  "WORKING_DIR",
      value: "/var/foo"
    });
  });

  describe("Parameter Model", function () {
    it("should initialize parameter model with name", function () {
      expect(parameter.name()).toBe("WORKING_DIR");
    });

    it("should initialize parameter model with value", function () {
      expect(parameter.value()).toBe('/var/foo');
    });


    describe("validations", function () {
      it("should add error when name is blank but value is not", function () {
        parameter.name("");
        parameter.value('foo');
        var errors = parameter.validate();
        expect(errors.errors('name')).toEqual(['Name must be present']);
      });

      it("should NOT add error when both name and value are blank", function () {
        parameter.name("");
        parameter.value("");

        var errors = parameter.validate();
        expect(errors._isEmpty()).toBe(true);
      });

      it("should not allow parameters with duplicate names", function () {
        var errorsOnOriginal = parameter.validate();
        expect(errorsOnOriginal._isEmpty()).toBe(true);

        var duplicateParameter = parameters.createParameter({
          name: "WORKING_DIR"
        });

        var errorsOnOriginal = parameter.validate();
        expect(errorsOnOriginal.errors('name')).toEqual(['Name is a duplicate']);

        var errorsOnDuplicate = duplicateParameter.validate();
        expect(errorsOnDuplicate.errors('name')).toEqual(['Name is a duplicate']);
      });
    });

    describe("Deserialization from JSON", function () {
      beforeEach(function () {
        parameter = Parameters.Parameter.fromJSON(sampleJSON());
      });

      it("should initialize from json", function () {
        expect(parameter.name()).toBe("WORKING_DIR");
        expect(parameter.value()).toBe('/var/foo');
      });

      function sampleJSON() {
        return {
          name:  "WORKING_DIR",
          value: "/var/foo"
        };
      }
    })

  });
});
