/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {Agents} from "models/new-environments/environment_agents";

import data from "./test_data";

const xmlAgent        = data.agent_association_in_xml_json();
const configRepoAgent = data.agent_association_in_config_repo_json();

describe("Environments Model - Agents", () => {
  it("should deserialize from json", () => {
    const agents = Agents.fromJSON([xmlAgent, configRepoAgent]);

    expect(agents.length).toEqual(2);
    expect(agents[0].uuid()).toEqual(xmlAgent.uuid);
    expect(agents[0].origin().type()).toEqual(xmlAgent.origin.type);
    expect(agents[1].uuid()).toEqual(configRepoAgent.uuid);
    expect(agents[1].origin().type()).toEqual(configRepoAgent.origin.type);
  });

  it("should return empty list if input is undefined", () => {
    const env = data.environment_json();
    // @ts-ignore
    delete env.agents;
    const agents = Agents.fromJSON(env.agents);

    expect(agents.length).toEqual(0);
  });
});
