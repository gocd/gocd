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
import {EnvironmentVariables} from "models/dashboard/environment_variables";

describe("Dashboard", () => {
  describe('Environment Variables Model', () => {


    it("should deserialize from json", () => {
      const envVars = EnvironmentVariables.fromJSON(json);

      expect(envVars.length).toBe(json.length);

      expect(envVars[0].name()).toBe(json[0].name);
      expect(envVars[0].value()).toEqual('');

      expect(envVars[1].name()).toBe(json[1].name);
      expect(envVars[1].value()).toBe(json[1].value);
    });

    it("should make environment variable non editable by default", () => {
      const envVars = EnvironmentVariables.fromJSON(json);

      expect(envVars[0].isEditingValue()).toBe(false);
      expect(envVars[1].isEditingValue()).toBe(true);
    });

    const json = [
      {
        "name":   "version",
        "secure": true,
        "value":  "***"
      },
      {
        "name":   "foobar",
        "secure": false,
        "value":  "asdf"
      }
    ];

  });
});
