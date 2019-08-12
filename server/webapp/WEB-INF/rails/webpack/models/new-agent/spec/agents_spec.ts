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

import {Agent, AgentConfigState, Agents, AgentsEnvironment, AgentState, BuildState} from "models/new-agent/agents";
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
      expect(agent.agentConfigState).toEqual(AgentConfigState.Enabled);
      expect(agent.agentState).toEqual(AgentState.Idle);
      expect(agent.buildState).toEqual(BuildState.Idle);
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

    it("should return environments", function () {
      const agentJSON = AgentsTestData.agentWithEnvironments("prod", "dev", "qa");

      const agent = Agent.fromJSON(agentJSON);

      expect(agent.environments).toHaveLength(3);
      expect(agent.environments[0]).toEqual(new AgentsEnvironment("prod", "gocd"));
      expect(agent.environments[1]).toEqual(new AgentsEnvironment("dev", "gocd"));
      expect(agent.environments[2]).toEqual(new AgentsEnvironment("qa", "gocd"));
    });

    it("should return environments name", function () {
      const agentJSON = AgentsTestData.agentWithEnvironments("prod", "dev", "qa");

      const agent = Agent.fromJSON(agentJSON);

      expect(agent.environmentNames()).toHaveLength(3);
      expect(agent.environmentNames()[0]).toEqual("prod");
      expect(agent.environmentNames()[1]).toEqual("dev");
      expect(agent.environmentNames()[2]).toEqual("qa");
    });

    it("should return empty array when agent is not associated with any environments", function () {
      const agentJSON = AgentsTestData.agentWithEnvironments();

      const agent = Agent.fromJSON(agentJSON);

      expect(agent.environmentNames()).toHaveLength(0);
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

  describe("Agent Status", () => {
    it("should show as pending when agent is in pending state", () => {
      const agentInPendingState = Agent.fromJSON(AgentsTestData.pendingAgent());

      const agentStatus = agentInPendingState.status();

      expect(agentStatus).toEqual("Pending");
    });

    describe("Enabled", () => {
      it("should show as Building (Cancelled) when agent is enabled and job is cancelled", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.buildingCancelledAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Building (Cancelled)");
      });

      it("should show as Building when agent is enabled and building a job", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.buildingAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Building");
      });

      it("should show as Idle when agent is idle", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.idleAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Idle");
      });

      it("should show as Missing when agent is in missing state", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.missingAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Missing");
      });

      it("should show as LostContact when agent is in lost contact state", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.lostContactAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("LostContact");
      });
    });

    describe("Disabled", () => {
      it("should show as Disabled(Building) when agent is disabled and job is running", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.disabledBuildingAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Disabled (Building)");
      });

      it("should show as Disabled(Cancelled) when agent is disabled and job is cancelled", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.disabledCancelledAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Disabled (Cancelled)");
      });

      it("should show as Disabled when agent is disabled and job is not running", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.disabledAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Disabled");
      });
    });
  });
});
