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

import {SortOrder} from "models/new_agent/agent_comparator";
import {Agent, AgentConfigState, Agents} from "models/new_agent/agents";
import {AgentsVM} from "models/new_agent/agents_vm";
import {AgentsTestData} from "models/new_agent/spec/agents_test_data";

describe("AgentVM", () => {
  let idleAgent: Agent, buildingAgent: Agent, disabledAgent: Agent, agents: Agents;
  beforeEach(() => {
    idleAgent     = Agent.fromJSON(AgentsTestData.idleAgent());
    buildingAgent = Agent.fromJSON(AgentsTestData.buildingAgent());
    disabledAgent = Agent.fromJSON(AgentsTestData.disabledAgent());
    agents        = new Agents(idleAgent, buildingAgent, disabledAgent);
  });

  describe("Sync", () => {
    it("should sync agents", () => {
      const agentsVM = new AgentsVM();
      expect(agentsVM.totalCount()).toBe(0);

      agentsVM.sync(agents);

      expect(agentsVM.totalCount()).toBe(3);
    });

    it("should remove agent uuid from selected agents list when agent is removed from server", () => {
      const agentOne   = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo   = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentThree = Agent.fromJSON(AgentsTestData.buildingCancelledAgent());
      const agentsVM   = new AgentsVM(new Agents(agentOne, agentTwo, agentThree));

      agentsVM.selectAgent(agentOne.uuid);
      agentsVM.selectAgent(agentTwo.uuid);
      expect(agentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid, agentTwo.uuid]);

      agentsVM.sync(new Agents(agentTwo, agentThree));

      expect(agentsVM.selectedAgentsUUID()).toEqual([agentTwo.uuid]);
    });

    it("should not modify selection when server returns an addition agents", () => {
      const agentOne   = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo   = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentThree = Agent.fromJSON(AgentsTestData.buildingCancelledAgent());
      const agentsVM   = new AgentsVM(new Agents(agentOne, agentTwo));

      agentsVM.selectAgent(agentOne.uuid);
      agentsVM.selectAgent(agentTwo.uuid);
      expect(agentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid, agentTwo.uuid]);

      agentsVM.sync(new Agents(agentOne, agentTwo, agentThree));

      expect(agentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid, agentTwo.uuid]);
    });
  });

  describe("FilterText", () => {
    it("should return all the agents when filter text is not specified", () => {
      const productionAgent = Agent.fromJSON(AgentsTestData.withHostname("Hostname-A")),
            buildAgent      = Agent.fromJSON(AgentsTestData.withHostname("Hostname-B"));
      const agentsVM        = new AgentsVM(new Agents(productionAgent, buildAgent));

      expect(agentsVM.filterText()).toBeUndefined();
      expect(agentsVM.list()).toHaveLength(2);
    });

    it("should return all the agents when filter text is empty", () => {
      const productionAgent = Agent.fromJSON(AgentsTestData.withHostname("Hostname-A")),
            buildAgent      = Agent.fromJSON(AgentsTestData.withHostname("Hostname-B"));
      const agentsVM        = new AgentsVM(new Agents(productionAgent, buildAgent));

      agentsVM.filterText("");

      expect(agentsVM.filterText()).toBe("");
      expect(agentsVM.list()).toHaveLength(2);
    });

    it("should filter by hostname", () => {
      const productionAgent = Agent.fromJSON(AgentsTestData.withHostname("Hostname-ABC")),
            buildAgent      = Agent.fromJSON(AgentsTestData.withHostname("Hostname-XYZ"));
      const agentsVM        = new AgentsVM(new Agents(productionAgent, buildAgent));
      expect(agentsVM.list()).toHaveLength(2);

      agentsVM.filterText("-AbC");

      expect(agentsVM.list()).toHaveLength(1);
      expect(agentsVM.list()).toContain(productionAgent);
    });

    it("should filter by os", () => {
      const productionAgent = Agent.fromJSON(AgentsTestData.withOs("Windows")),
            buildAgent      = Agent.fromJSON(AgentsTestData.withOs("Mac"));
      const agentsVM        = new AgentsVM(new Agents(productionAgent, buildAgent));
      expect(agentsVM.list()).toHaveLength(2);

      agentsVM.filterText("WiNdOwS");

      expect(agentsVM.list()).toHaveLength(1);
      expect(agentsVM.list()).toContain(productionAgent);
    });

    it("should filter by sandbox", () => {
      const productionAgent = Agent.fromJSON(AgentsTestData.withSandbox("c://go/agent")),
            buildAgent      = Agent.fromJSON(AgentsTestData.withSandbox("/var/lib/go/agent"));
      const agentsVM        = new AgentsVM(new Agents(productionAgent, buildAgent));
      expect(agentsVM.list()).toHaveLength(2);

      agentsVM.filterText("/var/li");

      expect(agentsVM.list()).toHaveLength(1);
      expect(agentsVM.list()).toContain(buildAgent);
    });

    it("should filter by ip", () => {
      const agentOne   = Agent.fromJSON(AgentsTestData.withIP("10.1.0.5")),
            agentTwo   = Agent.fromJSON(AgentsTestData.withIP("10.1.0.6")),
            agentThree = Agent.fromJSON(AgentsTestData.withIP("172.1.0.5"));
      const agentsVM   = new AgentsVM(new Agents(agentOne, agentTwo, agentThree));
      expect(agentsVM.list()).toHaveLength(3);

      agentsVM.filterText("0.5");

      expect(agentsVM.list()).toHaveLength(2);
      expect(agentsVM.list()).toContain(agentOne);
      expect(agentsVM.list()).toContain(agentThree);
    });

    it("should filter by freespace", () => {
      const agentOne   = Agent.fromJSON(AgentsTestData.withFreespace(1024)),
            agentTwo   = Agent.fromJSON(AgentsTestData.withFreespace(2048)),
            agentThree = Agent.fromJSON(AgentsTestData.withFreespace(10240));
      const agentsVM   = new AgentsVM(new Agents(agentOne, agentTwo, agentThree));
      expect(agentsVM.list()).toHaveLength(3);

      agentsVM.filterText("024");

      expect(agentsVM.list()).toHaveLength(2);
      expect(agentsVM.list()).toContain(agentOne);
      expect(agentsVM.list()).toContain(agentThree);
    });

    it("should filter by status", () => {
      const agentOne   = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo   = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentThree = Agent.fromJSON(AgentsTestData.buildingCancelledAgent());
      const agentsVM   = new AgentsVM(new Agents(agentOne, agentTwo, agentThree));
      expect(agentsVM.list()).toHaveLength(3);

      agentsVM.filterText("building");

      expect(agentsVM.list()).toHaveLength(2);
      expect(agentsVM.list()).toContain(agentTwo);
      expect(agentsVM.list()).toContain(agentThree);
    });

    it("should filter by resources", () => {
      const agentOne = Agent.fromJSON(AgentsTestData.withResources("Chrome", "Firefox")),
            agentTwo = Agent.fromJSON(AgentsTestData.withResources("Chrome", "Safari"));
      const agentsVM = new AgentsVM(new Agents(agentOne, agentTwo));
      expect(agentsVM.list()).toHaveLength(2);

      agentsVM.filterText("FiReFox");

      expect(agentsVM.list()).toHaveLength(1);
      expect(agentsVM.list()).toContain(agentOne);
    });

    it("should filter by environments", () => {
      const agentOne = Agent.fromJSON(AgentsTestData.withEnvironments("Production")),
            agentTwo = Agent.fromJSON(AgentsTestData.withEnvironments("Test", "QA"));
      const agentsVM = new AgentsVM(new Agents(agentOne, agentTwo));
      expect(agentsVM.list()).toHaveLength(2);

      agentsVM.filterText("PrOd");

      expect(agentsVM.list()).toHaveLength(1);
      expect(agentsVM.list()).toContain(agentOne);
    });

    it("should filter skip agent if property is not defined", () => {
      const agentOneJson = AgentsTestData.withHostname("Hostname-AAA");
      delete agentOneJson.hostname;

      const agentOne = Agent.fromJSON(agentOneJson),
            agentTwo = Agent.fromJSON(AgentsTestData.withHostname("Hostname-BBB"));
      const agentsVM = new AgentsVM(new Agents(agentOne, agentTwo));
      expect(agentsVM.list()).toHaveLength(2);

      agentsVM.filterText("Hostname-");

      expect(agentsVM.list()).toHaveLength(1);
      expect(agentsVM.list()).toContain(agentTwo);
    });
  });

  describe("StaticAgentSortHandler", () => {
    it("should initialize static agent sort handler", () => {
      const agentVM = new AgentsVM(new Agents());

      expect(agentVM.staticAgentSortHandler()).not.toBeNull();
      expect(agentVM.staticAgentSortHandler().getSortableColumns()).toEqual([1, 2, 3, 4, 5, 6, 7, 8]);
    });

    it("should update column index and sort order on click of a column", () => {
      const agentVM = new AgentsVM(new Agents());

      expect(agentVM.staticAgentSortHandler().currentSortedColumnIndex()).toBe(-1);
      expect(agentVM.staticAgentSortHandler().currentSortOrder()).toBeNull();

      agentVM.staticAgentSortHandler().onColumnClick(3);

      expect(agentVM.staticAgentSortHandler().currentSortedColumnIndex()).toBe(3);
      expect(agentVM.staticAgentSortHandler().currentSortOrder()).toBe(SortOrder.ASC);
    });

    it("should change the sort order if column clicked twice", () => {
      const agentVM = new AgentsVM(new Agents());

      agentVM.staticAgentSortHandler().onColumnClick(3);
      expect(agentVM.staticAgentSortHandler().currentSortOrder()).toBe(SortOrder.ASC);

      agentVM.staticAgentSortHandler().onColumnClick(3);
      expect(agentVM.staticAgentSortHandler().currentSortOrder()).toBe(SortOrder.DESC);
    });
  });

  describe("ElasticAgentSortHandler", () => {
    it("should initialize elastic agent sort handler", () => {
      const agentVM = new AgentsVM(new Agents());

      expect(agentVM.elasticAgentSortHandler()).not.toBeNull();
      expect(agentVM.elasticAgentSortHandler().getSortableColumns()).toEqual([0, 1, 2, 3, 4, 5, 6]);
    });

    it("should update column index and sort order on click of the column on elastic agents", () => {
      const agentVM = new AgentsVM(new Agents());

      expect(agentVM.elasticAgentSortHandler().currentSortedColumnIndex()).toBe(-1);
      expect(agentVM.elasticAgentSortHandler().currentSortOrder()).toBeNull();

      agentVM.elasticAgentSortHandler().onColumnClick(3);

      expect(agentVM.elasticAgentSortHandler().currentSortedColumnIndex()).toBe(3);
      expect(agentVM.elasticAgentSortHandler().currentSortOrder()).toBe(SortOrder.ASC);
    });

    it("should change the sort order if column clicked twice", () => {
      const agentVM = new AgentsVM(new Agents());

      agentVM.elasticAgentSortHandler().onColumnClick(3);
      expect(agentVM.elasticAgentSortHandler().currentSortOrder()).toBe(SortOrder.ASC);

      agentVM.elasticAgentSortHandler().onColumnClick(3);
      expect(agentVM.elasticAgentSortHandler().currentSortOrder()).toBe(SortOrder.DESC);
    });
  });

  describe("selectAgent", () => {
    it("should select agent with uuid", () => {
      const agentOne = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo = Agent.fromJSON(AgentsTestData.buildingAgent());
      const agentsVM = new AgentsVM(new Agents(agentOne, agentTwo));

      expect(agentsVM.selectedAgentsUUID()).toHaveLength(0);

      agentsVM.selectAgent(agentOne.uuid);

      expect(agentsVM.selectedAgentsUUID()).toHaveLength(1);
      expect(agentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid]);
    });

    it("should not add agent to selection list if it is already added", () => {
      const agentOne = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo = Agent.fromJSON(AgentsTestData.buildingAgent());
      const agentsVM = new AgentsVM(new Agents(agentOne, agentTwo));

      agentsVM.selectAgent(agentOne.uuid);
      expect(agentsVM.selectedAgentsUUID()).toHaveLength(1);
      expect(agentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid]);

      agentsVM.selectAgent(agentOne.uuid);
      expect(agentsVM.selectedAgentsUUID()).toHaveLength(1);
      expect(agentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid]);
    });
  });

  describe("unselectAll", () => {
    it("should clear selection", () => {
      const agentOne = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo = Agent.fromJSON(AgentsTestData.buildingAgent());
      const agentsVM = new AgentsVM(new Agents(agentOne, agentTwo));

      agentsVM.selectAgent(agentOne.uuid);
      expect(agentsVM.selectedAgentsUUID()).toHaveLength(1);
      expect(agentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid]);

      agentsVM.unselectAll();

      expect(agentsVM.selectedAgentsUUID()).toHaveLength(0);
      expect(agentsVM.selectedAgentsUUID()).toEqual([]);
    });
  });

  describe("isAllStaticAgentSelected", () => {
    it("should be true if all agents are selected", () => {
      const agentOne = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo = Agent.fromJSON(AgentsTestData.buildingAgent());

      const agentsVM = new AgentsVM(new Agents(agentOne, agentTwo));
      agentsVM.selectAgent(agentOne.uuid);
      agentsVM.selectAgent(agentTwo.uuid);

      expect(agentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid, agentTwo.uuid]);
      expect(agentsVM.isAllStaticAgentSelected()).toBeTruthy();
    });

    it("should be false when at least one agent is not selected", () => {
      const agentOne = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo = Agent.fromJSON(AgentsTestData.buildingAgent());

      const agentsVM = new AgentsVM(new Agents(agentOne, agentTwo));
      agentsVM.selectAgent(agentTwo.uuid);

      expect(agentsVM.selectedAgentsUUID()).toEqual([agentTwo.uuid]);
      expect(agentsVM.isAllStaticAgentSelected()).toBeFalsy();
    });
  });

  describe("toggleAgentsSelection", () => {
    it("should unselect all agents when all are selected", () => {
      const agentOne = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo = Agent.fromJSON(AgentsTestData.buildingAgent());
      const agentsVM = new AgentsVM(new Agents(agentOne, agentTwo));

      agentsVM.selectAgent(agentOne.uuid);
      agentsVM.selectAgent(agentTwo.uuid);
      expect(agentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid, agentTwo.uuid]);

      agentsVM.toggleAgentsSelection();

      expect(agentsVM.selectedAgentsUUID()).toEqual([]);
    });

    it("should select all agents when all agents are unselected", () => {
      const agentOne = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo = Agent.fromJSON(AgentsTestData.buildingAgent());
      const agentsVM = new AgentsVM(new Agents(agentOne, agentTwo));

      expect(agentsVM.selectedAgentsUUID()).toHaveLength(0);

      agentsVM.toggleAgentsSelection();

      expect(agentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid, agentTwo.uuid]);
    });

    it("should select all agents when some agents are selected", () => {
      const agentOne = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo = Agent.fromJSON(AgentsTestData.buildingAgent());
      const agentsVM = new AgentsVM(new Agents(agentOne, agentTwo));
      agentsVM.selectAgent(agentOne.uuid);

      expect(agentsVM.selectedAgentsUUID()).toHaveLength(1);

      agentsVM.toggleAgentsSelection();

      expect(agentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid, agentTwo.uuid]);
    });
  });

  describe("FilterByAgentConfigState", () => {
    it("should return all agents by agent config state", () => {
      const agentA   = Agent.fromJSON(AgentsTestData.disabledAgent()),
            agentB   = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentC   = Agent.fromJSON(AgentsTestData.pendingAgent()),
            agentD   = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentE   = Agent.fromJSON(AgentsTestData.pendingAgent());
      const agentsVM = new AgentsVM(new Agents(agentA, agentB, agentC, agentD, agentE));

      let filteredAgents = agentsVM.filterBy(AgentConfigState.Pending);
      expect(filteredAgents).toHaveLength(2);
      expect(filteredAgents).toEqual([agentC, agentE]);

      filteredAgents = agentsVM.filterBy(AgentConfigState.Enabled);
      expect(filteredAgents).toHaveLength(2);
      expect(filteredAgents).toEqual([agentB, agentD]);

      filteredAgents = agentsVM.filterBy(AgentConfigState.Disabled);
      expect(filteredAgents).toHaveLength(1);
      expect(filteredAgents).toContain(agentA);
    });

    it("should return agents by agent config state after applying search filter", () => {
      const agentA   = Agent.fromJSON(AgentsTestData.disabledAgent()),
            agentB   = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentC   = Agent.fromJSON(AgentsTestData.pendingAgent()),
            agentD   = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentE   = Agent.fromJSON(AgentsTestData.pendingAgent());
      const agentsVM = new AgentsVM(new Agents(agentA, agentB, agentC, agentD, agentE));

      agentsVM.filterText("Building");

      let filteredAgents = agentsVM.filterBy(AgentConfigState.Pending);
      expect(filteredAgents).toHaveLength(0);

      filteredAgents = agentsVM.filterBy(AgentConfigState.Enabled);
      expect(filteredAgents).toHaveLength(1);
      expect(filteredAgents).toContain(agentB);

      filteredAgents = agentsVM.filterBy(AgentConfigState.Disabled);
      expect(filteredAgents).toHaveLength(0);
    });
  });
});
