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

import {TestData} from "models/environments/spec/test_data";
import {Environments} from "models/environments/types";

describe("EnvironmentModel", () => {

  it("should deserialize json to Environments", () => {
    const env1             = TestData.newEnvironment("env1", ["pipeline1"]);
    const env2             = TestData.newEnvironment("env2");
    const environmentsJSON = TestData.environmentList(env1, env2);
    const environments     = Environments.fromJSON(environmentsJSON);

    expect(environments.length).toEqual(2);

    const environment1 = environments[0];

    expect(environment1.name()).toBe(env1.name);
    expect(environment1.pipelines().length).toBe(1);
    expect(environment1.pipelines()[0].name()).toBe("pipeline1");
    expect(environment1.environmentVariables().length).toBe(0);

    const environment2 = environments[1];

    expect(environment2.name()).toBe(env2.name);
    expect(environment2.pipelines().length).toBe(0);
    expect(environment2.environmentVariables().length).toBe(0);

  });
});
