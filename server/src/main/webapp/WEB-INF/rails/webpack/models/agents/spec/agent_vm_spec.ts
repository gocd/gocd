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

import {Agent, AgentConfigState, Agents} from "models/agents/agents";
import {ElasticAgentVM, StaticAgentsVM} from "models/agents/agents_vm";
import {AgentsTestData} from "models/agents/spec/agents_test_data";
import {SortOrder} from "views/components/table";

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
      const staticAgentsVM = new StaticAgentsVM();
      expect(staticAgentsVM.totalCount()).toBe(0);

      staticAgentsVM.sync(agents);

      expect(staticAgentsVM.totalCount()).toBe(3);
    });

    it("should remove agent uuid from selected agents list when agent is removed from server", () => {
      const agentOne       = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo       = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentThree     = Agent.fromJSON(AgentsTestData.buildingCancelledAgent());
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo, agentThree));

      staticAgentsVM.selectAgent(agentOne.uuid);
      staticAgentsVM.selectAgent(agentTwo.uuid);
      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid, agentTwo.uuid]);

      staticAgentsVM.sync(new Agents(agentTwo, agentThree));

      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([agentTwo.uuid]);
    });

    it("should not modify selection when server returns an addition agents", () => {
      const agentOne       = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo       = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentThree     = Agent.fromJSON(AgentsTestData.buildingCancelledAgent());
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo));

      staticAgentsVM.selectAgent(agentOne.uuid);
      staticAgentsVM.selectAgent(agentTwo.uuid);
      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid, agentTwo.uuid]);

      staticAgentsVM.sync(new Agents(agentOne, agentTwo, agentThree));

      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([agentTwo.uuid, agentOne.uuid]);
    });
  });

  describe("FilterText", () => {
    it("should return all the agents when filter text is not specified", () => {
      const productionAgent = Agent.fromJSON(AgentsTestData.withHostname("Hostname-A")),
            buildAgent      = Agent.fromJSON(AgentsTestData.withHostname("Hostname-B"));
      const staticAgentsVM  = new StaticAgentsVM(new Agents(productionAgent, buildAgent));

      expect(staticAgentsVM.filterText()).toBeUndefined();
      expect(staticAgentsVM.list()).toHaveLength(2);
    });

    it("should return all the agents when filter text is empty", () => {
      const productionAgent = Agent.fromJSON(AgentsTestData.withHostname("Hostname-A")),
            buildAgent      = Agent.fromJSON(AgentsTestData.withHostname("Hostname-B"));
      const staticAgentsVM  = new StaticAgentsVM(new Agents(productionAgent, buildAgent));

      staticAgentsVM.filterText("");

      expect(staticAgentsVM.filterText()).toBe("");
      expect(staticAgentsVM.list()).toHaveLength(2);
    });

    it("should filter by hostname", () => {
      const productionAgent = Agent.fromJSON(AgentsTestData.withHostname("Hostname-ABC")),
            buildAgent      = Agent.fromJSON(AgentsTestData.withHostname("Hostname-XYZ"));
      const staticAgentsVM  = new StaticAgentsVM(new Agents(productionAgent, buildAgent));
      expect(staticAgentsVM.list()).toHaveLength(2);

      staticAgentsVM.filterText("-AbC");

      expect(staticAgentsVM.list()).toHaveLength(1);
      expect(staticAgentsVM.list()).toContain(productionAgent);
    });

    it("should filter by os", () => {
      const productionAgent = Agent.fromJSON(AgentsTestData.withOs("Windows")),
            buildAgent      = Agent.fromJSON(AgentsTestData.withOs("Mac"));
      const staticAgentsVM  = new StaticAgentsVM(new Agents(productionAgent, buildAgent));
      expect(staticAgentsVM.list()).toHaveLength(2);

      staticAgentsVM.filterText("WiNdOwS");

      expect(staticAgentsVM.list()).toHaveLength(1);
      expect(staticAgentsVM.list()).toContain(productionAgent);
    });

    it("should filter by sandbox", () => {
      const productionAgent = Agent.fromJSON(AgentsTestData.withSandbox("c://go/agent")),
            buildAgent      = Agent.fromJSON(AgentsTestData.withSandbox("/var/lib/go/agent"));
      const staticAgentsVM  = new StaticAgentsVM(new Agents(productionAgent, buildAgent));
      expect(staticAgentsVM.list()).toHaveLength(2);

      staticAgentsVM.filterText("/var/li");

      expect(staticAgentsVM.list()).toHaveLength(1);
      expect(staticAgentsVM.list()).toContain(buildAgent);
    });

    it("should filter by ip", () => {
      const agentOne       = Agent.fromJSON(AgentsTestData.withIP("10.1.0.5")),
            agentTwo       = Agent.fromJSON(AgentsTestData.withIP("10.1.0.6")),
            agentThree     = Agent.fromJSON(AgentsTestData.withIP("172.1.0.5"));
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo, agentThree));
      expect(staticAgentsVM.list()).toHaveLength(3);

      staticAgentsVM.filterText("0.5");

      expect(staticAgentsVM.list()).toHaveLength(2);
      expect(staticAgentsVM.list()).toContain(agentOne);
      expect(staticAgentsVM.list()).toContain(agentThree);
    });

    it("should filter by freespace", () => {
      const agentOne       = Agent.fromJSON(AgentsTestData.withFreespace(1024)),
            agentTwo       = Agent.fromJSON(AgentsTestData.withFreespace(2048)),
            agentThree     = Agent.fromJSON(AgentsTestData.withFreespace(10240));
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo, agentThree));
      expect(staticAgentsVM.list()).toHaveLength(3);

      staticAgentsVM.filterText("024");

      expect(staticAgentsVM.list()).toHaveLength(2);
      expect(staticAgentsVM.list()).toContain(agentOne);
      expect(staticAgentsVM.list()).toContain(agentThree);
    });

    it("should filter by status", () => {
      const agentOne       = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo       = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentThree     = Agent.fromJSON(AgentsTestData.buildingCancelledAgent());
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo, agentThree));
      expect(staticAgentsVM.list()).toHaveLength(3);

      staticAgentsVM.filterText("building");

      expect(staticAgentsVM.list()).toHaveLength(2);
      expect(staticAgentsVM.list()).toContain(agentTwo);
      expect(staticAgentsVM.list()).toContain(agentThree);
    });

    it("should filter by resources", () => {
      const agentOne       = Agent.fromJSON(AgentsTestData.withResources("Chrome", "Firefox")),
            agentTwo       = Agent.fromJSON(AgentsTestData.withResources("Chrome", "Safari"));
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo));
      expect(staticAgentsVM.list()).toHaveLength(2);

      staticAgentsVM.filterText("FiReFox");

      expect(staticAgentsVM.list()).toHaveLength(1);
      expect(staticAgentsVM.list()).toContain(agentOne);
    });

    it("should filter by environments", () => {
      const agentOne       = Agent.fromJSON(AgentsTestData.withEnvironments("Production")),
            agentTwo       = Agent.fromJSON(AgentsTestData.withEnvironments("Test", "QA"));
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo));
      expect(staticAgentsVM.list()).toHaveLength(2);

      staticAgentsVM.filterText("PrOd");

      expect(staticAgentsVM.list()).toHaveLength(1);
      expect(staticAgentsVM.list()).toContain(agentOne);
    });

    it("should filter skip agent if property is not defined", () => {
      const agentOneJson = AgentsTestData.withHostname("Hostname-AAA");
      // @ts-ignore
      delete agentOneJson.hostname;

      const agentOne       = Agent.fromJSON(agentOneJson),
            agentTwo       = Agent.fromJSON(AgentsTestData.withHostname("Hostname-BBB"));
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo));
      expect(staticAgentsVM.list()).toHaveLength(2);

      staticAgentsVM.filterText("Hostname-");

      expect(staticAgentsVM.list()).toHaveLength(1);
      expect(staticAgentsVM.list()).toContain(agentTwo);
    });
  });

  describe("StaticAgentSortHandler", () => {
    it("should initialize static agent sort handler", () => {
      const staticAgentVM = new StaticAgentsVM(new Agents());

      expect(staticAgentVM.agentsSortHandler).not.toBeNull();
      expect(staticAgentVM.agentsSortHandler.getSortableColumns()).toEqual([1, 2, 3, 4, 5, 6, 7, 8]);
    });

    it("should update column index on click of a column", () => {
      const staticAgentVM = new StaticAgentsVM(new Agents());

      expect(staticAgentVM.agentsSortHandler.currentSortedColumnIndex()).toBe(5);
      expect(staticAgentVM.agentsSortHandler.getCurrentSortOrder()).toBe(SortOrder.ASC);

      staticAgentVM.agentsSortHandler.onColumnClick(3);

      expect(staticAgentVM.agentsSortHandler.currentSortedColumnIndex()).toBe(3);
      expect(staticAgentVM.agentsSortHandler.getCurrentSortOrder()).toBe(SortOrder.ASC);
    });

    it("should change the sort order if column clicked twice", () => {
      const staticAgentVM = new StaticAgentsVM(new Agents());

      staticAgentVM.agentsSortHandler.onColumnClick(3);
      expect(staticAgentVM.agentsSortHandler.getCurrentSortOrder()).toBe(SortOrder.ASC);

      staticAgentVM.agentsSortHandler.onColumnClick(3);
      expect(staticAgentVM.agentsSortHandler.getCurrentSortOrder()).toBe(SortOrder.DESC);
    });

    it("should reverse the result if sort order is desc", () => {
      const productionAgent = Agent.fromJSON(AgentsTestData.withHostname("Hostname-A")),
            buildAgent      = Agent.fromJSON(AgentsTestData.withHostname("Hostname-B"));
      const staticAgentsVM  = new StaticAgentsVM(new Agents(productionAgent, buildAgent));

      staticAgentsVM.agentsSortHandler.onColumnClick(1);
      staticAgentsVM.agentsSortHandler.onColumnClick(1);

      expect(staticAgentsVM.agentsSortHandler.getCurrentSortOrder()).toBe(SortOrder.DESC);
      expect(staticAgentsVM.list()).toEqual([buildAgent, productionAgent]);
    });
  });

  describe("ElasticAgentSortHandler", () => {
    it("should initialize elastic agent sort handler", () => {
      const elasticAgentVM = new ElasticAgentVM(new Agents());

      expect(elasticAgentVM.agentsSortHandler).not.toBeNull();
      expect(elasticAgentVM.agentsSortHandler.getSortableColumns()).toEqual([1, 2, 3, 4, 5, 6, 7]);
    });

    it("should update column index on click of the column on elastic agents", () => {
      const elasticAgentVM = new ElasticAgentVM(new Agents());

      expect(elasticAgentVM.agentsSortHandler.currentSortedColumnIndex()).toBe(5);
      expect(elasticAgentVM.agentsSortHandler.getCurrentSortOrder()).toBe(SortOrder.ASC);

      elasticAgentVM.agentsSortHandler.onColumnClick(3);

      expect(elasticAgentVM.agentsSortHandler.currentSortedColumnIndex()).toBe(3);
      expect(elasticAgentVM.agentsSortHandler.getCurrentSortOrder()).toBe(SortOrder.ASC);
    });

    it("should change the sort order if column clicked twice", () => {
      const elasticAgentVM = new ElasticAgentVM(new Agents());

      elasticAgentVM.agentsSortHandler.onColumnClick(3);
      expect(elasticAgentVM.agentsSortHandler.getCurrentSortOrder()).toBe(SortOrder.ASC);

      elasticAgentVM.agentsSortHandler.onColumnClick(3);
      expect(elasticAgentVM.agentsSortHandler.getCurrentSortOrder()).toBe(SortOrder.DESC);
    });

    it("should reverse the result if sort order is desc", () => {
      const productionAgent = Agent.fromJSON(AgentsTestData.withHostname("Hostname-A")),
            buildAgent      = Agent.fromJSON(AgentsTestData.withHostname("Hostname-B"));
      const elasticAgentVM  = new ElasticAgentVM(new Agents(productionAgent, buildAgent));

      elasticAgentVM.agentsSortHandler.onColumnClick(1);
      elasticAgentVM.agentsSortHandler.onColumnClick(1);

      expect(elasticAgentVM.agentsSortHandler.getCurrentSortOrder()).toBe(SortOrder.DESC);
      expect(elasticAgentVM.all()).toEqual([buildAgent, productionAgent]);
    });
  });

  describe("selectAgent", () => {
    it("should select agent with uuid", () => {
      const agentOne       = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo       = Agent.fromJSON(AgentsTestData.buildingAgent());
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo));

      expect(staticAgentsVM.selectedAgentsUUID()).toHaveLength(0);

      staticAgentsVM.selectAgent(agentOne.uuid);

      expect(staticAgentsVM.selectedAgentsUUID()).toHaveLength(1);
      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid]);
    });

    it("should not add agent to selection list if it is already added", () => {
      const agentOne       = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo       = Agent.fromJSON(AgentsTestData.buildingAgent());
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo));

      staticAgentsVM.selectAgent(agentOne.uuid);
      expect(staticAgentsVM.selectedAgentsUUID()).toHaveLength(1);
      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid]);

      staticAgentsVM.selectAgent(agentOne.uuid);
      expect(staticAgentsVM.selectedAgentsUUID()).toHaveLength(1);
      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid]);
    });
  });

  describe("unselectAll", () => {
    it("should clear selection", () => {
      const agentOne       = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo       = Agent.fromJSON(AgentsTestData.buildingAgent());
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo));

      staticAgentsVM.selectAgent(agentOne.uuid);
      expect(staticAgentsVM.selectedAgentsUUID()).toHaveLength(1);
      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid]);

      staticAgentsVM.unselectAll();

      expect(staticAgentsVM.selectedAgentsUUID()).toHaveLength(0);
      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([]);
    });
  });

  describe("isAllStaticAgentSelected", () => {
    it("should be true if all agents are selected", () => {
      const agentOne = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo = Agent.fromJSON(AgentsTestData.buildingAgent());

      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo));
      staticAgentsVM.selectAgent(agentOne.uuid);
      staticAgentsVM.selectAgent(agentTwo.uuid);

      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid, agentTwo.uuid]);
      expect(staticAgentsVM.isAllStaticAgentSelected()).toBeTruthy();
    });

    it("should be false when at least one agent is not selected", () => {
      const agentOne = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo = Agent.fromJSON(AgentsTestData.buildingAgent());

      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo));
      staticAgentsVM.selectAgent(agentTwo.uuid);

      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([agentTwo.uuid]);
      expect(staticAgentsVM.isAllStaticAgentSelected()).toBeFalsy();
    });
  });

  describe("toggleAgentsSelection", () => {
    it("should unselect all agents when all are selected", () => {
      const agentOne       = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo       = Agent.fromJSON(AgentsTestData.buildingAgent());
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo));

      staticAgentsVM.selectAgent(agentOne.uuid);
      staticAgentsVM.selectAgent(agentTwo.uuid);
      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid, agentTwo.uuid]);

      staticAgentsVM.toggleAgentsSelection();

      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([]);
    });

    it("should select all agents when all agents are unselected", () => {
      const agentOne       = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo       = Agent.fromJSON(AgentsTestData.buildingAgent());
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo));

      expect(staticAgentsVM.selectedAgentsUUID()).toHaveLength(0);

      staticAgentsVM.toggleAgentsSelection();

      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid, agentTwo.uuid]);
    });

    it("should select all agents when some agents are selected", () => {
      const agentOne       = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentTwo       = Agent.fromJSON(AgentsTestData.buildingAgent());
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentOne, agentTwo));
      staticAgentsVM.selectAgent(agentOne.uuid);

      expect(staticAgentsVM.selectedAgentsUUID()).toHaveLength(1);

      staticAgentsVM.toggleAgentsSelection();

      expect(staticAgentsVM.selectedAgentsUUID()).toEqual([agentOne.uuid, agentTwo.uuid]);
    });
  });

  describe("FilterByAgentConfigState", () => {
    it("should return all agents by agent config state", () => {
      const agentA         = Agent.fromJSON(AgentsTestData.disabledAgent()),
            agentB         = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentC         = Agent.fromJSON(AgentsTestData.pendingAgent()),
            agentD         = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentE         = Agent.fromJSON(AgentsTestData.pendingAgent());
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentA, agentB, agentC, agentD, agentE));

      let filteredAgents = staticAgentsVM.filterBy(AgentConfigState.Pending);
      expect(filteredAgents).toHaveLength(2);
      expect(filteredAgents).toEqual([agentC, agentE]);

      filteredAgents = staticAgentsVM.filterBy(AgentConfigState.Enabled);
      expect(filteredAgents).toHaveLength(2);
      expect(filteredAgents).toEqual([agentB, agentD]);

      filteredAgents = staticAgentsVM.filterBy(AgentConfigState.Disabled);
      expect(filteredAgents).toHaveLength(1);
      expect(filteredAgents).toContain(agentA);
    });

    it("should return agents by agent config state after applying search filter", () => {
      const agentA         = Agent.fromJSON(AgentsTestData.disabledAgent()),
            agentB         = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentC         = Agent.fromJSON(AgentsTestData.pendingAgent()),
            agentD         = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentE         = Agent.fromJSON(AgentsTestData.pendingAgent());
      const staticAgentsVM = new StaticAgentsVM(new Agents(agentA, agentB, agentC, agentD, agentE));

      staticAgentsVM.filterText("Building");

      let filteredAgents = staticAgentsVM.filterBy(AgentConfigState.Pending);
      expect(filteredAgents).toHaveLength(0);

      filteredAgents = staticAgentsVM.filterBy(AgentConfigState.Enabled);
      expect(filteredAgents).toHaveLength(1);
      expect(filteredAgents).toContain(agentB);

      filteredAgents = staticAgentsVM.filterBy(AgentConfigState.Disabled);
      expect(filteredAgents).toHaveLength(0);
    });
  });
});
