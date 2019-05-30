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

import {EnvironmentVariableConfig} from "models/pipeline_configs/environment_variable_config";

describe("EnvironmentVariableConfig model", () => {
  function validEnvironmentVariableConfig() {
    return new EnvironmentVariableConfig(false, "KEY", "value");
  }

  function validSecureEnvironmentVariableConfig() {
    return new EnvironmentVariableConfig(true, "KEY", "value");
  }

  it("should validate presence of name", () => {
    let envVar = validEnvironmentVariableConfig();
    expect(envVar.isValid()).toBe(true);
    expect(envVar.errors().count()).toBe(0);

    validSecureEnvironmentVariableConfig();
    expect(envVar.isValid()).toBe(true);
    expect(envVar.errors().count()).toBe(0);

    envVar = new EnvironmentVariableConfig(false, "", "");
    expect(envVar.isValid()).toBe(false);
    expect(envVar.errors().count()).toBe(1);

    envVar = new EnvironmentVariableConfig(true, "", "");
    expect(envVar.isValid()).toBe(false);
    expect(envVar.errors().count()).toBe(1);
  });

  it("adopts errors in server response", () => {
    const env = validEnvironmentVariableConfig();

    const unmatched = env.consumeErrorsResponse({
      errors: { name: ["this name is uncreative"], not_exist: ["well, ain't that a doozy"] }
    });

    expect(env.errors().errorsForDisplay("name")).toBe("this name is uncreative.");

    expect(unmatched.hasErrors()).toBe(true);
    expect(unmatched.errorsForDisplay("environmentVariableConfig.notExist")).toBe("well, ain't that a doozy.");
  });

  it("should serialize correctly", () => {
    const envVar = validEnvironmentVariableConfig();
    expect(envVar.toApiPayload()).toEqual({
      name: "KEY",
      value: "value",
      secure: false
    });
    const secureVar = validSecureEnvironmentVariableConfig();
    expect(secureVar.toApiPayload()).toEqual({
      name: "KEY",
      value: "value",
      secure: true
    });
  });
});
