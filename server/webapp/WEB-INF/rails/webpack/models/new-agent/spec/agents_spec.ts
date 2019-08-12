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

import {Agent, Agents} from "models/new-agent/agents";
import {AgentsTestData} from "models/new-agent/spec/agents_test_data";

describe("AgentsModel", () => {
  describe("Deserialize", () => {
    it("should deserialize all agents from JSON", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());

      expect(agents.count()).toBe(3);
    });

    it("should deserialize agent from JSON", () => {
      const agentJSON = AgentsTestData.agent("45583959-97e2-4b8e-8eab-2f98d73f2e23", "1773fcad85d8");
      const agent     = Agent.fromJSON(agentJSON);

      expect(agent.uuid).toEqual(agentJSON.uuid);
      expect(agent.hostname).toEqual(agentJSON.hostname);
      expect(agent.ipAddress).toEqual(agentJSON.ip_address);
      expect(agent.sandbox).toEqual(agentJSON.sandbox);
      expect(agent.operatingSystem).toEqual(agentJSON.operating_system);
      expect(agent.operatingSystem).toEqual(agentJSON.operating_system);
      expect(agent.agentConfigState).toEqual(agentJSON.agent_config_state);
      expect(agent.agentState).toEqual(agentJSON.agent_state);
      expect(agent.buildState).toEqual(agentJSON.build_state);
      expect(agent.freeSpace).toEqual(agentJSON.free_space);
      expect(agent.resources).toEqual(agentJSON.resources);
    });

    it("should return free space in human readable format", () => {
      const agentFreeSpaceInGB = 5.5 * 1024 * 1024 * 1024;
      const agentJSON          = AgentsTestData.agent("45583959-97e2-4b8e-8eab-2f98d73f2e23",
                                                      "1773fcad85d8",
                                                      undefined,
                                                      undefined,
                                                      agentFreeSpaceInGB);

      const agent = Agent.fromJSON(agentJSON);

      expect(agent.readableFreeSpace()).toEqual("5.5 GB");

    });
  });

  describe("Sort", () => {

    it("should get the sortable column index", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());

      expect(agents.getSortableColumns()).toEqual([0, 1, 2, 3, 4, 5, 6, 7]);
    });

    it("should sort the data based on the column clicked", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());
      const agentA = agents.list()[0];
      const agentB = agents.list()[1];
      const agentC = agents.list()[2];

      expect(agents.list()).toEqual([agentA, agentB, agentC]);

      agents.onColumnClick(1);

      expect(agents.list()).toEqual([agentB, agentC, agentA]);
    });

    it("should sort the data in reverser order when the same column is clicked", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());
      const agentA = agents.list()[0];
      const agentB = agents.list()[1];
      const agentC = agents.list()[2];

      expect(agents.list()).toEqual([agentA, agentB, agentC]);

      agents.onColumnClick(1);
      const agentsToRepresentAfterClick = agents.list();
      const expectedAfterClick          = [agentB, agentC, agentA];
      expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);

      agents.onColumnClick(1);
      const agentsToRepresentAfterClickOnSameColumn = agents.list();
      const expectedAfterClickOnSameColumn          = [agentA, agentC, agentB];
      expect(agentsToRepresentAfterClickOnSameColumn).toEqual(expectedAfterClickOnSameColumn);
    });

    it("should always sort a new column in ascending order regardless of previous column's sort order", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());
      const agentA = agents.list()[0];
      const agentB = agents.list()[1];
      const agentC = agents.list()[2];

      expect(agents.list()).toEqual([agentA, agentB, agentC]);

      agents.onColumnClick(1);
      const agentsToRepresentAfterClick = agents.list();
      const expectedAfterClick          = [agentB, agentC, agentA];
      expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);

      agents.onColumnClick(0);
      const agentsToRepresentAfterClickOnAnotherColumn = agents.list();
      const expectedAfterClickOnAnotherColumn          = [agentA, agentB, agentC];
      expect(agentsToRepresentAfterClickOnAnotherColumn).toEqual(expectedAfterClickOnAnotherColumn);
    });
  });

  describe("Search", () => {
    let agents: Agents, agentA: Agent, agentB: Agent, agentC: Agent;
    beforeEach(() => {
      agents = Agents.fromJSON(AgentsTestData.list());
      agentA = agents.list()[0];
      agentB = agents.list()[1];
      agentC = agents.list()[2];
    });

    it("should search agents based on hostname", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText("windows");

      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agentA]);
    });

    it("should search agents based on sandbox", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText("Xx");

      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agentB]);
    });

    it("should search agents based on os", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText("mac");

      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agentC]);
    });

    it("should search agents based on ip address", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText(".5");

      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agentA]);
    });

    it("should search agents based on free space", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText("2859");

      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agentB]);
    });

    it("should search agents based on resources", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText("fire");

      expect(agents.list().length).toEqual(3);
      expect(agents.list()).toEqual([agentA, agentB, agentC]);
    });

    it("should search agents case-insensitively", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText("WinDow");

      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agentA]);
    });
  });
});
