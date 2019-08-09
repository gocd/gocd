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

import {Agent, AgentJSON, Agents, AgentsJSON} from "models/new-agent/agents";

describe("Agents Model", () => {
  describe("Deserialize", () => {
    it("should deserialize all agents from JSON", () => {
      const agents = Agents.fromJSON(agentsJSON);
      expect(agents.count()).toBe(2);
    });

    it("should deserialize agent from JSON", () => {
      const agent = Agent.fromJSON(agent1JSON);

      expect(agent.uuid).toEqual(agent1JSON.uuid);
      expect(agent.hostname).toEqual(agent1JSON.hostname);
      expect(agent.ipAddress).toEqual(agent1JSON.ip_address);
      expect(agent.sandbox).toEqual(agent1JSON.sandbox);
      expect(agent.operatingSystem).toEqual(agent1JSON.operating_system);
      expect(agent.operatingSystem).toEqual(agent1JSON.operating_system);
      expect(agent.agentConfigState).toEqual(agent1JSON.agent_config_state);
      expect(agent.agentState).toEqual(agent1JSON.agent_state);
      expect(agent.buildState).toEqual(agent1JSON.build_state);
      expect(agent.freeSpace).toEqual(agent1JSON.free_space);
      expect(agent.resources).toEqual(agent1JSON.resources);
    });
  });

  describe("Sort", () => {

    it("should get the sortable column index", () => {
      const agents = Agents.fromJSON(agentsJSON);

      expect(agents.getSortableColumns()).toEqual([0, 1, 2, 3, 4, 5, 6, 7]);
    });

    it("should sort the data based on the column clicked", () => {
      const agents = Agents.fromJSON(agentsJSON);
      const agent1 = Agent.fromJSON(agent1JSON);
      const agent2 = Agent.fromJSON(agent2JSON);

      expect(agents.list()).toEqual([agent2, agent1]);

      agents.onColumnClick(1);
      expect(agents.list()).toEqual([agent1, agent2]);
    });

    it("should sort the data in reverser order when the same column is clicked", () => {
      const agents = Agents.fromJSON(agentsJSON);
      const agent1 = Agent.fromJSON(agent1JSON);
      const agent2 = Agent.fromJSON(agent2JSON);

      const agentsToRepresent = agents.list();
      const expected          = [agent2, agent1];
      expect(agentsToRepresent).toEqual(expected);

      agents.onColumnClick(1);
      const agentsToRepresentAfterClick = agents.list();
      const expectedAfterClick          = [agent1, agent2];
      expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);

      agents.onColumnClick(1);
      const agentsToRepresentAfterClickOnSameColumn = agents.list();
      const expectedAfterClickOnSameColumn          = [agent2, agent1];
      expect(agentsToRepresentAfterClickOnSameColumn).toEqual(expectedAfterClickOnSameColumn);
    });

    it("should always sort a new column in ascending order regardless of previous column's sort order", () => {
      const agents = Agents.fromJSON(agentsJSON);
      const agent1 = Agent.fromJSON(agent1JSON);
      const agent2 = Agent.fromJSON(agent2JSON);

      const agentsToRepresent = agents.list();
      const expected          = [agent2, agent1];
      expect(agentsToRepresent).toEqual(expected);

      agents.onColumnClick(1);
      const agentsToRepresentAfterClick = agents.list();
      const expectedAfterClick          = [agent1, agent2];
      expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);

      agents.onColumnClick(1);
      const agentsToRepresentAfterClickOnSameColumn = agents.list();
      const expectedAfterClickOnSameColumn          = [agent2, agent1];
      expect(agentsToRepresentAfterClickOnSameColumn).toEqual(expectedAfterClickOnSameColumn);

      agents.onColumnClick(0);
      const agentsToRepresentAfterClickOnAnotherColumn = agents.list();
      const expectedAfterClickOnAnotherColumn          = [agent2, agent1];
      expect(agentsToRepresentAfterClickOnAnotherColumn).toEqual(expectedAfterClickOnAnotherColumn);
    });
  });

  describe("Search", () => {
    let agents: Agents, agent1: Agent, agent2: Agent;
    beforeEach(() => {
      agents = Agents.fromJSON(agentsJSON);
      agent1 = Agent.fromJSON(agent1JSON);
      agent2 = Agent.fromJSON(agent2JSON);
    });

    it("should search agents based on hostname", () => {
      expect(agents.list().length).toEqual(2);

      agents.filterText("windows");
      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agent1]);
    });

    it("should search agents based on sandbox", () => {
      expect(agents.list().length).toEqual(2);

      agents.filterText("xy");
      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agent2]);
    });

    it("should search agents based on os", () => {
      expect(agents.list().length).toEqual(2);

      agents.filterText("mac");
      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agent2]);
    });

    it("should search agents based on ip address", () => {
      expect(agents.list().length).toEqual(2);

      agents.filterText(".5");
      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agent1]);
    });

    it("should search agents based on free space", () => {
      expect(agents.list().length).toEqual(2);

      agents.filterText("2598");
      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agent1]);
    });

    it("should search agents based on resources", () => {
      expect(agents.list().length).toEqual(2);

      agents.filterText("fire");
      expect(agents.list().length).toEqual(2);
      expect(agents.list()).toEqual([agent2, agent1]);
    });

    it("should search agents case-insensitively", () => {
      expect(agents.list().length).toEqual(2);

      agents.filterText("WinDow");
      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agent1]);
    });
  });

  const agent1JSON: AgentJSON = {
    uuid: "uuid1",
    hostname: "windows-10-pro",
    ip_address: "10.1.0.5",
    sandbox: "go",
    operating_system: "Windows 10",
    // @ts-ignore
    agent_config_state: "Enabled",
    // @ts-ignore
    agent_state: "Idle",
    environments: [{
      name: "gocd",
      origin: {
        type: "gocd",
        _links: {
          self: {
            href: "https://build.gocd.org/go/admin/config_xml"
          },
          doc: {
            href: "https://api.gocd.org/19.8.0/#get-configuration"
          }
        }
      }
    }, {
      name: "internal",
      origin: {
        type: "gocd",
        _links: {
          self: {
            href: "https://build.gocd.org/go/admin/config_xml"
          },
          doc: {
            href: "https://api.gocd.org/19.8.0/#get-configuration"
          }
        }
      }
    }],
    // @ts-ignore
    build_state: "Idle",
    free_space: 93259825152,
    resources: ["dev", "fat", "ie9", "windows", "firefox"]
  };

  const agent2JSON: AgentJSON = {
    uuid: "uuid2",
    hostname: "mac-mini",
    ip_address: "10.1.0.10",
    sandbox: "xyz",
    operating_system: "Mac OS X",
    // @ts-ignore
    agent_config_state: "Disabled",
    // @ts-ignore
    agent_state: "Idle",
    environments: [{
      name: "gocd",
      origin: {
        type: "gocd",
        _links: {
          self: {
            href: "https://build.gocd.org/go/admin/config_xml"
          },
          doc: {
            href: "https://api.gocd.org/19.8.0/#get-configuration"
          }
        }
      }
    }, {
      name: "internal",
      origin: {
        type: "gocd",
        _links: {
          self: {
            href: "https://build.gocd.org/go/admin/config_xml"
          },
          doc: {
            href: "https://api.gocd.org/19.8.0/#get-configuration"
          }
        }
      }
    }],
    // @ts-ignore
    build_state: "Building",
    // @ts-ignore
    free_space: "unknown",
    resources: ["firefox"]
  };

  const agentsJSON: AgentsJSON = {
    _embedded: {
      agents: [agent1JSON, agent2JSON]
    }
  };

});
