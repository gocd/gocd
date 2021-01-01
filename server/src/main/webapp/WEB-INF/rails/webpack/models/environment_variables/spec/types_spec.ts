/*
 * Copyright 2021 ThoughtWorks, Inc.
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
  EnvironmentEnvironmentVariableJSON,
  EnvironmentVariable,
  EnvironmentVariables,
  EnvironmentVariablesWithOrigin,
  EnvironmentVariableWithOrigin
} from "models/environment_variables/types";
import data from "./test_data";

const plainTextEnvVar1                 = data.environment_variable_json();
const plainTextEnvVar2                 = data.environment_variable_json();
const secureEnvVar1                    = data.environment_variable_json(true);
const secureEnvVar2                    = data.environment_variable_json(true);
const xmlEnvVarWithOrigin              = data.environment_variable_with_origin_xml_json();
const configRepoEnvVarWithOrigin       = data.environment_variable_with_origin_config_repo_json();
const secureXmlEnvVarWithOrigin        = data.environment_variable_with_origin_xml_json(true);
const secureConfigRepoEnvVarWithOrigin = data.environment_variable_with_origin_config_repo_json(true);

describe("Environment Variables Model", () => {
  it("should deserialize from json", () => {
    const environmentVariables = EnvironmentVariables.fromJSON([plainTextEnvVar1, plainTextEnvVar2]);

    expect(environmentVariables.length).toEqual(2);
    expect(environmentVariables[0].name()).toEqual(plainTextEnvVar1.name);
    expect(environmentVariables[0].value()).toEqual(plainTextEnvVar1.value);
    expect(environmentVariables[0].secure()).toEqual(false);

    expect(environmentVariables[1].name()).toEqual(plainTextEnvVar2.name);
    expect(environmentVariables[1].value()).toEqual(plainTextEnvVar2.value);
    expect(environmentVariables[1].secure()).toEqual(false);
  });

  it("should filter plain text variables", () => {
    const environmentVariables = EnvironmentVariables.fromJSON([plainTextEnvVar1, plainTextEnvVar2, secureEnvVar1, secureEnvVar2]);
    const plainTextVariables   = environmentVariables.plainTextVariables();

    expect(plainTextVariables.length).toBe(2);
    expect(plainTextVariables[0].name()).toEqual(plainTextEnvVar1.name);
    expect(plainTextVariables[1].name()).toEqual(plainTextEnvVar2.name);
  });

  it("should filter secure variables", () => {
    const environmentVariables = EnvironmentVariables.fromJSON([plainTextEnvVar1, plainTextEnvVar2, secureEnvVar1, secureEnvVar2]);
    const secureVariables      = environmentVariables.secureVariables();

    expect(secureVariables.length).toBe(2);
    expect(secureVariables[0].name()).toEqual(secureEnvVar1.name);
    expect(secureVariables[1].name()).toEqual(secureEnvVar2.name);
  });

  it("should remove environment variable", () => {
    const environmentVariables = EnvironmentVariables.fromJSON([plainTextEnvVar1, secureEnvVar1]);

    expect(environmentVariables.length).toBe(2);
    expect(environmentVariables[0].name()).toBe(plainTextEnvVar1.name);
    expect(environmentVariables[1].name()).toBe(secureEnvVar1.name);

    environmentVariables.remove(environmentVariables[0]);

    expect(environmentVariables.length).toBe(1);
    expect(environmentVariables[0].name()).toBe(secureEnvVar1.name);

    environmentVariables.remove(environmentVariables[0]);

    expect(environmentVariables.length).toBe(0);
  });
});

describe("Environment Variable Model", () => {
  describe("Validations", () => {
    it("should validate presence of name if both value and encrypted value are present", () => {
      const envVar = EnvironmentVariable.fromJSON(plainTextEnvVar1);

      expect(envVar.isValid()).toBe(true);

      envVar.name("");

      expect(envVar.isValid()).toBe(false);
      expect(envVar.errors().errors("name")).toContain("Name must be present");
    });
  });

  describe("toJSON", () => {
    describe("Plain Text", () => {
      it("should generate json for plain text variable", () => {
        const envVar = EnvironmentVariable.fromJSON(plainTextEnvVar1);

        const expected = {
          name: plainTextEnvVar1.name,
          value: plainTextEnvVar1.value,
          secure: plainTextEnvVar1.secure
        };
        expect(envVar.toJSON()).toEqual(expected);
      });

      it("should generate json for plain text variable when value is absent", () => {
        const envVar = EnvironmentVariable.fromJSON(plainTextEnvVar1);

        envVar.value(undefined);

        const expected = {
          name: plainTextEnvVar1.name,
          value: "",
          secure: plainTextEnvVar1.secure
        };
        expect(envVar.toJSON()).toEqual(expected);
      });
    });

    describe("Secure Text", () => {
      it("should generate json for secure text variable", () => {
        const envVar = EnvironmentVariable.fromJSON(secureEnvVar1);

        const expected = {
          name: secureEnvVar1.name,
          encrypted_value: secureEnvVar1.encrypted_value,
          secure: secureEnvVar1.secure
        };

        expect(envVar.toJSON()).toEqual(expected);
      });

      it("should generate json for secure text variable when value is edited", () => {
        const envVar = EnvironmentVariable.fromJSON(secureEnvVar1);

        envVar.encryptedValue().edit();
        envVar.encryptedValue().value("blah");

        const expected = {
          name: secureEnvVar1.name,
          value: "blah",
          secure: secureEnvVar1.secure
        };

        expect(envVar.toJSON()).toEqual(expected);
      });

      it("should generate json for secure text variable when value is edited and set to empty", () => {
        const envVar = EnvironmentVariable.fromJSON(secureEnvVar1);

        envVar.encryptedValue().edit();
        envVar.encryptedValue().value(undefined);

        const expected = {
          name: secureEnvVar1.name,
          value: "",
          secure: secureEnvVar1.secure
        };

        expect(envVar.toJSON()).toEqual(expected);
      });
    });
  });

  describe("equals()", () => {
    it("should return true for environment variables with same values", () => {
      const envVar  = EnvironmentVariable.fromJSON(plainTextEnvVar1);
      const envVar2 = new EnvironmentVariable(envVar.name(),
                                              envVar.value(),
                                              envVar.secure(),
                                              envVar.encryptedValue().value());
      expect(envVar.equals(envVar2)).toBe(true);
    });

    describe("should return false for environment variables", () => {
      it("different value", () => {
        const envVar  = EnvironmentVariable.fromJSON(plainTextEnvVar1);
        const envVar2 = new EnvironmentVariable(envVar.name(),
                                                "test",
                                                envVar.secure(),
                                                envVar.encryptedValue().value());
        expect(envVar.equals(envVar2)).toBe(false);
      });

      it("different name", () => {
        const envVar  = EnvironmentVariable.fromJSON(plainTextEnvVar1);
        const envVar2 = new EnvironmentVariable("some-random-name",
                                                envVar.value(),
                                                envVar.secure(),
                                                envVar.encryptedValue().value());
        expect(envVar.equals(envVar2)).toBe(false);
      });

      it("different encrypted value", () => {
        const envVar  = EnvironmentVariable.fromJSON(plainTextEnvVar1);
        const envVar2 = new EnvironmentVariable(envVar.name(), envVar.value(), envVar.secure(), "encrypted-value");
        expect(envVar.equals(envVar2)).toBe(false);
      });
    });
  });

  describe("Editable", () => {
    it("should set all environment variables editable by default", () => {
      const envVar = EnvironmentVariable.fromJSON(plainTextEnvVar1);

      expect(envVar.editable()).toBeTrue();
      expect(envVar.reasonForNonEditable()).toBeFalsy();
    });

    it("should make environment variable non editable", () => {
      const envVar = EnvironmentVariable.fromJSON(plainTextEnvVar1);

      expect(envVar.editable()).toBeTrue();
      envVar.isEditable(false);
      expect(envVar.editable()).toBeFalse();
    });
  });
});

describe("EnvironmentVariablesWithOrigin", () => {
  it("should deserialize from json", () => {
    const environmentVariables = EnvironmentVariablesWithOrigin.fromJSON([xmlEnvVarWithOrigin as EnvironmentEnvironmentVariableJSON, configRepoEnvVarWithOrigin]);

    expect(environmentVariables.length).toEqual(2);
    expect(environmentVariables[0].name()).toEqual(xmlEnvVarWithOrigin.name);
    expect(environmentVariables[0].value()).toEqual(xmlEnvVarWithOrigin.value);
    expect(environmentVariables[0].secure()).toEqual(false);
    expect(environmentVariables[0].origin().type()).toEqual(xmlEnvVarWithOrigin.origin.type);

    expect(environmentVariables[1].name()).toEqual(configRepoEnvVarWithOrigin.name);
    expect(environmentVariables[1].value()).toEqual(configRepoEnvVarWithOrigin.value);
    expect(environmentVariables[1].secure()).toEqual(false);
    expect(environmentVariables[1].origin().type()).toEqual(configRepoEnvVarWithOrigin.origin.type);
  });

  it("should filter plain text variables", () => {
    const environmentVariables = EnvironmentVariablesWithOrigin.fromJSON([xmlEnvVarWithOrigin, configRepoEnvVarWithOrigin, secureXmlEnvVarWithOrigin, secureConfigRepoEnvVarWithOrigin]);
    const plainTextVariables   = environmentVariables.plainTextVariables();

    expect(plainTextVariables.length).toBe(2);
    expect(plainTextVariables[0].name()).toEqual(xmlEnvVarWithOrigin.name);
    expect(plainTextVariables[1].name()).toEqual(configRepoEnvVarWithOrigin.name);
  });

  it("should filter secure variables", () => {
    const environmentVariables = EnvironmentVariablesWithOrigin.fromJSON([xmlEnvVarWithOrigin, configRepoEnvVarWithOrigin, secureXmlEnvVarWithOrigin, secureConfigRepoEnvVarWithOrigin]);
    const secureVariables      = environmentVariables.secureVariables();

    expect(secureVariables.length).toBe(2);
    expect(secureVariables[0].name()).toEqual(secureXmlEnvVarWithOrigin.name);
    expect(secureVariables[1].name()).toEqual(secureConfigRepoEnvVarWithOrigin.name);
  });

  it("should remove environment variable", () => {
    const environmentVariables = EnvironmentVariablesWithOrigin.fromJSON([xmlEnvVarWithOrigin, secureXmlEnvVarWithOrigin]);

    expect(environmentVariables.length).toBe(2);
    expect(environmentVariables[0].name()).toBe(xmlEnvVarWithOrigin.name);
    expect(environmentVariables[1].name()).toBe(secureXmlEnvVarWithOrigin.name);

    environmentVariables.remove(environmentVariables[0]);

    expect(environmentVariables.length).toBe(1);
    expect(environmentVariables[0].name()).toBe(secureXmlEnvVarWithOrigin.name);

    environmentVariables.remove(environmentVariables[0]);

    expect(environmentVariables.length).toBe(0);
  });
});

describe("EnvironmentVariableWithOrigin", () => {
  describe("editable()", () => {
    it("should be editable if origin is gocd", () => {
      const envVar = EnvironmentVariableWithOrigin.fromJSON(xmlEnvVarWithOrigin as EnvironmentEnvironmentVariableJSON);
      expect(envVar.editable()).toBe(true);
    });
    it("should not be editable if origin is config repo", () => {
      const envVar = EnvironmentVariableWithOrigin.fromJSON(configRepoEnvVarWithOrigin as EnvironmentEnvironmentVariableJSON);
      expect(envVar.editable()).toBe(false);
    });

    it("should show info icon if origin is config repo", () => {
      const envVar = EnvironmentVariableWithOrigin.fromJSON(configRepoEnvVarWithOrigin as EnvironmentEnvironmentVariableJSON);
      expect(envVar.reasonForNonEditable())
        .toEqual("Cannot edit this environment variable as it is defined in config repo");
    });

  });

});
