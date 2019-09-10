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

import {Environments} from "models/new-environments/environments";
import data from "models/new-environments/spec/test_data";

const envJSON          = data.environment_json();
const environmentsJSON = {
  _embedded: {
    environments: [envJSON]
  }
};

describe("Environments Model - Environments", () => {
  it("should deserialize from json", () => {
    const environments = Environments.fromJSON(environmentsJSON);
    expect(environments.length).toEqual(1);

    expect(environments[0].name()).toEqual(envJSON.name);
    expect(environments[0].origins().length).toEqual(2);
    expect(environments[0].origins()[0].type()).toEqual(envJSON.origins[0].type);
    expect(environments[0].origins()[1].type()).toEqual(envJSON.origins[1].type);
    expect(environments[0].agents().length).toEqual(2);
    expect(environments[0].pipelines().length).toEqual(2);
    expect(environments[0].environmentVariables().length).toEqual(4);
  });
});
