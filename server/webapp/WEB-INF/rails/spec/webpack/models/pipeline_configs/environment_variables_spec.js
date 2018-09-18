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

describe("EnvironmentVariable Model", () => {

  const s = require('string-plus');

  const EnvironmentVariables = require("models/pipeline_configs/environment_variables");

  let variables, plainVariable, secureVariable;
  beforeEach(() => {
    variables = new EnvironmentVariables();

    plainVariable = variables.createVariable({
      name:   "WORKING_DIR",
      value:  "/var/foo",
      secure: false
    });

    secureVariable = variables.createVariable({
      name:           "HTTP_PASSWORD",
      secure:         true,
      encryptedValue: "c!ph3rt3xt"
    });
  });

  it("should initialize variable model with name", () => {
    expect(plainVariable.name()).toBe("WORKING_DIR");
  });

  it("should initialize variable model with value", () => {
    expect(plainVariable.value()).toBe('/var/foo');
  });

  it("should initialize variable model with secure flag", () => {
    expect(plainVariable.isSecureValue()).toBe(false);
  });

  it("should initialize variable model with encryptedValue", () => {
    expect(secureVariable.value()).toBe('c!ph3rt3xt');
  });

  describe("validations", () => {
    it("should add error when name is blank but value is not", () => {
      plainVariable.name("");
      plainVariable.value('foo');
      const errors = plainVariable.validate();
      expect(errors.errors('name')).toEqual(['Name must be present']);
    });

    it("should NOT add error when both name and value are blank", () => {
      plainVariable.name("");
      plainVariable.value("");

      const errors = plainVariable.validate();
      expect(errors._isEmpty()).toBe(true);
    });

    it("should not allow variables with duplicate names", () => {
      let errorsOnOriginal = plainVariable.validate();
      expect(errorsOnOriginal._isEmpty()).toBe(true);

      const duplicateVariable = variables.createVariable({
        name: "WORKING_DIR"
      });

      errorsOnOriginal = plainVariable.validate();
      expect(errorsOnOriginal.errors('name')).toEqual(['Name is a duplicate']);

      const errorsOnDuplicate = duplicateVariable.validate();
      expect(errorsOnDuplicate.errors('name')).toEqual(['Name is a duplicate']);
    });
  });

  describe("Deserialization from JSON", () => {
    beforeEach(() => {
      plainVariable  = EnvironmentVariables.Variable.fromJSON(samplePlainVariableJSON());
      secureVariable = EnvironmentVariables.Variable.fromJSON(sampleSecureVariableJSON());
    });

    it("should initialize plain variable from json", () => {
      expect(plainVariable.name()).toBe("WORKING_DIR");
      expect(plainVariable.value()).toBe('/var/foo');
      expect(plainVariable.isSecureValue()).toBe(false);
    });

    it("should initialize secure variable from json", () => {
      expect(secureVariable.name()).toBe("HTTP_PASSWORD");
      expect(secureVariable.isSecureValue()).toBe(true);
      expect(secureVariable.value()).toBe('c!ph3rt3xt');
    });

    it("should serialize plain variables to json", () => {
      expect(JSON.parse(JSON.stringify(plainVariable, s.snakeCaser))).toEqual(samplePlainVariableJSON());
    });

    it("should serialize encrypted variables to json", () => {
      expect(JSON.parse(JSON.stringify(secureVariable, s.snakeCaser))).toEqual(sampleSecureVariableJSON());
    });

    function samplePlainVariableJSON() {
      return {
        name:   "WORKING_DIR",
        value:  "/var/foo",
        secure: false
      };
    }

    function sampleSecureVariableJSON() {
      return {
        name:            "HTTP_PASSWORD",
        encrypted_value: "c!ph3rt3xt", // eslint-disable-line camelcase
        secure:          true
      };
    }
  });
});
