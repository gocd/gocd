/*
 * Copyright 2018 ThoughtWorks, Inc.
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

describe("Dashboard", () => {
  describe('Environment Variables Model', () => {

    const EnvironmentVariables = require('models/dashboard/environment_variables');

    it("should deserialize from json", () => {
      const envVars = EnvironmentVariables.fromJSON(json);

      expect(envVars.length).toBe(json.length);

      expect(envVars[0].name()).toBe(json[0].name);
      expect(envVars[0].value()).toBe(json[0].value);

      expect(envVars[1].name()).toBe(json[1].name);
      expect(envVars[1].value()).toBe(json[1].value);
    });

    const json = [
      {
        "name":  "version",
        "value": "asdf"
      },
      {
        "name":  "foobar",
        "value": "asdf"
      }
    ];

  });
});
