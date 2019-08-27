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


import {
  EnvironmentVariable,
  EnvironmentVariableJSON,
  EnvironmentVariables
} from "models/new_pipeline_configs/environment_variables";

describe("Pipeline Config - Environment Variables", () => {
  let variables: EnvironmentVariables;
  let plainTextVariable: EnvironmentVariable;
  let secureVariableSpecifiedAsEncryptedValue: EnvironmentVariable;
  let secureVariableSpecifiedAsValue: EnvironmentVariable;

  beforeEach(() => {
    plainTextVariable                       = EnvironmentVariable.fromJSON(plainTextVariableJSON);
    secureVariableSpecifiedAsValue          = EnvironmentVariable.fromJSON(secureVariableSpecifiedAsValueJSON);
    secureVariableSpecifiedAsEncryptedValue = EnvironmentVariable.fromJSON(secureVariableSpecifiedAsEncryptedValueJSON);

    variables = EnvironmentVariables.fromJSON(json);
  });

  describe("Environment Variables", () => {
    it("should deserialize from json", () => {
      expect(variables.variables().length).toEqual(3);
      const expected = [plainTextVariable, secureVariableSpecifiedAsEncryptedValue, secureVariableSpecifiedAsValue];
      expect(JSON.stringify(variables.variables())).toEqual(JSON.stringify(expected));
    });

    it("should filter plain text variables", () => {
      expect(variables.plainVariables().length).toEqual(1);
    });

    it("should filter secure variables", () => {
      expect(variables.secureVariables().length).toEqual(2);
    });
  });

  describe("Environment Variable", () => {
    it("should initialize variable model with a name", () => {
      expect(plainTextVariable.name()).toBe("USERNAME");
    });

    it("should initialize variable model with a value", () => {
      expect(plainTextVariable.value().value()).toBe("admin");
    });

    it("should initialize variable model with secure flag", () => {
      expect(plainTextVariable.value().isSecure()).toBe(false);
    });

    it("should initialize variable model with encrypted value", () => {
      expect(secureVariableSpecifiedAsEncryptedValue.value().value()).toBe("AES:1f3rrs9uhn63hd");
    });

    it("should answer if the environment variable value is secure", () => {
      expect(plainTextVariable.value().isSecure()).toBe(false);
      expect(secureVariableSpecifiedAsEncryptedValue.value().isSecure()).toBe(true);
      expect(secureVariableSpecifiedAsValue.value().isSecure()).toBe(true);
    });

    it("should answer if the environment variable value is plain", () => {
      expect(plainTextVariable.value().isPlain()).toBe(true);
      expect(secureVariableSpecifiedAsEncryptedValue.value().isPlain()).toBe(false);
      expect(secureVariableSpecifiedAsValue.value().isPlain()).toBe(false);
    });

    it("should initialize environment variable is editing to true for plain text variables", () => {
      expect(plainTextVariable.value().isEditing()).toBe(true);
    });

    it("should initialize environment variable is editing to false for secure variables", () => {
      expect(secureVariableSpecifiedAsEncryptedValue.value().isEditing()).toBe(false);
    });

    it("should answer if the environment variable value is edited", () => {
      expect(secureVariableSpecifiedAsEncryptedValue.value().isEditing()).toBe(false);
      secureVariableSpecifiedAsEncryptedValue.value().edit();
      expect(secureVariableSpecifiedAsEncryptedValue.value().isEditing()).toBe(true);
    });

    it("should allow editing plain text variable", () => {
      expect(plainTextVariable.value().isEditing()).toBe(true);
      plainTextVariable.value().value("new-value");

      expect(plainTextVariable.value().value()).toEqual("new-value");
    });

    it("should allow editing secure text variable", () => {
      expect(secureVariableSpecifiedAsEncryptedValue.value().isEditing()).toBe(false);
      secureVariableSpecifiedAsEncryptedValue.value().edit();

      expect(secureVariableSpecifiedAsEncryptedValue.value().isEditing()).toBe(true);
      secureVariableSpecifiedAsEncryptedValue.value().value("new-value");

      expect(secureVariableSpecifiedAsEncryptedValue.value().value()).toEqual("new-value");
    });

    it("should throw error when edit is not allowed for secure text variable", () => {
      const errMsg = "You cannot edit a cipher text value!";
      expect(() => secureVariableSpecifiedAsEncryptedValue.value().value("new-value")).toThrow(errMsg);
    });

    it("should answer whether environment variable value has changed", () => {
      expect(plainTextVariable.value().isDirty()).toEqual(false);
      plainTextVariable.value().value("new-value");
      expect(plainTextVariable.value().isDirty()).toEqual(true);
    });
  });

  const plainTextVariableJSON: EnvironmentVariableJSON = {
    "name": "USERNAME",
    "value": "admin",
    "secure": false
  };

  const secureVariableSpecifiedAsEncryptedValueJSON: EnvironmentVariableJSON = {
    "name": "PASSWORD",
    "encrypted_value": "AES:1f3rrs9uhn63hd",
    "secure": true
  };

  const secureVariableSpecifiedAsValueJSON: EnvironmentVariableJSON = {
    "name": "SSH_PASSPHRASE",
    "value": "p@ssw0rd",
    "secure": true
  };

  const json = [
    plainTextVariableJSON,
    secureVariableSpecifiedAsEncryptedValueJSON,
    secureVariableSpecifiedAsValueJSON
  ] as EnvironmentVariableJSON[];

});
