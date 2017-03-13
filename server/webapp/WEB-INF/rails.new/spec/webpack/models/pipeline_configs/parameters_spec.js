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
describe("Parameter Model", () => {

  var Parameters = require("models/pipeline_configs/parameters");
  var parameters, parameter;
  beforeEach(() => {
    parameters = new Parameters();

    parameter = parameters.createParameter({
      name:  "WORKING_DIR",
      value: "/var/foo"
    });
  });

  it("should initialize parameter model with name", () => {
    expect(parameter.name()).toBe("WORKING_DIR");
  });

  it("should initialize parameter model with value", () => {
    expect(parameter.value()).toBe('/var/foo');
  });


  describe("validations", () => {
    it("should add error when name is blank but value is not", () => {
      parameter.name("");
      parameter.value('foo');
      var errors = parameter.validate();
      expect(errors.errors('name')).toEqual(['Name must be present']);
    });

    it("should NOT add error when both name and value are blank", () => {
      parameter.name("");
      parameter.value("");

      var errors = parameter.validate();
      expect(errors._isEmpty()).toBe(true);
    });

    it("should not allow parameters with duplicate names", () => {
      var errorsOnOriginal = parameter.validate();
      expect(errorsOnOriginal._isEmpty()).toBe(true);

      var duplicateParameter = parameters.createParameter({
        name: "WORKING_DIR"
      });

      errorsOnOriginal = parameter.validate();
      expect(errorsOnOriginal.errors('name')).toEqual(['Name is a duplicate']);

      var errorsOnDuplicate = duplicateParameter.validate();
      expect(errorsOnDuplicate.errors('name')).toEqual(['Name is a duplicate']);
    });
  });

  describe("Deserialization from JSON", () => {
    beforeEach(() => {
      parameter = Parameters.Parameter.fromJSON(sampleJSON());
    });

    it("should initialize from json", () => {
      expect(parameter.name()).toBe("WORKING_DIR");
      expect(parameter.value()).toBe('/var/foo');
    });

    function sampleJSON() {
      return {
        name:  "WORKING_DIR",
        value: "/var/foo"
      };
    }
  });

});
