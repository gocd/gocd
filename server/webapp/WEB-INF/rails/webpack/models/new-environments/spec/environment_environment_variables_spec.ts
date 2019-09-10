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

import {EnvironmentVariables} from "models/new-environments/environment_environment_variables";

import data from "./test_data";

const xmlEnvVar              = data.environment_variable_association_in_xml_json();
const configRepoEnvVar       = data.environment_variable_association_in_config_repo_json();
const secureXmlEnvVar        = data.environment_variable_association_in_xml_json(true);
const secureConfigRepoEnvVar = data.environment_variable_association_in_config_repo_json(true);

describe("Environments Model - EnvironmentVariables", () => {
  it("should deserialize from json", () => {
    const environmentVariables = EnvironmentVariables.fromJSON([xmlEnvVar, configRepoEnvVar]);

    expect(environmentVariables.length).toEqual(2);
    expect(environmentVariables[0].name()).toEqual(xmlEnvVar.name);
    expect(environmentVariables[0].value()).toEqual(xmlEnvVar.value);
    expect(environmentVariables[0].secure()).toEqual(false);
    expect(environmentVariables[0].origin().type()).toEqual(xmlEnvVar.origin.type);

    expect(environmentVariables[1].name()).toEqual(configRepoEnvVar.name);
    expect(environmentVariables[1].value()).toEqual(configRepoEnvVar.value);
    expect(environmentVariables[1].secure()).toEqual(false);
    expect(environmentVariables[1].origin().type()).toEqual(configRepoEnvVar.origin.type);
  });

  it("should filter plain text variables", () => {
    const environmentVariables = EnvironmentVariables.fromJSON([xmlEnvVar, configRepoEnvVar, secureXmlEnvVar, secureConfigRepoEnvVar]);
    const plainTextVariables   = environmentVariables.plainTextVariables();

    expect(plainTextVariables.length).toBe(2);
    expect(plainTextVariables[0].name()).toEqual(xmlEnvVar.name);
    expect(plainTextVariables[1].name()).toEqual(configRepoEnvVar.name);
  });

  it("should filter secure variables", () => {
    const environmentVariables = EnvironmentVariables.fromJSON([xmlEnvVar, configRepoEnvVar, secureXmlEnvVar, secureConfigRepoEnvVar]);
    const secureVariables      = environmentVariables.secureVariables();

    expect(secureVariables.length).toBe(2);
    expect(secureVariables[0].name()).toEqual(secureXmlEnvVar.name);
    expect(secureVariables[1].name()).toEqual(secureConfigRepoEnvVar.name);
  });

  it("should remove environment variable", () => {
    const environmentVariables = EnvironmentVariables.fromJSON([xmlEnvVar, secureXmlEnvVar]);

    expect(environmentVariables.length).toBe(2);
    expect(environmentVariables[0].name()).toBe(xmlEnvVar.name);
    expect(environmentVariables[1].name()).toBe(secureXmlEnvVar.name);

    environmentVariables.remove(environmentVariables[0]);

    expect(environmentVariables.length).toBe(1);
    expect(environmentVariables[0].name()).toBe(secureXmlEnvVar.name);

    environmentVariables.remove(environmentVariables[0]);

    expect(environmentVariables.length).toBe(0);
  });
});
